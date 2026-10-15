"""按图片生成工具产出的固定图集网格拆分运行素材，保留原始图集。"""
from pathlib import Path
from PIL import Image
import hashlib
import json

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'generated-assets/activities-2026-10-15'
KINDS = ['casino', 'horse', 'roulette', 'packet', 'rps', 'rotation']
SHEETS = [
    ('backgrounds', 2, 3, [f'{kind}-background' for kind in KINDS]),
    ('landmarks', 3, 2, [f'{kind}-tile' for kind in KINDS]),
    ('props', 4, 4, [*[f'horse-{n}' for n in range(1, 9)], 'cylinder', 'roulette-toy', 'packet-closed', 'packet-open', 'rps-0', 'rps-1', 'rps-2', 'lucky-clover']),
]


def main():
    """无输入，按既定网格提取PNG及WebP，并记录原图、裁切坐标、大小和摘要。"""
    output = ROOT / 'src/web/public/images/activities'
    output.mkdir(parents=True, exist_ok=True)
    originals = ASSETS / 'extracted'
    originals.mkdir(exist_ok=True)
    entries = []
    for sheet, columns, rows, names in SHEETS:
        source = ASSETS / '_sources' / f'{sheet}.png'
        with Image.open(source) as image:
            for index, name in enumerate(names):
                x, y = index % columns, index // columns
                box = [round(x * image.width / columns) + 2, round(y * image.height / rows) + 2, round((x + 1) * image.width / columns) - 2, round((y + 1) * image.height / rows) - 2]
                asset = image.crop(box).convert('RGB')
                asset.save(originals / f'{name}.png')
                target = output / f'{name}.webp'
                asset.save(target, format='WEBP', quality=86, method=6)
                entries.append({'name': name, 'source': str(source.relative_to(ROOT)), 'box': box, 'width': asset.width, 'height': asset.height, 'path': str(target.relative_to(ROOT)), 'bytes': target.stat().st_size, 'sha256': hashlib.sha256(target.read_bytes()).hexdigest()})
    atlas_dir = ASSETS / 'atlases'
    atlas_dir.mkdir(exist_ok=True)
    with Image.open(ASSETS / '_sources/props.png') as source:
        w, h = source.size
        groups = {'horses': (0, 0, w, round(h / 2)), 'roulette': (0, round(h / 2), round(w / 2), round(h * 3 / 4)), 'packets': (round(w / 2), round(h / 2), w, round(h * 3 / 4)), 'rps': (0, round(h * 3 / 4), round(w * 3 / 4), h)}
        for name, box in groups.items():
            source.crop(box).save(atlas_dir / f'{name}.png')
    (ASSETS / 'image-manifest.json').write_text(json.dumps(entries, ensure_ascii=False, indent=2) + '\n')
    print(f'Extracted {len(entries)} assets, {sum(e["bytes"] for e in entries):,} bytes of WebP')


if __name__ == '__main__':
    main()
