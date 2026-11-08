"""从保留的透明原图生成逐格裁切坐标和归属边界，不改写图片。

运行依赖：Pillow、NumPy、SciPy。生成后须通过真实组件图册逐格复核。
"""
import json
from collections import defaultdict
from pathlib import Path

import numpy as np
from PIL import Image
from scipy import ndimage

ROOT = Path(__file__).resolve().parents[1]
PROP_ROWS = {
    'props-0.png': [0, 224, 406, 596, 786, 978, 1161],
    'props-1.png': [0, 220, 430, 616, 812, 994, 1161],
    'props-2.png': [0, 202, 392, 580, 777, 970, 1161],
    'props-3.png': [0, 220, 418, 611, 803, 990, 1161],
    'props-4.png': [0, 232, 439, 635, 811, 987, 1161],
    'props-5.png': [0, 216, 411, 609, 797, 986, 1161],
}


def simplify(points, tolerance=0.8):
    """输入折线点和像素容差，返回保留轮廓的简化折线。"""
    if len(points) < 3:
        return points
    start, end = np.array(points[0]), np.array(points[-1])
    delta = end - start
    data = np.array(points)
    distances = np.abs(delta[0] * (data[:, 1] - start[1]) - delta[1] * (data[:, 0] - start[0])) / max(np.linalg.norm(delta), 1)
    index = int(np.argmax(distances))
    if distances[index] <= tolerance:
        return [points[0], points[-1]]
    return simplify(points[:index + 1], tolerance)[:-1] + simplify(points[index:], tolerance)


def outline(mask, origin):
    """输入图格归属掩模及其原图偏移，返回原图坐标系中的 SVG 裁切路径。"""
    edges = defaultdict(list)
    padded = np.pad(mask, 1)
    for direction, adjacent in enumerate([padded[:-2, 1:-1], padded[1:-1, 2:], padded[2:, 1:-1], padded[1:-1, :-2]]):
        ys, xs = np.nonzero(mask & ~adjacent)
        for x, y in zip(xs.tolist(), ys.tolist()):
            corners = [(x, y), (x + 1, y), (x + 1, y + 1), (x, y + 1)]
            edges[corners[direction]].append(corners[(direction + 1) % 4])
    paths = []
    while edges:
        start = next(iter(edges))
        point = start
        ring = [point]
        while True:
            following = edges[point].pop()
            if not edges[point]:
                del edges[point]
            point = following
            ring.append(point)
            if point == start:
                break
        if len(ring) < 5:
            continue
        split = max(range(len(ring) - 1), key=lambda i: (ring[i][0] - start[0]) ** 2 + (ring[i][1] - start[1]) ** 2)
        ring = simplify(ring[:split + 1])[:-1] + simplify(ring[split:])[:-1]
        coordinates = [(int(x + origin[0]), int(y + origin[1])) for x, y in ring]
        paths.append('M' + 'L'.join(f'{x},{y}' for x, y in coordinates) + 'Z')
    return ''.join(paths)


def extract(path, columns, rows, count, animated=False, overrides=()):
    """输入原图、网格及有效格数，返回每格独立取图矩形和邻图排除路径；动画格共用尺寸与原始中心。"""
    rgba = np.array(Image.open(path).convert('RGBA'))
    height, width = rgba.shape[:2]
    labels, _ = ndimage.label(rgba[:, :, 3] > 160)
    counts = np.bincount(labels.ravel())
    objects = ndimage.find_objects(labels)
    component_cells = {}
    for label, box in enumerate(objects, 1):
        if counts[label] < 3:
            continue
        y, x = ndimage.center_of_mass(labels[box] == label)
        x += box[1].start
        y += box[0].start
        row = int(np.searchsorted(PROP_ROWS[path.name], y, side='right')) - 1 if path.name in PROP_ROWS else min(rows - 1, int(y / height * rows))
        cell = row * columns + min(columns - 1, int(x / width * columns))
        if cell < count:
            component_cells[label] = cell
    # 放大逐帧核对后，对跨过名义格线的独立碎片明确指定所属帧。
    for x, y, cell in overrides:
        component_cells[int(labels[y, x])] = cell
    lookup = np.zeros(len(counts), dtype=np.int16)
    for label, cell in component_cells.items():
        lookup[label] = cell + 1
    owner_seeds = lookup[labels]
    _, nearest = ndimage.distance_transform_edt(owner_seeds == 0, return_indices=True)
    owners = owner_seeds[tuple(nearest)]
    bounds = []
    for cell in range(count):
        ys, xs = np.nonzero((owners == cell + 1) & (rgba[:, :, 3] > 32))
        if not len(xs):
            raise ValueError(f'{path.name} 第 {cell} 格没有主体')
        bounds.append((int(xs.min()), int(ys.min()), int(xs.max()) + 1, int(ys.max()) + 1))
    nominal = max(width / columns, height / rows)
    margin = 0
    if animated:
        for cell, (left, top, right, bottom) in enumerate(bounds):
            cx, cy = (cell % columns + .5) * width / columns, (cell // columns + .5) * height / rows
            margin = max(margin, cx - left - nominal / 2, right - cx - nominal / 2, cy - top - nominal / 2, bottom - cy - nominal / 2)
    crops = []
    for cell, (left, top, right, bottom) in enumerate(bounds):
        if animated:
            cx, cy = (cell % columns + .5) * width / columns, (cell // columns + .5) * height / rows
            size = int(np.ceil(nominal + margin * 2 + 8))
        else:
            cx, cy = (left + right) / 2, (top + bottom) / 2
            size = int(np.ceil(max(nominal, right - left + 8, bottom - top + 8)))
        x, y = int(np.floor(cx - size / 2)), int(np.floor(cy - size / 2))
        x0, y0, x1, y1 = max(0, x), max(0, y), min(width, x + size), min(height, y + size)
        region = owners[y0:y1, x0:x1] == cell + 1
        crops.append({'box': [x, y, size, size], 'path': outline(region, (x0, y0))})
    return {'width': width, 'height': height, 'cells': crops}


def main():
    """无入参；读取本机原图和既有来源记录，输出正式使用的逐格元数据。"""
    manifest = json.loads((ROOT / 'src/web/public/images/full-card-spells/manifest.json').read_text())
    result = {}
    for atlas in manifest['atlases']:
        result[atlas['file']] = extract(ROOT / 'src/web/public/images/full-card-spells' / atlas['file'], atlas['columns'], atlas['rows'], len(atlas['cards']))
    for name in ['firefly-parts.png', 'spell-atlas.png']:
        folder = 'ur-spell-preview' if name == 'spell-atlas.png' else 'full-card-spells'
        path = ROOT / 'src/web/public/images' / folder / name
        width, height = Image.open(path).size
        if name == 'spell-atlas.png':
            result[name] = {'width': width, 'height': height, 'cells': [{'box': [i % 2 * width / 2, i // 2 * height / 2, width / 2, height / 2]} for i in range(4)]}
        else:
            result[name] = extract(path, 2, 2, 4)
    effects = json.loads((ROOT / 'generated-assets/board-effects-2026-10-18/manifest.json').read_text())
    for effect in effects['entries']:
        if effect['skinId'] not in ['board-effect-meteor', 'board-effect-treasure']:
            continue
        overrides = [(1001, 930, 15)] if effect['skinId'] == 'board-effect-meteor' else []
        result[effect['skinId']] = extract(Path(effect['sourcePath']), 4, 4, 16, animated=True, overrides=overrides)
    destination = ROOT / 'src/web/effect-atlas-crops.json'
    lines = []
    for name, atlas in result.items():
        cells = ',\n'.join('      ' + json.dumps(cell, separators=(',', ':')) for cell in atlas['cells'])
        lines.append(f'  "{name}": {{"width":{atlas["width"]},"height":{atlas["height"]},"cells":[\n{cells}\n  ]}}')
    destination.write_text('{\n' + ',\n'.join(lines) + '\n}\n')
    print(f'{len(result)} 张图集，{sum(len(a["cells"]) for a in result.values())} 格，{destination.stat().st_size} 字节')


if __name__ == '__main__':
    main()
