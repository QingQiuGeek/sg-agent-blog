package com.sg.blog.modules.agent.tools;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.sg.blog.modules.knowledge.context.KbContext;
import com.sg.blog.modules.knowledge.model.vo.KbHitVO;
import com.sg.blog.modules.knowledge.service.KnowledgeBaseVectorService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 知识库语义检索 Tool：基于用户勾选的知识库范围检索文档 chunk。
 * <p>注册条件：仅当用户在前端勾选了至少一个知识库时，本工具才会出现在 LLM 的工具列表里
 * （由 AgentChatServiceImpl#resolveExcludedTools 控制）。是否真正调用本工具由 LLM 决定。
 * <p>kbIds 通过 {@link KbContext} 注入，LLM 看到的入参仅 query / topK，避免越权或乱传。
 */
@Slf4j
@Component
public class KnowledgeBaseSearchTool implements ITool {

    public static final String TOOL_NAME = "searchKnowledgeBase";

    /** 绝对阈值（同 ArticleSearchTool） */
    private static final double ABS_MIN_SCORE = 0.55;
    /** 相对截断 gap */
    private static final double REL_GAP = 0.12;

    @Resource
    private KnowledgeBaseVectorService kbVectorService;

    @Override
    public String getName() {
        return TOOL_NAME;
    }

    @Override
    public String getDescription() {
        return "知识库检索";
    }

    @Tool(name = TOOL_NAME, value = """
            搜索用户当前会话中已勾选的「私人知识库」中最相关的文档片段。
            适用场景：用户提问与他自己上传到知识库里的资料相关（笔记、论文、报告、合同、技术文档等）；
            或用户明确要求「在我的知识库里查」「基于这些资料回答」时调用本工具。
            不适用场景：用户问的是站内博客文章（用 searchArticles）、需要实时联网信息（用 webSearch）、
            或纯闲聊。也不要在用户没有明确要求基于私人资料回答时主动调用。
            返回字段：每条 hit 包含 fileName, kbName, chunkIndex, chunkText, score。
            回答用户时请用自然语言总结 chunkText 的关键信息，并在合适处提及来源文件名（如「根据《XX.pdf》……」）。
            """)
    public String searchKnowledgeBase(
            @P("精炼后的搜索关键字或短句，使用与用户问题语义一致的表达")
                    String query,
            @P("返回的片段数量上限，建议 3-5，默认 5；最大不超过 10")
                    Integer topK
    ) {
        List<Long> kbIds = KbContext.get();
        if (kbIds.isEmpty()) {
            // 理论上不会进入：注册时已被 excluded。但兜底防御 LLM 看到旧 spec 异常调用
            log.warn("[searchKnowledgeBase] 未勾选知识库就被调用，返回空结果");
            return new JSONObject()
                    .set("query", query)
                    .set("count", 0)
                    .set("results", new JSONArray())
                    .set("note", "用户未勾选任何知识库，无可检索范围")
                    .toString();
        }

        int k = (topK == null || topK <= 0) ? 5 : Math.min(topK, 10);
        List<KbHitVO> hits = kbVectorService.searchByQuery(query, kbIds, k);

        // 低分过滤：绝对阈值 + 相对截断
        int beforeFilter = hits.size();
        if (!hits.isEmpty()) {
            double topScore = hits.get(0).getScore() == null ? 0 : hits.get(0).getScore();
            if (topScore < ABS_MIN_SCORE) {
                hits = List.of();
            } else {
                double cutoff = Math.max(ABS_MIN_SCORE, topScore - REL_GAP);
                hits = hits.stream()
                        .filter(h -> h.getScore() != null && h.getScore() >= cutoff)
                        .toList();
            }
        }
        if (beforeFilter != hits.size()) {
            log.info("[searchKnowledgeBase] score 过滤：{} → {}（abs>={} & gap<={}）",
                    beforeFilter, hits.size(), ABS_MIN_SCORE, REL_GAP);
        }

        JSONArray arr = new JSONArray();
        for (KbHitVO hit : hits) {
            arr.add(new JSONObject()
                    .set("kbId", hit.getKbId())
                    .set("kbName", hit.getKbName())
                    .set("fileId", hit.getFileId())
                    .set("fileName", hit.getFileName())
                    .set("chunkIndex", hit.getChunkIndex())
                    .set("chunkText", hit.getChunkText())
                    .set("score", hit.getScore()));
        }
        return new JSONObject()
                .set("query", query)
                .set("count", hits.size())
                .set("results", arr)
                .toString();
    }
}
