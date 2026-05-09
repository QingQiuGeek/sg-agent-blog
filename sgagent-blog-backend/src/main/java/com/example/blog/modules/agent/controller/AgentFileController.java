package com.example.blog.modules.agent.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.example.blog.common.base.Result;
import com.example.blog.common.constants.Constants;
import com.example.blog.common.enums.ResultCode;
import com.example.blog.core.annotation.AuthCheck;
import com.example.blog.core.annotation.RateLimit;
import com.example.blog.core.exception.CustomerException;
import com.example.blog.modules.agent.model.vo.AgentAttachmentVO;
import com.example.blog.modules.agent.service.FileExtractService;
import com.example.blog.modules.file.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * Agent 对话附件上传：上传到 OSS + 立即用 Tika 提取纯文本，一并返回给前端。
 * <p>
 * 前端持有返回对象，发送消息时把它放在 ChatRequestDTO.attachments 一并送给后端，
 * 后端只把 content 拼到 prompt 前临时使用，不持久化。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/agent/files")
@AuthCheck
@Tag(name = "前台/Agent 文件上传")
public class AgentFileController {

    /** 单文件大小上限：5 MB */
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;

    /**
     * 允许上传的扩展名白名单（小写）。涵盖文档 + 表格 + 常见代码 / 配置文件。
     * 明确排除：音频、视频、图片（图片应走 ArticleSearchTool 或图片识别工具）、可执行文件、压缩包。
     */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            // 文档
            "md", "txt", "docx", "doc", "rtf", "odt", "pdf",
            // 表格
            "xlsx", "xls", "csv", "tsv",
            // 数据
            "json", "xml", "yaml", "yml", "toml", "ini", "properties", "log",
            // 网页
            "html", "htm", "css",
            // 代码
            "java", "kt", "scala", "groovy",
            "js", "mjs", "cjs", "ts", "tsx", "jsx", "vue", "svelte",
            "py", "rb", "php", "go", "rs",
            "c", "cc", "cpp", "cxx", "h", "hpp",
            "cs", "swift", "m", "mm",
            "sh", "bash", "zsh", "ps1", "bat", "cmd",
            "sql", "graphql", "gql", "proto",
            "dockerfile", "makefile", "gradle"
    );

    @Resource
    private FileService fileService;

    @Resource
    private FileExtractService fileExtractService;

    @PostMapping("/upload")
    @RateLimit(key = "userId", time = 60, count = 30)
    @Operation(summary = "上传 Agent 对话附件（自动 Tika 提取文本）")
    public Result<AgentAttachmentVO> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomerException(ResultCode.PARAM_ERROR, "文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new CustomerException(ResultCode.PARAM_ERROR, "单个文件不能超过 5MB");
        }

        String originalName = StrUtil.blankToDefault(file.getOriginalFilename(), "unknown");
        String ext = FileUtil.extName(originalName);
        if (StrUtil.isBlank(ext)) {
            throw new CustomerException(ResultCode.PARAM_ERROR, "文件必须带扩展名");
        }
        ext = ext.toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new CustomerException(ResultCode.PARAM_ERROR,
                    "不支持的文件类型：." + ext + "（仅支持文档/表格/代码文件）");
        }

        // 1. 用 Tika 解析（在上传前就解，否则上传后再读 OSS 慢得多）
        String content = "";
        try (InputStream in = file.getInputStream()) {
            content = fileExtractService.extract(in, originalName);
        } catch (IOException e) {
            log.warn("读取上传流失败 name={}", originalName, e);
            throw new CustomerException(ResultCode.INTERNAL_SERVER_ERROR, "文件读取失败");
        } catch (RuntimeException e) {
            // Tika 解析失败不阻断上传：仍然返回 url，但 content 为空，AI 会知道无法读取
            log.warn("Tika 解析失败 name={} 仍允许上传：{}", originalName, e.getMessage());
        }

        // 2. 上传到 OSS（注意：getInputStream 已被消费一次，需要重新拿，但 fileService 接收 MultipartFile 它内部自己 getInputStream）
        String url = fileService.upload(file, Constants.UPLOAD_DIR_AGENT_FILE);

        return Result.success(AgentAttachmentVO.builder()
                .url(url)
                .name(originalName)
                .size(file.getSize())
                .ext(ext)
                .content(content)
                .build());
    }
}
