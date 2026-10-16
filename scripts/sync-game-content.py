"""将已确认的 Markdown 素材同步为运行配置；只生成内容，不执行对局或测试。"""
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RARITIES = {'R', 'SR', 'SSR', 'UR'}


def classify(pool, effect):
    """输入素材池和效果原文，返回已确认机制及完整参数，不改动原效果数值。"""
    nums = [int(n) for n in re.findall(r'\d+', effect)]
    if effect.startswith(('指定入狱', '自身入狱')): return 'jail', {}
    if pool == 'card':
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
        if effect.startswith('全图建筑'): return 'global_demolition', dict(compensationPercent=nums[0])
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
    if pool == 'card':
        if kind == 'gamble': return 'UR'
        if kind in {'remote', 'immunity', 'demolish', 'swap', 'blast', 'jail', 'freeUpgrade', 'steal'}: return 'SSR'
        if kind == 'cash': return 'R' if values['amount'] <= 600 else 'SR'
        if kind in {'fixed', 'move', 'bonus'}: return 'SR'
        return 'SR'
    if kind in {'income', 'fee'}:
        amount = values['amount']
        return 'R' if amount <= 600 else 'SR' if amount <= 1300 else 'SSR' if amount < 3000 else 'UR'
    if kind in {'opponent_destroy', 'full_upgrade', 'talent', 'global_demolition'}: return 'UR'
    if kind in {'cards', 'cards_one', 'cards_two'}: return 'R' if values['count'] == 1 else 'SR'
    if kind in {'forward', 'back', 'skip', 'gamble'}: return 'SR'
    return 'SSR'


def generate(pool, filename):
    """输入素材文件，按完整效果去重并分配影响等级，输出无机制及金额档权重的运行配置。"""
    mechanisms = {}
    for line in (ROOT / 'docs/content' / filename).read_text().splitlines():
        cells = [c.strip() for c in line.split('|')[1:-1]]
        if len(cells) != 9 or not cells[0].isdigit() or cells[3] not in RARITIES: continue
        skin_id, name, rarity = cells[1:4]
        quote, description, effect = cells[5:8] if pool == 'card' else cells[4:7]
        kind, values = classify(pool, effect)
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
    expected = {'chance':126,'fortune':115,'card':115}[pool]
    count = sum(len(p['skins']) for m in mechanisms.values() for g in m['groups'] for p in g['parameters'])
    if count != expected: raise ValueError(f'{pool} 素材数 {count}，应为 {expected}')
    file = ROOT / 'backend/src/main/resources/game-rules' / ('card-events.json' if pool == 'card' else pool+'-events.json')
    data = json.loads(file.read_text()); data['content'] = {'mechanisms': list(mechanisms.values())}
    file.write_text(json.dumps(data, ensure_ascii=False, indent=2)+'\n')
    enabled = [p for m in mechanisms.values() if m['enabled'] for g in m['groups'] for p in g['parameters']]
    counts = {rarity: sum(p['rarity'] == rarity for p in enabled) for rarity in ['R', 'SR', 'SSR', 'UR']}
    if not all(counts.values()): raise ValueError(f'{pool} 缺少启用等级：{counts}')
    print(f'{pool}: {count} 条素材；启用 {sum(len(p["skins"]) for p in enabled)} 条；效果组合 {counts}')


if __name__ == '__main__':
    for pool, filename in [('chance','chance.md'),('fortune','fortune.md'),('card','cards.md')]: generate(pool,filename)
