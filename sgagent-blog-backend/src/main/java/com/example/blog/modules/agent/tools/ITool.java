package com.example.blog.modules.agent.tools;

/**
 * Agent 工具标记接口
 * <p>
 * 实现该接口的 Spring Bean 会被 {@link AgentToolRegister} 自动收集，
 * 其类中标注了 {@code @Tool} 的方法将被注册到 LLM 的可用工具列表。
 * <p>
 * name / description 是供日志 + 前端「工具调用 chips」展示用的友好信息，
 * 与 {@code @Tool} 注解的 name/value 相互独立（后者是给 LLM 看的）。
 */
public interface ITool {

    /** 工具逻辑名，唯一，建议与类中 {@code @Tool(name=...)} 保持一致便于排查 */
    String getName();

    /** 工具在前端展示的中文描述 / 简短用途，用于调用气泡提示 */
    String getDescription();
}
