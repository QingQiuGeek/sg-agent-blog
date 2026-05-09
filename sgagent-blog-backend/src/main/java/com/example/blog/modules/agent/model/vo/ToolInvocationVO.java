package com.example.blog.modules.agent.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * AI 一次回复中调用过的某个工具的简要信息
 * <p>
 * 用于前端在 assistant 气泡上方展示「🔍 站内搜索 · Web3」之类的 chip，
 * 让用户清楚知道 AI 经过了哪些步骤。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI 工具调用记录")
public class ToolInvocationVO implements Serializable {

    @Schema(description = "工具逻辑名（与 @Tool name 一致）")
    private String name;

    @Schema(description = "工具中文展示名")
    private String label;

    @Schema(description = "本次调用的关键摘要参数（已省略冗余），如 query / prompt 字段值")
    private String summary;
}
