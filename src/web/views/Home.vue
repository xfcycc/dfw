<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { readSession, useGameStore } from '../stores/game';
import Logo from '../components/Logo.vue';
import City from '../components/City.vue';
const game = useGameStore();
const router = useRouter();
const mode = ref<'create' | 'join' | 'spectate'>('create');
const name = ref(localStorage.getItem('dfw:nickname') || '');
const code = ref('');
const loading = ref(false);
const error = ref('');
onMounted(() => { game.disconnect(); });
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
    <header class="site-header"><Logo /><span class="header-note"><span class="status-dot"></span>好朋友，好运气</span></header>
    <main class="home-main">
      <section class="home-story">
        <div class="pill">🎲 2～6 人在线 · 每局都是新地图</div>
        <h1>下一位大富翁，<br /><span>就是你。</span></h1>
        <p>掷一颗骰子，买一条街。<br />把朋友叫上，一起开启这场财富派对。</p>
        <div class="home-art"><City /><span class="floating-card card-one">⌂<small>购入地产</small></span><span class="floating-card card-two">+ ¥1,000<small>好运正在路上</small></span><span class="floating-die">⚄</span></div>
        <div class="feature-row"><span><i>↗</i>随机城市</span><span><i>⇄</i>实时联机</span><span><i>▣</i>聚会投屏</span></div>
      </section>
      <section class="entry-card">
        <div class="entry-icon">🎉</div><h2>朋友就位，派对开始</h2><p class="muted">一个房间号，就能把大家聚在一起。</p>
        <div class="tabs"><button v-for="[value, label] in [['create', '创建房间'], ['join', '加入房间'], ['spectate', '投屏观战']]" :key="value" :class="{ active: mode === value }" @click="mode = value as typeof mode; error = ''">{{ label }}</button></div>
        <form @submit.prevent="submit">
          <label v-if="mode !== 'spectate'" class="field">你的昵称 <span class="optional">选填</span><input v-model="name" placeholder="今天的财富新星" autocomplete="nickname" /></label>
          <label v-if="mode !== 'create'" class="field">房间号<input v-model="code" class="room-code-input" placeholder="输入 6 位房间号" inputmode="numeric" pattern="[1-9][0-9]{5}" maxlength="6" required /></label>
          <div v-if="mode === 'create'" class="entry-tip"><span>✦</span><div><strong>你的城市，你的主场</strong><p>邀请朋友加入，大家准备后即可开局。<br />不需要注册，刷新也能回到原座位。</p></div></div>
          <div v-else class="entry-tip"><span>{{ mode === 'join' ? '↗' : '▣' }}</span><div><strong>{{ mode === 'join' ? '找到你的朋友' : '把派对放到大屏上' }}</strong><p>{{ mode === 'join' ? '向房主索取房间号，即可加入。' : '只显示公开局面，所有人的手牌都保密。' }}</p></div></div>
          <p v-if="error" class="error-text" role="alert">{{ error }}</p>
          <button class="button primary full" :disabled="loading">{{ loading ? '正在进入…' : mode === 'create' ? '创建我的房间' : mode === 'join' ? '加入好友的房间' : '打开观战大屏' }}<span>↗</span></button>
        </form>
        <div class="entry-footer"><span>电脑手机都能玩</span><span>无需下载</span><span>实时同步</span></div>
      </section>
    </main>
    <footer class="home-footer"><span>小小棋盘，大大野心。</span><span>买地 · 建房 · 收路费 · 交朋友</span></footer>
  </div>
</template>
