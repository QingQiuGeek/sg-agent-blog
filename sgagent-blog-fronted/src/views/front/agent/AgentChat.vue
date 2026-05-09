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
            <el-button
                text
                circle
                :icon="Fold"
                class="toggle-btn"
                @click="sidebarCollapsed = true"
            />
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
          <div class="bubble" :class="m.role" @click="onBubbleClick">
            <!-- 工具调用 chips：仅 assistant 消息且本次调用过工具时显示 -->
            <div
                v-if="m.role === 'assistant' && m.toolCalls && m.toolCalls.length"
                class="tool-calls-block"
            >
              <span
                  v-for="(t, idx) in m.toolCalls"
                  :key="idx"
                  class="tool-chip"
                  :class="`tool-chip--${toolChipType(t.name) || 'default'}`"
                  :title="t.summary ? `${t.label || t.name} · ${t.summary}` : (t.label || t.name)"
              >
                <el-icon class="tool-chip-icon">
                  <component :is="toolChipIcon(t.name)" />
                </el-icon>
                <span class="tool-chip-text">
                  <span class="tool-chip-label">{{ t.label || t.name }}</span>
                  <span v-if="t.summary" class="tool-chip-summary">· {{ truncateSummary(t.summary) }}</span>
                </span>
              </span>
            </div>

            <!-- streaming 且尚未有任何文本时，气泡内显示“思考中”动画，避免空白气泡 -->
            <div
                v-if="m.role === 'assistant' && m.streaming && !m.content"
                class="thinking-inline"
                aria-label="思考中"
            >
              <span class="dot" />
              <span class="dot" />
              <span class="dot" />
              <span class="thinking-text">思考中…</span>
            </div>

            <MdPreview
                v-else-if="m.role === 'assistant'"
                :model-value="m.content || ''"
                preview-theme="default"
                :theme="mdTheme"
                :code-foldable="false"
                no-img-zoom-in
                class="md-bubble"
            />
            <template v-else>
              <!-- user 消息附件：从上到下纵向排列（按上传顺序） -->
              <div v-if="m.attachments && m.attachments.length" class="msg-attachment-list">
                <a
                    v-for="(att, i) in m.attachments"
                    :key="i"
                    :href="att.url"
                    target="_blank"
                    rel="noopener noreferrer"
                    class="msg-attachment-card"
                    :title="att.name"
                >
                  <div class="msg-attachment-icon">
                    <el-icon><Document /></el-icon>
                  </div>
                  <div class="msg-attachment-info">
                    <div class="msg-attachment-name">{{ att.name }}</div>
                    <div class="msg-attachment-meta">
                      <span class="msg-attachment-ext">{{ (att.ext || '').toUpperCase() }}</span>
                      <span v-if="att.size">· {{ formatFileSize(att.size) }}</span>
                    </div>
                  </div>
                </a>
              </div>
              <!-- 文本：在文件卡片下方 -->
              <div v-if="m.content" class="text-bubble">{{ m.content }}</div>
            </template>

            <!-- 来源引用：兼容 article / web 两种类型，默认折叠 -->
            <div
                v-if="m.role === 'assistant' && m.sources && m.sources.length"
                class="sources-block"
            >
              <div
                  class="sources-title"
                  role="button"
                  tabindex="0"
                  @click="toggleSources(m.id)"
                  @keydown.enter.prevent="toggleSources(m.id)"
                  @keydown.space.prevent="toggleSources(m.id)"
              >
                <el-icon><Document /></el-icon>
                <span>来源 · 共 {{ m.sources.length }} 条引用</span>
                <el-icon class="sources-toggle-icon" :class="{ 'is-open': isSourcesOpen(m.id) }">
                  <ArrowDown />
                </el-icon>
              </div>
              <div v-show="isSourcesOpen(m.id)" class="sources-list">
                <div
                    v-for="(s, idx) in m.sources"
                    :key="idx"
                    class="source-card"
                    @click="goToSource(s)"
                >
                  <div class="source-card-title">{{ s.title }}</div>
                  <div class="source-card-meta">
                    <span v-if="s.type === 'web'" class="source-card-tag">联网</span>
                    <span v-else class="source-card-tag">站内</span>
                    <span v-if="s.author">· {{ s.author }}</span>
                    <span v-if="s.type === 'web' && s.url" class="source-card-host">
                      · {{ formatHost(s.url) }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 消息操作：复制 / 重试 / 删除；仅当消息有正式 id（已持久化）且非 streaming 时显示 -->
          <div
              v-if="canShowActions(m)"
              class="message-actions"
              :class="{ 'is-user-actions': m.role === 'user' }"
          >
            <el-tooltip content="复制" placement="top">
              <el-button text size="small" class="msg-action-btn" @click="copyMessage(m)">
                <el-icon><CopyDocument /></el-icon>
              </el-button>
            </el-tooltip>
            <el-tooltip content="重试" placement="top">
              <el-button
                  text
                  size="small"
                  class="msg-action-btn"
                  :disabled="isSendingHere"
                  @click="retryMessage(m)"
              >
                <el-icon><RefreshRight /></el-icon>
              </el-button>
            </el-tooltip>
            <el-tooltip content="删除" placement="top">
              <el-button
                  text
                  size="small"
                  class="msg-action-btn msg-action-danger"
                  :disabled="isSendingHere"
                  @click="deleteMessageItem(m)"
              >
                <el-icon><Delete /></el-icon>
              </el-button>
            </el-tooltip>
          </div>
        </div>

        <!-- 等待 AI 回复占位：
             只在「placeholder 还没被 push 到 messages」时显示（避免与上面 streaming 气泡重复） -->
        <div
            v-if="sendingForId && sendingForId === activeSessionId && !hasStreamingPlaceholder"
            class="message-row is-assistant"
        >
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
        <div class="composer">
          <!-- 附件卡片列表：上传中显示 spinner，上传完显示文件信息 + 删除按钮 -->
          <div v-if="attachments.length" class="attachment-list">
            <div
                v-for="(att, idx) in attachments"
                :key="att.uid"
                class="attachment-card"
                :class="{ 'is-loading': att.status === 'uploading', 'is-error': att.status === 'error' }"
            >
              <div class="attachment-icon">
                <el-icon v-if="att.status === 'uploading'" class="is-loading"><Loading /></el-icon>
                <el-icon v-else-if="att.status === 'error'"><CircleClose /></el-icon>
                <el-icon v-else><Document /></el-icon>
              </div>
              <div class="attachment-info">
                <div class="attachment-name" :title="att.name">{{ att.name }}</div>
                <div class="attachment-meta">
                  <span v-if="att.status === 'uploading'">上传解析中…</span>
                  <span v-else-if="att.status === 'error'" class="attachment-err">{{ att.error || '上传失败' }}</span>
                  <span v-else>{{ formatFileSize(att.size) }} · 已就绪</span>
                </div>
              </div>
              <el-button
                  text
                  circle
                  size="small"
                  class="attachment-close"
                  @click="removeAttachment(idx)"
              >
                <el-icon><Close /></el-icon>
              </el-button>
            </div>
          </div>

          <!-- 隐藏的原生 file input：由 paperclip 按钮触发 -->
          <input
              ref="fileInputRef"
              type="file"
              multiple
              :accept="ACCEPT_EXTENSIONS"
              style="display: none"
              @change="onFileChosen"
          />

          <el-input
              v-model="inputText"
              type="textarea"
              :autosize="{ minRows: 1, maxRows: 6 }"
              placeholder="输入消息，Enter 发送，Shift+Enter 换行；可直接粘贴文件"
              resize="none"
              class="chat-textarea"
              @keydown.enter="handleEnter"
              @paste="onTextareaPaste"
              :disabled="isSendingHere"
          />
          <div class="composer-toolbar">
            <div class="composer-toolbar-left">
                <el-button
                    text
                    circle
                    size="small"
                    class="composer-action"
                    @click="onUploadFileClick"
                >
                  <el-icon><Paperclip /></el-icon>
                </el-button>
          
                <el-button
                    text
                    round
                    size="small"
                    :class="['composer-action', 'composer-toggle', { active: webSearchEnabled }]"
                    @click="onToggleWebSearch"
                >
                  <el-icon><Search /></el-icon>
                  <span class="toggle-label">联网搜索</span>
                </el-button>

                <!-- 知识库按钮：上拉 popover 显示多选列表，按需勾选 -->
                <el-popover
                    placement="top-start"
                    :width="300"
                    trigger="click"
                    popper-class="kb-popover"
                    @show="onKbPopoverShow"
                >
                  <template #reference>
                    <el-button
                        text
                        round
                        size="small"
                        :class="['composer-action', 'composer-toggle', { active: selectedKbIds.length > 0 }]"
                    >
                      <el-icon><Collection /></el-icon>
                      <span class="toggle-label">知识库</span>
                      <el-badge
                          v-if="selectedKbIds.length"
                          :value="selectedKbIds.length"
                          class="kb-badge"
                      />
                    </el-button>
                  </template>

                  <div class="kb-popover-head">
                    <span class="kb-popover-title">我的知识库</span>
                  </div>
                  <div v-loading="kbLoading" class="kb-popover-body">
                    <el-empty
                        v-if="!kbLoading && kbList.length === 0"
                        :image-size="60"
                        description="暂无知识库"
                    >
                      <el-button size="small" type="primary" @click="goCreateKb">去创建</el-button>
                    </el-empty>
                    <el-checkbox-group v-else v-model="selectedKbIds" class="kb-check-list">
                      <el-checkbox
                          v-for="kb in kbList"
                          :key="kb.id"
                          :value="kb.id"
                          class="kb-check-item"
                      >
                        <div class="kb-check-content">
                          <div class="kb-check-name">{{ kb.name }}</div>
                          <div class="kb-check-desc">{{ kb.fileCount || 0 }} 个文件{{ kb.description ? ' · ' + kb.description : '' }}</div>
                        </div>
                      </el-checkbox>
                    </el-checkbox-group>
                  </div>
                  <div class="kb-popover-foot">
                    勾选后，AI 会按需检索这些知识库；不勾选则不启用知识库工具。
                  </div>
                </el-popover>
            </div>
            <el-button
                type="primary"
                :icon="Promotion"
                class="send-btn"
                size="small"
                :loading="isSendingHere"
                :disabled="!inputText.trim()"
                @click="handleSend()"
            >
              发送
            </el-button>
          </div>
        </div>
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
  UserFilled,
  Document,
  Paperclip,
  Search,
  Picture,
  Clock,
  Link,
  MagicStick,
  ArrowDown,
  CopyDocument,
  Delete,
  RefreshRight,
  Loading,
  Close,
  CircleClose,
  Collection,
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
  deleteMessage as apiDeleteMessage,
  uploadAgentFile,
  sendChat,
  streamChat,
} from '@/api/front/agent/agent.js'
import { getMyKbsBrief } from '@/api/front/user/kb.js'

const route = useRoute()
const router = useRouter()

const userStore = useUserStore()
const userAvatar = computed(() => userStore.userInfo?.avatar || '')

/* ========== 登录守卫 ========== */
const isLoggedIn = computed(() => userStore.isLoggedIn)

function promptLogin(tip = '请先登录后再使用 SGAgent') {
  ElMessage.warning(tip)
  userStore.openAuthDialog('login')
}

/** 行为前置守卫：未登录时弹出登录提示并返回 false */
function requireLogin(tip) {
  if (!isLoggedIn.value) {
    promptLogin(tip)
    return false
  }
  return true
}

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
// 当前 messages 列表里是否已经存在「正在 streaming 的 assistant 占位」
// 用于避免顶部 streaming 气泡（带思考动画）跟底部独立"等待占位"同时出现
const hasStreamingPlaceholder = computed(() =>
    messages.value.some(m => m.role === 'assistant' && m.streaming)
)
const messagesEl = ref(null)

const suggestions = [
  '帮我查下Web3相关的博客，给我博客标题、链接',
  '生成一张5:2高达图片',
  '解释这个文件的内容（上传文件）',
  '今日伊朗战争局势',
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
  if (!isLoggedIn.value) {
    // 未登录：仅展示欢迎屏，不调任何接口；若路径携带会话 id 则回退根路径
    if (route.params.id) {
      router.replace('/agent')
    } else {
      startTyping()
    }
    return
  }
  await loadSessions()
  await syncFromRoute()
})

// 登录状态变化：登录后自动载入会话；登出后清空
watch(isLoggedIn, async (val) => {
  if (val) {
    await loadSessions()
    await syncFromRoute()
  } else {
    sessions.value = []
    messages.value = []
    activeSessionId.value = null
    sendingForId.value = null
  }
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
    // 未登录：禁止根据 id 拉历史，回退欢迎屏
    if (!isLoggedIn.value) {
      router.replace('/agent')
      return
    }
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
  if (!requireLogin('请先登录后再新建会话')) return
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

/**
 * 气泡内 click 代理：拦截所有 <a> 标签点击 → 新标签页打开
 * - 相对路径（如 /post/123）通过 new URL(href, location.href) 解析为当前 origin 的绝对地址
 * - 绝对外链直接 window.open
 * - 锚点 #xxx 不拦截，交给浏览器默认行为
 */
function onBubbleClick(e) {
  const a = e.target.closest('a')
  if (!a) return
  const href = a.getAttribute('href')
  if (!href || href.startsWith('#')) return
  e.preventDefault()
  const absolute = new URL(href, window.location.href).href
  window.open(absolute, '_blank', 'noopener,noreferrer')
}

/** 来源卡片点击：article 跳站内详情，web 直接外链 */
function goToSource(s) {
  if (!s) return
  if (s.type === 'web' && s.url) {
    window.open(s.url, '_blank', 'noopener')
    return
  }
  if (s.articleId) {
    const url = router.resolve(`/post/${s.articleId}`).href
    window.open(url, '_blank', 'noopener')
  }
}

/** 截取 URL 的 host 部分用于卡片副标题 */
function formatHost(url) {
  try {
    return new URL(url).host
  } catch {
    return url
  }
}

/** 工具调用 chip 图标映射（按 @Tool name 分发） */
function toolChipIcon(name) {
  switch (name) {
    case 'searchArticles': return Document
    case 'webSearch':      return Link
    case 'generateImage':
    case 'qwenGenerateImage':
    case 'wanxGenerateImage': return Picture
    case 'dateTimeTool':   return Clock
    default:               return MagicStick
  }
}

/** 消息 id -> 是否展开来源列表；默认全部折叠 */
const expandedSources = ref(new Set())

function toggleSources(messageId) {
  if (!messageId) return
  const set = new Set(expandedSources.value)
  if (set.has(messageId)) set.delete(messageId)
  else set.add(messageId)
  expandedSources.value = set
}

function isSourcesOpen(messageId) {
  return expandedSources.value.has(messageId)
}

/** 截断 chip summary 避免超长损坏布局 */
function truncateSummary(text) {
  if (!text) return ''
  const max = 18
  return text.length > max ? text.slice(0, max) + '…' : text
}

/** 工具调用 chip 颜色（决定 CSS 变体名）*/
function toolChipType(name) {
  switch (name) {
    case 'searchArticles': return 'primary'
    case 'webSearch':      return 'success'
    case 'generateImage':
    case 'qwenGenerateImage':
    case 'wanxGenerateImage': return 'warning'
    case 'dateTimeTool':   return 'info'
    default:               return ''
  }
}

/* ===== 消息操作：复制 / 删除 / 重试 ===== */

/**
 * 是否展示气泡下方的操作条：
 * - 必须有正式 id（数据库已落地，前缀 tmp- 的乐观消息暂不显示）
 * - 排除正在 streaming 的占位 assistant 消息
 * - assistant 消息内容为空（极个别异常）也不显示
 */
function canShowActions(m) {
  if (!m || !m.id) return false
  if (typeof m.id === 'string' && m.id.startsWith('tmp-')) return false
  if (m.streaming) return false
  if (m.role === 'assistant' && !m.content) return false
  return true
}

/** 复制消息文本到剪贴板 */
async function copyMessage(m) {
  if (!m?.content) return
  try {
    await navigator.clipboard.writeText(m.content)
    ElMessage.success('已复制')
  } catch {
    // 兜底：低版本浏览器或非 https 环境
    const ta = document.createElement('textarea')
    ta.value = m.content
    document.body.appendChild(ta)
    ta.select()
    try { document.execCommand('copy'); ElMessage.success('已复制') }
    catch { ElMessage.error('复制失败，请手动选择') }
    finally { document.body.removeChild(ta) }
  }
}

/** 删除单条消息：确认 → 调接口 → 本地数组移除 */
async function deleteMessageItem(m) {
  if (!m?.id) return
  try {
    await ElMessageBox.confirm('确定删除这条消息？', '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch { return /* 用户取消 */ }
  try {
    await apiDeleteMessage(m.id)
    const idx = messages.value.findIndex(x => x.id === m.id)
    if (idx > -1) messages.value.splice(idx, 1)
    ElMessage.success('已删除')
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

/**
 * 重试消息：
 * - user 消息：删除该消息及其后所有消息（前后端同步）→ 用相同 content 重新发送
 * - assistant 消息：找到它前面最近一条 user 消息作为 retry 源 → 删除从该 user 起的所有消息 → 重新发送
 *
 * 重发依赖 callLlm 取数据库历史，所以必须先把这些消息从数据库删掉，避免重复对话污染上下文。
 */
async function retryMessage(m) {
  if (!m?.id || isSendingHere.value) return

  // 1. 找出"重发起点"——必为某条 user 消息
  let anchor = m
  if (m.role === 'assistant') {
    const myIdx = messages.value.findIndex(x => x.id === m.id)
    if (myIdx <= 0) {
      ElMessage.warning('找不到对应的用户提问')
      return
    }
    // 反向找最近一条 user
    for (let i = myIdx - 1; i >= 0; i--) {
      if (messages.value[i].role === 'user') {
        anchor = messages.value[i]
        break
      }
    }
    if (anchor.role !== 'user') {
      ElMessage.warning('找不到对应的用户提问')
      return
    }
  }
  const retryContent = anchor.content
  if (!retryContent) return

  const anchorIdx = messages.value.findIndex(x => x.id === anchor.id)
  if (anchorIdx < 0) return

  // 2. 收集起点及以后所有"已落地"消息（带正式 id），调批量接口删除
  const tail = messages.value.slice(anchorIdx)
  const idsToDelete = tail
      .filter(x => x.id && !(typeof x.id === 'string' && x.id.startsWith('tmp-')))
      .map(x => x.id)

  try {
    await Promise.all(idsToDelete.map(id => apiDeleteMessage(id)))
  } catch (e) {
    ElMessage.error('清理历史消息失败，重试已取消')
    return
  }

  // 3. 本地从 anchor 起截断
  messages.value.splice(anchorIdx)

  // 4. 复用 handleSend 重新发送
  await handleSend(retryContent)
}

/* ===== 联网搜索 ===== */
const webSearchEnabled = ref(false)

function onToggleWebSearch() {
  webSearchEnabled.value = !webSearchEnabled.value
}

/* ===== 知识库多选 =====
 * 仅在引用面板打开时拉取一次列表，避免页面进入就预请求
 * selectedKbIds 隨请求带上；所以 selectedKbIds 以多个会话 / 页面生命周期共享是可接受的
 */
const selectedKbIds = ref([])
const kbList = ref([])
const kbLoading = ref(false)

async function onKbPopoverShow() {
  if (!requireLogin('请先登录后再使用知识库')) return
  kbLoading.value = true
  try {
    const res = await getMyKbsBrief()
    if (res?.code === 200) {
      kbList.value = res.data || []
      // 已选中但被删除的 kbId 顺手清掉，避免发请求时被后端 ownership 过滤后静默丢弃
      const liveIds = new Set(kbList.value.map(k => k.id))
      selectedKbIds.value = selectedKbIds.value.filter(id => liveIds.has(id))
    }
  } finally {
    kbLoading.value = false
  }
}

function goCreateKb() {
  router.push('/user/kbs')
}

/* ===== 附件上传 ===== */

/** 与后端 ALLOWED_EXTENSIONS 保持同步：超出此白名单前端就直接拒绝，不发请求 */
const ALLOWED_EXTENSIONS = new Set([
  // 文档
  'md', 'txt', 'docx', 'doc', 'rtf', 'odt', 'pdf',
  // 表格
  'xlsx', 'xls', 'csv', 'tsv',
  // 数据
  'json', 'xml', 'yaml', 'yml', 'toml', 'ini', 'properties', 'log',
  // 网页
  'html', 'htm', 'css',
  // 代码
  'java', 'kt', 'scala', 'groovy',
  'js', 'mjs', 'cjs', 'ts', 'tsx', 'jsx', 'vue', 'svelte',
  'py', 'rb', 'php', 'go', 'rs',
  'c', 'cc', 'cpp', 'cxx', 'h', 'hpp',
  'cs', 'swift', 'm', 'mm',
  'sh', 'bash', 'zsh', 'ps1', 'bat', 'cmd',
  'sql', 'graphql', 'gql', 'proto',
  'dockerfile', 'makefile', 'gradle',
])

/** input[type=file] accept 属性值，仅作输入提示，真正校验在 onFileChosen */
const ACCEPT_EXTENSIONS = Array.from(ALLOWED_EXTENSIONS).map(e => '.' + e).join(',')

/** 单文件最大 5MB；与后端一致 */
const MAX_FILE_SIZE = 5 * 1024 * 1024
/** 同时存在的最大文件数 */
const MAX_ATTACHMENTS = 5

/** 已选附件列表：每项含上传状态 */
const attachments = ref([])
const fileInputRef = ref(null)

function onUploadFileClick() {
  if (!requireLogin('请先登录后再上传文件')) return
  fileInputRef.value?.click()
}

/** input change 回调：批量校验 + 并发上传 */
async function onFileChosen(e) {
  const files = Array.from(e.target.files || [])
  // 重置 input value，让用户可以再次选择同一个文件
  e.target.value = ''
  await processFiles(files)
}

/**
 * textarea 粘贴事件：clipboardData.files 有内容时拦截默认行为去走上传，
 * 其他情况（粘贴文本）保持默认行为，不影响输入。
 */
async function onTextareaPaste(e) {
  const cd = e.clipboardData || window.clipboardData
  if (!cd) return
  const files = Array.from(cd.files || [])
  if (!files.length) return  // 粘贴的是文本，不接管
  e.preventDefault()
  if (!requireLogin('请先登录后再上传文件')) return
  await processFiles(files)
}

/** 共用：批量校验 + 并发上传到 attachments */
async function processFiles(files) {
  if (!files || !files.length) return
  for (const f of files) {
    if (attachments.value.length >= MAX_ATTACHMENTS) {
      ElMessage.warning(`最多上传 ${MAX_ATTACHMENTS} 个文件`)
      break
    }
    const ext = (f.name.split('.').pop() || '').toLowerCase()
    if (!ALLOWED_EXTENSIONS.has(ext)) {
      ElMessage.error(`不支持的文件类型：${f.name}（不允许 .${ext}）`)
      continue
    }
    if (f.size > MAX_FILE_SIZE) {
      ElMessage.error(`文件超过 5MB：${f.name}`)
      continue
    }

    const item = {
      uid: `att-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
      name: f.name,
      size: f.size,
      ext,
      status: 'uploading',  // uploading | done | error
      error: null,
      url: null,
      content: null,
    }
    attachments.value.push(item)

    // 并发上传：每个文件独立 promise，互不阻塞
    uploadAgentFile(f)
        .then(res => {
          const data = res?.data || res
          const target = attachments.value.find(a => a.uid === item.uid)
          if (!target) return
          target.url = data.url
          target.size = data.size ?? f.size
          target.ext = (data.ext || ext).toLowerCase()
          target.content = data.content || ''
          target.status = 'done'
        })
        .catch(err => {
          const target = attachments.value.find(a => a.uid === item.uid)
          if (!target) return
          target.status = 'error'
          target.error = err?.message || '上传失败'
          ElMessage.error(`${item.name}：${target.error}`)
        })
  }
}

function removeAttachment(idx) {
  attachments.value.splice(idx, 1)
}

/** 工具：把 size 转成可读字符串 */
function formatFileSize(bytes) {
  if (bytes == null) return ''
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(1) + ' MB'
}

async function handleSend(forcedContent) {
  if (!requireLogin('请先登录后再发送消息')) return
  const content = (forcedContent ?? inputText.value).trim()
  if (!content || isSendingHere.value) return

  // 附件状态校验：仍在上传中阻断；有失败项让用户先处理
  if (attachments.value.some(a => a.status === 'uploading')) {
    ElMessage.warning('文件还在解析中，请稍候')
    return
  }
  if (attachments.value.some(a => a.status === 'error')) {
    ElMessage.warning('有文件上传失败，请先移除再发送')
    return
  }
  // 仅取已就绪的附件，构造后端期望的 DTO 形态
  const sendAttachments = attachments.value
      .filter(a => a.status === 'done')
      .map(a => ({
        url: a.url, name: a.name, size: a.size, ext: a.ext, content: a.content,
      }))

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

  // 乐观追加用户消息（带附件元信息，与后端持久化字段一致）
  const optimistic = {
    id: 'tmp-' + Date.now(),
    role: 'user',
    content,
    attachments: sendAttachments.length
        ? sendAttachments.map(a => ({ url: a.url, name: a.name, size: a.size, ext: a.ext }))
        : null,
    createTime: new Date().toISOString(),
  }
  messages.value.push(optimistic)
  if (forcedContent === undefined) inputText.value = ''  // 重试场景不动输入框
  // 附件已随本次请求"消费"，立即清空 UI；失败时也不保留（避免重复发送）
  if (sendAttachments.length) attachments.value = []
  await scrollToBottom()

  // 乐观更新标题：首条消息后立即同步到侧栏 / header（无需等 AI 回复）
  const sessionRef = sessions.value.find(s => s.id === idForThisSend)
  if (sessionRef && (!sessionRef.title || sessionRef.title === '新会话')) {
    sessionRef.title = buildAutoTitle(content)
    sessionRef.updateTime = formatTime(new Date())
  }

  // 标记发送中的会话 id；切换到其他会话时 isSendingHere 会变 false，思考气泡不再显示
  sendingForId.value = idForThisSend

  // 占位 assistant 消息：随 SSE delta 增量更新内容
  // 注意：push 进 ref 数组后，必须用数组里的 reactive proxy 引用，
  // 否则修改原对象不会触发视图更新（Vue 3 ref 会把内层对象自动转为 reactive）。
  let placeholder = {
    id: 'tmp-ai-' + Date.now(),
    role: 'assistant',
    content: '',
    sources: [],
    toolCalls: [],
    createTime: new Date().toISOString(),
    streaming: true,
  }
  let placeholderPushed = false
  const ensurePlaceholder = () => {
    if (placeholderPushed) return
    if (activeSessionId.value !== idForThisSend) return
    messages.value.push(placeholder)
    // 重新拿数组里的 reactive proxy，后续所有改动都走它才会触发响应式
    placeholder = messages.value[messages.value.length - 1]
    placeholderPushed = true
    // placeholder 一入栈就不再需要 thinking 占位气泡
    if (sendingForId.value === idForThisSend) {
      sendingForId.value = null
    }
  }

  let stopped = false
  try {
    await streamChat(
        {
          sessionId: idForThisSend,
          content,
          webSearchEnabled: webSearchEnabled.value,
          attachments: sendAttachments,
          selectedKbIds: selectedKbIds.value.length ? selectedKbIds.value : null,
        },
        {
          onMeta: (data) => {
            if (data?.userMessageId) optimistic.id = data.userMessageId
            ensurePlaceholder()
          },
          onToolCall: (invocation) => {
            ensurePlaceholder()
            if (invocation && placeholder.toolCalls) {
              placeholder.toolCalls.push(invocation)
            }
          },
          onSources: (sources) => {
            ensurePlaceholder()
            if (Array.isArray(sources)) placeholder.sources = sources
          },
          onDelta: (data) => {
            ensurePlaceholder()
            if (data?.text) {
              placeholder.content += data.text
              scrollToBottom()
            }
          },
          onDone: (data) => {
            ensurePlaceholder()
            placeholder.streaming = false
            if (data?.assistantMessageId) placeholder.id = data.assistantMessageId
            if (data?.content) placeholder.content = data.content
          },
          onError: (err) => {
            stopped = true
            if (placeholderPushed) {
              const idx = messages.value.findIndex(m => m === placeholder)
              if (idx > -1) messages.value.splice(idx, 1)
            }
            ElMessage.error(err?.message || 'AI 服务暂时不可用')
          },
        },
    ).done

    if (!stopped) {
      await loadSessions()
      await scrollToBottom()
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
/* ========== 未登录提示卡 ========== */
.agent-login-required {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.login-prompt-card {
  width: min(480px, 92%);
  padding: 40px 32px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-light);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.06);
  text-align: center;
}

.login-prompt-logo {
  width: 96px;
  height: 96px;
  border-radius: 24px;
  object-fit: contain;
  margin-bottom: 16px;
  filter: drop-shadow(0 6px 18px rgba(0, 0, 0, 0.12));
}

.login-prompt-title {
  font-size: 20px;
  font-weight: 600;
  margin: 8px 0 12px;
  color: var(--el-text-color-primary);
}

.login-prompt-desc {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  margin-bottom: 24px;
  line-height: 1.7;
}

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
  flex-wrap: wrap;
  gap: 10px;
  margin: 0 auto 18px;
  max-width: 860px;
  width: 100%;
}

.message-row.is-user {
  flex-direction: row-reverse;
}

/* 操作条：换到第二行，与气泡起点对齐；hover 父行才显示 */
.message-actions {
  flex-basis: 100%;
  display: flex;
  gap: 4px;
  /* avatar 宽度 36 + gap 10 = 46，让按钮对齐 bubble 起点 */
  padding-left: 46px;
  margin-top: -4px;
  opacity: 0;
  transition: opacity 0.18s ease;
}
.message-actions.is-user-actions {
  padding-left: 0;
  padding-right: 46px;
  justify-content: flex-end;
}
.message-row:hover .message-actions {
  opacity: 1;
}
.msg-action-btn {
  height: 26px;
  padding: 0 8px !important;
  color: var(--el-text-color-secondary);
}
.msg-action-btn:hover {
  color: var(--el-color-primary);
  background: var(--el-fill-color-light);
}
.msg-action-btn.msg-action-danger:hover {
  color: var(--el-color-danger);
}
.msg-action-btn.is-disabled {
  opacity: 0.4;
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

/* ========== 已发送消息中的附件卡片：纵向，文件在上、文本在下 ========== */
.msg-attachment-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 8px;
  width: 100%;
}
.msg-attachment-card {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.18);
  text-decoration: none;
  color: inherit;
  transition: background 0.18s;
  min-width: 0;
}
.msg-attachment-card:hover {
  background: rgba(255, 255, 255, 0.3);
}
/* 助手气泡内（理论上目前 user 才显示，预留以防扩展） */
.bubble.assistant .msg-attachment-card {
  background: var(--el-fill-color);
}
.bubble.assistant .msg-attachment-card:hover {
  background: var(--el-fill-color-darker);
}
.msg-attachment-icon {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.25);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
}
.bubble.assistant .msg-attachment-icon {
  background: var(--el-color-primary-light-8);
  color: var(--el-color-primary);
}
.msg-attachment-info {
  flex: 1;
  min-width: 0;
  font-size: 12px;
  line-height: 1.4;
}
.msg-attachment-name {
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.msg-attachment-meta {
  opacity: 0.8;
  font-size: 11px;
  margin-top: 1px;
}
.msg-attachment-ext {
  font-weight: 600;
  letter-spacing: 0.5px;
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

/* ========== 工具调用 chips ========== */
.tool-calls-block {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 10px;
}

.tool-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  max-width: 280px;
  height: 22px;
  padding: 0 8px;
  border-radius: 11px;
  font-size: 12px;
  line-height: 1;
  background: var(--el-fill-color-light);
  color: var(--el-text-color-regular);
  border: 1px solid var(--el-border-color-lighter);
  white-space: nowrap;
  overflow: hidden;
}

.tool-chip--primary {
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  border-color: var(--el-color-primary-light-7);
}

.tool-chip--success {
  background: var(--el-color-success-light-9);
  color: var(--el-color-success);
  border-color: var(--el-color-success-light-7);
}

.tool-chip--warning {
  background: var(--el-color-warning-light-9);
  color: var(--el-color-warning);
  border-color: var(--el-color-warning-light-7);
}

.tool-chip--info {
  background: var(--el-color-info-light-9);
  color: var(--el-color-info);
  border-color: var(--el-color-info-light-7);
}

.tool-chip-icon {
  font-size: 13px;
  flex: 0 0 auto;
}

.tool-chip-text {
  display: inline-block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-width: 0;
}

.tool-chip-label {
  font-weight: 500;
}

.tool-chip-summary {
  margin-left: 4px;
  color: var(--el-text-color-secondary);
  font-weight: 400;
}

/* ========== 输入栏 composer ========== */
.composer {
  display: flex;
  flex-direction: column;
  border: 1px solid var(--el-border-color);
  border-radius: 12px;
  padding: 4px 8px;
  background: var(--el-bg-color);
  transition: border-color 0.15s;
}

.composer:focus-within {
  border-color: var(--el-color-primary);
}

.composer .chat-textarea :deep(.el-textarea__inner) {
  border: none !important;
  box-shadow: none !important;
  padding: 4px 4px 0 !important;
  min-height: 28px !important;
  background: transparent;
  line-height: 1.5;
}

.composer-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  padding-top: 0;
  margin-top: 2px;
}

.composer-toolbar-left {
  display: flex;
  align-items: center;
  gap: 4px;
}

.composer-action {
  color: var(--el-text-color-regular);
}

.composer-toggle {
  padding: 4px 10px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: 1px solid var(--el-border-color);
  border-radius: 14px;
}

.composer-toggle .toggle-label {
  font-size: 12px;
}

.composer-toggle.active {
  color: var(--el-color-primary);
  border-color: var(--el-color-primary-light-5);
  background: var(--el-color-primary-light-9);
}

/* ========== 知识库按钮角标（小数字气泡） ========== */
.kb-badge {
  margin-left: 2px;
}
.kb-badge :deep(.el-badge__content) {
  height: 14px;
  line-height: 14px;
  padding: 0 5px;
  font-size: 10px;
  border: none;
}

/* ========== 来源引用卡片 ========== */
.source-card-tag {
  display: inline-block;
  padding: 1px 6px;
  border-radius: 4px;
  font-size: 10px;
  background: var(--el-fill-color);
  color: var(--el-text-color-regular);
  margin-right: 4px;
}

.source-card-host {
  color: var(--el-text-color-secondary);
}

.sources-block {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px dashed var(--el-border-color-light);
}

.sources-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  cursor: pointer;
  user-select: none;
  border-radius: 4px;
  padding: 2px 4px;
  margin: -2px -4px 6px;
  transition: background-color 0.15s;
}

.sources-title:hover {
  background: var(--el-fill-color-light);
}

.sources-toggle-icon {
  margin-left: auto;
  transition: transform 0.2s ease;
  color: var(--el-text-color-secondary);
}

.sources-toggle-icon.is-open {
  transform: rotate(180deg);
}

.sources-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.source-card {
  flex: 1 1 calc(50% - 8px);
  min-width: 200px;
  max-width: 100%;
  padding: 8px 12px;
  background: var(--el-fill-color-lighter);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.15s, border-color 0.15s, transform 0.15s;
}

.source-card:hover {
  background: var(--el-color-primary-light-9);
  border-color: var(--el-color-primary-light-5);
  transform: translateY(-1px);
}

.source-card-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--el-text-color-primary);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.source-card-meta {
  margin-top: 4px;
  font-size: 11px;
  color: var(--el-text-color-secondary);
}

/* ========== 思考动画 ========== */
.bubble.thinking {
  display: flex;
  gap: 4px;
  align-items: center;
  padding: 14px 16px;
}

/* 气泡内的思考动画（streaming 但 content 还为空时）：
   .dot 的关键帧已在下方定义，这里只控制布局 + 文字 */
.thinking-inline {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 0;
}
.thinking-inline .thinking-text {
  margin-left: 6px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  letter-spacing: 0.5px;
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
  justify-content: center;
  max-width: 760px;
  width: 100%;
  margin: 0 auto;
  padding: 12px 16px 18px;
  background: transparent;
  border-top: none;
}

.composer {
  width: 100%;
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
  height: 30px;
  padding: 0 12px;
  border-radius: 8px;
  font-size: 13px;
}

/* ========== 附件卡片 ========== */
.attachment-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 0 4px 8px;
}
.attachment-card {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  background: var(--el-fill-color-light);
  min-width: 180px;
  max-width: 240px;
  transition: border-color 0.2s, background 0.2s;
}
.attachment-card.is-loading {
  border-color: var(--el-color-primary-light-5);
  background: var(--el-color-primary-light-9);
}
.attachment-card.is-error {
  border-color: var(--el-color-danger-light-5);
  background: var(--el-color-danger-light-9);
}
.attachment-icon {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  border-radius: 6px;
  background: var(--el-color-primary-light-8);
  color: var(--el-color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
}
.attachment-card.is-error .attachment-icon {
  background: var(--el-color-danger-light-8);
  color: var(--el-color-danger);
}
.attachment-icon .is-loading {
  animation: spin 1s linear infinite;
}
@keyframes spin {
  from { transform: rotate(0deg); }
  to   { transform: rotate(360deg); }
}
.attachment-info {
  flex: 1;
  min-width: 0;
  font-size: 12px;
}
.attachment-name {
  color: var(--el-text-color-primary);
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.attachment-meta {
  color: var(--el-text-color-secondary);
  margin-top: 2px;
}
.attachment-meta .attachment-err {
  color: var(--el-color-danger);
}
.attachment-close {
  padding: 0 !important;
  width: 22px !important;
  height: 22px !important;
  color: var(--el-text-color-secondary);
}
.attachment-close:hover {
  color: var(--el-color-danger);
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

<!-- popover 渲染到 body 下，作用域选择器选不到，这里用全局样式 -->
<style>
.kb-popover.el-popover {
  padding: 0 !important;
  border-radius: 10px;
}
.kb-popover .kb-popover-head {
  padding: 10px 14px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.kb-popover .kb-popover-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}
.kb-popover .kb-popover-body {
  max-height: 280px;
  overflow-y: auto;
  padding: 6px 0;
}
.kb-popover .kb-check-list {
  display: flex;
  flex-direction: column;
}
.kb-popover .kb-check-item {
  margin-right: 0;
  width: 100%;
  padding: 6px 14px;
  height: auto;
  white-space: normal;
}
.kb-popover .kb-check-item:hover {
  background: var(--el-fill-color-light);
}
.kb-popover .kb-check-item .el-checkbox__label {
  width: 100%;
  padding-left: 8px;
}
.kb-popover .kb-check-content {
  display: flex;
  flex-direction: column;
}
.kb-popover .kb-check-name {
  font-size: 13px;
  color: var(--el-text-color-primary);
  line-height: 1.4;
}
.kb-popover .kb-check-desc {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 2px;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.kb-popover .kb-popover-foot {
  padding: 8px 14px;
  font-size: 11px;
  color: var(--el-text-color-secondary);
  border-top: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-lighter);
}
</style>
