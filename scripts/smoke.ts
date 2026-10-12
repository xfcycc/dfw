/**
 * 真实 HTTP / WebSocket 集成冒烟验证。
 * 只使用独立临时数据库，受控落点夹具在服务停止时写入；不调用引擎函数、不修改开发数据。
 * 先执行 npm run compile:server，再执行 npm run smoke。
 */
import assert from 'node:assert/strict';
import { spawn, type ChildProcess } from 'node:child_process';
import { mkdtempSync, rmSync, existsSync, mkdirSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';
import { randomUUID } from 'node:crypto';
import { createServer } from 'node:net';
import type { Connection, RowDataPacket } from 'mysql2/promise';
import { connectDatabase, databaseConfig, initializeDatabase, identifier, rowCounts } from './database.js';
import { readLegacy, importLegacy } from './sqlite-import.js';
import { createHash } from 'node:crypto';
import { generateMap } from '../src/shared/rules.js';
import { RealtimeClient } from '../src/shared/realtime.js';
import type { Card, CommandResult, GameAction, GameCommand, PrivatePlayerState, PublicRoomState, Session } from '../src/shared/types.js';

type SavedRoom = Omit<PublicRoomState, 'players' | 'tradePending'> & {
  players: (PublicRoomState['players'][number] & { hand: Card[] })[];
  trade: unknown;
  seed: string;
  activeCardUsed: boolean;
  eventDrawn: boolean;
  upgradedTiles: number[];
};
interface Client { session: Session; socket: RealtimeClient; state: PublicRoomState | null; privateData: PrivatePlayerState | null; privateCount: number }
const temporary = mkdtempSync(join(tmpdir(), 'dfw-smoke-'));
let databaseName = '';
let admin: Connection | undefined;
let db: Connection | undefined;
const ownedDatabases: string[] = [];
const webPath = join(temporary, 'web');
mkdirSync(webPath);
writeFileSync(join(webPath, 'index.html'), '<!doctype html><title>dfw-static-smoke</title>dfw-static-smoke');
writeFileSync(join(webPath, 'probe.txt'), 'static-asset-ok');
let service: ChildProcess | undefined;
let output = '';
let port = 0;
let base = '';
const clients: Client[] = [];
const groups = new Map<string, Session[]>();
const results: string[] = [];

/** 返回仅指向本次临时库的 JDBC 地址，保留本地连接参数。 */
function jdbcUrl(): string {
  const url = new URL((process.env.DB_URL ?? 'jdbc:mysql://127.0.0.1:3306/dfw').replace(/^jdbc:/, ''));
  url.pathname = '/' + databaseName; return 'jdbc:' + url.toString();
}
/** 创建唯一临时 MySQL 库，只有创建成功的库才加入清理名单。 */
async function allocateDatabase(initialize = false): Promise<Connection> {
  admin ??= await connectDatabase(null);
  const name = 'dfw_smoke_' + randomUUID().replaceAll('-', '');
  await admin.query(`CREATE DATABASE ${identifier(name)} CHARACTER SET utf8mb4 COLLATE utf8mb4_bin`);
  ownedDatabases.push(name); databaseName = name;
  return initialize ? await initializeDatabase(admin, name) : await connectDatabase(name);
}
/** 输入状态判断，等待实际服务事件满足条件，超时抛出错误。 */
async function waitFor(predicate: () => boolean, label: string, timeout = 7000): Promise<void> {
  const deadline = Date.now() + timeout;
  while (!predicate()) {
    if (Date.now() > deadline) throw new Error(`等待超时：${label}\n${output.slice(-2000)}`);
    await new Promise(resolve => setTimeout(resolve, 20));
  }
}
/** 输入本地接口名和数据，返回真实 HTTP 状态与 JSON。 */
async function post(path: string, body: unknown): Promise<{ status: number; data: Session & CommandResult }> {
  const response = await fetch(`${base}/api/rooms/${path}`, { method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify(body) });
  return { status: response.status, data: await response.json() as Session & CommandResult };
}
/** 启动已编译服务并等待 HTTP 健康检查通过。 */
async function startService(): Promise<void> {
  output = '';
  service = spawn('java', ['-jar', resolve('backend/target/party-monopoly.jar')], { env: { ...process.env, PORT: String(port), DB_URL: jdbcUrl(), DB_USERNAME: databaseConfig.user, DB_PASSWORD: databaseConfig.password, WEB_PATH: webPath, APP_ORIGIN: '' }, stdio: ['ignore', 'pipe', 'pipe'] });
  service.stdout?.on('data', chunk => { output += String(chunk); });
  service.stderr?.on('data', chunk => { output += String(chunk); });
  await waitFor(() => output.includes('Started MonopolyApplication'), '服务启动', 20000);
  assert.equal((await fetch(`${base}/api/health`)).status, 200);
}
/** 断开客户端并正常停止临时服务，允许安全读取测试快照。 */
async function stopService(): Promise<void> {
  clients.forEach(client => client.socket.disconnect());
  if (!service || service.exitCode !== null) return;
  const stopped = new Promise<void>(resolve => service!.once('exit', () => resolve()));
  service.kill('SIGTERM');
  await stopped;
  service = undefined;
}
/** 输入会话，建立独立 WebSocket 客户端并取得首个完整快照。 */
async function connect(session: Session): Promise<Client> {
  const socket = new RealtimeClient(base.replace('http:', 'ws:') + '/ws', session.token, false);
  const client: Client = { session, socket, state: null, privateData: null, privateCount: 0 };
  socket.on('room:snapshot', state => { client.state = state; });
  socket.on('player:private', state => { client.privateData = state; client.privateCount++; });
  clients.push(client);
  socket.connect();
  await waitFor(() => !!client.state && (session.playerId === null || !!client.privateData), '客户端首次同步');
  return client;
}
/** 输入客户端，主动请求最新完整公开与私人状态。 */
async function sync(client: Client): Promise<void> {
  const received = new Promise<void>((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error('状态同步超时')), 5000);
    client.socket.once('room:snapshot', () => { clearTimeout(timer); resolve(); });
  });
  client.socket.emit('room:sync');
  await received;
}
/** 输入命令或动作，走真实网络并取得服务端回执。 */
async function send(client: Client, action: GameAction, envelope?: GameCommand): Promise<CommandResult> {
  if (!envelope) await sync(client);
  const command = envelope ?? { id: randomUUID(), expectedVersion: client.state!.version, action };
  return await client.socket.request(command, 5000);
}
/** 输入动作，要求实际服务接受操作。 */
async function act(client: Client, action: GameAction): Promise<void> {
  const result = await send(client, action);
  assert.equal(result.ok, true, JSON.stringify({ action, result }));
}
/** 创建指定人数的新房间，连接、准备并由实际房主开局。 */
async function newGame(count: number): Promise<Client[]> {
  const first = await post('create', { name: `玩家1-${count}` });
  assert.equal(first.status, 200);
  const sessions = [first.data];
  for (let index = 1; index < count; index++) sessions.push((await post('join', { code: first.data.code, name: `玩家${index + 1}-${count}` })).data);
  const participants: Client[] = [];
  for (const session of sessions) participants.push(await connect(session));
  for (const client of participants) await act(client, { type: 'ready', value: true });
  await sync(participants[0]);
  const host = participants.find(client => client.session.playerId === participants[0].state!.hostId)!;
  await act(host, { type: 'start' });
  for (const client of participants) await sync(client);
  groups.set(first.data.code, sessions);
  return participants;
}
/** 输入公开地图，验证实际开局数据的固定格、区段与档位约束。 */
function verifyMap(state: PublicRoomState, count: number): void {
  assert.equal(state.board.length, 16 + count * 4);
  const quarter = state.board.length / 4;
  ['start', 'chance', 'fortune', 'jail'].forEach((kind, index) => assert.equal(state.board[index * quarter].type, kind));
  assert.equal(state.board.filter(tile => tile.type === 'chance').length, 2);
  assert.equal(state.board.filter(tile => tile.type === 'fortune').length, 2);
  assert.equal(new Set(state.board.filter(tile => tile.type === 'property').map(tile => tile.name)).size, state.board.length - 6);
  for (let side = 0; side < 4; side++) {
    const properties = state.board.filter(tile => tile.district === side && tile.type === 'property');
    assert.deepEqual([...new Set(properties.map(tile => tile.price))].sort((a, b) => a - b), [800, 1200, 1600]);
    const frequencies = [800, 1200, 1600].map(price => properties.filter(tile => tile.price === price).length);
    assert.ok(Math.max(...frequencies) - Math.min(...frequencies) <= 1);
  }
  const special = state.board.filter(tile => tile.type !== 'property').map(tile => tile.id);
  special.forEach(id => assert.ok(!special.includes((id + 1) % state.board.length)));
  const json = JSON.stringify(state);
  ['hand', 'token', 'seed', 'giveCash', 'wantCash', 'cardId', 'activeCardUsed'].forEach(key => assert.ok(!json.includes(`"${key}"`), `公开快照泄露 ${key}`));
}
/** 输入客户端，返回该角色在公开快照中的玩家信息。 */
function player(client: Client) { return client.state!.players.find(value => value.id === client.session.playerId)!; }
/** 输入候选房间，把回合固定在首位玩家，返回目标地产和自有地产夹具。 */
function prepareFixture(room: SavedRoom): { target: number; owned: number } {
  room.turnOrder = room.players.map(value => value.id);
  room.turnIndex = 0; room.turnNumber = 1; room.phase = 'pre_roll'; room.pending = null;
  room.trade = null; room.pausedReason = null; room.activeCardUsed = false; room.eventDrawn = false; room.upgradedTiles = [];
  room.players.forEach(value => { value.hand = []; value.cash = 6000; value.skipTurns = 0; value.jailed = false; value.position = 0; value.bankrupt = false; value.online = false; });
  const target = room.board.find(tile => tile.type === 'property' && tile.price === 1600 && tile.id > 1)!.id;
  const owned = room.board.find(tile => tile.type === 'property' && tile.price === 800 && tile.id !== target)!.id;
  room.players[0].position = target - 1;
  room.players[0].hand = [{ id: randomUUID(), kind: 'remote' }];
  return { target, owned };
}
/** 输入客户端与种类，返回其收到的真实私有手牌 ID。 */
function cardId(client: Client, kind: Card['kind']): string {
  const card = client.privateData?.hand.find(value => value.kind === kind);
  assert.ok(card, `缺少 ${kind} 夹具手牌`);
  return card.id;
}
/** 输入认证前探测消息，等待真实服务关闭连接并返回已收到的信封。 */
async function probe(message?: { type: string; payload?: unknown }): Promise<{ messages: { type: string; payload: CommandResult }[]; elapsed: number }> {
  const started = Date.now(); const messages: { type: string; payload: CommandResult }[] = [];
  const socket = new WebSocket(base.replace('http:', 'ws:') + '/ws');
  return await new Promise((resolve, reject) => {
    const timer = setTimeout(() => { socket.close(); reject(new Error('未认证连接未按期限关闭')); }, 8000);
    socket.addEventListener('open', () => { if (message) socket.send(JSON.stringify(message)); });
    socket.addEventListener('message', event => { messages.push(JSON.parse(String(event.data))); });
    socket.addEventListener('close', () => { clearTimeout(timer); resolve({ messages, elapsed: Date.now() - started }); });
  });
}
/** 输入测试结果文案，记录可交付的真实验证证据。 */
function passed(text: string): void { results.push(text); console.log(`✓ ${text}`); }

try {
  const available = createServer();
  await new Promise<void>(resolve => available.listen(0, '127.0.0.1', resolve));
  port = (available.address() as { port: number }).port;
  await new Promise<void>(resolve => available.close(() => resolve()));
  base = `http://127.0.0.1:${port}`;
  db = await allocateDatabase();
  await startService();
  for (const path of ['/', '/room/123456', '/watch/123456']) {
    const response = await fetch(base + path); assert.equal(response.status, 200); assert.ok((await response.text()).includes('dfw-static-smoke'));
  }
  assert.equal(await (await fetch(base + '/probe.txt')).text(), 'static-asset-ok');
  passed('Java 同端口静态资源及玩家／观战页面直接访问回退（独立 HTML 夹具，无前端 build）');
  const denied = await probe({ type: 'room:sync' });
  assert.equal(denied.messages[0].payload.code, 'FORBIDDEN');
  assert.ok(denied.messages.every(message => message.type !== 'room:snapshot' && message.type !== 'player:private'));
  const invalidIdentity = await probe({ type: 'session:authenticate', payload: { token: '00'.repeat(32) } });
  assert.equal(invalidIdentity.messages[0].payload.ok, false);
  const silent = await probe(); assert.equal(silent.messages.length, 0); assert.ok(silent.elapsed >= 5000 && silent.elapsed < 8000);
  assert.equal((await post('create', { name: null })).data.code, 'INVALID_INPUT');
  assert.equal((await post('join', { code: 'abc' })).data.code, 'INVALID_INPUT');
  passed('原生 WebSocket 首消息认证、认证前隔离、无效身份与五秒认证超时，以及 HTTP 参数兼容校验');
  const maps: string[] = [];
  let six: Client[] = [];
  for (let count = 2; count <= 6; count++) {
    const participants = await newGame(count);
    verifyMap(participants[0].state!, count);
    maps.push(JSON.stringify(participants[0].state!.board));
    if (count === 6) six = participants;
  }
  for (let seed = 0; seed < 4; seed++) {
    const participants = await newGame(2);
    verifyMap(participants[0].state!, 2);
    assert.notEqual(JSON.stringify(participants[0].state!.board), maps[0]);
  }
  passed('2～6 人地图格数、特殊格间距、区域档位与多种子随机配置');
  assert.equal((await post('join', { code: six[0].session.code })).data.code, 'GAME_STARTED');
  const full = await post('create', { name: '中文玩家🎲🧧' });
  for (let i = 1; i < 6; i++) assert.equal((await post('join', { code: full.data.code })).status, 200);
  assert.equal((await post('join', { code: full.data.code })).data.code, 'ROOM_FULL');
  const spectator = await connect((await post('spectate', { code: six[0].session.code })).data);
  assert.equal((await send(spectator, { type: 'roll' })).code, 'FORBIDDEN');
  assert.equal(spectator.privateCount, 0);
  six.forEach(client => assert.equal(client.privateData!.playerId, client.session.playerId));
  passed('满房拒绝、开局后拒绝新席位、观战操作隔离与私有消息隔离');
  const actorId = six[0].state!.turnOrder[six[0].state!.turnIndex];
  const actor = six.find(client => client.session.playerId === actorId)!;
  const receiver = six.find(client => client !== actor)!;
  const outsider = six.find(client => client !== actor && client !== receiver)!;
  assert.equal((await send(outsider, { type: 'roll' })).ok, false);
  const cashBefore = player(actor).cash;
  await act(actor, { type: 'tradeOffer', toId: receiver.session.playerId!, giveCash: 100, wantCash: 50, giveTiles: [], wantTiles: [] });
  await sync(outsider); await sync(receiver);
  assert.equal(outsider.privateData!.trade, null);
  assert.ok(receiver.privateData!.trade);
  assert.equal((await send(outsider, { type: 'tradeRespond', accept: true })).ok, false);
  await act(receiver, { type: 'tradeRespond', accept: true });
  await sync(actor);
  assert.equal(player(actor).cash, cashBefore - 50);
  await sync(actor);
  const once: GameCommand = { id: randomUUID(), expectedVersion: actor.state!.version, action: { type: 'roll' } };
  const firstRoll = await send(actor, once.action, once);
  assert.ok(firstRoll.ok);
  const rolledState = JSON.stringify(actor.state);
  assert.deepEqual(await send(actor, once.action, once), firstRoll);
  assert.equal(JSON.stringify(actor.state), rolledState);
  assert.equal((await send(actor, { type: 'endTurn' }, { ...once, id: randomUUID() })).code, 'STALE_VERSION');
  passed('回合权限、报价只向双方发送、交易资金结算、重复掷骰去重与旧版本拒绝');

  const fixtureNames = ['buy', 'upgrade', 'barrier', 'stay', 'demolish', 'swap', 'jailedSwap', 'immunity', 'rent', 'debt', 'bankrupt', 'trade', 'saveFailure', 'timeout', 'chance', 'fortune'];
  const fixtureGroups = new Map<string, Session[]>();
  for (const name of fixtureNames) {
    const participants = await newGame(2);
    fixtureGroups.set(name, participants.map(client => client.session));
  }
  await stopService();
  const [savedRows] = await db.query<RowDataPacket[]>('SELECT code,state FROM rooms');
  const saved = new Map(savedRows.map(row => [row.code as string, JSON.parse(row.state as string) as SavedRoom]));
  for (const room of saved.values()) {
    if (room.board.length) {
      const expected = generateMap(room.players.length, room.seed);
      const configuration = (board: PublicRoomState['board']) => board.map(({ id, type, name, district, price }) => ({ id, type, name, district, price }));
      assert.deepEqual(configuration(room.board), configuration(expected), 'Java 与 TypeScript 相同种子地图不一致');
    }
  }
  passed('Java 与原 TypeScript 在多个实际人数和种子下生成完全一致的地图配置');
  const changes: { code: string; state: string }[] = [];
  const fixtureTiles = new Map<string, { target: number; owned: number }>();
  for (const name of fixtureNames) {
    const room = saved.get(fixtureGroups.get(name)![0].code)!;
    const { target, owned } = prepareFixture(room);
    fixtureTiles.set(name, { target, owned });
    const first = room.players[0]; const second = room.players[1];
    if (name === 'upgrade' || name === 'stay') room.board[target].ownerId = first.id;
    if (name === 'stay') { first.position = target; first.hand = [{ id: randomUUID(), kind: 'stay' }]; }
    if (name === 'barrier') first.hand = [{ id: randomUUID(), kind: 'barrier' }, { id: randomUUID(), kind: 'swap' }];
    if (name === 'demolish') { room.board[target].ownerId = second.id; room.board[target].level = 2; first.hand = [{ id: randomUUID(), kind: 'demolish' }]; }
    if (name === 'swap' || name === 'jailedSwap') {
      first.position = 0;
      second.position = name === 'swap' ? 8 : room.board.find(tile => tile.type === 'jail')!.id;
      second.skipTurns = 1;
      second.jailed = name === 'jailedSwap';
      first.hand = [{ id: randomUUID(), kind: 'swap' }];
    }
    if (name === 'immunity' || name === 'rent') {
      room.board[target].ownerId = second.id; room.board[target].level = 2;
      if (name === 'immunity') first.hand.push({ id: randomUUID(), kind: 'immunity' });
    }
    if (name === 'debt' || name === 'bankrupt') {
      room.board[target].ownerId = second.id; room.board[target].level = name === 'debt' ? 0 : 3;
      first.cash = name === 'debt' ? 0 : 50;
      room.board[owned].ownerId = first.id; room.board[owned].level = name === 'debt' ? 1 : 0;
    }
    if (name === 'trade') { room.board[target].ownerId = first.id; room.board[target].level = 2; room.board[owned].ownerId = second.id; room.board[owned].mortgaged = true; }
    if (name === 'chance' || name === 'fortune') first.position = room.board.find(tile => tile.type === name)!.id - 1;
    room.version++;
    changes.push({ code: room.code, state: JSON.stringify(room) });
  }
  // 一次批量更新独立测试房间，不在循环中逐条查询数据库。
  await db.execute(`UPDATE rooms r JOIN JSON_TABLE(?, '$[*]' COLUMNS (
    code VARCHAR(6) PATH '$.code', state LONGTEXT PATH '$.state'
  )) fixture ON fixture.code = r.code SET r.state = fixture.state`, [JSON.stringify(changes)]);
  await db.query(`CREATE TRIGGER smoke_reject_receipt BEFORE INSERT ON receipts FOR EACH ROW
    BEGIN IF NEW.command_id = '00000000-0000-4000-8000-000000000099' THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'smoke save failure'; END IF; END`);
  await startService();
  const unicode = await connect(full.data); assert.equal(player(unicode).name, '中文玩家🎲🧧');
  passed('空 MySQL 库由 Java 自动建表，中文与四字节 emoji 保存后重启正确恢复');
  const live = new Map<string, Client[]>();
  for (const [name, sessions] of fixtureGroups) {
    const participants: Client[] = [];
    for (const session of sessions) participants.push(await connect(session));
    await sync(participants[0]);
    live.set(name, participants);
  }
  const roomPlayers = (name: string) => live.get(name)!;
  for (const name of ['buy', 'upgrade', 'stay']) {
    const [first] = roomPlayers(name);
    const kind = name === 'stay' ? 'stay' : 'remote';
    await act(first, { type: 'card', cardId: cardId(first, kind), point: 1 });
    assert.equal(first.state!.phase, name === 'buy' ? 'buy' : 'upgrade');
    const target = fixtureTiles.get(name)!.target;
    const cash = player(first).cash;
    await act(first, { type: name === 'buy' ? 'buy' : 'upgrade' });
    assert.equal(first.state!.board[target].ownerId, first.session.playerId);
    assert.equal(player(first).cash, cash - first.state!.board[target].price * (name === 'buy' ? 1 : 0.5));
    assert.equal((await send(first, { type: 'upgrade' })).ok, false);
  }
  const [builder] = roomPlayers('upgrade'); const builtId = fixtureTiles.get('upgrade')!.target;
  await act(builder, { type: 'sellBuilding', tileId: builtId });
  await act(builder, { type: 'mortgage', tileId: builtId });
  assert.ok(builder.state!.board[builtId].mortgaged);
  await act(builder, { type: 'redeem', tileId: builtId });
  assert.equal(builder.state!.board[builtId].mortgaged, false);
  await act(builder, { type: 'sellLand', tileId: builtId });
  assert.equal(builder.state!.board[builtId].ownerId, null);
  passed('实际落点购买、落点建房、停留再次结算、重复升级拒绝与卖房抵押赎回卖地');
  const [barrier] = roomPlayers('barrier'); const barrierId = fixtureTiles.get('barrier')!.target;
  await act(barrier, { type: 'card', cardId: cardId(barrier, 'barrier'), tileId: barrierId });
  assert.equal((await send(barrier, { type: 'card', cardId: cardId(barrier, 'swap'), playerId: roomPlayers('barrier')[1].session.playerId! })).ok, false);
  await act(barrier, { type: 'roll' });
  assert.equal(player(barrier).position, barrierId);
  assert.equal(barrier.state!.board[barrierId].barrier, false);
  const [demolish] = roomPlayers('demolish'); const demolishId = fixtureTiles.get('demolish')!.target;
  await act(demolish, { type: 'card', cardId: cardId(demolish, 'demolish'), tileId: demolishId });
  assert.equal(demolish.state!.board[demolishId].level, 1);
  const [swap, swapTarget] = roomPlayers('swap');
  await act(swap, { type: 'card', cardId: cardId(swap, 'swap'), playerId: swapTarget.session.playerId! });
  assert.equal(player(swap).position, 8); assert.equal(swap.state!.players[1].position, 0); assert.equal(player(swap).cash, 6000);
  const [jailedSwap, jailedTarget] = roomPlayers('jailedSwap');
  assert.equal((await send(jailedSwap, { type: 'card', cardId: cardId(jailedSwap, 'swap'), playerId: jailedTarget.session.playerId! })).ok, false);
  assert.equal(jailedSwap.privateData!.hand.length, 1);
  const [immune] = roomPlayers('immunity');
  await act(immune, { type: 'card', cardId: cardId(immune, 'remote'), point: 1 });
  assert.equal(immune.state!.phase, 'rent');
  await act(immune, { type: 'rentDecision', useCard: true });
  assert.equal(player(immune).cash, 6000); assert.equal(immune.privateData!.hand.length, 0);
  const [rent] = roomPlayers('rent'); const rentId = fixtureTiles.get('rent')!.target;
  await act(rent, { type: 'card', cardId: cardId(rent, 'remote'), point: 1 });
  const expectedRent = rent.state!.board[rentId].price * 0.1 * 6;
  assert.equal(player(rent).cash, 6000 - expectedRent); assert.equal(rent.state!.players[1].cash, 6000 + expectedRent);
  passed('六种道具、每回合主动道具限制、路障截停、防御免租与租金双边结算');
  const [debtor] = roomPlayers('debt');
  await act(debtor, { type: 'card', cardId: cardId(debtor, 'remote'), point: 1 });
  assert.equal(debtor.state!.phase, 'debt');
  assert.equal((await send(debtor, { type: 'roll' })).ok, false);
  assert.equal((await send(debtor, { type: 'redeem', tileId: fixtureTiles.get('debt')!.owned })).ok, false);
  const debtAmount = debtor.state!.pending!.amount;
  await act(debtor, { type: 'sellBuilding', tileId: fixtureTiles.get('debt')!.owned });
  assert.equal(debtor.state!.phase, 'manage'); assert.equal(player(debtor).cash, 200 - debtAmount);
  const [loser, winner] = roomPlayers('bankrupt'); const oldBoard = JSON.stringify(loser.state!.board);
  await act(loser, { type: 'card', cardId: cardId(loser, 'remote'), point: 1 });
  assert.equal(loser.state!.status, 'finished'); assert.equal(loser.state!.winnerId, winner.session.playerId);
  assert.equal(loser.state!.board[fixtureTiles.get('bankrupt')!.owned].ownerId, null);
  assert.equal(loser.state!.players[1].cash, 6450); assert.equal(loser.privateData!.hand.length, 0);
  const host = loser.state!.hostId === loser.session.playerId ? loser : winner;
  await act(host, { type: 'newGame' });
  await act(loser, { type: 'ready', value: true }); await act(winner, { type: 'ready', value: true });
  await act(host, { type: 'start' });
  assert.notEqual(JSON.stringify(host.state!.board), oldBoard);
  passed('偿债阶段权限、筹足自动支付、破产资产清算、最后一人胜利及再开新地图');
  const [trader, traderTarget] = roomPlayers('trade'); const tradeIds = fixtureTiles.get('trade')!;
  await act(trader, { type: 'tradeOffer', toId: traderTarget.session.playerId!, giveCash: 100, wantCash: 200, giveTiles: [tradeIds.target], wantTiles: [tradeIds.owned] });
  await act(traderTarget, { type: 'tradeRespond', accept: true });
  await sync(trader);
  assert.equal(trader.state!.board[tradeIds.target].ownerId, traderTarget.session.playerId);
  assert.equal(trader.state!.board[tradeIds.target].level, 2);
  assert.equal(trader.state!.board[tradeIds.owned].ownerId, trader.session.playerId);
  assert.equal(trader.state!.board[tradeIds.owned].mortgaged, true);
  const beforeFailedTrade = JSON.stringify(trader.state);
  assert.equal((await send(trader, { type: 'tradeOffer', toId: traderTarget.session.playerId!, giveCash: 999999, wantCash: 0, giveTiles: [], wantTiles: [] })).ok, false);
  assert.equal(JSON.stringify(trader.state), beforeFailedTrade);
  const [saveFailure] = roomPlayers('saveFailure');
  await sync(saveFailure);
  const failedEnvelope: GameCommand = { id: '00000000-0000-4000-8000-000000000099', expectedVersion: saveFailure.state!.version, action: { type: 'roll' } };
  const savedBefore = JSON.stringify(saveFailure.state);
  assert.equal((await send(saveFailure, failedEnvelope.action, failedEnvelope)).code, 'SAVE_FAILED');
  await sync(saveFailure);
  assert.equal(JSON.stringify(saveFailure.state), savedBefore);
  const saveTarget = roomPlayers('saveFailure')[1];
  await act(saveFailure, { type: 'tradeOffer', toId: saveTarget.session.playerId!, giveCash: 100, wantCash: 50, giveTiles: [], wantTiles: [] });
  await sync(saveFailure); await sync(saveTarget);
  const beforeAtomicFailure = JSON.stringify(saveFailure.state);
  const failedAccept: GameCommand = { id: failedEnvelope.id, expectedVersion: saveTarget.state!.version, action: { type: 'tradeRespond', accept: true } };
  assert.equal((await send(saveTarget, failedAccept.action, failedAccept)).code, 'SAVE_FAILED');
  await sync(saveFailure); await sync(saveTarget);
  assert.equal(JSON.stringify(saveFailure.state), beforeAtomicFailure);
  assert.ok(saveFailure.privateData!.trade); assert.ok(saveTarget.privateData!.trade);
  await act(saveFailure, { type: 'tradeCancel' });
  passed('带建筑和抵押地产的原子交易、资金不足零变更，以及掷骰和接受交易的数据库失败整体回滚');
  const [chance] = roomPlayers('chance'); const [fortune] = roomPlayers('fortune');
  await act(chance, { type: 'card', cardId: cardId(chance, 'remote'), point: 1 });
  await act(fortune, { type: 'card', cardId: cardId(fortune, 'remote'), point: 1 });
  assert.ok(['buy', 'upgrade', 'manage', 'rent', 'debt'].includes(chance.state!.phase));
  assert.equal(fortune.state!.phase, 'manage');
  passed('机会和财富落点的真实随机事件结算');

  const [offline, online] = roomPlayers('timeout');
  await act(offline, { type: 'tradeOffer', toId: online.session.playerId!, giveCash: 1, wantCash: 0, giveTiles: [], wantTiles: [] });
  online.socket.disconnect();
  await waitFor(() => !!offline.state?.pausedReason, '交易接收人断线暂停');
  const replacement = await connect(online.session);
  await waitFor(() => !offline.state?.pausedReason, '交易重连继续');
  assert.ok(replacement.privateData?.trade);
  await act(replacement, { type: 'tradeRespond', accept: false });
  const replaced = await connect(offline.session);
  await waitFor(() => !offline.socket.connected, '旧连接替换');
  assert.equal(player(replaced).online, true);
  await sync(replaced);
  const restartEnvelope: GameCommand = { id: randomUUID(), expectedVersion: replaced.state!.version, action: { type: 'tradeOffer', toId: replacement.session.playerId!, giveCash: 3, wantCash: 0, giveTiles: [], wantTiles: [] } };
  const restartReceipt = await send(replaced, restartEnvelope.action, restartEnvelope);
  assert.ok(restartReceipt.ok);
  const isolationVersion = chance.state!.version;
  await sync(chance); assert.equal(chance.state!.version, isolationVersion);
  await stopService();
  await startService();
  const resumedActor = await connect(offline.session);
  assert.ok(resumedActor.state!.pausedReason);
  const resumedTarget = await connect(online.session);
  await sync(resumedActor);
  assert.equal(resumedActor.state!.pausedReason, null);
  assert.ok(resumedActor.privateData!.trade);
  assert.ok(resumedActor.privateData!.trade!.remainingMs > 50000);
  assert.deepEqual(await send(resumedActor, restartEnvelope.action, restartEnvelope), restartReceipt);
  await act(resumedActor, { type: 'tradeCancel' });
  resumedActor.socket.disconnect();
  await waitFor(() => resumedTarget.state?.hostId === resumedTarget.session.playerId && !!resumedTarget.state?.pausedReason, '房主移交及行动者离线暂停');
  await act(resumedTarget, { type: 'removePlayer', playerId: resumedActor.session.playerId! });
  assert.equal(resumedTarget.state!.status, 'finished');
  assert.equal(resumedTarget.state!.winnerId, resumedTarget.session.playerId);
  passed('房间隔离、刷新替换连接、交易离线冻结、重启恢复报价和命令回执、房主移交及移出清算');

  await stopService();
  const [timeoutRows] = await db.execute<RowDataPacket[]>('SELECT state FROM rooms WHERE code = ?', [fixtureGroups.get('buy')![0].code]);
  const timeoutRoom = JSON.parse(timeoutRows[0].state as string) as SavedRoom;
  timeoutRoom.phase = 'manage'; timeoutRoom.pending = null;
  timeoutRoom.trade = { id: randomUUID(), fromId: timeoutRoom.players[0].id, toId: timeoutRoom.players[1].id, giveCash: 1, wantCash: 0, giveTiles: [], wantTiles: [], remainingMs: 150, runningSince: null };
  await db.execute('UPDATE rooms SET state = ? WHERE code = ?', [JSON.stringify(timeoutRoom), timeoutRoom.code]);
  await startService();
  const timeoutFirst = await connect(fixtureGroups.get('buy')![0]);
  await connect(fixtureGroups.get('buy')![1]);
  await waitFor(() => timeoutFirst.state?.tradePending === false, '交易到期自动取消');
  passed('交易正常连接时到期自动取消');
  // SQLite 源库只读一致性备份，转换到第二个临时 MySQL 库，原库不受影响。
  if (existsSync('data/game.sqlite')) {
    await stopService(); await db.end(); db = await allocateDatabase(true);
    const legacy = await readLegacy(resolve('data/game.sqlite'));
    const imported = await importLegacy(db, legacy);
    assert.deepEqual(imported, { rooms: legacy.rooms.length, sessions: legacy.sessions.length, receipts: legacy.receipts.length });
    const [restoredRows] = await db.query<RowDataPacket[]>('SELECT code,state FROM rooms');
    assert.deepEqual(new Map(restoredRows.map(row => [row.code, row.state])), new Map(legacy.rooms.map(row => [row.code, row.state])));
    const [restoredSessions] = await db.query<RowDataPacket[]>('SELECT token_hash,code,player_id FROM sessions');
    assert.deepEqual(new Map(restoredSessions.map(row => [row.token_hash, [row.code, row.player_id]])), new Map(legacy.sessions.map(row => [row.token_hash, [row.code, row.player_id]])));
    const [restoredReceipts] = await db.query<RowDataPacket[]>('SELECT code,command_id,actor_id,result FROM receipts');
    assert.deepEqual(new Map(restoredReceipts.map(row => [row.code + ':' + row.command_id, [row.actor_id, row.result]])), new Map(legacy.receipts.map(row => [row.code + ':' + row.command_id, [row.actor_id, row.result]])));
    await assert.rejects(importLegacy(db, legacy), /目标库已有/);
    assert.deepEqual(await rowCounts(db), imported);
    const restored = legacy.rooms.find(row => (JSON.parse(row.state) as SavedRoom).board.length > 0) ?? legacy.rooms[0];
    if (restored) {
      const previous = JSON.parse(restored.state) as SavedRoom;
      const token = 'ab'.repeat(32); const playerId = previous.players[0].id;
      // 仅在临时 MySQL 副本建立已知令牌，以验证摘要身份兼容。
      await db.execute('INSERT INTO sessions(token_hash,code,player_id) VALUES (?,?,?) ON DUPLICATE KEY UPDATE code=?,player_id=?', [createHash('sha256').update(token).digest('hex'), previous.code, playerId, previous.code, playerId]);
      await startService();
      const restoredClient = await connect({ code: previous.code, token, playerId });
      assert.deepEqual(restoredClient.state!.board, previous.board);
      assert.equal(restoredClient.state!.gameId, previous.gameId);
      assert.deepEqual(restoredClient.privateData!.hand, previous.players[0].hand);
      assert.equal(player(restoredClient).cash, previous.players[0].cash);
      assert.equal(player(restoredClient).position, previous.players[0].position);
      assert.deepEqual(restoredClient.state!.pending, previous.pending);
      const viewer = await connect((await post('spectate', { code: previous.code })).data);
      assert.equal(viewer.privateCount, 0);
    }
    passed('SQLite 到 MySQL 导入的三表内容一致、非空库拒绝覆盖、旧身份和对局恢复');
  }
  console.log(`\n${results.length} 组集成场景全部通过（仅临时数据库）。`);
} finally {
  try { await stopService(); } finally {
    await db?.end();
    if (admin) {
      try { for (const name of ownedDatabases) await admin.query(`DROP DATABASE ${identifier(name)}`); }
      finally { await admin.end(); }
    }
    rmSync(temporary, { recursive: true, force: true });
  }
}
