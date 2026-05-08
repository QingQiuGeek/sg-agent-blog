package com.example.blog.modules.agent.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
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
import com.example.blog.modules.agent.model.vo.ChatMessageVO;
import com.example.blog.modules.agent.model.vo.ChatReplyVO;
import com.example.blog.modules.agent.model.vo.ChatSessionVO;
import com.example.blog.modules.agent.service.AgentChatService;
import com.example.blog.modules.user.model.dto.UserPayloadDTO;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * AI 对话业务实现
 */
@Slf4j
@Service
public class AgentChatServiceImpl implements AgentChatService {

    /** 系统提示词，定义 AI 角色 */
    private static final String SYSTEM_PROMPT = """
            你是 SGAgent，一个友善、专业的中文 AI 助手，由 SGAgent-Blog 提供。
            请用简洁、清晰的中文回答用户问题，适当使用 Markdown 格式（标题、列表、代码块）提升可读性。
            如果不知道答案就如实说明，不要编造信息。
            """;

    /** 上下文窗口：每次最多带最近 N 条消息发给模型 */
    private static final int CONTEXT_WINDOW = 20;

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
            result.add(vo);
        }
        return result;
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

        // 3. 取最近上下文 + 调模型
        String reply;
        try {
            reply = callLlm(session.getId(), userContent);
        } catch (Exception e) {
            log.error("AI 调用失败，sessionId={}", session.getId(), e);
            throw new CustomerException(ResultCode.INTERNAL_SERVER_ERROR,
                    "AI 服务暂时不可用，请稍后再试");
        }

        // 4. 持久化 AI 回复
        ChatMessage aiMsg = ChatMessage.builder()
                .sessionId(session.getId())
                .userId(userId)
                .role("assistant")
                .content(reply)
                .tokenCount(estimateTokens(reply))
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
                .build();
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

    /** 取最近上下文 + 当前问题，调 LLM 返回纯文本 */
    private String callLlm(String sessionId, String currentUserContent) {
        // 取最近 CONTEXT_WINDOW 条历史（不含当前刚插入的 user 消息）
        List<ChatMessage> historyDesc = chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .orderByDesc(ChatMessage::getCreateTime)
                        .orderByDesc(ChatMessage::getId)
                        .last("LIMIT " + (CONTEXT_WINDOW + 1)));
        // 反转为按时间正序
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
            // system 角色历史一律忽略，避免覆盖
        }

        // 历史最后一条理论上就是刚刚插入的 user message；
        // 兜底：如最后一条不是 user 或内容不一致，补一条
        if (messages.isEmpty()
                || !(messages.get(messages.size() - 1) instanceof UserMessage last)
                || !currentUserContent.equals(last.singleText())) {
            messages.add(UserMessage.from(currentUserContent));
        }

        ChatResponse response = chatModel.chat(messages);
        String text = response.aiMessage().text();
        return StrUtil.isBlank(text) ? "（AI 暂时无话可说）" : text;
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
