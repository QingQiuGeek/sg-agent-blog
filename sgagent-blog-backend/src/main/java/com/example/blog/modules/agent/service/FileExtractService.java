package com.example.blog.modules.agent.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

/**
 * 用 Apache Tika 从字节流提取纯文本。
 * <p>
 * 仅支持文档型文件：md/txt/docx/doc/xlsx/xls/pdf/csv/html/json 以及常见代码文件。
 * 提取结果会被截断（{@link #MAX_CHARS}），避免单文件吃满 LLM 上下文。
 */
@Slf4j
@Service
public class FileExtractService {

    /** 单文件提取上限：约 5 万字（≈ 几十 KB），既能覆盖典型论文/文档，又不会爆 LLM 上下文 */
    public static final int MAX_CHARS = 50_000;

    /**
     * 用 Tika 解析输入流，返回纯文本。失败时抛运行时异常，由调用方决定如何处理。
     * @param input             二进制流（调用方负责关闭）
     * @param resourceHint      文件名提示（帮助 Tika 选择 parser，可为空）
     */
    public String extract(InputStream input, String resourceHint) {
        // BodyContentHandler 传入 -1 表示无限制，但我们用 MAX_CHARS 截断更安全
        ContentHandler handler = new BodyContentHandler(MAX_CHARS);
        Metadata metadata = new Metadata();
        if (resourceHint != null && !resourceHint.isBlank()) {
            metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, resourceHint);
        }
        try {
            new AutoDetectParser().parse(input, handler, metadata, new ParseContext());
            String text = handler.toString();
            return text == null ? "" : text.trim();
        } catch (SAXException e) {
            // 触发字符上限时会抛 SAXException（WriteLimitReachedException 内部子类），
            // 此时 handler 已累积部分文本，直接返回视作正常截断
            String partial = handler.toString();
            if (partial != null && partial.length() >= MAX_CHARS / 2) {
                log.info("Tika 提取触发上限截断 hint={} chars={}", resourceHint, partial.length());
                return partial.trim();
            }
            log.warn("Tika 解析 SAX 异常 hint={} reason={}", resourceHint, e.getMessage());
            throw new RuntimeException("文件解析失败：" + e.getMessage(), e);
        } catch (IOException | TikaException e) {
            log.warn("Tika 提取失败 hint={} reason={}", resourceHint, e.getMessage());
            throw new RuntimeException("文件解析失败：" + e.getMessage(), e);
        }
    }
}
