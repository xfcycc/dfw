"""生成与《编织愿景》音高分析相配的原创短音效及试听串；不采样原曲。"""
from pathlib import Path
import wave
import numpy as np

RATE = 44100
OUTPUT = Path(__file__).resolve().parents[1] / 'src/web/public/audio/sfx'
RNG = np.random.default_rng(20261014)


def hz(midi):
    """输入 MIDI 音高，返回频率。"""
    return 440 * 2 ** ((midi - 69) / 12)


def tone(midi, duration, color='pluck'):
    """输入音高、时长和音色，返回带柔和起音的单声道波形。"""
    t = np.arange(round(duration * RATE)) / RATE
    f = hz(midi)
    if color == 'bell':
        x = (np.sin(2 * np.pi * f * t) * np.exp(-t / .22)
             + .22 * np.sin(2 * np.pi * f * 2.01 * t) * np.exp(-t / .1)
             + .035 * np.sin(2 * np.pi * f * 3.94 * t) * np.exp(-t / .06))
    else:
        decay = .045 if color == 'wood' else .14
        x = (np.sin(2 * np.pi * f * t)
             + .2 * np.sin(2 * np.pi * f * 2 * t) * np.exp(-t / .05)
             + .06 * np.sin(2 * np.pi * f * 3 * t) * np.exp(-t / .025)) * np.exp(-t / decay)
    x *= 1 - np.exp(-t / .006)
    fade = min(round(.015 * RATE), len(x))
    x[-fade:] *= np.linspace(1, 0, fade)
    return x


def rustle(duration):
    """输入时长，返回低通、渐入渐出的轻纸张气流声。"""
    length = round(duration * RATE)
    noise = RNG.standard_normal(length)
    noise = np.convolve(noise, np.ones(18) / 18, mode='same')
    return noise * np.sin(np.linspace(0, np.pi, length)) ** 2


def mix(events, air=0, space=.12):
    """输入音符事件、气流幅度和空间感，返回混合音效及少量衰减反射。"""
    end = max(at + duration for at, _, duration, _, _ in events)
    x = np.zeros(round((end + .22) * RATE))
    for at, midi, duration, color, gain in events:
        voice = tone(midi, duration, color) * gain
        start = round(at * RATE)
        x[start:start + len(voice)] += voice
    if air:
        breath = rustle(.23) * air
        x[:len(breath)] += breath
    dry = x.copy()
    reflections = np.convolve(dry, np.ones(5) / 5, mode='same')
    for delay, gain in ((.029, 1), (.063, .65), (.101, .4), (.167, .2)):
        offset = round(delay * RATE)
        x[offset:] += reflections[:-offset] * space * gain
    x -= np.mean(x)
    peak = np.max(np.abs(x))
    if peak > .62:
        x *= .62 / peak
    # 移除合成后的全静音尾段，保留 30 ms 和短淡出，避免文件边界点击。
    audible = np.flatnonzero(np.abs(x) > .00015)
    x = x[:min(len(x), int(audible[-1]) + round(.03 * RATE))]
    x[:220] *= np.linspace(0, 1, 220)
    x[-440:] *= np.linspace(1, 0, 440)
    return x


def write(path, samples):
    """输入路径与波形，写入 44.1 kHz、16 位单声道 WAV。"""
    pcm = np.round(np.clip(samples, -1, 1) * 32767).astype('<i2')
    with wave.open(str(path), 'wb') as output:
        output.setparams((1, 2, RATE, 0, 'NONE', 'not compressed'))
        output.writeframes(pcm.tobytes())


def sequence(notes, spacing, color='pluck', gain=.32, duration=.38):
    """输入音高序列、间隔与音色参数，返回按时间展开的音符事件。"""
    return [(index * spacing, midi, duration, color, gain) for index, midi in enumerate(notes)]


def main():
    """无入参，生成十三类游戏音效和一份带间隔的试听串。"""
    OUTPUT.mkdir(parents=True, exist_ok=True)
    sounds = {
        'click': mix([(0, 84, .16, 'wood', .2)], space=.03),
        'turn': mix(sequence([81, 88], .15, 'bell', .3, .55)),
        'chat': mix(sequence([84, 88], .085, 'bell', .18, .28), space=.06),
        'dice': mix(sequence([57, 62, 64, 60, 57], .055, 'wood', .22, .1), space=.02),
        'step': mix([(0, 57, .13, 'wood', .19)], space=.02),
        'coin': mix(sequence([88, 93, 96], .075, 'bell', .23, .52), space=.1),
        'pay': mix(sequence([64, 57], .09, 'pluck', .27, .26), space=.07),
        'buy': mix(sequence([69, 72, 76, 81], .07, gain=.27)),
        'build': mix(sequence([69, 76, 84], .095, gain=.3), space=.09),
        'card': mix(sequence([81, 88], .12, 'bell', .25, .48), air=.09),
        'magic': mix(sequence([81, 84, 88, 91, 93], .07, 'bell', .23, .65), air=.04, space=.16),
        'error': mix(sequence([64, 62], .1, 'pluck', .22, .24), space=.03),
        'victory': mix(sequence([69, 72, 76, 79, 81], .12, 'bell', .26, .65)
                       + [( .62, note, .72, 'pluck', .16) for note in (69, 76, 84)], space=.17),
    }
    preview = []
    for name, samples in sounds.items():
        write(OUTPUT / f'{name}.wav', samples)
        preview.extend((samples, np.zeros(round(.5 * RATE))))
        print(f'{name}: {len(samples) / RATE:.3f}s, peak={np.max(np.abs(samples)):.3f}')
    write(OUTPUT / 'preview.wav', np.concatenate(preview))


if __name__ == '__main__':
    main()
