<script setup lang="ts">
import { imageUrl } from '../image-assets';
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { forgetRecentRoom, readSession, recentRooms, useGameStore } from '../stores/game';
import { selectArchiveCode } from '../archive';
import Modal from '../components/Modal.vue';
import AudioSettings from '../components/AudioSettings.vue';
import Archives from './Archives.vue';
const game = useGameStore();
const router = useRouter();
const mode = ref<'create' | 'join' | 'spectate'>('create');
const name = ref(localStorage.getItem('dfw:nickname') || '');
const code = ref('');
const loading = ref(false);
const error = ref('');
const entryOpen = ref(false);
const archivesOpen = ref(false);
const archiveRestoring = ref(false);
const recent = ref(recentRooms().slice(0, 3));
onMounted(() => { game.disconnect(); });
/** 输入永久房间标识，删除本地历史并刷新最近三个房间，无返回值。 */
function removeRecent(roomId: string): void {
  forgetRecentRoom(roomId);
  recent.value = recentRooms().slice(0, 3);
}
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
    <img class="home-background" :src="imageUrl('/images/home-town.png')" alt="" fetchpriority="high" />
    <header class="home-toolbar">
      <AudioSettings class="home-tool-button" />
      <button class="home-tool-button home-history" aria-haspopup="dialog" @click="archivesOpen = true">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M3 11a9 9 0 1 1 2.6 7.4M3 5v6h6M12 7v5l3 2" /></svg>
        历史与恢复
      </button>
    </header>
    <main class="home-menu">
      <h1 class="home-title"><img :src="imageUrl('/images/home-title.png')" alt="大富翁-奇迹大陆" width="1774" height="887" fetchpriority="high" /></h1>
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
        <div v-for="room in recent" :key="room.roomId" class="home-recent-room">
          <RouterLink :to="readSession(room.code)?.playerId ? `/room/${room.code}` : '/archives'" @click="selectArchiveCode(room.archiveCode)">{{ readSession(room.code)?.playerId ? '继续房间' : '查看存档' }} <strong>{{ room.code }}</strong><span aria-hidden="true">↗</span></RouterLink>
          <button type="button" class="home-recent-remove" :aria-label="`删除房间 ${room.code} 的本地历史`" title="删除本地历史" @click.stop="removeRecent(room.roomId)"><span aria-hidden="true">×</span></button>
        </div>
      </nav>
    </main>
    <Modal v-if="archivesOpen" title="历史与恢复" panel-class="home-archive-modal" :close-disabled="archiveRestoring" @close="archivesOpen = false">
      <Archives embedded @restoring="archiveRestoring = $event" />
    </Modal>
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
.home-page{position:relative;isolation:isolate;min-height:100svh;max-width:none;margin:0;overflow:hidden;background:#e3edcd;color:#45644e}
.home-background{position:absolute;inset:0;width:100%;height:100%;object-fit:cover;object-position:center;z-index:-2;pointer-events:none}
.home-page::before{content:'';position:absolute;inset:0;z-index:-1;pointer-events:none;background:linear-gradient(180deg,#fff9e51a,transparent 45%)}
.home-toolbar{position:absolute;top:24px;right:30px;z-index:1;display:flex;align-items:center;gap:8px}
.home-toolbar :deep(.home-tool-button){display:flex;align-items:center;justify-content:center;gap:6px;width:112px;min-height:40px;padding:9px 8px;border:1px solid #fffdf0;border-radius:30px;background:#fff9e6d9;color:#45644e;backdrop-filter:blur(12px);box-shadow:0 3px 12px #45644e14;font-size:11px;font-weight:600;white-space:nowrap;transition:background .18s,transform .18s}
.home-history svg{width:18px;height:18px}
.home-toolbar :deep(.home-tool-button:hover){background:#fffdf0;transform:translateY(-2px)}
.home-menu{display:flex;flex-direction:column;align-items:center;min-height:100svh;padding:22svh 24px 40px;text-align:center}
.home-title{width:min(70vw,420px);margin:0 0 clamp(28px,5svh,48px)}
.home-title img{display:block;width:100%;height:auto}
.home-actions{display:flex;flex-direction:column;gap:12px;width:min(100%,328px)}
.home-action{display:flex;align-items:center;justify-content:center;gap:10px;min-height:52px;width:100%;border:1px solid #fff8d9;border-radius:16px;padding:12px 18px;font-size:14px;font-weight:800;letter-spacing:1px;transition:transform .18s,filter .18s;position:relative}
.home-action svg{width:22px;height:22px;flex-shrink:0}
.home-action:not(:disabled):hover{transform:translateY(-3px);filter:brightness(1.05)}
.home-action:not(:disabled):active{transform:translateY(2px)}
.home-create{min-height:62px;background:linear-gradient(180deg,#fff2bc,#f3cd7f);color:#855021;box-shadow:inset 0 2px 0 #fff9dc,0 5px 0 #c58a3c,0 9px 0 #ffffff80,0 12px 25px #45644e26;font-size:20px;border-width:2px}
.home-create svg{width:27px;height:27px;transform:rotate(-10deg)}
.home-action-arrow{position:absolute;right:19px;font-size:22px;font-weight:500}
.home-secondary-actions{display:grid;grid-template-columns:1fr 1fr;gap:12px;margin-top:4px}
.home-join{background:linear-gradient(180deg,#fffdf0,#ffebc7);color:#8a6037;box-shadow:inset 0 2px 0 #fff,0 4px 0 #c69a60,0 8px 16px #45644e26}
.home-watch{background:linear-gradient(180deg,#eef6df,#d3e7bb);color:#4f714b;border-color:#f5fbe9;box-shadow:inset 0 2px 0 #fff,0 4px 0 #8fa86c,0 8px 16px #45644e26}
.home-recent{display:flex;flex-wrap:wrap;justify-content:center;gap:8px;max-width:500px;margin-top:24px}
.home-recent-room{display:flex;align-items:center;border:1px solid #ffffff94;border-radius:20px;background:#fff9e6d9;backdrop-filter:blur(8px);box-shadow:0 3px 10px #235f891c;overflow:hidden}
.home-recent a{display:flex;align-items:center;gap:6px;padding:7px 8px 7px 12px;color:#77552f;font-size:11px;transition:background .18s}
.home-recent a:hover{background:#fff9e6}
.home-recent-remove{display:flex;align-items:center;justify-content:center;align-self:stretch;min-width:28px;padding:0 8px 0 4px;border:0;background:transparent;color:#77552f;font-size:18px;line-height:1;transition:background .18s}
.home-recent-remove:hover{background:#f5dfc2}
.home-recent-room:focus-within{overflow:visible}
.home-recent strong{font-size:11px;letter-spacing:.5px}
.home-page :is(button,a):focus-visible,.home-entry-form :is(button,input):focus-visible{outline:3px solid #fff3b3;outline-offset:5px;box-shadow:0 0 0 7px #356996}
.home-entry-form .field{color:#785936}
.home-entry-form .field input{background:#fffcf4;border-color:#e5d8bc;color:#665039}
.home-entry-form .field input:focus{background:#fff;border-color:#c69a60}
.home-entry-form .home-action{margin:28px 0 10px;min-height:54px;font-size:16px}
@media(min-aspect-ratio:2/1){.home-create{min-height:54px}.home-action{padding-top:10px;padding-bottom:10px}}
@media(max-width:600px) and (orientation:portrait){.home-page{background:linear-gradient(180deg,#f5efdc,#e3edcd 60%)}.home-background{height:52svh;object-position:center;mask-image:linear-gradient(#000 65%,transparent)}.home-toolbar{top:18px;right:18px}.home-menu{padding:34svh 20px 32px}.home-title{width:min(80vw,300px);margin-bottom:28px}.home-actions{width:min(100%,300px)}.home-recent{max-width:320px;margin-top:24px}.home-page::before{background:none}}
@media(max-height:600px) and (orientation:landscape){.home-menu{padding-top:16svh}.home-title{width:260px;margin-bottom:24px}.home-action{min-height:44px;padding:8px 14px}.home-create{min-height:48px;font-size:18px}.home-actions{gap:8px}.home-recent{margin-top:16px}}
</style>
