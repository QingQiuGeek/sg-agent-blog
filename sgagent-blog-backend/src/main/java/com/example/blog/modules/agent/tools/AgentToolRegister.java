package com.example.blog.modules.agent.tools;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import dev.langchain4j.service.tool.ToolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent Tool 注册中心
 * <p>
 * 自动收集所有实现 {@link ITool} 的 Spring Bean，扫描其中带
 * {@code @Tool} 注解的方法，构建：
 * <ul>
 *   <li>LLM 可用的 {@link ToolSpecification} 列表（用于 ChatRequest）</li>
 *   <li>工具名 → {@link ToolExecutor} 映射（用于 ReAct 循环里执行 tool 调用）</li>
 * </ul>
 */
@Slf4j
@Component
public class AgentToolRegister {

    private final List<ToolSpecification> specifications = new ArrayList<>();
    private final Map<String, ToolExecutor> executorByName = new HashMap<>();
    /** toolName(@Tool name) → ITool bean，便于反查中文 label */
    private final Map<String, ITool> beanByName = new HashMap<>();

    public AgentToolRegister(List<ITool> tools) {
        if (tools == null || tools.isEmpty()) {
            log.info("未发现任何 ITool 实现，Agent 将不携带任何工具运行");
            return;
        }
        for (ITool tool : tools) {
            specifications.addAll(ToolSpecifications.toolSpecificationsFrom(tool));
            registerInvokers(tool);
        }
        log.info("AgentToolRegister 初始化完成：共注册 {} 个工具：{}",
                executorByName.size(), executorByName.keySet());
    }

    private void registerInvokers(ITool tool) {
        Class<?> targetClass = AopUtils.getTargetClass(tool);
        for (Method method : targetClass.getDeclaredMethods()) {
            Tool toolAnnotation = method.getAnnotation(Tool.class);
            if (toolAnnotation == null) {
                continue;
            }
            String toolName = toolAnnotation.name();
            if (toolName == null || toolName.isBlank()) {
                toolName = method.getName();
            }
            method.setAccessible(true);
            executorByName.put(toolName, new DefaultToolExecutor(tool, method));
            beanByName.put(toolName, tool);
        }
    }

    /** 根据 @Tool name 反查 ITool 中文 description（找不到回退到 name 自身） */
    public String getLabel(String toolName) {
        ITool t = beanByName.get(toolName);
        if (t == null) {
            return toolName;
        }
        String desc = t.getDescription();
        return (desc == null || desc.isBlank()) ? toolName : desc;
    }

    /** LLM 可用工具规格列表 */
    public List<ToolSpecification> getSpecifications() {
        return Collections.unmodifiableList(specifications);
    }

    /**
     * 按名字过滤后的工具规格列表，用于按用户开关动态屏蔽某些工具。
     * @param excludedNames 要排除的工具 @Tool name 集合（null/empty 等价于全量）
     */
    public List<ToolSpecification> getSpecifications(java.util.Set<String> excludedNames) {
        if (excludedNames == null || excludedNames.isEmpty()) {
            return getSpecifications();
        }
        List<ToolSpecification> filtered = new ArrayList<>(specifications.size());
        for (ToolSpecification spec : specifications) {
            if (!excludedNames.contains(spec.name())) {
                filtered.add(spec);
            }
        }
        return Collections.unmodifiableList(filtered);
    }

    /**
     * 执行一次工具调用，返回工具的字符串结果（已是 JSON 或纯文本）
     * 失败时返回错误描述，由调用方决定是否重试
     */
    public String execute(ToolExecutionRequest request) {
        ToolExecutor executor = executorByName.get(request.name());
        if (executor == null) {
            log.warn("LLM 调用了未注册的工具：{}", request.name());
            return "Tool not found: " + request.name();
        }
        try {
            String result = executor.execute(request, null);
            return (result == null || result.isBlank()) ? "ok" : result;
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            log.warn("工具执行失败 tool={} args={} reason={}",
                    request.name(), request.arguments(), cause.getMessage());
            return "Tool error: " + cause.getMessage();
        }
    }

    /** 是否注册了任何工具 */
    public boolean isEmpty() {
        return executorByName.isEmpty();
    }
}
