package com.example.blog.modules.knowledge.context;

import java.util.Collections;
import java.util.List;

/**
 * 知识库检索上下文（ThreadLocal）：
 * <p>每次 AI 聊天请求中，由 ChatService 在调 LLM 前 set 用户勾选的 kbIds，
 * KnowledgeBaseSearchTool 在 LLM 调用本工具时通过 {@link #get()} 拿到 kbIds 做范围过滤。
 * <p>这样 LLM 能看到的工具入参仅为 query/topK，不需要也无法传 kbIds，
 * 避免了 LLM 凭空猜测 ID、检索越权或越界的风险。
 * <p>请务必在 ChatService 处理完一次请求后调 {@link #clear()}，避免线程池复用时上下文污染。
 */
public final class KbContext {

    private static final ThreadLocal<List<Long>> HOLDER = new ThreadLocal<>();

    private KbContext() {}

    public static void set(List<Long> kbIds) {
        if (kbIds == null || kbIds.isEmpty()) {
            HOLDER.remove();
        } else {
            HOLDER.set(kbIds);
        }
    }

    /** 获取当前线程绑定的 kbIds；从未 set 时返回空集合 */
    public static List<Long> get() {
        List<Long> ids = HOLDER.get();
        return ids == null ? Collections.emptyList() : ids;
    }

    public static boolean isEmpty() {
        return get().isEmpty();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
