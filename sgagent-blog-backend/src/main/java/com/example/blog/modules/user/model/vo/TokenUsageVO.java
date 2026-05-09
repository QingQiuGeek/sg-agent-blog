package com.example.blog.modules.user.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 前台个人中心：AI 对话 Token 用量统计 VO
 * <p>数据来源：chat_message 表，按 user_id 聚合
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI 对话 Token 用量统计")
public class TokenUsageVO {

    @Schema(description = "总用量（user + assistant）")
    private Long total;

    @Schema(description = "AI 回复用量（assistant 消息）")
    private Long aiTotal;

    @Schema(description = "用户提问用量（user 消息）")
    private Long userTotal;

    @Schema(description = "近 7 天每日用量曲线（含 0 天占位）")
    private List<Daily> daily;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "每日 token 用量")
    public static class Daily {
        @Schema(description = "日期（yyyy-MM-dd）")
        private String date;

        @Schema(description = "当日 AI 用量")
        private Long aiTokens;

        @Schema(description = "当日用户用量")
        private Long userTokens;

        @Schema(description = "当日总用量")
        private Long total;
    }
}
