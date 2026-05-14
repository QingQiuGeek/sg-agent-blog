package com.sg.blog.modules.agent.controller;

import com.sg.blog.common.base.Result;
import com.sg.blog.core.annotation.AuthCheck;
import com.sg.blog.core.annotation.RateLimit;
import com.sg.blog.modules.agent.model.dto.ChatRequestDTO;
import com.sg.blog.modules.agent.model.dto.SessionRenameDTO;
import com.sg.blog.modules.agent.model.vo.ChatMessageVO;
import com.sg.blog.modules.agent.model.vo.ChatReplyVO;
import com.sg.blog.modules.agent.model.vo.ChatSessionVO;
import com.sg.blog.modules.agent.service.AgentChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * SGAgent 对话接口（前台，登录用户）
 */
@Validated
@RestController
@RequestMapping("/api/v1/agent")
@AuthCheck
@Tag(name = "前台/SGAgent 对话")
public class AgentChatController {

    /** UUID 格式限制，防止路径注入（兼容 32 位无连字符 / 36 位带连字符） */
    private static final String SESSION_ID_REGEX = "^[0-9a-fA-F\\-]{32,36}$";

    @Resource
    private AgentChatService agentChatService;

    @GetMapping("/sessions")
    @Operation(summary = "获取当前用户的会话列表")
    public Result<List<ChatSessionVO>> listSessions() {
        return Result.success(agentChatService.listSessions());
    }

    @PostMapping("/sessions")
    @RateLimit(key = "userId", time = 60, count = 30)
    @Operation(summary = "新建空会话")
    public Result<ChatSessionVO> createSession() {
        return Result.success(agentChatService.createSession());
    }

    @PutMapping("/sessions/{id}")
    @Operation(summary = "重命名会话")
    public Result<Void> renameSession(
            @PathVariable("id") @NotBlank @Pattern(regexp = SESSION_ID_REGEX, message = "会话ID非法") String id,
            @Valid @RequestBody SessionRenameDTO dto) {
        agentChatService.renameSession(id, dto);
        return Result.success();
    }

    @DeleteMapping("/sessions/{id}")
    @Operation(summary = "删除会话")
    public Result<Void> deleteSession(
            @PathVariable("id") @NotBlank @Pattern(regexp = SESSION_ID_REGEX, message = "会话ID非法") String id) {
        agentChatService.deleteSession(id);
        return Result.success();
    }

    @GetMapping("/sessions/{id}/messages")
    @Operation(summary = "获取会话历史消息")
    public Result<List<ChatMessageVO>> listMessages(
            @PathVariable("id") @NotBlank @Pattern(regexp = SESSION_ID_REGEX, message = "会话ID非法") String id) {
        return Result.success(agentChatService.listMessages(id));
    }

    @DeleteMapping("/messages/{id}")
    @RateLimit(key = "userId", time = 60, count = 60)
    @Operation(summary = "删除单条消息（用于前端单条删除 / 重试前清理历史）")
    public Result<Void> deleteMessage(
            @PathVariable("id") @NotBlank @Pattern(regexp = SESSION_ID_REGEX, message = "消息ID非法") String id) {
        agentChatService.deleteMessage(id);
        return Result.success();
    }

    @PostMapping("/chat")
    @RateLimit(key = "userId", time = 60, count = 20)
    @Operation(summary = "发送消息并获取 AI 回复")
    public Result<ChatReplyVO> chat(@Valid @RequestBody ChatRequestDTO dto) {
        return Result.success(agentChatService.chat(dto));
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RateLimit(key = "userId", time = 60, count = 20)
    @Operation(summary = "发送消息并以 SSE 流式返回 AI 回复")
    public SseEmitter chatStream(@Valid @RequestBody ChatRequestDTO dto) {
        // 5 分钟超时，覆盖大部分多轮工具调用场景
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);
        agentChatService.chatStream(dto, emitter);
        return emitter;
    }
}
