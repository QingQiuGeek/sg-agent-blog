package com.example.blog.modules.agent.tools;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.example.blog.modules.article.model.vo.ArticleHitVO;
import com.example.blog.modules.article.service.ArticleVectorService;
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

    @Tool(name = TOOL_NAME, value = """
            搜索本站已发布的博客文章，按语义相似度返回最相关的若干篇。
            适用场景：用户希望了解站内是否写过某个话题、想看某类文章列表、想引用文章数据（标题、作者、阅读量、点赞数、发布时间）等。
            不适用场景：纯闲聊、与本博客内容无关的常识问题，请直接基于自身知识回答而不要调用本工具。
            返回字段：每篇文章包含 articleId, title, summary, author, viewCount, likeCount, publishTime, score, path。
            其中 path 是文章的站内相对路径（形如 /post/文章id）。
            回答用户时请用自然语言总结；如需给出文章链接，**必须直接使用返回的 path 字段原样写成 Markdown 链接**，
            形如 [文章标题](/post/文章id)，**严禁自行拼接域名**（如 http://xxx.com/...），前端会自动按当前域名解析。
            """)
    public String searchArticles(
            @P("精炼后的搜索关键字或短句，使用与用户问题语义一致的表达，如『Web3 区块链』『Spring Boot 性能优化』")
                    String query,
            @P("返回的文章数量上限，建议 0-5，默认 5；最大不超过 10")
                    Integer topK
    ) {
        int k = (topK == null || topK <= 0) ? 5 : Math.min(topK, 10);
        List<ArticleHitVO> hits = articleVectorService.searchByQuery(query, k);

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
}
