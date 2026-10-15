"""生成活动专属原创短音，不覆盖原有十三类游戏音效。"""
from pathlib import Path
import importlib.util
import json

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('soft_sounds', ROOT / 'scripts/generate-soft-sounds.py')
soft = importlib.util.module_from_spec(spec)
spec.loader.exec_module(soft)


def main():
    """无输入，输出十六份活动 WAV 和可追溯的音效清单。"""
    seq, mix = soft.sequence, soft.mix
    sounds = {
        'casino_dice': mix(seq([57, 62, 60, 64, 57, 69], .08, 'wood', .25, .12)),
        'casino_win': mix(seq([76, 81, 88, 93], .1, 'bell', .3, .55)),
        'casino_loss': mix(seq([69, 64, 60], .15, 'pluck', .23, .35)),
        'horse_start': mix(seq([72, 76, 84], .15, 'bell', .28, .45)),
        'horse_run': mix([(0, 45, .09, 'wood', .3), (.1, 52, .1, 'wood', .25), (.24, 45, .09, 'wood', .27), (.34, 52, .09, 'wood', .23)], space=.025),
        'horse_finish': mix(seq([72, 79, 84, 88, 91], .11, 'bell', .28, .65)),
        'roulette_spin': mix(seq([57, 60, 62, 64, 67, 69, 72], .075, 'wood', .23, .1)),
        'roulette_click': mix([(0, 62, .08, 'wood', .3), (.08, 69, .1, 'wood', .17)], space=.03),
        'roulette_hit': mix(seq([60, 72, 84, 88], .06, 'bell', .25, .35), air=.08),
        'packet_open': mix(seq([76, 81, 84], .12, 'bell', .24, .45), air=.15),
        'packet_lucky': mix(seq([81, 88, 93, 96, 100], .1, 'bell', .27, .6)),
        'rps_count': mix(seq([69, 69, 76], .3, 'wood', .28, .16)),
        'rps_reveal': mix(seq([76, 84], .09, 'wood', .28, .24)),
        'rps_win': mix(seq([72, 76, 79, 84], .12, 'bell', .3, .6)),
        'rotation_count': mix(seq([81, 88], .1, 'bell', .18, .28)),
        'rotation_switch': mix(seq([69, 76, 81, 88, 93], .13, 'bell', .25, .55), air=.06),
    }
    output = ROOT / 'src/web/public/audio/sfx'
    output.mkdir(parents=True, exist_ok=True)
    manifest = {}
    for name, samples in sounds.items():
        soft.write(output / f'{name}.wav', samples)
        manifest[name] = {'seconds': round(len(samples) / soft.RATE, 3), 'rate': soft.RATE, 'channels': 1}
    target = ROOT / 'generated-assets/activities-2026-10-15'
    target.mkdir(parents=True, exist_ok=True)
    (target / 'audio-manifest.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + '\n')
    print(f'Generated {len(sounds)} original activity sounds')


if __name__ == '__main__':
    main()
