package com.sg.blog.modules.agent.tools;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.sg.blog.modules.article.model.vo.ArticleHitVO;
import com.sg.blog.modules.article.service.ArticleVectorService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 文章语义检索 Tool
 * <p>
 * LLM 在用户问题与「站内博客文章」相关时调用本工具，传入精炼后的查询关键字，
 * 返回最相关文章的标题、作者、阅读量、点赞数、发布时间等结构化信息，
 * 由 LLM 据此组织自然语言答案；同时由上层 ChatService 收集 articleId/title
 * 形成「来源引用」给前端展示。
 */
@Slf4j
@Component
public class ArticleSearchTool implements ITool {

    /** 工具名常量，便于在 ChatService 中识别并解析结果累积 sources */
    public static final String TOOL_NAME = "searchArticles";

    @Override
    public String getName() {
        return TOOL_NAME;
    }

    @Override
    public String getDescription() {
        return "站内文章语义检索";
    }

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Resource
    private ArticleVectorService articleVectorService;

    /**
     * 绝对阈值：任何 score 低于此值都丢弃。
     * 1024 维 embedding 中文场景下，同主题文本一般 >= 0.6，不同主题下澌中出现的伪相关联上限在 0.5 上下。
     */
    private static final double ABS_MIN_SCORE = 0.55;

    /**
     * 相对截断：query 语义不同时 max score 会有差别（如问「mysql」可能最高 0.85，问「打仗」可能最高 0.45）。
     * 在同一 query 内除了顶部几个外，后面分数常明显下降、需丢弃。
     * 仅保留 score >= max(ABS_MIN_SCORE, maxScore - REL_GAP) 的 hits。
     */
    private static final double REL_GAP = 0.12;

    @Tool(name = TOOL_NAME, value = """
            搜索本站已发布的博客文章，按语义相似度返回最相关的若干篇。
            适用场景：用户希望了解站内是否写过某个话题、想看某类文章列表、想引用文章数据（标题、作者、阅读量、点赞数、发布时间）等。
            不适用场景：纯闲聊、与本博客内容无关的常识问题，请直接基于自身知识回答而不要调用本工具。
            返回字段：每篇文章包含 articleId, title, summary, author, viewCount, likeCount, publishTime, score, path。
            其中 path 是文章的站内相对路径（形如 /post/文章id）。
            回答用户时请用自然语言总结；如需给出文章链接，**必须直接使用返回的 path 字段原样写成 Markdown 链接**，
            形如 [文章标题](/post/文章id)，**严禁自行拼接域名**（如 http://xxx.com/...），前端会自动按当前域名解析。
            过滤参数说明：
            - 用户明确提到某个分类（如「Spring Boot 分类下…」「在 Web3 分类」）时，把分类名填到 categoryName；
            - 用户明确提到某些标签（如「带 Java 标签的」「关于 React 和 Hooks」）时，把标签名按英文逗号分隔填到 tagNames；
            - 用户明确提到某个作者（如「张三写过的」「我（XXX）的文章」）时，把作者昵称填到 authorName；
            - 否则全部留空，仅靠语义召回。**不要凭空猜测分类/标签/作者**，没明说就别填。
            """)
    public String searchArticles(
            @P("精炼后的搜索关键字或短句，使用与用户问题语义一致的表达，如『Web3 区块链』『Spring Boot 性能优化』")
                    String query,
            @P("返回的文章数量上限，建议 0-5，默认 5；最大不超过 10")
                    Integer topK,
            @P(value = "可选，分类名精确匹配，如『Spring Boot』『前端』；用户没明说请留空字符串", required = false)
                    String categoryName,
            @P(value = "可选，标签名列表，英文逗号分隔，如『Java,Spring』；任一命中即可；用户没明说请留空字符串", required = false)
                    String tagNames,
            @P(value = "可选，作者昵称精确匹配；用户没明说请留空字符串", required = false)
                    String authorName
    ) {
        int k = (topK == null || topK <= 0) ? 5 : Math.min(topK, 10);
        java.util.List<String> tagList = null;
        if (tagNames != null && !tagNames.isBlank()) {
            tagList = java.util.Arrays.stream(tagNames.split("[,，]"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
            if (tagList.isEmpty()) tagList = null;
        }
        List<ArticleHitVO> hits = articleVectorService.searchByQuery(
                query, k, categoryName, tagList, authorName);

        // 低分过滤：绝对阈值 + 相对截断，避免不相关文章进入「来源」误导用户
        int beforeFilter = hits.size();
        hits = filterByScore(hits);
        if (beforeFilter != hits.size()) {
            log.info("[searchArticles] score 过滤：{} → {}（abs>={} & gap<={}）",
                    beforeFilter, hits.size(), ABS_MIN_SCORE, REL_GAP);
        }

        JSONArray arr = new JSONArray();
        for (ArticleHitVO hit : hits) {
            arr.add(new JSONObject()
                    .set("articleId", hit.getArticleId())
                    .set("title", hit.getTitle())
                    .set("summary", hit.getSummary())
                    .set("author", hit.getAuthorNickname())
                    .set("viewCount", hit.getViewCount())
                    .set("likeCount", hit.getLikeCount())
                    .set("favoriteCount", hit.getFavoriteCount())
                    .set("publishTime", hit.getPublishTime() == null ? null
                            : hit.getPublishTime().format(DATE_FMT))
                    .set("score", hit.getScore())
                    // 站内相对路径，前端按当前域名拼绝对地址（环境无关）
                    .set("path", "/post/" + hit.getArticleId())
            );
        }
        return new JSONObject()
                .set("query", query)
                .set("count", hits.size())
                .set("results", arr)
                .toString();
    }

    /**
     * 同时应用绝对阈值与相对截断：
     * 保留 score >= max(ABS_MIN_SCORE, topScore - REL_GAP) 的 hits。
     */
    private List<ArticleHitVO> filterByScore(List<ArticleHitVO> hits) {
        if (hits == null || hits.isEmpty()) return hits;
        double topScore = hits.get(0).getScore() == null ? 0 : hits.get(0).getScore();
        if (topScore < ABS_MIN_SCORE) {
            // top 都不够格，说明 query 与本站全集不相关
            return List.of();
        }
        double cutoff = Math.max(ABS_MIN_SCORE, topScore - REL_GAP);
        return hits.stream()
                .filter(h -> h.getScore() != null && h.getScore() >= cutoff)
                .toList();
    }
}
