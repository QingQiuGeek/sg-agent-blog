package com.example.blog.modules.agent.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.blog.common.constants.MessageConstants;
import com.example.blog.common.enums.ResultCode;
import com.example.blog.core.exception.CustomerException;
import com.example.blog.core.security.UserContext;
import com.example.blog.modules.agent.mapper.ChatMessageMapper;
import com.example.blog.modules.agent.mapper.ChatSessionMapper;
import com.example.blog.modules.agent.model.dto.ChatRequestDTO;
import com.example.blog.modules.agent.model.dto.SessionRenameDTO;
import com.example.blog.modules.agent.model.entity.ChatMessage;
import com.example.blog.modules.agent.model.entity.ChatSession;
import com.example.blog.modules.agent.model.vo.ArticleSourceVO;
import com.example.blog.modules.agent.model.vo.ChatMessageVO;
import com.example.blog.modules.agent.model.vo.ChatReplyVO;
import com.example.blog.modules.agent.model.vo.ChatSessionVO;
import com.example.blog.modules.agent.model.vo.ToolInvocationVO;
import com.example.blog.modules.agent.service.AgentChatService;
import com.example.blog.modules.agent.tools.AgentToolRegister;
import com.example.blog.modules.agent.tools.ArticleSearchTool;
import com.example.blog.modules.agent.tools.WebSearchTool;
import com.example.blog.modules.user.model.dto.UserPayloadDTO;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * AI 对话业务实现
 */
@Slf4j
@Service
public class AgentChatServiceImpl implements AgentChatService {

    /** 系统提示词，定义 AI 角色 + 工具使用策略 */
    private static final String SYSTEM_PROMPT = """
            你是 SGAgent，一个友善、专业的中文 AI 助手，由 SGAgent-Blog 提供。
            请用简洁、清晰的中文回答用户问题，适当使用 Markdown 格式（标题、列表、代码块）提升可读性。

            【工具使用策略】
            - 用户询问站内博客内容（如"我写过哪些 X 文章"、"博客里关于 Y 的内容"）→ 调用 searchArticles。
              引用文章时，**必须使用工具返回的 path 字段（形如 /post/文章id）原样写成 Markdown 链接** [标题](/post/文章id)，
              **严禁自己拼接 http://xxx.com/articles/... 之类的假域名**，前端会自动按当前访问域名解析。
            - 用户询问实时信息、新闻、最新数据等 → 调用 webSearch（联网搜索）。
            - 用户要求绘图/生成图片/出海报/出 icon → 优先调用 wanxGenerateImage 或 qwenGenerateImage；二者失败再降级 generateImage。
              工具返回的 `![alt](url)` Markdown 必须原样写进最终回复，否则用户看不到图。
            - 用户问当前时间/日期 → 调用 dateTimeTool。
            - 闲聊或自身知识能回答的常识问题 → 直接回答，不要调用工具。
            - 同一类工具最多重复调用 2 次；results 为空时不要编造，直接告知用户。

            如果不知道答案就如实说明，不要编造信息。
            """;

    /** 上下文窗口：每次最多带最近 N 条消息发给模型 */
    private static final int CONTEXT_WINDOW = 20;

    /** ReAct 循环最大步数（防止 LLM 无限调用工具） */
    private static final int MAX_TOOL_STEPS = 5;

    /** 会话默认标题 */
    private static final String DEFAULT_TITLE = "新会话";

    /** 自动生成标题的最大长度 */
    private static final int AUTO_TITLE_MAX_LEN = 30;

    @Resource
    private ChatSessionMapper chatSessionMapper;

    @Resource
    private ChatMessageMapper chatMessageMapper;

    @Resource
    private ChatModel chatModel;

    @Resource
    private AgentToolRegister agentToolRegister;

    /** SSE 流式回复专用线程池：守护线程 + 命名前缀，便于排查 */
    private final ExecutorService streamExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "agent-chat-stream-" + System.nanoTime());
        t.setDaemon(true);
        return t;
    });

    @Override
    public List<ChatSessionVO> listSessions() {
        Long userId = currentUserId();
        List<ChatSession> list = chatSessionMapper.selectList(
                new LambdaQueryWrapper<ChatSession>()
                        .eq(ChatSession::getUserId, userId)
                        .orderByDesc(ChatSession::getUpdateTime));
        List<ChatSessionVO> result = new ArrayList<>(list.size());
        for (ChatSession s : list) {
            ChatSessionVO vo = new ChatSessionVO();
            BeanUtil.copyProperties(s, vo);
            result.add(vo);
        }
        return result;
    }

    @Override
    public ChatSessionVO createSession() {
        ChatSession session = doCreateSession();
        ChatSessionVO vo = new ChatSessionVO();
        BeanUtil.copyProperties(session, vo);
        return vo;
    }

    /** 内部创建会话，返回实体（id 由 MP @TableId(ASSIGN_UUID) 自动填充） */
    private ChatSession doCreateSession() {
        Long userId = currentUserId();
        ChatSession session = ChatSession.builder()
                .userId(userId)
                .title(DEFAULT_TITLE)
                .isDeleted(0)
                .build();
        chatSessionMapper.insert(session);
        return session;
    }

    @Override
    public void renameSession(String sessionId, SessionRenameDTO dto) {
        ChatSession session = requireOwnedSession(sessionId);
        session.setTitle(dto.getTitle());
        chatSessionMapper.updateById(session);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSession(String sessionId) {
        ChatSession session = requireOwnedSession(sessionId);
        // 会话走 deleteById，有主键条件与 @TableLogic 能正确生成 SET is_deleted=1
        chatSessionMapper.deleteById(session.getId());
        // ChatMessage 无主键条件时部分 MP 版本会遗漏 SET子句；这里显式指定避免坑
        UpdateWrapper<ChatMessage> uw = new UpdateWrapper<>();
        uw.eq("session_id", session.getId()).set("is_deleted", 1);
        chatMessageMapper.update(null, uw);
    }

    @Override
    public List<ChatMessageVO> listMessages(String sessionId) {
        ChatSession session = requireOwnedSession(sessionId);
        List<ChatMessage> list = chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, session.getId())
                        .orderByAsc(ChatMessage::getCreateTime)
                        .orderByAsc(ChatMessage::getId));
        List<ChatMessageVO> result = new ArrayList<>(list.size());
        for (ChatMessage m : list) {
            ChatMessageVO vo = new ChatMessageVO();
            BeanUtil.copyProperties(m, vo);
            vo.setSources(parseJsonList(m.getSourcesJson(), ArticleSourceVO.class, "sources_json"));
            vo.setToolCalls(parseJsonList(m.getToolCallsJson(), ToolInvocationVO.class, "tool_calls_json"));
            result.add(vo);
        }
        return result;
    }

    /** 通用 JSON 数组反序列化，失败返回 null 不影响主流程 */
    private <T> List<T> parseJsonList(String json, Class<T> clazz, String fieldHint) {
        if (StrUtil.isBlank(json)) {
            return null;
        }
        try {
            return JSONUtil.toList(json, clazz);
        } catch (Exception e) {
            log.warn("反序列化 {} 失败：{}", fieldHint, e.getMessage());
            return null;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatReplyVO chat(ChatRequestDTO requestDTO) {
        Long userId = currentUserId();

        // 1. 获取或新建会话
        ChatSession session;
        if (StrUtil.isBlank(requestDTO.getSessionId())) {
            session = doCreateSession();
        } else {
            session = requireOwnedSession(requestDTO.getSessionId());
        }

        // 2. 持久化用户消息
        String userContent = requestDTO.getContent().trim();
        ChatMessage userMsg = ChatMessage.builder()
                .sessionId(session.getId())
                .userId(userId)
                .role("user")
                .content(userContent)
                .tokenCount(estimateTokens(userContent))
                .isDeleted(0)
                .build();
        chatMessageMapper.insert(userMsg);

        // 3. 取最近上下文 + 调模型（ReAct 循环，可能触发 tool 调用）
        LlmAnswer answer;
        try {
            answer = callLlm(session.getId(), userContent, null);
        } catch (Exception e) {
            log.error("AI 调用失败，sessionId={}", session.getId(), e);
            throw new CustomerException(ResultCode.INTERNAL_SERVER_ERROR,
                    "AI 服务暂时不可用，请稍后再试");
        }

        String reply = sanitizeArticleLinks(answer.text());
        List<ArticleSourceVO> sources = answer.sources();
        List<ToolInvocationVO> toolCalls = answer.toolCalls();
        String sourcesJson = CollUtil.isEmpty(sources) ? null : JSONUtil.toJsonStr(sources);
        String toolCallsJson = CollUtil.isEmpty(toolCalls) ? null : JSONUtil.toJsonStr(toolCalls);

        // 4. 持久化 AI 回复（含来源引用 + 工具调用记录）
        ChatMessage aiMsg = ChatMessage.builder()
                .sessionId(session.getId())
                .userId(userId)
                .role("assistant")
                .content(reply)
                .tokenCount(estimateTokens(reply))
                .sourcesJson(sourcesJson)
                .toolCallsJson(toolCallsJson)
                .isDeleted(0)
                .build();
        chatMessageMapper.insert(aiMsg);

        // 5. 首条消息时自动设置标题；同时刷新 update_time
        boolean isFirstUserMessage = DEFAULT_TITLE.equals(session.getTitle());
        if (isFirstUserMessage) {
            session.setTitle(buildAutoTitle(userContent));
        }
        session.setUpdateTime(LocalDateTime.now());
        chatSessionMapper.updateById(session);

        return ChatReplyVO.builder()
                .sessionId(session.getId())
                .userMessageId(userMsg.getId())
                .assistantMessageId(aiMsg.getId())
                .content(reply)
                .sources(sources)
                .toolCalls(toolCalls)
                .build();
    }

    @Override
    public void chatStream(ChatRequestDTO requestDTO, SseEmitter emitter) {
        // 在 servlet 线程预取 userId（异步线程拿不到 UserContext）
        final Long userId = currentUserId();
        final String content = requestDTO.getContent().trim();
        final String reqSessionId = requestDTO.getSessionId();

        // 客户端断开 / 超时 / 异常时清理
        emitter.onCompletion(() -> log.debug("SSE completed"));
        emitter.onTimeout(() -> {
            log.warn("SSE timeout");
            emitter.complete();
        });
        emitter.onError(ex -> log.warn("SSE error: {}", ex.getMessage()));

        streamExecutor.execute(() -> {
            try {
                // 1. 解析或新建会话（异步线程内不能调用 currentUserId / requireOwnedSession）
                ChatSession session;
                if (StrUtil.isBlank(reqSessionId)) {
                    session = ChatSession.builder().userId(userId).title(DEFAULT_TITLE).isDeleted(0).build();
                    chatSessionMapper.insert(session);
                } else {
                    session = chatSessionMapper.selectById(reqSessionId);
                    if (session == null) {
                        sendEvent(emitter, "error", Map.of("msg", "会话不存在"));
                        emitter.complete();
                        return;
                    }
                    if (!session.getUserId().equals(userId)) {
                        sendEvent(emitter, "error", Map.of("msg", "无权访问该会话"));
                        emitter.complete();
                        return;
                    }
                }

                // 2. 持久化用户消息
                ChatMessage userMsg = ChatMessage.builder()
                        .sessionId(session.getId())
                        .userId(userId)
                        .role("user")
                        .content(content)
                        .tokenCount(estimateTokens(content))
                        .isDeleted(0)
                        .build();
                chatMessageMapper.insert(userMsg);

                // 3. meta 事件：让前端拿到 sessionId（首次新建时） + userMessageId
                sendEvent(emitter, "meta", Map.of(
                        "sessionId", session.getId(),
                        "userMessageId", userMsg.getId()));

                // 4. 调 LLM（ReAct），工具调用即时推送 tool_call 事件
                LlmAnswer answer;
                try {
                    answer = callLlm(session.getId(), content,
                            invocation -> sendEvent(emitter, "tool_call", invocation));
                } catch (Exception e) {
                    log.error("AI 调用失败，sessionId={}", session.getId(), e);
                    sendEvent(emitter, "error", Map.of("msg", "AI 服务暂时不可用，请稍后再试"));
                    emitter.complete();
                    return;
                }

                String reply = sanitizeArticleLinks(answer.text());
                List<ArticleSourceVO> sources = answer.sources();
                List<ToolInvocationVO> toolCalls = answer.toolCalls();

                // 5. 先推 sources（让前端来源卡片可在文本之前先就位）
                if (CollUtil.isNotEmpty(sources)) {
                    sendEvent(emitter, "sources", sources);
                }

                // 6. 文本伪流式：按字符切片 + 微小 sleep，体感与原生 streaming 一致
                streamText(emitter, reply);

                // 7. 持久化 assistant 消息
                String sourcesJson = CollUtil.isEmpty(sources) ? null : JSONUtil.toJsonStr(sources);
                String toolCallsJson = CollUtil.isEmpty(toolCalls) ? null : JSONUtil.toJsonStr(toolCalls);
                ChatMessage aiMsg = ChatMessage.builder()
                        .sessionId(session.getId())
                        .userId(userId)
                        .role("assistant")
                        .content(reply)
                        .tokenCount(estimateTokens(reply))
                        .sourcesJson(sourcesJson)
                        .toolCallsJson(toolCallsJson)
                        .isDeleted(0)
                        .build();
                chatMessageMapper.insert(aiMsg);

                // 8. 首条消息时自动设置标题；同时刷新 update_time
                if (DEFAULT_TITLE.equals(session.getTitle())) {
                    session.setTitle(buildAutoTitle(content));
                }
                session.setUpdateTime(LocalDateTime.now());
                chatSessionMapper.updateById(session);

                // 9. done 事件：回传完整内容 + assistantMessageId，便于前端校准
                sendEvent(emitter, "done", Map.of(
                        "assistantMessageId", aiMsg.getId(),
                        "content", reply));
                emitter.complete();
            } catch (Exception e) {
                log.error("chatStream 异步处理异常", e);
                try {
                    sendEvent(emitter, "error", Map.of("msg", "服务异常"));
                } catch (Exception ignore) {
                }
                emitter.completeWithError(e);
            }
        });
    }

    /** 文本按字符分块发送，模拟流式输出 */
    private void streamText(SseEmitter emitter, String text) throws Exception {
        if (StrUtil.isBlank(text)) return;
        // 每片 6 个字符；中文每字按字符算，英文一片 ≈ 1~2 个单词
        final int chunkSize = 6;
        for (int i = 0; i < text.length(); i += chunkSize) {
            String piece = text.substring(i, Math.min(i + chunkSize, text.length()));
            sendEvent(emitter, "delta", Map.of("text", piece));
            // 控制速率：约 60 字/秒，避免过快导致前端动效失效；长文末段提速
            Thread.sleep(i < 600 ? 25 : 10);
        }
    }

    /**
     * 兜底净化 LLM 输出里的「假文章链接」：不管 LLM 写成什么域名，
     * 一律重写为站内相对路径 /post/{id}，由前端按当前 origin 解析。
     * <p>
     * 命中规则（任意 host + 常见路径段 + 纯数字 id）：
     * <ul>
     *   <li>http(s)://任意host/articles/123</li>
     *   <li>http(s)://任意host/article/123</li>
     *   <li>http(s)://任意host/post/123</li>
     *   <li>http(s)://任意host/posts/123</li>
     * </ul>
     */
    private static final java.util.regex.Pattern FAKE_ARTICLE_URL = java.util.regex.Pattern.compile(
            "https?://[^\\s)\\]\"']+?/(?:articles?|posts?)/(\\d+)",
            java.util.regex.Pattern.CASE_INSENSITIVE);

    private String sanitizeArticleLinks(String text) {
        if (StrUtil.isBlank(text)) return text;
        java.util.regex.Matcher m = FAKE_ARTICLE_URL.matcher(text);
        if (!m.find()) return text;
        StringBuilder sb = new StringBuilder();
        m.reset();
        while (m.find()) {
            m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement("/post/" + m.group(1)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /** 统一 SSE 事件发送（封装异常打印） */
    private void sendEvent(SseEmitter emitter, String name, Object data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data));
        } catch (Exception e) {
            log.warn("SSE send 失败 event={}: {}", name, e.getMessage());
        }
    }

    /* ===================== 辅助方法 ===================== */

    private Long currentUserId() {
        UserPayloadDTO user = UserContext.get();
        if (user == null) {
            throw new CustomerException(ResultCode.UNAUTHORIZED, MessageConstants.MSG_NOT_LOGIN);
        }
        return user.getId();
    }

    /** 校验会话存在且属于当前用户，否则抛错 */
    private ChatSession requireOwnedSession(String sessionId) {
        if (StrUtil.isBlank(sessionId)) {
            throw new CustomerException(ResultCode.PARAM_ERROR, "会话ID不能为空");
        }
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new CustomerException(ResultCode.NOT_FOUND, "会话不存在");
        }
        if (!session.getUserId().equals(currentUserId())) {
            throw new CustomerException(ResultCode.FORBIDDEN, "无权访问该会话");
        }
        return session;
    }

    /**
     * 取最近上下文 + 当前问题，走 ReAct 循环（最多 MAX_TOOL_STEPS 步）
     * 返回 LLM 最终回复 + 过程中所有 tool 调用累积的 sources
     */
    private LlmAnswer callLlm(String sessionId, String currentUserContent,
                              Consumer<ToolInvocationVO> onToolCall) {
        // 取最近 CONTEXT_WINDOW 条历史（含当前刚插入的 user 消息）
        List<ChatMessage> historyDesc = chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .orderByDesc(ChatMessage::getCreateTime)
                        .orderByDesc(ChatMessage::getId)
                        .last("LIMIT " + (CONTEXT_WINDOW + 1)));
        List<ChatMessage> history = new ArrayList<>(historyDesc);
        java.util.Collections.reverse(history);

        List<dev.langchain4j.data.message.ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(SYSTEM_PROMPT));
        for (ChatMessage m : history) {
            if ("user".equals(m.getRole())) {
                messages.add(UserMessage.from(m.getContent()));
            } else if ("assistant".equals(m.getRole())) {
                messages.add(AiMessage.from(m.getContent()));
            }
        }
        // 兜底：如最后一条不是与当前问题对应的 user message，补一条
        if (messages.isEmpty()
                || !(messages.get(messages.size() - 1) instanceof UserMessage last)
                || !currentUserContent.equals(last.singleText())) {
            messages.add(UserMessage.from(currentUserContent));
        }

        // 收集 sources：按唯一 key 去重，保留首次出现顺序
        Map<String, ArticleSourceVO> sourceMap = new LinkedHashMap<>();
        // 收集 toolCalls：按调用顺序，列表可能有重复（多次调同一工具），全部保留
        List<ToolInvocationVO> toolCalls = new ArrayList<>();

        for (int step = 0; step < MAX_TOOL_STEPS; step++) {
            ChatRequest.Builder builder = ChatRequest.builder().messages(messages);
            if (!agentToolRegister.isEmpty()) {
                builder.toolSpecifications(agentToolRegister.getSpecifications());
            }
            ChatResponse response = chatModel.chat(builder.build());
            AiMessage ai = response.aiMessage();
            messages.add(ai);

            if (!ai.hasToolExecutionRequests()) {
                String text = ai.text();
                if (StrUtil.isBlank(text)) {
                    text = "（AI 暂时无话可说）";
                }
                return new LlmAnswer(text, new ArrayList<>(sourceMap.values()), toolCalls);
            }

            // 执行所有 tool 调用，并把结果消息追加到上下文；同步记录调用元信息
            for (ToolExecutionRequest req : ai.toolExecutionRequests()) {
                ToolInvocationVO invocation = buildToolInvocation(req);
                toolCalls.add(invocation);
                if (onToolCall != null) {
                    try {
                        onToolCall.accept(invocation);
                    } catch (Exception ex) {
                        log.warn("tool call 回调异常：{}", ex.getMessage());
                    }
                }
                String result = agentToolRegister.execute(req);
                accumulateSources(req.name(), result, sourceMap);
                messages.add(ToolExecutionResultMessage.from(req, result));
            }
        }

        // 超过最大步数仍未停止：尽量取最后一次 AI 文本，没有就提示
        String fallback = "AI 调用工具次数超过上限，未能给出最终答案。请重试或换种问法。";
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i) instanceof AiMessage ai && StrUtil.isNotBlank(ai.text())) {
                fallback = ai.text();
                break;
            }
        }
        log.warn("ReAct 循环达到最大步数 {}，提前返回。sessionId={}", MAX_TOOL_STEPS, sessionId);
        return new LlmAnswer(fallback, new ArrayList<>(sourceMap.values()), toolCalls);
    }

    /**
     * 从 ToolExecutionRequest 提取要展示给前端的「工具调用 chip」
     * - label: 通过 AgentToolRegister 反查 ITool.getDescription
     * - summary: 优先从 arguments 中取 query/prompt 字段，截断展示
     */
    private ToolInvocationVO buildToolInvocation(ToolExecutionRequest req) {
        String summary = "";
        try {
            String args = req.arguments();
            if (StrUtil.isNotBlank(args) && JSONUtil.isTypeJSONObject(args)) {
                JSONObject obj = JSONUtil.parseObj(args);
                for (String key : new String[]{"query", "prompt", "message", "text"}) {
                    String v = obj.getStr(key);
                    if (StrUtil.isNotBlank(v)) {
                        summary = v.length() > 40 ? v.substring(0, 40) + "..." : v;
                        break;
                    }
                }
            }
        } catch (Exception ignore) {
            // ignore parse error
        }
        return ToolInvocationVO.builder()
                .name(req.name())
                .label(agentToolRegister.getLabel(req.name()))
                .summary(summary)
                .build();
    }

    /**
     * 从 tool 执行结果中解析「来源引用」并合并到累积 map
     * <p>
     * 不同工具返回的 JSON 结构不同，需要按工具名分支解析：
     * <ul>
     *   <li>{@code searchArticles}：{@code {results:[{articleId,title,author}]}} → article 类型</li>
     *   <li>{@code webSearch}：{@code {results:[{url,title,content}]}} → web 类型</li>
     * </ul>
     * 这与「LLM 自动选哪个工具」是两件事——LLM 选完工具调用结束后，
     * 后端要把异构的工具输出归一成统一的 source 卡片结构供前端渲染。
     */
    private void accumulateSources(String toolName, String resultJson,
                                   Map<String, ArticleSourceVO> sourceMap) {
        if (StrUtil.isBlank(resultJson) || !JSONUtil.isTypeJSONObject(resultJson)) {
            return;
        }
        try {
            JSONObject json = JSONUtil.parseObj(resultJson);
            JSONArray results = json.getJSONArray("results");
            if (results == null || results.isEmpty()) {
                return;
            }
            if (ArticleSearchTool.TOOL_NAME.equals(toolName)) {
                for (int i = 0; i < results.size(); i++) {
                    JSONObject item = results.getJSONObject(i);
                    Long id = item.getLong("articleId");
                    if (id == null) continue;
                    String key = "article:" + id;
                    if (sourceMap.containsKey(key)) continue;
                    sourceMap.put(key, ArticleSourceVO.builder()
                            .type("article")
                            .articleId(id)
                            .title(item.getStr("title"))
                            .author(item.getStr("author"))
                            .build());
                }
            } else if (WebSearchTool.TOOL_NAME.equals(toolName)) {
                for (int i = 0; i < results.size(); i++) {
                    JSONObject item = results.getJSONObject(i);
                    String url = item.getStr("url");
                    if (StrUtil.isBlank(url)) continue;
                    String key = "web:" + url;
                    if (sourceMap.containsKey(key)) continue;
                    sourceMap.put(key, ArticleSourceVO.builder()
                            .type("web")
                            .url(url)
                            .title(item.getStr("title", url))
                            .build());
                }
            }
            // 其他工具（图片生成、时间等）不产生来源卡片
        } catch (Exception e) {
            log.warn("解析 {} 工具结果失败：{}", toolName, e.getMessage());
        }
    }

    /** LLM 单轮回答 = 最终文本 + 累积来源 + 工具调用记录 */
    private record LlmAnswer(String text,
                             List<ArticleSourceVO> sources,
                             List<ToolInvocationVO> toolCalls) {
    }

    /** 简单的 token 估算：1 token ≈ 1 中文字符 / 4 英文字符。仅用于统计，非精确计费 */
    private Integer estimateTokens(String text) {
        if (StrUtil.isBlank(text)) {
            return 0;
        }
        int chinese = 0;
        int other = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 0x4E00 && c <= 0x9FFF) {
                chinese++;
            } else {
                other++;
            }
        }
        return chinese + other / 4;
    }

    /** 取首条消息的前 N 个字符做会话标题 */
    private String buildAutoTitle(String userContent) {
        String trimmed = userContent.replaceAll("\\s+", " ").trim();
        if (CollUtil.isEmpty(java.util.Collections.singletonList(trimmed))) {
            return DEFAULT_TITLE;
        }
        return trimmed.length() > AUTO_TITLE_MAX_LEN
                ? trimmed.substring(0, AUTO_TITLE_MAX_LEN) + "…"
                : trimmed;
    }
}
