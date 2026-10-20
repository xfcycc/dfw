import { afterEach, expect, it, vi } from 'vitest';
import { createApp, nextTick, type App } from 'vue';
import { createPinia } from 'pinia';
import { createRouter, createMemoryHistory } from 'vue-router';
import RoomView from '../src/web/views/RoomView.vue';
import { useGameStore, rememberSession } from '../src/web/stores/game';
import type { PublicRoomState } from '../src/shared/types';

let app: App | undefined;
let width = 1440,
  height = 900;
const queries: {
  media: string;
  matches: boolean;
  listeners: Set<(e: { matches: boolean }) => void>;
}[] = [];
function matches(media: string) {
  return media
    .split(',')
    .some((part) =>
      [...part.matchAll(/max-(width|height): (\d+)px/g)].every(
        ([, axis, value]) =>
          (axis === 'width' ? width : height) <= Number(value),
      ),
    );
}
function resize(w: number, h: number) {
  width = w;
  height = h;
  for (const q of queries) {
    const next = matches(q.media);
    if (next !== q.matches) {
      q.matches = next;
      q.listeners.forEach((fn) => fn({ matches: next }));
    }
  }
}
async function flush() {
  for (let i = 0; i < 6; i++) await nextTick();
}
function button(label: string) {
  const el = [...document.querySelectorAll<HTMLButtonElement>('button')].find(
    (el) =>
      el.getAttribute('aria-label') === label ||
      el.textContent?.trim() === label,
  );
  if (!el) throw new Error(`Missing button ${label}`);
  return el;
}
async function mount(layout = 'classic') {
  queries.length = 0;
  resize(1440, 900);
  localStorage.clear();
  localStorage.setItem('dfw:room-layout', layout);
  vi.stubGlobal('matchMedia', (media: string) => {
    const q = {
      media,
      matches: matches(media),
      listeners: new Set<(e: { matches: boolean }) => void>(),
    };
    queries.push(q);
    return {
      get matches() {
        return q.matches;
      },
      media,
      addEventListener: (_: string, fn: (e: { matches: boolean }) => void) =>
        q.listeners.add(fn),
      removeEventListener: (_: string, fn: (e: { matches: boolean }) => void) =>
        q.listeners.delete(fn),
    };
  });
  window.matchMedia = globalThis.matchMedia;
  const pinia = createPinia();
  const game = useGameStore(pinia);
  const state: PublicRoomState = {
    code: '123456',
    roomId: 'layout-test',
    version: 1,
    status: 'playing',
    hostId: 'a',
    gameId: 'g',
    players: ['a', 'b'].map((id, i) => ({
      id,
      name: id,
      color: '#fff',
      cash: 12000,
      position: i,
      ready: true,
      online: true,
      bankrupt: false,
      skipTurns: 0,
      jailed: false,
      eliminationOrder: null,
    })),
    board: Array.from({ length: 32 }, (_, id) => ({
      id,
      type: id === 0 ? ('start' as const) : ('property' as const),
      name: `地块${id}`,
      district: Math.floor(id / 8),
      price: 800,
      ownerId: id === 1 ? 'a' : id === 2 ? 'b' : null,
      level: 1,
      mortgaged: false,
      barrier: false,
    })),
    turnOrder: ['a', 'b'],
    turnIndex: 0,
    turnNumber: 1,
    phase: 'pre_roll',
    pending: null,
    tradePending: false,
    pausedReason: null,
    dice: null,
    motion: null,
    winnerId: null,
    logs: [],
  };
  const session = {
    code: '123456',
    token: 'fixture',
    playerId: 'a',
    roomId: 'layout-test',
  };
  rememberSession(session);
  game.connect = () => {
    game.session = session;
    game.room = structuredClone(state);
    game.privateData = {
      playerId: 'a',
      hand: [{ id: 'barrier1', kind: 'barrier', name: '路障测试' }],
      trade: null,
      activeCardUsed: false,
    };
    game.connection = 'online';
  };
  const act = vi.spyOn(game, 'act').mockResolvedValue(true);
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: { template: '<div />' } },
      { path: '/room/:code', component: RoomView },
    ],
  });
  await router.push('/room/123456');
  await router.isReady();
  const errors: unknown[] = [];
  document.body.innerHTML = '<div id="test-app"></div>';
  app = createApp(RoomView).use(pinia).use(router);
  app.config.errorHandler = (e) => errors.push(e);
  app.mount('#test-app');
  await flush();
  return { game, act, errors };
}
afterEach(() => {
  app?.unmount();
  app = undefined;
  document.body.innerHTML = '';
  vi.restoreAllMocks();
});
it('keeps the room usable across layout switches and responsive boundaries without commands', async () => {
  const { act, errors } = await mount();
  for (const [w, h] of [
    [900, 900],
    [390, 844],
    [1440, 900],
    [900, 900],
    [390, 844],
    [1440, 900],
  ]) {
    resize(w, h);
    await flush();
    button('切换为单屏布局').click();
    await flush();
    button('切换为经典布局').click();
    await flush();
    expect(errors).toEqual([]);
    expect(document.querySelector('textarea')?.isConnected).toBe(true);
  }
  expect(act).not.toHaveBeenCalled();
});

it('preserves chat, a selected card and map target through reparenting', async () => {
  const { act, errors } = await mount();
  const chat = document.querySelector('textarea')!;
  chat.value = '未发送的聊天';
  chat.dispatchEvent(new Event('input', { bubbles: true }));
  button('使用卡牌 路障测试').click();
  await flush();
  document
    .querySelector<HTMLButtonElement>('.target-option:not(:disabled)')!
    .click();
  await flush();
  const selected = document
    .querySelector('.target-option[aria-pressed="true"]')!
    .getAttribute('aria-label');
  button('地图点选 ↗').click();
  await flush();
  for (const [w, h] of [
    [900, 900],
    [390, 844],
    [1440, 900],
  ]) {
    resize(w, h);
    await flush();
    button('切换为单屏布局').click();
    await flush();
    button('切换为经典布局').click();
    await flush();
  }
  expect(document.querySelector('textarea')).toBe(chat);
  expect(chat.value).toBe('未发送的聊天');
  expect(
    document
      .querySelector('.target-option[aria-pressed="true"]')
      ?.getAttribute('aria-label'),
  ).toBe(selected);
  expect(document.body.textContent).toContain('继续使用：路障测试');
  expect(act).not.toHaveBeenCalled();
  expect(errors).toEqual([]);
});

it('preserves quote amounts and property selections across resize and closing to switch layout', async () => {
  const { act, errors } = await mount();
  button('交易').click();
  await flush();
  const dialog = document.querySelector('[aria-label="交易报价与成交后结果"]')!;
  const cash = dialog.querySelector<HTMLInputElement>('input[type=number]')!;
  cash.value = '120';
  cash.dispatchEvent(new Event('input', { bubbles: true }));
  dialog.querySelector<HTMLInputElement>('input[type=checkbox]')!.click();
  await flush();
  resize(390, 844);
  await flush();
  resize(900, 900);
  await flush();
  dialog.querySelector<HTMLButtonElement>('[aria-label="关闭弹窗"]')!.click();
  await flush();
  button('切换为单屏布局').click();
  await flush();
  button('更多').click();
  await flush();
  // Both the desktop action and the mobile menu have the same label.
  document
    .querySelector<HTMLButtonElement>('.mobile-more-grid [aria-label="交易"]')!
    .click();
  await flush();
  expect(
    dialog.querySelector<HTMLInputElement>('input[type=number]')!.value,
  ).toBe('120');
  expect(
    dialog.querySelector<HTMLInputElement>('input[type=checkbox]')!.checked,
  ).toBe(true);
  expect(act).not.toHaveBeenCalled();
  expect(errors).toEqual([]);
});

it('keeps pending hand choices while changing layout without auto-confirming', async () => {
  const { game, act, errors } = await mount();
  game.room!.pending = {
    id: 'choice1',
    type: 'choice',
    playerId: 'a',
  } as PublicRoomState['pending'];
  game.privateData!.choice = {
    id: 'choice1',
    type: 'hand_limit',
    tileIds: [],
    cards: game.privateData!.hand,
    cardId: null,
    content: null,
    skipTurns: 0,
    jailed: false,
    discardCash: 100,
    handLimit: 1,
  };
  await flush();
  const checkbox = document.querySelector<HTMLInputElement>(
    '.choice-card-grid input[type=checkbox]',
  )!;
  checkbox.click();
  await flush();
  for (const [w, h] of [
    [900, 900],
    [390, 844],
    [1440, 900],
  ]) {
    resize(w, h);
    await flush();
    button('切换为单屏布局').click();
    await flush();
    button('切换为经典布局').click();
    await flush();
    expect(checkbox.isConnected).toBe(true);
    expect(checkbox.checked).toBe(true);
  }
  expect(game.room!.pending?.id).toBe('choice1');
  expect(act).not.toHaveBeenCalled();
  expect(errors).toEqual([]);
});

it('retains an unacknowledged card reveal across both presentation modes', async () => {
  const { game, act, errors } = await mount();
  game.cardReveals = [
    {
      key: 'reveal1',
      content: { id: 'cash1', kind: 'cash', pool: 'card', name: '补给测试' },
      mode: 'draw',
      actor: 'a',
    },
  ];
  await flush();
  for (const [w, h] of [
    [390, 844],
    [900, 900],
    [1440, 900],
  ]) {
    resize(w, h);
    await flush();
    button('切换为单屏布局').click();
    await flush();
    button('切换为经典布局').click();
    await flush();
    expect(game.cardReveals.map((card) => card.key)).toEqual(['reveal1']);
    expect(document.querySelector('.card-reveal-panel')?.isConnected).toBe(
      true,
    );
  }
  expect(act).not.toHaveBeenCalled();
  expect(errors).toEqual([]);
});
