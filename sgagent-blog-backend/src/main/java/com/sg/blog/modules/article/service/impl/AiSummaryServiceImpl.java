package com.sg.blog.modules.article.service.impl;

import com.sg.blog.modules.article.service.AiSummaryService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * AI 摘要生成服务实现类 (基于 langchain4j OpenAI 兼容模式，对接百炼)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiSummaryServiceImpl implements AiSummaryService {

    private final ChatModel chatModel;

    private final StreamingChatModel streamingChatModel;

    @Override
    public String generateSummary(String title, String content) {
        log.info("开始请求 AI 生成摘要，文章标题：{}", title);

        String prompt = """
                请你担任一个专业的编辑。请阅读以下文章内容，并生成一段简短的摘要（150字以内）。
                摘要要求：结合文章标题，抓住核心观点，语言精炼，通俗易懂，不要输出多余的寒暄语。
                
                文章标题：%s
                文章内容：
                %s
                """.formatted(title, content);

        try {
            String summary = chatModel.chat(prompt);
            log.info("AI 摘要生成完成");
            return summary;
        } catch (Exception e) {
            log.error("AI 摘要生成失败，文章标题：{}", title, e);
            // 建议这里可以抛出自定义异常，或者返回一个默认的友好提示，防止因为 AI 故障导致文章发布失败
            return "文章内容精彩，点击阅读详情...";
        }
    }
}