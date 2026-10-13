"""根据仓库配置计算四人局经济及简化随机模型；不是游戏测试或真实对局验收。"""
import argparse
import csv
import hashlib
import json
import math
import random
import statistics
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RULES = ROOT / "backend/src/main/resources/game-rules"
CONFIG = json.loads((RULES / "economy.json").read_text())
CHANCE = json.loads((RULES / "chance-events.json").read_text())
FORTUNE = json.loads((RULES / "fortune-events.json").read_text())
RENT_MULTIPLIERS = [1, 3, 6, 10]
SEED = 20261013


def money(key, base):
    """输入金额键和基准值，复现配置的最终金额及非负四舍五入。"""
    return CONFIG["fixedAmounts"].get(key, math.floor(base * CONFIG["moneyMultiplier"] + 0.5))


def derived(key, actual):
    """输入已按地价计算的金额，应用固定覆盖，不再次乘倍率。"""
    return CONFIG["fixedAmounts"].get(key, actual)


def map_random(seed):
    """输入地图种子，生成与地图算法相同的 32 位整数随机序列。"""
    mask = 0xFFFFFFFF
    value = 2166136261
    for char in seed:
        value = ((value ^ ord(char)) * 16777619) & mask
    while True:
        value = (value + 0x6D2B79F5) & mask
        item = ((value ^ (value >> 15)) * (value | 1)) & mask
        item ^= (item + (((item ^ (item >> 7)) * (item | 61)) & mask)) & mask
        yield ((item ^ (item >> 14)) & mask) / 4294967296


def shuffle(items, generator):
    """输入序列和地图随机源，返回 Fisher-Yates 洗牌结果。"""
    items = list(items)
    for index in range(len(items) - 1, 0, -1):
        other = int(next(generator) * (index + 1))
        items[index], items[other] = items[other], items[index]
    return items


def make_board(seed):
    """输入种子，按四人 48 格与同源命名随机消耗计算地图价格分布。"""
    generator = map_random(seed)
    board = [dict(type="property", owner=None, level=0, mortgage=None, price=0) for _ in range(48)]
    for index, kind in zip([0, 12, 24, 36], ["start", "chance", "fortune", "jail"]):
        board[index]["type"] = kind
    for _ in range(2):
        for kind in ["chance", "fortune"]:
            available = [i for i, tile in enumerate(board) if tile["type"] == "property" and board[(i-1) % 48]["type"] == "property" and board[(i+1) % 48]["type"] == "property"]
            board[available[int(next(generator) * len(available))]]["type"] = kind
    regions = json.loads((RULES / "map-regions.json").read_text())["regions"]
    groups = shuffle(regions, generator)
    for group in groups:
        shuffle(group["names"], generator)
    for side in range(4):
        properties = [tile for tile in board[side*12:(side+1)*12] if tile["type"] == "property"]
        prices = shuffle([money(f"landPrice.{i % 3}", [800, 1200, 1600][i % 3]) for i in range(len(properties))], generator)
        for tile, price in zip(properties, prices):
            tile["price"] = price
    return board


def build_cost(tile):
    """输入地产，返回本局建设成本。"""
    return derived(f'buildCost.{tile["price"]}', tile["price"] // 2)


def property_value(tile):
    """输入地产，返回土地与当前建筑的成本估值。"""
    return tile["price"] + tile["level"] * build_cost(tile)


def rent(tile):
    """输入地产，返回实际等级租金；抵押期间为零。"""
    return 0 if tile["mortgage"] is not None else derived(f'rent.{tile["price"]}.{tile["level"]}', tile["price"] // 10 * RENT_MULTIPLIERS[tile["level"]])


def run_game(rng, reserve, defense, rent_scale, max_actions, checkpoints):
    """输入简化策略与行动上限，模拟现金、产权、建造、抵押和当前事件池；不模拟谈判及卡牌操作。"""
    board = make_board(str(rng.getrandbits(64)))
    players = [dict(cash=money("initialCash", 6000), position=0, skip=0, alive=True) for _ in range(4)]
    actions = 0
    cursor = 0
    history = {}
    counts = dict(chance=0, fortune=0, rent=0, purchase=0, upgrade=0, mortgage=0)

    def pay(index, amount, creditor=None):
        """输入债务人与最终费用，以收益较低的地产先抵押；无法偿还时清算。"""
        player = players[index]
        if not player["alive"]:
            return
        available = [tile for tile in board if tile["owner"] == index and tile["mortgage"] is None]
        available.sort(key=lambda tile: (rent(tile) / max(1, property_value(tile)//2), property_value(tile)))
        while player["cash"] < amount and available:
            tile = available.pop(0)
            tile["mortgage"] = property_value(tile)
            player["cash"] += tile["mortgage"] // 2
            counts["mortgage"] += 1
        if player["cash"] < amount:
            if creditor is not None and players[creditor]["alive"]:
                players[creditor]["cash"] += player["cash"]
            player["cash"] = 0
            player["alive"] = False
            for tile in board:
                if tile["owner"] == index:
                    tile.update(owner=None, level=0, mortgage=None)
        else:
            player["cash"] -= amount
            if creditor is not None and players[creditor]["alive"]:
                players[creditor]["cash"] += amount

    def move(index, steps):
        """输入行动者和位移，仅正向越过起点给本局起点收入。"""
        player = players[index]
        if steps > 0 and player["position"] + steps >= 48:
            player["cash"] += money("startIncome", 1000)
        player["position"] = (player["position"] + steps) % 48

    def settle(index, event_drawn=False):
        """输入行动者，复现当前事件 JSON 的已支持效果；同回合最多抽一次事件。"""
        player = players[index]
        tile = board[player["position"]]
        kind = tile["type"]
        if kind == "jail":
            player["skip"] = 1
        elif kind in ["chance", "fortune"] and not event_drawn:
            counts[kind] += 1
            pool = CHANCE["events"] if kind == "chance" else FORTUNE["events"]
            event = rng.choice(pool)
            for step_index, step in enumerate(event["steps"]):
                params = step.get("params", {})
                base = params.get("amount", 0)
                effect = step["type"]
                amount = money(f'{event["id"]}.{step_index}.amount', base) if effect in ["GRANT_MONEY", "PAY_FEE", "CHARGE_ALL"] else base
                if effect == "GRANT_MONEY":
                    player["cash"] += amount
                elif effect == "PAY_FEE":
                    pay(index, amount)
                elif effect == "CHARGE_ALL":
                    for target in range(4):
                        pay(target, amount)
                elif effect == "MOVE":
                    move(index, amount)
                elif effect == "SETTLE_TILE":
                    settle(index, True)
                elif effect == "SKIP_TURN":
                    player["skip"] = amount
                elif effect == "DESTROY_BUILDINGS":
                    targets = [item for item in board if item["level"] > 0]
                    if targets:
                        target = rng.choice(targets)
                        loss = target["level"] * build_cost(target)
                        target["level"] = 0
                        for passive in CHANCE.get("passiveRules", []):
                            if rng.randrange(100) < passive["probability"]:
                                players[target["owner"]]["cash"] += loss * passive["compensationPercent"] // 100
                # DRAW_CARDS / TRAP_IMMUNITY 不在此经济近似中转为无限免费收益。
        elif kind == "property":
            if tile["owner"] is None and player["cash"] >= tile["price"] + reserve:
                player["cash"] -= tile["price"]
                tile["owner"] = index
                counts["purchase"] += 1
            elif tile["owner"] == index and tile["mortgage"] is None and tile["level"] < 3 and player["cash"] >= build_cost(tile) + reserve:
                player["cash"] -= build_cost(tile)
                tile["level"] += 1
                counts["upgrade"] += 1
            elif tile["owner"] is not None and tile["owner"] != index and tile["mortgage"] is None:
                counts["rent"] += 1
                if rng.random() >= defense:
                    pay(index, math.floor(rent(tile) * rent_scale + 0.5), tile["owner"])

    while actions < max_actions and sum(player["alive"] for player in players) > 1:
        index = cursor % 4
        cursor += 1
        player = players[index]
        if not player["alive"]:
            continue
        if player["skip"]:
            player["skip"] -= 1
            continue
        actions += 1
        move(index, rng.randint(1, 6))
        settle(index)
        if player["alive"]:
            for tile in board:
                if tile["owner"] == index and tile["mortgage"] is not None and property_value(tile) > tile["mortgage"] * 55 // 100:
                    redemption = tile["mortgage"] * 55 // 100
                    if player["cash"] >= redemption + reserve + money("landPrice.2", 1600):
                        player["cash"] -= redemption
                        tile["mortgage"] = None
        if actions in checkpoints:
            history[actions] = dict(alive=sum(p["alive"] for p in players), owned=sum(t["owner"] is not None for t in board), buildings=sum(t["level"] for t in board), chance=counts["chance"], fortune=counts["fortune"])
    finished = sum(p["alive"] for p in players) <= 1
    for point in checkpoints:
        if point not in history:
            history[point] = dict(alive=sum(p["alive"] for p in players), owned=sum(t["owner"] is not None for t in board), buildings=sum(t["level"] for t in board), chance=counts["chance"], fortune=counts["fortune"])
    return actions if finished else None, history


def main():
    """输入命令行样本数及封顶行动数，输出可复算的统计与敏感性表。"""
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--games", type=int, default=2000)
    parser.add_argument("--max-actions", type=int, default=1200)
    args = parser.parse_args()
    out = ROOT / "docs/analysis"
    out.mkdir(exist_ok=True)
    checkpoints = [80, 120, 160, 180, 240, 360, 600, args.max_actions]
    configurations = [("积极投入", 0, 0, 1), ("保留最高档地价现金", money("landPrice.2", 1600), 0, 1), ("租金20%被防御_假设", 0, 0.2, 1), ("租金另乘2_假设", 0, 0, 2), ("租金另乘3_假设", 0, 0, 3)]
    results = []
    for label, reserve, defense, rent_scale in configurations:
        rng = random.Random(SEED)
        simulations = [run_game(rng, reserve, defense, rent_scale, args.max_actions, checkpoints) for _ in range(args.games)]
        completed = sorted(value for value, _ in simulations if value is not None)
        summary = dict(policy=label, games=args.games, censored_at=args.max_actions, unfinished_at_cap=1-len(completed)/args.games, completed_sample_median=statistics.median(completed) if completed else None, overall_median_actions=completed[math.ceil(args.games * 0.5)-1] if len(completed) >= math.ceil(args.games * 0.5) else None, p10_actions=completed[math.ceil(args.games * 0.1)-1] if len(completed) >= math.ceil(args.games * 0.1) else None, p90_actions=completed[math.ceil(args.games * 0.9)-1] if len(completed) >= math.ceil(args.games * 0.9) else None, rent_scale=rent_scale, checkpoints=[])
        for point in checkpoints:
            rows = [history[point] for _, history in simulations]
            ended = sum(value is not None and value <= point for value, _ in simulations)
            summary["checkpoints"].append(dict(actions=point, finished_fraction=ended/args.games, mean_alive=statistics.mean(row["alive"] for row in rows), mean_owned=statistics.mean(row["owned"] for row in rows), mean_buildings=statistics.mean(row["buildings"] for row in rows), mean_chance=statistics.mean(row["chance"] for row in rows), mean_fortune=statistics.mean(row["fortune"] for row in rows)))
        results.append(summary)
        print(label, json.dumps(summary, ensure_ascii=False), flush=True)
    boards = [make_board(f"cost-{i}") for i in range(2000)]
    land_cost = statistics.mean(sum(tile["price"] for tile in board) for board in boards)
    build_total = statistics.mean(sum(3 * build_cost(tile) for tile in board if tile["type"] == "property") for board in boards)
    fortune_cash = sum(sum(money(f'{event["id"]}.{i}.amount', step["params"]["amount"]) for i, step in enumerate(event["steps"]) if step["type"] == "GRANT_MONEY") for event in FORTUNE["events"]) / len(FORTUNE["events"])
    chance_global = 0
    for event in CHANCE["events"]:
        for i, step in enumerate(event["steps"]):
            if step["type"] in ["PAY_FEE", "CHARGE_ALL", "GRANT_MONEY"]:
                amount = money(f'{event["id"]}.{i}.amount', step["params"]["amount"])
                chance_global += amount * (1 if step["type"] == "GRANT_MONEY" else -4 if step["type"] == "CHARGE_ALL" else -1)
    chance_global /= len(CHANCE["events"])
    analysis = dict(seed=SEED, model="现金/地产/抵押/当前事件近似，非完整游戏实现", input_sha256={name:hashlib.sha256((RULES / name).read_bytes()).hexdigest() for name in ["economy.json", "map-regions.json", "chance-events.json", "fortune-events.json"]}, initial_total=4*money("initialCash", 6000), mean_land_total=land_cost, mean_full_build_total=build_total, fortune_direct_cash_expectation=fortune_cash, chance_system_cash_expectation=chance_global, uniform_system_cash_per_action=money("startIncome", 1000)*3.5/48+3/48*(fortune_cash+chance_global), scenarios=results)
    (out / "economy-model-results.json").write_text(json.dumps(analysis, ensure_ascii=False, indent=2)+"\n")
    with (out / "economy-model-checkpoints.csv").open("w", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=["policy"]+list(results[0]["checkpoints"][0]))
        writer.writeheader()
        for result in results:
            for point in result["checkpoints"]:
                writer.writerow(dict(policy=result["policy"], **point))
    print("analytic", json.dumps({key:value for key,value in analysis.items() if key != "scenarios"}, ensure_ascii=False), flush=True)


if __name__ == "__main__":
    main()
