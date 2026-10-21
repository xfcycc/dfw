"""将已确认的 Markdown 素材同步为运行配置；只生成内容，不执行对局或测试。"""
import json
import re
from fractions import Fraction
from math import lcm, gcd
from functools import reduce
from pathlib import Path
from construction_content import RARITIES as CONSTRUCTION_RARITIES, assign as assign_construction

ROOT = Path(__file__).resolve().parents[1]
RARITIES = {'R', 'SR', 'SSR', 'UR'}
NEW_EVENT = {'chance': {'concert': '4', 'shuffle': '4'}, 'fortune': {'treasure': '5', 'all_cards': '6'}}
ANIME_RARITIES = {'timeRewind': 'SSR', 'projection': 'UR', 'starburst': 'SSR', 'imagineBreaker': 'UR', 'classTrial': 'SR', 'timeRestore': 'SSR', 'vectorReflect': 'UR', 'deathReturn': 'UR', 'explosion': 'UR', 'corruption': 'SSR', 'contract': 'SSR', 'commandSpell': 'UR', 'sevenSwords': 'UR', 'railgun': 'UR', 'sealCard': 'UR', 'swordRain': 'SSR', 'voidTears': 'SSR', 'absoluteZero': 'UR'}
NEW_RARITIES = {'shield': 'SSR', 'recycle': 'SR', 'discover': 'SSR', 'sap': 'SR', 'exchange': 'UR', 'slow': 'R', 'swapAny': 'SSR', 'kamui': 'UR', 'handDestruction': 'SSR', 'gravity': 'SSR', 'reaper': 'UR', 'shadowstep': 'SR', 'homeTeleport': 'SSR', 'blackhole': 'SSR', 'meteor': 'SSR', 'treasure': 'SSR', 'concert': 'SSR', 'shuffle': 'SSR', 'all_cards': 'SR'}


def classify(pool, effect):
    """输入素材池和效果原文，返回已确认机制及完整参数，不改动原效果数值。"""
    if effect.startswith('扩展:'):
        kind = effect.split()[0].split(':')[1]
        return kind, {'charges': 3} if kind == 'shield' and effect.startswith('扩展:shield 三层 ') else {}
    nums = [int(n) for n in re.findall(r'\d+', effect)]
    if effect.startswith(('指定入狱', '自身入狱')): return 'jail', {}
    if pool == 'card':
        if effect.startswith('指定全拆'): return 'fullDemolish', {}
        if effect.startswith('免费直升满级'): return 'fullUpgrade', {}
        if effect.startswith('随机传送'): return 'blast', {}
        if effect.startswith('赌博'): return 'gamble', dict(wager=nums[0], probability=nums[1], prize=nums[2])
        if effect.startswith('替代掷骰'): return ('remote', {}) if '自选' in effect else ('move', dict(steps=nums[0]))
        if effect.startswith('主动位移'): return 'move', dict(steps=nums[0] * (-1 if '后退' in effect else 1))
        if effect.startswith('掷骰加码'): return 'bonus', dict(steps=nums[0])
        if effect.startswith('即时现金'): return 'cash', dict(amount=nums[0])
        for prefix, kind in [('路障','barrier'),('拆房','demolish'),('换位','swap'),('免租','immunity'),('免费升级','freeUpgrade'),('付款减免','halfPayment'),('解除监禁','release'),('抢夺手牌','steal')]:
            if effect.startswith(prefix): return kind, {}
    else:
        if effect.startswith('全员振奋'): return 'all_morale', {}
        if effect.startswith('人才引进'): return 'talent', dict(amount=nums[0], count=nums[1])
        if effect.startswith('全图建筑'): return 'global_demolition', dict(compensationPercent=nums[-1])
        if effect.startswith('获得现金'): return 'income', dict(amount=nums[0])
        if effect.startswith('支付现金'): return 'fee', dict(amount=nums[0])
        if effect.startswith('前进'): return 'forward', dict(steps=nums[0])
        if effect.startswith('后退'): return 'back', dict(steps=-nums[0])
        if effect.startswith('休息'): return 'skip', dict(count=nums[0])
        if effect.startswith('抽 '): return ('cards' if pool == 'chance' else 'cards_one' if nums[0] == 1 else 'cards_two'), dict(count=nums[0])
        if effect.startswith('定向'): return 'directed', dict(cardType=0 if '免租' in effect else 1 if '免费升级' in effect else 2)
        if effect.startswith('全体各获得'): return 'all_income', dict(amount=nums[0])
        if effect.startswith('全体各支付'): return 'all_fee', dict(amount=nums[0])
        if effect.startswith('净资产最高'): return 'rich_fee', dict(amount=nums[0])
        if effect.startswith('净资产最低'): return 'bottom_income', dict(amount=nums[0])
        if effect.startswith('赌博'): return 'gamble', dict(wager=nums[0], probability=nums[1], prize=nums[2])
        if effect.startswith('摧毁自己'): return 'self_destroy', {}
        if effect.startswith('指定对手'): return 'opponent_destroy', {}
        if effect.startswith('随机摧毁'): return 'disaster', {}
        if effect.startswith('直升满级'): return 'full_upgrade', dict(fallback=400)
        if effect.startswith('免费升级'): return 'free_upgrade', dict(fallback=400)
        if '陷阱免疫' in effect: return 'trap_immunity', dict(count=3)
        if '翻倍' in effect: return 'rent_double', {}
        if '免除' in effect: return 'next_waiver', {}
        if effect.startswith('落格者'): return 'bottom_bonus', dict(amount=nums[0], bonus=nums[1])
        if effect.startswith('立即再次'): return 'extra_action', {}
    raise ValueError(f'未识别的效果：{pool} {effect}')


def impact_rarity(pool, kind, values):
    """输入池、机制和基准参数，按收支幅度、落点连锁、资产及多人影响返回固定等级。"""
    if kind == 'shield' and values.get('charges') == 3: return 'UR'
    if kind in CONSTRUCTION_RARITIES: return CONSTRUCTION_RARITIES[kind]
    if kind in {'freeUpgrade', 'free_upgrade'}: return 'SR'
    if kind in ANIME_RARITIES: return ANIME_RARITIES[kind]
    if kind in NEW_RARITIES: return NEW_RARITIES[kind]
    if pool == 'card':
        if kind in {'gamble', 'fullDemolish', 'fullUpgrade'}: return 'UR'
        if kind in {'remote', 'immunity', 'barrier', 'blast', 'jail', 'freeUpgrade'}: return 'SSR'
        if kind == 'cash': return 'R' if values['amount'] <= 600 else 'SR'
        if kind in {'fixed', 'move', 'bonus'}: return 'R'
        return 'SR'
    if kind in {'income', 'fee'}:
        amount = values['amount']
        return 'R' if amount <= 600 else 'SR' if amount <= 1300 else 'SSR' if amount < 3000 else 'UR'
    if kind in {'opponent_destroy', 'full_upgrade', 'talent', 'global_demolition'}: return 'UR'
    if kind in {'cards', 'cards_one', 'cards_two'}: return 'R' if values['count'] == 1 else 'SR'
    if kind in {'forward', 'back', 'skip', 'gamble'}: return 'SR'
    return 'SSR'


def assign_weights(pool, mechanisms):
    """输入内容池与完整效果；道具权重统一为1，事件沿用精确概率，写入参数权重。"""
    candidates = [(m['id'], p) for m in mechanisms if m['enabled'] for g in m['groups'] for p in g['parameters']]
    if pool == 'card':
        # 道具由ContentCatalog先按固定档位概率抽取，再均分该档全部完整效果。
        for _, parameter in candidates: parameter['weight'] = 1
        return
    construction_candidates = candidates
    candidates = [(k,p) for k,p in candidates if k not in CONSTRUCTION_RARITIES]
    all_candidates = candidates
    candidates = [(k,p) for k,p in candidates if k not in ANIME_RARITIES]
    probabilities = []
    rarities = dict(R=40, SR=30, SSR=25, UR=5)
    # 2026-10-18 文案删减前的参数档数量；不按删减后的档位数重分整个事件池。
    original_counts = dict(R=7, SR=21, SSR=21, UR=3) if pool == 'chance' else dict(R=4, SR=5, SSR=11, UR=5)
    for kind, p in candidates:
        if kind in NEW_EVENT[pool]:
            probabilities.append(Fraction(NEW_EVENT[pool][kind]) / 100 / sum(k == kind for k, _ in candidates))
            continue
        baseline_rarity = 'SSR' if kind == 'free_upgrade' else p['rarity']
        probability = Fraction(rarities[baseline_rarity], 100 * original_counts[baseline_rarity])
        if pool == 'chance':
            if kind == 'opponent_destroy': probability = Fraction(3,100)
            elif kind == 'income': probability *= Fraction(113,120)
            elif kind in {'bottom_income', 'gamble'}:
                probability *= Fraction(4 if kind == 'bottom_income' else 7, sum(k == kind for k, _ in candidates))
        else:
            if kind == 'global_demolition': probability = Fraction(3,100)
            elif kind == 'income': probability *= Fraction(633,655)
            elif kind == 'directed': probability *= Fraction(3, sum(k == kind for k, _ in candidates))
        probability *= Fraction(92,100) if pool == 'chance' else Fraction(89,100)
        probabilities.append(probability)
    if sum(probabilities) != 1: raise ValueError(f'{pool} 原效果概率合计须为100%')
    original = {(kind,p['id']): probability for (kind,p),probability in zip(candidates,probabilities)}
    probabilities = []
    new_regular = sum(kind in ANIME_RARITIES and p['rarity']!='UR' for kind,p in all_candidates)
    for kind,p in all_candidates:
        if pool == 'chance': probability = Fraction(3,100)/new_regular if kind in ANIME_RARITIES else original[(kind,p['id'])]*Fraction(97,100)
        else: probability = original[(kind,p['id'])]
        probabilities.append(probability)
    candidates, probabilities = assign_construction(pool, construction_candidates, all_candidates, probabilities)
    if sum(probabilities) != 1: raise ValueError(f'{pool} 新效果概率合计须为100%')
    denominator = lcm(*(p.denominator for p in probabilities))
    weights = [int(p * denominator) for p in probabilities]
    divisor = reduce(gcd, weights)
    for (_, parameter), weight in zip(candidates, weights): parameter['weight'] = weight // divisor


def generate(pool, filename):
    """输入素材文件，按已有卡面身份保留机制参数，新卡解析效果，再生成档位与权重。"""
    file = ROOT / 'backend/src/main/resources/game-rules' / ('card-events.json' if pool == 'card' else pool+'-events.json')
    data = json.loads(file.read_text())
    known_skins = {skin['id']: (m['id'], p['values']) for m in data['content']['mechanisms']
                   for g in m['groups'] for p in g['parameters'] for skin in p['skins']}
    mechanisms = {}
    for line in (ROOT / 'docs/content' / filename).read_text().splitlines():
        cells = [c.strip() for c in line.split('|')[1:-1]]
        if len(cells) != 9 or not cells[0].isdigit() or cells[3] not in RARITIES: continue
        skin_id, name, rarity = cells[1:4]
        quote, description, effect = cells[5:8] if pool == 'card' else cells[4:7]
        known = known_skins.get(f'{pool}_{skin_id}')
        kind, values = known if known is not None else classify(pool, effect)
        rarity = impact_rarity(pool, kind, values)
        if cells[3] != rarity: raise ValueError(f'{pool} {skin_id} 标注 {cells[3]}，实际影响应为 {rarity}')
        mechanism = mechanisms.setdefault(kind, {'id': kind, 'enabled': kind != 'extra_action', 'groups': [{'id': 'default', 'parameters': []}]})
        group = mechanism['groups'][0]
        parameter_id = '_'.join(f'{k}-{v}' for k,v in sorted(values.items())) or 'default'
        parameter = next((p for p in group['parameters'] if p['id'] == parameter_id), None)
        if parameter is None:
            parameter = {'id': parameter_id, 'rarity': rarity, 'values': values, 'skins': []}; group['parameters'].append(parameter)
        skin = dict(id=f'{pool}_{skin_id}', name=name, rarity=rarity, quote=quote, description=description)
        if pool == 'card': skin['cardType'] = cells[4]
        parameter['skins'].append(skin)
    expected = {'chance':100,'fortune':76}.get(pool)
    count = sum(len(p['skins']) for m in mechanisms.values() for g in m['groups'] for p in g['parameters'])
    if expected is not None and count != expected: raise ValueError(f'{pool} 素材数 {count}，应为 {expected}')
    assign_weights(pool, list(mechanisms.values()))
    data['content'] = {'mechanisms': list(mechanisms.values())}
    file.write_text(json.dumps(data, ensure_ascii=False, indent=2)+'\n')
    enabled = [p for m in mechanisms.values() if m['enabled'] for g in m['groups'] for p in g['parameters']]
    counts = {rarity: sum(p['rarity'] == rarity for p in enabled) for rarity in ['R', 'SR', 'SSR', 'UR']}
    if not all(counts.values()): raise ValueError(f'{pool} 缺少启用等级：{counts}')
    print(f'{pool}: {count} 条素材；启用 {sum(len(p["skins"]) for p in enabled)} 条；效果组合 {counts}')


if __name__ == '__main__':
    for pool, filename in [('chance','chance.md'),('fortune','fortune.md'),('card','cards.md')]: generate(pool,filename)
