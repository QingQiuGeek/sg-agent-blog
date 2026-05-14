# 🚀 SG-Agent-Blog

<p align="center">
  <strong>SGAgent-Blog</strong> 是一个基于 <code>Spring Boot 3 + LangChain4j + Vue 3</code> 的前后端分离现代化 <strong>AI 智能博客平台 </strong>。
  项目以“博客 + AI Agent”为主线，在传统内容发布能力之上集成了
  <strong>AI 智能问答、语义检索、 RAG 知识库、 Tool Calling、多轮会话记忆、违禁词检测 </strong> 等一整套生产级 AI 能力，
  致力于为开发者提供一个“开箱即用 + 完全掌控”的个人 AI 内容发布与智能助手平台。
</p>

## 项目截图

### agent 聊天首页

![agent 聊天首页 ](asset/agent聊天首页.png)

### agent 知识库问答

![agent 知识库问答 ](asset/agent知识库问答.png)

### 登陆注册

![ 登陆注册 ](asset/登陆注册.png)

### 点赞收藏评论

![ 点赞收藏评论 ](asset/点赞收藏评论.png)

### 分类和标签管理

![ 分类和标签管理 ](asset/分类和标签管理.png)

### 个人中心

![ 个人中心 ](asset/个人中心.png)

### 关于我

![ 关于我 ](asset/关于我.png)

### 后台管理 - 仪表盘

![ 后台管理 - 仪表盘 ](asset/后台管理-仪表盘.png)

### 联网搜索

![ 联网搜索 ](asset/联网搜索.png)

### 留言板

![ 留言板 ](asset/留言板.png)

### 首页

![ 首页 ](asset/首页.png)

### 文章标签

![ 文章标签 ](asset/文章标签.png)

### 文章分类

![ 文章分类 ](asset/文章分类.png)

### 文章管理

![ 文章管理 ](asset/文章管理.png)

### 文章归档

![ 文章归档 ](asset/文章归档.png)

### 文章搜索和消息通知

![ 文章搜索和消息通知 ](asset/文章搜索和消息通知.png)

### 文章详情

![ 文章详情 ](asset/文章详情.png)

### 我的知识库

![ 我的知识库 ](asset/我的知识库.png)

### 系统管理

![ 系统管理 ](asset/系统管理.png)

### 系统监控

![ 系统监控 ](asset/系统监控.png)

### 新建文章

![ 新建文章 ](asset/新建文章.png)

### 意见反馈

![ 意见反馈 ](asset/意见反馈.png)

### 运营管理

![ 运营管理 ](asset/运营管理.png)

### 站内文章语义检索

![ 站内文章语义检索 ](asset/站内文章语义检索.png)

## ✨ 核心特性

### 🤖 AI 能力（项目亮点）

- **智能问答 Agent**：基于 LangChain4j + ReAct 循环，支持大模型多轮推理与自主调度工具；后端 SSE 流式输出，前端打字机渲染。
- **Tool Calling 工具箱**：内置 6+ 工具可供 LLM 自动选调——
  `searchArticles`（站内文章语义检索）、`searchKnowledgeBase`（私有 RAG）、`webSearch`（联网搜索）、
  `wanxGenerateImage`/`qwenGenerateImage`（文生图）、`dateTimeTool`（时间）等。
- **RAG 知识库检索**：支持 PDF /DOCX /TXT /MD 上传，后端按 chunk 切分后调用 Embedding 模型入库，
  提问时走「余弦相似度 + 绝对阈值 + 相对截断」双策略过滤，避免低相关性内容污染回答。
- **语义检索 Semantic Search**：文章发布后自动生成向量入库，支持跨词面检索（比如「如何优化接口响应」可召回「接口性能调优」），
  与传统 SQL LIKE 检索互补。
- **多会话记忆持久化**：会话 /消息双表模型（`chat_session` + `chat_message`），支持上下文窗口滑动累计、
  会话重命名、重试生成、选择性删除、会话标题自动总结。
- **Token 用量统计**：从 `chat_message` 表聊天记录中聚合，个人中心展示总用量、近一周曲线、 AI/用户用量占比（ Echarts）。
- **文件与附件理解**：上传 PDF /DOCX /Markdown /代码文件等，后端 Apache Tika 提取文本后拼接进上下文以供 LLM 阅读。
- **AI 辅助创作**：文章发布时一键 AI 生成摘要 /标题 /分类 /标签，后台可配置提示词模板。
- **来源引用（Citations）**： AI 回复中文章 /网页引用会以可点击卡片形式呈现，点击直达原文。

### � 博客与社区

- **沉浸式创作**： MdEditorV3 、 Emoji、图片裁剪上传、草稿箱、定时发布。
- **丰富互动**：文章点赞 /收藏 /多级评论（带 Emoji） /弹幕留言墙。
- **“我的创作”**：所有用户可发文，个人中心仅展示本人文章与编辑入口，与后台“全站文章”视图隔离。

### 🛡️ 安全与风控

- **违禁词 /敏感词检测**： DFA 多模式匹配，应用于文章 /评论 /弹幕 /AI Prompt，高命中拦截低命中打码。
- **JWT + BCrypt**： Auth0 JWT 双 Token + Spring Security Crypto 密码哈希，防 CSRF /重放。
- **图形验证码 + 限流**：登录 /AI 对话 /发布接口都有频率限制，防恶意调用。
- **XSS 过滤 + 举报机制**：前后端双重转义，社区举报后端处理闭环。

### 📊 数据与监控

- **所有 AI 调用可追溯**：`chat_message` 记录 prompt /completion /model /latency /tokens /tool 调用链路。
- **Echarts 可视化**： PV /UV /IP 属地分布 /Token 趋势。
- **操作日志 + 登录日志**： AOP 拦截，落到独立表供审计。
- **IP2Region**：本地 IP 属地查询，零依赖外部 API。

### ⚙️ 后台与调度

- **用户 /分类 /标签 /评论 /举报 /反馈** 一体化管理。
- **Quartz 定时任务**：可动态增 /改 /启停，内置「过期会话清理」「热门文章重算」「违禁词重加载」等作业。
- **系统消息 /意见反馈**：站内信 + 指定用户推送。

## 🧠 AI 架构设计

```
用户提问
   │
   ▼
[ AgentChatController ]  ── SSE 流式返回
   │
   ▼
[ AgentChatService ]  ──── 会话 /消息 /token 统计 落库
   │
   ▼
+─────────────────────────+
|     ReAct 循环（ LangChain4j）     |
|  ╔═══════════════════════╗     |
|  ║ LLM 推理 → 选择 Tool       ║     |
|  ║   ↓                          ║     |
|  ║ AgentToolRegister 调度      ║     |
|  ║   ├─ searchArticles         ║     |
|  ║   ├─ searchKnowledgeBase    ║ RAG |
|  ║   ├─ webSearch              ║     |
|  ║   ├─ wanx/qwenGenerateImage ║     |
|  ║   └─ dateTimeTool           ║     |
|  ║   ↓                          ║     |
|  ║ 合并 ToolResult → 下一轮 LLM ║     |
|  ╚════════════════════════╝     |
+─────────────────────────+
   │
   ▼
输出：纯文本 + 来源引用 + 工具调用记录
```

- **上下文构造**：静态 SystemPrompt + 动态注入（如「本轮勾选了 N 个知识库」） + 最近 N 轮历史 + 附件提取文本。
- **向量检索**： MySQL `JSON` 存 embedding（`article_vector` /`kb_chunk`）， JVM 内存计算余弦相似度 → 阈值过滤 → topK；中小规模下零运维依赖、足够胜任。
- **提示词工程**：统一在 SystemPrompt 里明确「何时调什么工具」「返回为空不要编造」，有效控制幻觉。

## 🛠️ 技术栈

### 后端 (Backend)

#### 中间件与框架

- **Spring Boot 3.5.9 /Java 21**：虚拟线程 + 现代语法
- **MyBatis-Plus /MySQL 8.0**： ORM + 主数据库（`JSON` 存向量）
- **Redis**：验证码 /Token 黑名单 /热点缓存 /SSE 会话发布订阅
- **Quartz**：动态调度作业
- **Knife4j (OpenAPI 3)**：接口文档

#### 🤖 AI 与 LLM

- **LangChain4j 0.x**：核心 LLM 编排， ChatModel /ToolSpecification /EmbeddingModel
- **DashScope (通义千问 Qwen)**：主聊天模型 + 文生图（ wanx /qwen-image）
- **Embedding 模型**：`text-embedding-v4`（ 1024 维，中文高质量）
- **Apache Tika**： PDF /DOCX /PPTX 上下文文本提取
- **SSE (Server-Sent Events)**：流式 token 下发
- **DFA 违禁词过滤**：自实现多模式匹配过滤器

#### 安全与工具

- **JWT (Auth0) + Spring Security Crypto (BCrypt)**：身份认证与密码哈希
- **Lombok /MapStruct /Hutool /Apache POI /FreeMarker**
- **阿里云 OSS**：静态资源 /AI 生图中转永久化
- **IP2Region**：本地 IP 属地库

### 前端 (Frontend)

- **Vue 3.5.25 (Composition API) + Vite**
- **Pinia /Vue Router**
- **Element Plus**： UI 组件库
- **TailwindCSS**：原子样式
- **Axios + EventSource Polyfill**： HTTP + SSE 流式接受
- **md-editor-v3**： Markdown 编辑器 + 预览器（ AI 回复复用）
- **Echarts**： PV /UV /Token 曲线
- **vue-cropper /xss /crypto-js**：裁剪 /防注入 /前端加密

---

## 🗄️ 核心数据表

| 模块    | 核心表                                            | 说明                                        |
| ------- | ------------------------------------------------- | ------------------------------------------- |
| AI 会话 | `chat_session` /`chat_message`                    | 会话与多轮消息、 token 计量、 tool 调用链路 |
| AI 检索 | `article_vector`                                  | 文章向量（ JSON） + 摘要索引                |
| RAG     | `knowledge_base` /`kb_file` /`kb_chunk`           | 知识库 /上传文件 /切片向量                  |
| 博客    | `article` /`category` /`tag` /`comment`           | 正文 /分类 /标签 /多级评论                  |
| 账号    | `user` /`role` /`permission`                      | RBAC                                        |
| 风控    | `sensitive_word` /`report` /`op_log` /`login_log` | 违禁词 /举报 /日志                          |

---

## 💻 环境要求

在运行该项目之前，请确保您的开发环境具备以下条件（括号内为测试通过的稳定版本）：

- **Java**: JDK 21+ (`v21.0.9`)
- **Node.js**: v20.19.0+ /v22.12.0+ (`v22.21.1`)
- **MySQL**: 8.0+ (`v8.0.44`)
- **Redis**: 5.0+ (`v5.0.14`)

---

## 🚀 快速开始

**1. 数据库准备**

- 运行项目根目录下的 `sg-agent-blog.sql` 创建数据库 `sg-agent-blog`，导入数据表结构。

**2. 后端启动**

- 修改 `application.yml` 中的 MySQL、 Redis 连接配置及相关密钥。
- 运行主启动类启动 Spring Boot 服务。
- 接口文档访问地址（默认）：`http://localhost:8080/doc.html`

**3. 前端启动**

```bash
# 安装依赖
npm install

# 启动本地开发服务
npm run dev
```

**4. AI 与向量能力配置**

- 在 `application.yml` 填入 DashScope /OpenAI 兼容端点的 `api-key` 与模型名。
- 首次启动后发布一篇文章即可触发语义入库；在 `个人中心 → 我的知识库` 上传文件可触发 RAG 入库。
- AI 对话入口：`http://localhost:5173/agent`。

---
