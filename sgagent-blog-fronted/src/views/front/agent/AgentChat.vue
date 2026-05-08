<template>
  <div class="agent-page">
    <!-- ========== 左侧：会话列表 ========== -->
    <aside class="agent-sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="sidebar-inner" v-show="!sidebarCollapsed">
        <div class="sidebar-header">
          <div class="sidebar-brand">
            <img src="/logo.png" alt="SGAgent" class="brand-logo"/>
            <span class="brand-text">SGAgent</span>
          </div>
          <el-tooltip content="收起侧边栏" placement="top">
            <el-button
                text
                circle
                :icon="Fold"
                class="toggle-btn"
                @click="sidebarCollapsed = true"
            />
          </el-tooltip>
        </div>
        <el-button
            type="primary"
            :icon="Plus"
            class="new-session-btn"
            @click="handleNewSession"
        >
          新建会话
        </el-button>

        <div class="session-list" v-loading="loadingSessions">
          <el-empty
              v-if="!loadingSessions && sessions.length === 0"
              :image-size="80"
              description="暂无会话"
          />
          <div
              v-for="s in sessions"
              :key="s.id"
              class="session-item"
              :class="{ active: s.id === activeSessionId }"
              @click="handleSelectSession(s.id)"
          >
            <el-icon class="session-icon"><ChatLineRound /></el-icon>
            <div class="session-meta">
              <div class="session-title">{{ s.title || '新会话' }}</div>
              <div class="session-time">{{ formatTime(s.updateTime) }}</div>
            </div>
            <el-dropdown
                trigger="click"
                @click.stop
                @command="cmd => handleSessionCommand(cmd, s)"
            >
              <el-button
                  text
                  circle
                  size="small"
                  class="session-action-btn"
                  @click.stop
              >
                <el-icon><MoreFilled /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="rename">
                    <el-icon><EditPen /></el-icon> 重命名
                  </el-dropdown-item>
                  <el-dropdown-item command="delete" divided>
                    <el-icon><Delete /></el-icon> 删除
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </div>

      <!-- 折叠态：仅展开按钮 -->
      <div v-show="sidebarCollapsed" class="collapsed-bar">
        <el-tooltip content="展开侧边栏" placement="right">
          <el-button
              text
              circle
              :icon="Expand"
              class="toggle-btn"
              @click="sidebarCollapsed = false"
          />
        </el-tooltip>
        <el-tooltip content="新建会话" placement="right">
          <el-button
              text
              circle
              :icon="Plus"
              class="toggle-btn"
              @click="handleNewSession"
          />
        </el-tooltip>
      </div>
    </aside>

    <!-- ========== 右侧：聊天区域 ========== -->
    <main class="agent-main">
      <header class="chat-header">
        <div class="chat-title">
          <img src="/logo.png" alt="SGAgent" class="title-logo"/>
          <span>{{ activeSession?.title || '新会话' }}</span>
        </div>
        <div class="chat-subtitle">基于大语言模型的智能助手 · 你的对话仅你可见</div>
      </header>

      <!-- 消息列表 -->
      <section
          class="chat-messages"
          :class="{ 'is-welcome': !loadingMessages && messages.length === 0 }"
          ref="messagesEl"
          v-loading="loadingMessages"
      >
        <!-- 空状态：欢迎屏 -->
        <div v-if="!loadingMessages && messages.length === 0" class="welcome-screen">
          <img src="/logo.png" alt="SGAgent" class="welcome-logo"/>
          <h2 class="welcome-title">
            <span class="typing-text">{{ typedTitle }}</span>
            <span class="typing-caret" v-show="showCaret">|</span>
          </h2>
          <p class="welcome-desc">文件理解、知识库RAG、图片生成、联网搜索，比如：</p>
          <div class="suggestion-grid">
            <div
                v-for="(s, idx) in suggestions"
                :key="idx"
                class="suggestion-card"
                @click="sendQuick(s)"
            >
              {{ s }}
            </div>
          </div>
        </div>

        <!-- 消息气泡 -->
        <div
            v-for="m in messages"
            :key="m.id"
            class="message-row"
            :class="{ 'is-user': m.role === 'user', 'is-assistant': m.role === 'assistant' }"
        >
          <div class="avatar">
            <el-avatar v-if="m.role === 'user'" :size="36" :src="userAvatar">
              <el-icon><UserFilled /></el-icon>
            </el-avatar>
            <div v-else class="avatar-bot">
              <img src="/logo.png" alt="AI" class="avatar-bot-img"/>
            </div>
          </div>
          <div class="bubble" :class="m.role">
            <MdPreview
                v-if="m.role === 'assistant'"
                :model-value="m.content || ''"
                preview-theme="default"
                :theme="mdTheme"
                :code-foldable="false"
                no-img-zoom-in
                class="md-bubble"
            />
            <div v-else class="text-bubble">{{ m.content }}</div>
          </div>
        </div>

        <!-- 等待 AI 回复占位：只当当前会话与发送中的会话 id 一致才显示 -->
        <div v-if="sendingForId && sendingForId === activeSessionId" class="message-row is-assistant">
          <div class="avatar">
            <div class="avatar-bot">
              <img src="/logo.png" alt="AI" class="avatar-bot-img"/>
            </div>
          </div>
          <div class="bubble assistant thinking">
            <span class="dot" />
            <span class="dot" />
            <span class="dot" />
          </div>
        </div>
      </section>

      <!-- 输入区 -->
      <footer class="chat-input-bar">
        <el-input
            v-model="inputText"
            type="textarea"
            :autosize="{ minRows: 1, maxRows: 6 }"
            placeholder="输入消息，Enter 发送，Shift+Enter 换行"
            resize="none"
            class="chat-textarea"
            @keydown.enter="handleEnter"
            :disabled="isSendingHere"
        />
        <el-button
            type="primary"
            :icon="Promotion"
            class="send-btn"
            :loading="isSendingHere"
            :disabled="!inputText.trim()"
            @click="handleSend"
        >
          发送
        </el-button>
      </footer>
    </main>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Plus,
  Fold,
  Expand,
  Promotion,
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MdPreview } from 'md-editor-v3'
import 'md-editor-v3/lib/preview.css'
import { useUserStore } from '@/store/user.js'
import {
  getSessionList,
  createSession,
  renameSession,
  deleteSession,
  getSessionMessages,
  sendChat,
} from '@/api/front/agent/agent.js'

const route = useRoute()
const router = useRouter()

const userStore = useUserStore()
const userAvatar = computed(() => userStore.userInfo?.avatar || '')

/* ========== md-editor 主题：跟随全局 html.dark ========== */
const mdTheme = ref(document.documentElement.classList.contains('dark') ? 'dark' : 'light')
let themeObserver = null
onMounted(() => {
  themeObserver = new MutationObserver(() => {
    mdTheme.value = document.documentElement.classList.contains('dark') ? 'dark' : 'light'
  })
  themeObserver.observe(document.documentElement, { attributes: true, attributeFilter: ['class'] })
})
onBeforeUnmount(() => {
  if (themeObserver) themeObserver.disconnect()
})

/* ========== 侧边栏 ========== */
const sidebarCollapsed = ref(false)

/* ========== 会话状态 ========== */
const sessions = ref([])
const loadingSessions = ref(false)
// activeSessionId 为 null 表示未进入任何会话（欢迎屏）
const activeSessionId = ref(null)
const activeSession = computed(() =>
    sessions.value.find(s => s.id === activeSessionId.value) || null
)

/* ========== 消息状态 ========== */
const messages = ref([])
const loadingMessages = ref(false)
const inputText = ref('')
// 记录当前发送中的会话 id；null 表示未发送。
const sendingForId = ref(null)
const isSendingHere = computed(() =>
    sendingForId.value !== null
    && sendingForId.value === activeSessionId.value
)
const messagesEl = ref(null)

const suggestions = [
  '我有几篇推文，把推文数据制作成表格',
  '哪些博客提到了Web3的知识？给我博客标题',
  '生成一张小猪佩奇图片',
  '解释这个文件的内容（附带文件链接）',
]

/* ========== 持续打字机动画 ========== */
const FULL_TITLE = '🤖你好，我是 SGAgent'
const typedTitle = ref('')
const showCaret = ref(true)
let typingTimer = null
let caretTimer = null
// 0=打字中, 1=打完暂停, 2=退格中, 3=退完暂停
let typingPhase = 0
let typingIdx = 0
let phaseHoldUntil = 0

const TYPE_INTERVAL = 90       // ms/char
const ERASE_INTERVAL = 45      // ms/char
const HOLD_AFTER_TYPE = 1500   // ms
const HOLD_AFTER_ERASE = 600   // ms

function startTyping() {
  stopTyping()
  typedTitle.value = ''
  typingPhase = 0
  typingIdx = 0
  const tick = () => {
    const now = Date.now()
    if (typingPhase === 1 || typingPhase === 3) {
      // 暂停阶段
      if (now < phaseHoldUntil) return
      typingPhase = typingPhase === 1 ? 2 : 0
      return
    }
    if (typingPhase === 0) {
      if (typingIdx < FULL_TITLE.length) {
        typingIdx++
        typedTitle.value = FULL_TITLE.slice(0, typingIdx)
      } else {
        typingPhase = 1
        phaseHoldUntil = now + HOLD_AFTER_TYPE
      }
    } else if (typingPhase === 2) {
      if (typingIdx > 0) {
        typingIdx--
        typedTitle.value = FULL_TITLE.slice(0, typingIdx)
      } else {
        typingPhase = 3
        phaseHoldUntil = now + HOLD_AFTER_ERASE
      }
    }
  }
  // 为了详细控制语速，在阶段切换后采用不同间隔：使用 25ms 心跳 + 主动跳过打字间隔
  let last = 0
  typingTimer = setInterval(() => {
    const now = Date.now()
    const interval = typingPhase === 0 ? TYPE_INTERVAL
        : typingPhase === 2 ? ERASE_INTERVAL : 25
    if (now - last < interval) return
    last = now
    tick()
  }, 25)
  caretTimer = setInterval(() => {
    showCaret.value = !showCaret.value
  }, 500)
}

function stopTyping() {
  if (typingTimer) clearInterval(typingTimer)
  if (caretTimer) clearInterval(caretTimer)
  typingTimer = null
  caretTimer = null
}

/* ========== 初始化与路由同步 ========== */
onMounted(async () => {
  await loadSessions()
  await syncFromRoute()
})

onBeforeUnmount(() => stopTyping())

// 侧栏点击 / 浏览器前进后退 → 路由变化 → 同步到状态
watch(() => route.params.id, () => {
  if (route.name === 'SgAgentChat') {
    syncFromRoute()
  }
})

async function syncFromRoute() {
  const id = route.params.id || null
  if (id) {
    if (id !== activeSessionId.value) {
      await switchSession(id)
    }
  } else {
    // /agent：进入欢迎屏
    activeSessionId.value = null
    messages.value = []
    startTyping()
  }
}

async function loadSessions() {
  try {
    loadingSessions.value = true
    const res = await getSessionList()
    sessions.value = res.data || []
  } finally {
    loadingSessions.value = false
  }
}

async function switchSession(id) {
  activeSessionId.value = id
  messages.value = []
  stopTyping()
  if (!id) return
  try {
    loadingMessages.value = true
    const res = await getSessionMessages(id)
    messages.value = res.data || []
    await scrollToBottom()
  } catch (e) {
    // 会话不存在 / 无权 → 退回欢迎屏
    router.replace('/agent')
  } finally {
    loadingMessages.value = false
  }
}

function handleSelectSession(id) {
  if (id !== activeSessionId.value) {
    router.push(`/agent/${id}`)
  }
}

// “新建会话”：仅本地重置态，不调后端 API；
// 发出首条消息时才创建后端会话
async function handleNewSession() {
  if (route.params.id) {
    router.push('/agent')
  } else {
    activeSessionId.value = null
    messages.value = []
    inputText.value = ''
    startTyping()
  }
}

async function handleSessionCommand(cmd, session) {
  if (cmd === 'rename') {
    try {
      const { value } = await ElMessageBox.prompt('输入新的会话标题', '重命名', {
        inputValue: session.title,
        inputPattern: /^.{1,100}$/,
        inputErrorMessage: '标题长度需在 1-100 之间',
        confirmButtonText: '确定',
        cancelButtonText: '取消',
      })
      await renameSession(session.id, value)
      ElMessage.success('已重命名')
      await loadSessions()
    } catch (e) {
      // 用户取消，忽略
    }
  } else if (cmd === 'delete') {
    try {
      await ElMessageBox.confirm('确定删除该会话及全部聊天记录？', '删除会话', {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消',
      })
      await deleteSession(session.id)
      ElMessage.success('已删除')
      const wasActive = session.id === activeSessionId.value
      await loadSessions()
      if (wasActive) {
        if (sessions.value.length > 0) {
          router.push(`/agent/${sessions.value[0].id}`)
        } else {
          router.push('/agent')
        }
      }
    } catch (e) {
      // 取消
    }
  }
}

/* ========== 发送 ========== */
function handleEnter(e) {
  if (e.shiftKey) return // 允许换行
  e.preventDefault()
  handleSend()
}

function sendQuick(text) {
  inputText.value = text
  handleSend()
}

async function handleSend() {
  const content = inputText.value.trim()
  if (!content || isSendingHere.value) return

  // 首条消息：先创建后端会话，拿到 id 后立即 push URL 并刷新侧栏
  let idForThisSend = activeSessionId.value
  if (!idForThisSend) {
    try {
      const createRes = await createSession()
      idForThisSend = createRes.data?.id
      if (!idForThisSend) {
        ElMessage.error('创建会话失败')
        return
      }
      // 同步状态与 URL，让 header 立即显示“新会话”标题
      activeSessionId.value = idForThisSend
      stopTyping()
      await loadSessions()
      router.replace(`/agent/${idForThisSend}`)
    } catch (e) {
      return
    }
  }

  // 乐观追加用户消息
  const optimistic = {
    id: 'tmp-' + Date.now(),
    role: 'user',
    content,
    createTime: new Date().toISOString(),
  }
  messages.value.push(optimistic)
  inputText.value = ''
  await scrollToBottom()

  // 乐观更新标题：首条消息后立即同步到侧栏 / header（无需等 AI 回复）
  const sessionRef = sessions.value.find(s => s.id === idForThisSend)
  if (sessionRef && (!sessionRef.title || sessionRef.title === '新会话')) {
    sessionRef.title = buildAutoTitle(content)
    sessionRef.updateTime = formatTime(new Date())
  }

  // 标记发送中的会话 id；切换到其他会话时 isSendingHere 会变 false，思考气泡不再显示
  sendingForId.value = idForThisSend
  try {
    const res = await sendChat({
      sessionId: idForThisSend,
      content,
    })
    const reply = res.data
    if (reply) {
      if (activeSessionId.value === reply.sessionId) {
        optimistic.id = reply.userMessageId || optimistic.id
        messages.value.push({
          id: reply.assistantMessageId,
          role: 'assistant',
          content: reply.content,
          createTime: new Date().toISOString(),
        })
        await scrollToBottom()
      }
      await loadSessions()
    }
  } catch (e) {
    if (activeSessionId.value === idForThisSend) {
      const idx = messages.value.findIndex(m => m.id === optimistic.id)
      if (idx > -1) messages.value.splice(idx, 1)
    }
  } finally {
    if (sendingForId.value === idForThisSend) {
      sendingForId.value = null
    }
  }
}

/* ========== 工具方法 ========== */
async function scrollToBottom() {
  await nextTick()
  if (messagesEl.value) {
    messagesEl.value.scrollTop = messagesEl.value.scrollHeight
  }
}

/** 截取消息内容作为会话标题（与后端策略保持一致：单行 + 上限 20 字） */
function buildAutoTitle(content) {
  if (!content) return '新会话'
  const oneLine = content.replace(/\s+/g, ' ').trim()
  return oneLine.length > 20 ? oneLine.slice(0, 20) + '…' : oneLine
}

function formatTime(dt) {
  if (!dt) return ''
  // 后端返回格式如 '2026-05-09 02:15:30'
  const d = new Date(typeof dt === 'string' ? dt.replace(' ', 'T') : dt)
  if (isNaN(d.getTime())) return dt
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} `
      + `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}
</script>

<style scoped>
/* ========== 整体布局 ========== */
.agent-page {
  display: flex;
  height: calc(100vh - 60px); /* header 约 60px */
  background-color: var(--el-bg-color-page);
  overflow: hidden;
}

/* ========== 侧边栏 ========== */
.agent-sidebar {
  width: 280px;
  flex-shrink: 0;
  background-color: var(--el-bg-color);
  border-right: 1px solid var(--el-border-color-light);
  transition: width 0.25s cubic-bezier(0.25, 0.8, 0.25, 1);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.agent-sidebar.collapsed {
  width: 56px;
}

.sidebar-inner {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 12px;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;
}

.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.brand-logo {
  width: 26px;
  height: 26px;
  border-radius: 6px;
  flex-shrink: 0;
  object-fit: contain;
}

.brand-text {
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  letter-spacing: 0.3px;
}

.new-session-btn {
  width: 100%;
  height: 38px;
  font-weight: 500;
  margin-bottom: 12px;
}

.toggle-btn {
  font-size: 18px;
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding-right: 2px;
}

.session-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.2s;
  margin-bottom: 4px;
}

.session-item:hover {
  background-color: var(--el-fill-color-light);
}

.session-item.active {
  background-color: var(--el-color-primary-light-9);
}

.session-item.active .session-title {
  color: var(--el-color-primary);
  font-weight: 600;
}

.session-icon {
  font-size: 16px;
  color: var(--el-text-color-secondary);
  flex-shrink: 0;
}

.session-meta {
  flex: 1;
  min-width: 0;
}

.session-title {
  font-size: 14px;
  color: var(--el-text-color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-time {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 2px;
}

.session-action-btn {
  opacity: 0;
  transition: opacity 0.2s;
}

.session-item:hover .session-action-btn {
  opacity: 1;
}

.collapsed-bar {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding-top: 16px;
}

/* 折叠态下两个按钮强制同宽同高，保证竖向对齐 */
.collapsed-bar .toggle-btn {
  width: 36px;
  height: 36px;
  margin: 0;
}

/* ========== 主聊天区 ========== */
.agent-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background-color: var(--el-bg-color-page);
}

.chat-header {
  padding: 14px 24px;
  border-bottom: 1px solid var(--el-border-color-light);
  background-color: var(--el-bg-color);
}

.chat-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  min-width: 0;
}

.chat-title > span {
  display: inline-block;
  max-width: 360px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
}

.title-logo {
  width: 22px;
  height: 22px;
  border-radius: 6px;
  object-fit: contain;
}

.chat-subtitle {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 2px;
}

/* ========== 消息列表 ========== */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px 16px;
}

/* 欢迎屏：禁用滚动条，并让内容垂直居中偏上 */
.chat-messages.is-welcome {
  overflow: hidden;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 0;
}

.welcome-screen {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  text-align: center;
  padding: 24px 16px 16px;
  max-width: 720px;
  width: 100%;
  margin: 0 auto;
}

.welcome-logo {
  width: 140px;
  height: 140px;
  border-radius: 28px;
  margin-bottom: 6px;
  object-fit: contain;
  filter: drop-shadow(0 8px 22px rgba(0, 0, 0, 0.12));
  animation: floatY 3.6s ease-in-out infinite;
}

@keyframes floatY {
  0%, 100% { transform: translateY(0); }
  50%      { transform: translateY(-6px); }
}

.typing-text {
  display: inline-block;
}

.typing-caret {
  display: inline-block;
  margin-left: 2px;
  color: var(--el-color-primary);
  font-weight: 300;
}

.welcome-title {
  font-size: 26px;
  font-weight: 600;
  margin: 0 0 6px;
  color: var(--el-text-color-primary);
}

.welcome-desc {
  color: var(--el-text-color-secondary);
  margin: 0 0 20px;
}

.suggestion-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  width: 100%;
}

.suggestion-card {
  padding: 14px 16px;
  background-color: var(--el-bg-color);
  border: 1px solid var(--el-border-color-light);
  border-radius: 10px;
  font-size: 14px;
  color: var(--el-text-color-regular);
  cursor: pointer;
  text-align: left;
  transition: all 0.2s;
}

.suggestion-card:hover {
  border-color: var(--el-color-primary);
  color: var(--el-color-primary);
  transform: translateY(-1px);
  box-shadow: var(--el-box-shadow-light);
}

/* ========== 消息气泡 ========== */
.message-row {
  display: flex;
  gap: 10px;
  margin: 0 auto 18px;
  max-width: 860px;
  width: 100%;
}

.message-row.is-user {
  flex-direction: row-reverse;
}

.avatar {
  flex-shrink: 0;
}

.avatar-bot {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-color: var(--el-bg-color);
  border: 1px solid var(--el-border-color-light);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.avatar-bot-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  padding: 4px;
}

.bubble {
  max-width: calc(100% - 60px);
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.7;
  word-break: break-word;
}

.bubble.user {
  background-color: var(--el-color-primary);
  color: #fff;
  border-top-right-radius: 4px;
}

.bubble.assistant {
  background-color: var(--el-bg-color);
  color: var(--el-text-color-primary);
  border: 1px solid var(--el-border-color-light);
  border-top-left-radius: 4px;
}

.text-bubble {
  white-space: pre-wrap;
}

/* md-editor 预览样式去除其默认外边距 */
.md-bubble :deep(.md-editor-preview) {
  padding: 0;
  background: transparent;
  font-size: 14px;
  line-height: 1.7;
}

.md-bubble :deep(.default-theme h1),
.md-bubble :deep(.default-theme h2),
.md-bubble :deep(.default-theme h3) {
  margin-top: 8px;
}

.md-bubble :deep(pre) {
  margin: 8px 0;
}

/* 取消 md-editor-v3 代码块 head 的 sticky 定位与超高 z-index：
   - sticky + z-index 10000 会遮挡 el-dialog（z-index 2000 级）等弹层
   - 用户需求：拖动滚动时代码块头随内容一起滚动 */
.md-bubble :deep(.md-editor-code-head) {
  position: relative !important;
  top: auto !important;
  z-index: auto !important;
}

/* 代码块 head 与 pre 之间的 8px 空白间隙去除 */
.md-bubble :deep(.md-editor-code) {
  margin: 8px 0;
}

.md-bubble :deep(.md-editor-code > pre),
.md-bubble :deep(.md-editor-code-head + pre) {
  margin: 0 !important;
  border-top-left-radius: 0;
  border-top-right-radius: 0;
}

/* ========== 思考动画 ========== */
.bubble.thinking {
  display: flex;
  gap: 4px;
  align-items: center;
  padding: 14px 16px;
}

.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background-color: var(--el-text-color-secondary);
  animation: bounce 1.2s infinite ease-in-out;
}

.dot:nth-child(2) { animation-delay: 0.2s; }
.dot:nth-child(3) { animation-delay: 0.4s; }

@keyframes bounce {
  0%, 80%, 100% { transform: translateY(0); opacity: 0.4; }
  40% { transform: translateY(-4px); opacity: 1; }
}

/* ========== 输入区：仅保留输入框形状，无 footer 背景与上边框 ========== */
.chat-input-bar {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  max-width: 760px;
  width: 100%;
  margin: 0 auto;
  padding: 12px 16px 18px;
  background: transparent;
  border-top: none;
}

.chat-textarea {
  flex: 1;
}

.chat-textarea :deep(.el-textarea__inner) {
  border-radius: 10px;
  padding: 10px 14px;
  font-size: 14px;
  line-height: 1.6;
  /* 隐藏滚动条，付代价不大：内容超 6 行时仍可鼠标滚动查看 */
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.chat-textarea :deep(.el-textarea__inner)::-webkit-scrollbar {
  display: none;
}

.send-btn {
  height: 40px;
  padding: 0 18px;
  border-radius: 10px;
}

/* ========== 响应式：移动端折起侧边栏 ========== */
@media screen and (max-width: 768px) {
  .agent-sidebar {
    width: 56px;
  }
  .agent-sidebar:not(.collapsed) .sidebar-inner {
    display: none;
  }
  .agent-sidebar:not(.collapsed) {
    width: 56px;
  }
  .agent-sidebar:not(.collapsed) .collapsed-bar {
    display: flex;
  }
  .suggestion-grid {
    grid-template-columns: 1fr;
  }
}
</style>
