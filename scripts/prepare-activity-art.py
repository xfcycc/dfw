"""按图片生成工具产出的固定图集网格拆分运行素材，从云端读取原始图集，生成文件仅放入临时目录。"""
from pathlib import Path
from PIL import Image
import hashlib
import json
import tempfile
from io import BytesIO
from urllib.request import urlopen

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'generated-assets/activities-2026-10-15'
KINDS = ['casino', 'horse', 'roulette', 'packet', 'rps', 'rotation']
SHEETS = [
    ('backgrounds', 2, 3, [f'{kind}-background' for kind in KINDS]),
    ('landmarks', 3, 2, [f'{kind}-tile' for kind in KINDS]),
    ('props', 4, 4, [*[f'horse-{n}' for n in range(1, 9)], 'cylinder', 'roulette-toy', 'packet-closed', 'packet-open', 'rps-0', 'rps-1', 'rps-2', 'lucky-clover']),
]


def open_source(name):
    """输入原始图集名，从云端清单加载图片并返回 Pillow 图像。"""
    manifest = json.loads((ROOT / 'generated-assets/cloud-images.json').read_text())
    path = f'generated-assets/activities-2026-10-15/_sources/{name}.png'
    url = next(entry['url'] for entry in manifest['entries'] if entry['path'] == path)
    with urlopen(url, timeout=60) as response:
        return Image.open(BytesIO(response.read()))


def main():
    """无输入，从云端图集提取PNG及WebP到临时目录，返回值为空；输出待上传清单。"""
    work = Path(tempfile.mkdtemp(prefix='dfw-activity-art-'))
    output = work / 'runtime'
    output.mkdir(parents=True, exist_ok=True)
    originals = work / 'extracted'
    originals.mkdir(exist_ok=True)
    entries = []
    for sheet, columns, rows, names in SHEETS:
        source = ASSETS / '_sources' / f'{sheet}.png'
        with open_source(sheet) as image:
            for index, name in enumerate(names):
                x, y = index % columns, index // columns
                box = [round(x * image.width / columns) + 2, round(y * image.height / rows) + 2, round((x + 1) * image.width / columns) - 2, round((y + 1) * image.height / rows) - 2]
                asset = image.crop(box).convert('RGB')
                asset.save(originals / f'{name}.png')
                target = output / f'{name}.webp'
                asset.save(target, format='WEBP', quality=86, method=6)
                entries.append({'name': name, 'source': str(source.relative_to(ROOT)), 'box': box, 'width': asset.width, 'height': asset.height, 'path': str(target), 'bytes': target.stat().st_size, 'sha256': hashlib.sha256(target.read_bytes()).hexdigest()})
    atlas_dir = work / 'atlases'
    atlas_dir.mkdir(exist_ok=True)
    with open_source('props') as source:
        w, h = source.size
        groups = {'horses': (0, 0, w, round(h / 2)), 'roulette': (0, round(h / 2), round(w / 2), round(h * 3 / 4)), 'packets': (round(w / 2), round(h / 2), w, round(h * 3 / 4)), 'rps': (0, round(h * 3 / 4), round(w * 3 / 4), h)}
        for name, box in groups.items():
            source.crop(box).save(atlas_dir / f'{name}.png')
    (work / 'image-manifest.json').write_text(json.dumps(entries, ensure_ascii=False, indent=2) + '\n')
    print(f'临时素材目录：{work}；上传并校验后更新云端映射，不复制回 public。')
    print(f'Extracted {len(entries)} assets, {sum(e["bytes"] for e in entries):,} bytes of WebP')


if __name__ == '__main__':
    main()
