package com.sg.blog.modules.agent.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 会话消息实体（不可更新，所以不继承 BaseUpdateEntity）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("chat_message")
public class ChatMessage implements Serializable {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String sessionId;

    private Long userId;

    /** user / assistant / system */
    private String role;

    private String content;

    private Integer tokenCount;

    /**
     * AI 回复关联的来源引用 JSON（仅 assistant 消息可能有值）
     * 数组形式：[{"articleId":1,"title":"...","author":"..."}]
     */
    @TableField("sources_json")
    private String sourcesJson;

    /**
     * AI 回复过程中调用过的工具 JSON（仅 assistant 消息可能有值）
     * 数组形式：[{"name":"searchArticles","label":"站内文章搜索","summary":"Web3"}]
     */
    @TableField("tool_calls_json")
    private String toolCallsJson;

    /**
     * 用户上传附件元信息 JSON（仅 user 消息可能有值，不含 Tika 提取文本）
     * 数组形式：[{"url":"...","name":"a.docx","size":1234,"ext":"docx"}]
     */
    @TableField("attachments_json")
    private String attachmentsJson;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}
