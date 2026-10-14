"""将已确认的 Markdown 素材同步为运行配置；只生成内容，不执行对局或测试。"""
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
WEIGHTS = {
    'chance': dict(income=12, fee=12, forward=8, back=8, skip=5, cards=8, all_income=5, all_fee=5, rich_fee=7, bottom_income=7, gamble=8, self_destroy=3, disaster=3, free_upgrade=5, trap_immunity=2, next_waiver=1, rent_double=1),
    'fortune': dict(income=60, cards_one=10, cards_two=3, directed=2, free_upgrade=10, all_income=5, next_waiver=5, bottom_bonus=5, extra_action=0),
    'card': dict(fixed=14, remote=4, barrier=8, demolish=8, swap=8, immunity=12, move=10, bonus=6, freeUpgrade=10, halfPayment=8, release=5, steal=3, cash=4),
}


def classify(pool, effect):
    """输入素材池和效果原文，返回已确认机制及完整参数，不改动原效果数值。"""
    nums = [int(n) for n in re.findall(r'\d+', effect)]
    if pool == 'card':
        if effect.startswith('替代掷骰'): return ('remote', {}) if '自选' in effect else ('fixed', dict(steps=nums[0]))
        if effect.startswith('主动位移'): return 'move', dict(steps=nums[0] * (-1 if '后退' in effect else 1))
        if effect.startswith('掷骰加码'): return 'bonus', dict(steps=nums[0])
        if effect.startswith('即时现金'): return 'cash', dict(amount=nums[0])
        for prefix, kind in [('路障','barrier'),('拆房','demolish'),('换位','swap'),('免租','immunity'),('免费升级','freeUpgrade'),('付款减免','halfPayment'),('解除监禁','release'),('抢夺手牌','steal')]:
            if effect.startswith(prefix): return kind, {}
    else:
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
        if effect.startswith('随机摧毁'): return 'disaster', {}
        if effect.startswith('免费升级'): return 'free_upgrade', dict(fallback=400)
        if '陷阱免疫' in effect: return 'trap_immunity', dict(count=3)
        if '翻倍' in effect: return 'rent_double', {}
        if '免除' in effect: return 'next_waiver', {}
        if effect.startswith('落格者'): return 'bottom_bonus', dict(amount=nums[0], bonus=nums[1])
        if effect.startswith('立即再次'): return 'extra_action', {}
    raise ValueError(f'未识别的效果：{pool} {effect}')


def generate(pool, filename):
    """输入素材文件，去重参数、分配机制及金额档权重，输出对应 JSON 内容段。"""
    mechanisms = {}
    for line in (ROOT / 'docs/content' / filename).read_text().splitlines():
        cells = [c.strip() for c in line.split('|')[1:-1]]
        if len(cells) != 9 or not cells[0].isdigit() or cells[3] not in ['R', 'SR', 'SSR']: continue
        skin_id, name, rarity = cells[1:4]
        quote, description, effect = cells[5:8] if pool == 'card' else cells[4:7]
        kind, values = classify(pool, effect)
        mechanism = mechanisms.setdefault(kind, {'id': kind, 'weight': WEIGHTS[pool][kind], 'groups': []})
        tier, weight = 'default', 100
        if kind in ['income', 'fee']:
            amount = values['amount']
            if pool == 'fortune': tier, weight = ('small',70) if amount <= 600 else ('medium',25) if amount <= 1300 else ('large',5)
            elif pool == 'chance': tier, weight = ('small',80) if amount <= 500 else ('medium',19) if amount <= (1200 if kind == 'income' else 800) else ('large',1)
        group = next((g for g in mechanism['groups'] if g['id'] == tier), None)
        if group is None:
            group = {'id': tier, 'weight': weight, 'parameters': []}; mechanism['groups'].append(group)
        parameter_id = '_'.join(f'{k}-{v}' for k,v in sorted(values.items())) or 'default'
        parameter = next((p for p in group['parameters'] if p['id'] == parameter_id), None)
        if parameter is None:
            parameter = {'id': parameter_id, 'values': values, 'skins': []}; group['parameters'].append(parameter)
        skin = dict(id=f'{pool}_{skin_id}', name=name, rarity=rarity, quote=quote, description=description)
        if pool == 'card': skin['cardType'] = cells[4]
        parameter['skins'].append(skin)
    expected = {'chance':121,'fortune':110,'card':111}[pool]
    count = sum(len(p['skins']) for m in mechanisms.values() for g in m['groups'] for p in g['parameters'])
    if count != expected: raise ValueError(f'{pool} 素材数 {count}，应为 {expected}')
    file = ROOT / 'backend/src/main/resources/game-rules' / ('card-events.json' if pool == 'card' else pool+'-events.json')
    data = json.loads(file.read_text()); data['content'] = {'mechanisms': list(mechanisms.values())}
    file.write_text(json.dumps(data, ensure_ascii=False, indent=2)+'\n')
    print(f'{pool}: {count} 条素材；启用 {sum(len(p["skins"]) for m in mechanisms.values() if m["weight"] > 0 for g in m["groups"] for p in g["parameters"])} 条')


if __name__ == '__main__':
    for pool, filename in [('chance','chance.md'),('fortune','fortune.md'),('card','cards.md')]: generate(pool,filename)
