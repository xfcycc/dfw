<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { readSession, recentRooms, useGameStore } from '../stores/game';
import { selectArchiveCode } from '../archive';
import Modal from '../components/Modal.vue';
const game = useGameStore();
const router = useRouter();
const mode = ref<'create' | 'join' | 'spectate'>('create');
const name = ref(localStorage.getItem('dfw:nickname') || '');
const code = ref('');
const loading = ref(false);
const error = ref('');
const entryOpen = ref(false);
const recent = recentRooms().slice(0, 3);
onMounted(() => { game.disconnect(); });
/** 输入房间入口模式，清除提示并打开对应表单，无返回值。 */
function openEntry(value: typeof mode.value): void {
  mode.value = value;
  error.value = '';
  entryOpen.value = true;
}
/** 关闭入口表单；提交期间保持弹窗打开，无入参和返回值。 */
function closeEntry(): void {
  if (!loading.value) entryOpen.value = false;
}
/** 按当前模式进入房间，优先恢复本浏览器已有玩家席位。 */
async function submit(): Promise<void> {
  loading.value = true; error.value = '';
  try {
    localStorage.setItem('dfw:nickname', name.value);
    const existing = mode.value === 'join' ? readSession(code.value) : null;
    const session = existing ?? await game.enter(mode.value, code.value, name.value);
    await router.push(`/${mode.value === 'spectate' ? 'watch' : 'room'}/${session.code}`);
  } catch (cause) { error.value = cause instanceof Error ? cause.message : '进入失败，请重试'; }
  finally { loading.value = false; }
}
</script>
<template>
  <div class="home-page">
    <img class="home-background" src="/images/home-party.png" alt="" fetchpriority="high" />
    <header class="home-toolbar">
      <RouterLink class="home-history" to="/archives">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M3 11a9 9 0 1 1 2.6 7.4M3 5v6h6M12 7v5l3 2" /></svg>
        历史与恢复
      </RouterLink>
    </header>
    <main class="home-menu">
      <h1 class="home-title">大富翁</h1>
      <nav class="home-actions" aria-label="游戏入口">
        <button class="home-action home-create" @click="openEntry('create')">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true"><rect x="3" y="3" width="18" height="18" rx="5"/><g fill="currentColor" stroke="none"><circle cx="8" cy="8" r="1.4"/><circle cx="16" cy="8" r="1.4"/><circle cx="12" cy="12" r="1.4"/><circle cx="8" cy="16" r="1.4"/><circle cx="16" cy="16" r="1.4"/></g></svg>
          创建房间<span class="home-action-arrow" aria-hidden="true">→</span>
        </button>
        <div class="home-secondary-actions">
          <button class="home-action home-join" @click="openEntry('join')">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M14 4h5a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-5M3 12h12m-4-4 4 4-4 4"/></svg>
            加入房间
          </button>
          <button class="home-action home-watch" @click="openEntry('spectate')">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="3" y="4" width="18" height="13" rx="3"/><path d="M8 21h8m-4-4v4"/></svg>
            投屏观战
          </button>
        </div>
      </nav>
      <nav v-if="recent.length" class="home-recent" aria-label="最近房间">
        <RouterLink v-for="room in recent" :key="room.roomId" :to="readSession(room.code)?.playerId ? `/room/${room.code}` : '/archives'" @click="selectArchiveCode(room.archiveCode)">{{ readSession(room.code)?.playerId ? '继续房间' : '查看存档' }} <strong>{{ room.code }}</strong><span aria-hidden="true">↗</span></RouterLink>
      </nav>
    </main>
    <Modal v-if="entryOpen" :title="mode === 'create' ? '创建房间' : mode === 'join' ? '加入房间' : '投屏观战'" @close="closeEntry">
      <form class="home-entry-form" :aria-busy="loading" @submit.prevent="submit">
        <label v-if="mode !== 'spectate'" class="field">你的昵称 <span class="optional">选填</span><input v-model="name" placeholder="今天的财富新星" autocomplete="nickname" :disabled="loading" /></label>
        <label v-if="mode !== 'create'" class="field">房间号<input v-model="code" class="room-code-input" placeholder="输入 6 位房间号" inputmode="numeric" pattern="[1-9][0-9]{5}" maxlength="6" required :disabled="loading" /></label>
        <p v-if="error" class="error-text" role="alert">{{ error }}</p>
        <button class="home-action home-create" :disabled="loading">{{ loading ? '正在进入…' : mode === 'create' ? '创建房间' : mode === 'join' ? '加入房间' : '打开观战' }}<span class="home-action-arrow" aria-hidden="true">→</span></button>
      </form>
    </Modal>
  </div>
</template>

<style scoped>
.home-page{position:relative;isolation:isolate;min-height:100svh;max-width:none;margin:0;overflow:hidden;background:#72b8ed;color:#fff8e6}
.home-background{position:absolute;inset:0;width:100%;height:100%;object-fit:cover;object-position:center top;z-index:-2;pointer-events:none}
.home-page::before{content:'';position:absolute;inset:0;z-index:-1;pointer-events:none;background:linear-gradient(180deg,#174f8920,transparent 45%)}
.home-toolbar{position:absolute;top:24px;right:30px;z-index:1}
.home-history{display:flex;align-items:center;gap:8px;padding:11px 17px;border:1px solid #ffffffa6;border-radius:30px;background:#174f893b;backdrop-filter:blur(12px);box-shadow:0 3px 12px #174f8914;font-size:12px;font-weight:600;transition:background .18s,transform .18s}
.home-history svg{width:18px;height:18px}
.home-history:hover{background:#174f896b;transform:translateY(-2px)}
.home-menu{display:flex;flex-direction:column;align-items:center;padding:max(41svh,23vw) 24px 32px;text-align:center}
.home-title{position:absolute;width:1px;height:1px;padding:0;margin:-1px;overflow:hidden;clip-path:inset(50%);white-space:nowrap}
.home-actions{display:flex;flex-direction:column;gap:12px;width:min(100%,328px)}
.home-action{display:flex;align-items:center;justify-content:center;gap:10px;min-height:52px;width:100%;border:1px solid #fff8d9;border-radius:16px;padding:12px 18px;font-size:14px;font-weight:800;letter-spacing:1px;transition:transform .18s,filter .18s;position:relative}
.home-action svg{width:22px;height:22px;flex-shrink:0}
.home-action:not(:disabled):hover{transform:translateY(-3px);filter:brightness(1.05)}
.home-action:not(:disabled):active{transform:translateY(2px)}
.home-create{min-height:62px;background:linear-gradient(180deg,#fff2ad,#ffd365);color:#855021;box-shadow:inset 0 2px 0 #fff9dc,0 5px 0 #c58a3c,0 9px 0 #ffffff80,0 12px 25px #235f8930;font-size:20px;border-width:2px}
.home-create svg{width:27px;height:27px;transform:rotate(-10deg)}
.home-action-arrow{position:absolute;right:19px;font-size:22px;font-weight:500}
.home-secondary-actions{display:grid;grid-template-columns:1fr 1fr;gap:12px;margin-top:4px}
.home-join{background:linear-gradient(180deg,#fffdf0,#ffebc7);color:#8a6037;box-shadow:inset 0 2px 0 #fff,0 4px 0 #c69a60,0 8px 16px #235f8926}
.home-watch{background:linear-gradient(180deg,#dff6ff,#bce6fc);color:#356996;border-color:#f0fbff;box-shadow:inset 0 2px 0 #fff,0 4px 0 #5e9fcc,0 8px 16px #235f8926}
.home-recent{display:flex;flex-wrap:wrap;justify-content:center;gap:8px;max-width:500px;margin-top:24px}
.home-recent a{display:flex;align-items:center;gap:6px;padding:7px 12px;border:1px solid #ffffff94;border-radius:20px;background:#fff9e6d9;color:#77552f;backdrop-filter:blur(8px);box-shadow:0 3px 10px #235f891c;font-size:11px;transition:background .18s}
.home-recent a:hover{background:#fff9e6}
.home-recent strong{font-size:11px;letter-spacing:.5px}
.home-page :is(button,a):focus-visible,.home-entry-form :is(button,input):focus-visible{outline:3px solid #fff3b3;outline-offset:5px;box-shadow:0 0 0 7px #356996}
.home-entry-form .field{color:#785936}
.home-entry-form .field input{background:#fffcf4;border-color:#e5d8bc;color:#665039}
.home-entry-form .field input:focus{background:#fff;border-color:#c69a60}
.home-entry-form .home-action{margin:28px 0 10px;min-height:54px;font-size:16px}
@media(min-aspect-ratio:2/1){.home-create{min-height:54px}.home-action{padding-top:10px;padding-bottom:10px}}
@media(max-width:600px) and (orientation:portrait){.home-page{background:linear-gradient(180deg,#72b8ed,#174a70 65%)}.home-background{height:60svh;object-position:center top;mask-image:linear-gradient(#000 80%,transparent)}.home-toolbar{top:18px;right:18px}.home-history{padding:9px 13px;font-size:11px}.home-menu{padding:58svh 20px 32px}.home-actions{width:min(100%,300px)}.home-recent{max-width:320px;margin-top:24px}.home-page::before{background:none}}
@media(max-height:600px) and (orientation:landscape){.home-action{min-height:44px;padding:8px 14px}.home-create{min-height:48px;font-size:18px}.home-actions{gap:8px}.home-recent{margin-top:16px}}
</style>
