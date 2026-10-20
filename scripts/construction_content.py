"""建设扩展分档和精确权重分配；不按稀有度重分旧效果的出货率。"""
from fractions import Fraction

RARITIES = {
    'geoResonance': 'SSR', 'growthLink': 'SSR', 'sereniteaPot': 'UR',
    'createBuild': 'SR', 'remoteConstruction': 'SR', 'shrineGrowth': 'SR',
    'allConstruction': 'SSR', 'erdtreeBlessing': 'SSR',
}


def assign(pool, all_candidates, old_candidates, old_probabilities):
    """输入原完整效果概率与新增候选，返回保留顺序的新概率；普通升级权重不变。"""
    probabilities = {(kind, p['id']): weight for (kind, p), weight in zip(old_candidates, old_probabilities)}
    if pool == 'card':
        added = {'geoResonance': Fraction(2, 100), 'growthLink': Fraction(2, 100),
                 'sereniteaPot': Fraction(1, 100), 'createBuild': Fraction(1, 100),
                 'remoteConstruction': Fraction(1, 100), 'shrineGrowth': Fraction(1, 100)}
        sources = [((kind, p['id']), probabilities[(kind, p['id'])]) for kind, p in old_candidates if kind == 'cash']
        budget = sum(value for _, value in sources)
        if budget <= Fraction(7, 100):
            raise ValueError('现金道具概率不足以分配建设扩展')
        for key, value in sources:
            probabilities[key] = value * (budget - Fraction(7, 100)) / budget
        sources = [((kind, p['id']), probabilities[(kind, p['id'])]) for kind, p in old_candidates if p['rarity'] == 'UR']
        budget = sum(value for _, value in sources)
        if budget <= Fraction(1, 100):
            raise ValueError('原UR额度不足以分配尘歌壶')
        for key, value in sources:
            probabilities[key] = value * (budget - Fraction(1, 100)) / budget
    elif pool == 'fortune':
        added = {'allConstruction': Fraction(2, 100), 'erdtreeBlessing': Fraction(2, 100)}
        sources = [((kind, p['id']), probabilities[(kind, p['id'])]) for kind, p in old_candidates if kind == 'income' and p['rarity'] != 'UR']
        budget = sum(value for _, value in sources)
        if budget <= Fraction(4, 100):
            raise ValueError('财富现金概率不足以分配建设事件')
        # 从最高概率现金参数档扣4个百分点，避免比例分摊产生过大的整数分母。
        key, value = max(sources, key=lambda item: item[1])
        if value <= Fraction(4, 100):
            raise ValueError('单档财富现金额度不足以保留精确整数权重')
        probabilities[key] = value - Fraction(4, 100)
    else:
        added = {}
    for kind, p in all_candidates:
        if kind in RARITIES:
            count = sum(k == kind for k, _ in all_candidates)
            probabilities[(kind, p['id'])] = added[kind] / count
    return all_candidates, [probabilities[(kind, p['id'])] for kind, p in all_candidates]
