package com.example.blog.modules.agent.service;

import com.example.blog.modules.agent.model.dto.ChatRequestDTO;
import com.example.blog.modules.agent.model.dto.SessionRenameDTO;
import com.example.blog.modules.agent.model.vo.ChatMessageVO;
import com.example.blog.modules.agent.model.vo.ChatReplyVO;
import com.example.blog.modules.agent.model.vo.ChatSessionVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * AI 对话业务服务
 */
public interface AgentChatService {

    /** 获取当前用户全部会话（按最近更新倒序） */
    List<ChatSessionVO> listSessions();

    /** 新建空会话，返回会话视图（含 code/title） */
    ChatSessionVO createSession();

    /** 重命名会话 */
    void renameSession(String sessionId, SessionRenameDTO dto);

    /** 删除会话（逻辑删除，连带消息一起隐藏） */
    void deleteSession(String sessionId);

    /** 获取会话历史消息（按时间正序） */
    List<ChatMessageVO> listMessages(String sessionId);

    /** 发送消息并同步获取 AI 回复（阻塞式） */
    ChatReplyVO chat(ChatRequestDTO requestDTO);

    /**
     * 发送消息并以 SSE 流式回送：
     * <ul>
     *   <li>meta：会话/用户消息 id</li>
     *   <li>tool_call：每次工具调用</li>
     *   <li>sources：累计来源引用</li>
     *   <li>delta：文本增量</li>
     *   <li>done：回复完成（含 assistantMessageId 与完整内容）</li>
     *   <li>error：异常</li>
     * </ul>
     */
    void chatStream(ChatRequestDTO requestDTO, SseEmitter emitter);
}
