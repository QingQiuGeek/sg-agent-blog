package com.sg.blog.modules.knowledge.controller;

import com.sg.blog.common.base.Result;
import com.sg.blog.core.annotation.AuthCheck;
import com.sg.blog.core.annotation.Log;
import com.sg.blog.core.annotation.RateLimit;
import com.sg.blog.modules.knowledge.model.dto.KnowledgeBaseAddDTO;
import com.sg.blog.modules.knowledge.model.dto.KnowledgeBaseUpdateDTO;
import com.sg.blog.modules.knowledge.model.vo.KnowledgeBaseFileVO;
import com.sg.blog.modules.knowledge.model.vo.KnowledgeBaseVO;
import com.sg.blog.modules.knowledge.service.KnowledgeBaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 我的知识库（前台用户私有）：CRUD + 文件上传 / 删除。
 * 所有接口在 Service 层强制按当前登录用户过滤；越权访问抛 403。
 */
@RestController
@Validated
@RequestMapping("/api/v1/user/kbs")
@AuthCheck
@Tag(name = "前台/我的知识库")
public class UserKnowledgeBaseController {

    @Resource
    private KnowledgeBaseService kbService;

    // =================== 知识库 CRUD ===================

    @GetMapping
    @Operation(summary = "我的知识库列表")
    public Result<List<KnowledgeBaseVO>> listMyKbs() {
        return Result.success(kbService.listMyKbs());
    }

    @GetMapping("/brief")
    @Operation(summary = "我的知识库简表（用于 SgAgent 输入框多选）")
    public Result<List<KnowledgeBaseVO>> listBrief() {
        return Result.success(kbService.listMyKbsBrief());
    }

    @GetMapping("/{kbId}")
    @Operation(summary = "知识库详情")
    public Result<KnowledgeBaseVO> getKb(@PathVariable @Positive Long kbId) {
        return Result.success(kbService.getMyKb(kbId));
    }

    @PostMapping
    @Log(module = "我的知识库", type = "新增", desc = "创建知识库")
    @Operation(summary = "新建知识库")
    public Result<Long> addKb(@Valid @RequestBody KnowledgeBaseAddDTO dto) {
        return Result.success(kbService.addKb(dto));
    }

    @PutMapping
    @Log(module = "我的知识库", type = "修改", desc = "修改知识库")
    @Operation(summary = "修改知识库")
    public Result<Void> updateKb(@Valid @RequestBody KnowledgeBaseUpdateDTO dto) {
        kbService.updateKb(dto);
        return Result.success();
    }

    @DeleteMapping("/{kbId}")
    @Log(module = "我的知识库", type = "删除", desc = "删除知识库（级联文件 + 向量）")
    @Operation(summary = "删除知识库")
    public Result<Void> deleteKb(@PathVariable @Positive Long kbId) {
        kbService.deleteKb(kbId);
        return Result.success();
    }

    // =================== 文件管理 ===================

    @GetMapping("/{kbId}/files")
    @Operation(summary = "知识库文件列表")
    public Result<List<KnowledgeBaseFileVO>> listFiles(@PathVariable @Positive Long kbId) {
        return Result.success(kbService.listFiles(kbId));
    }

    @PostMapping("/{kbId}/files")
    @RateLimit(key = "userId", time = 60, count = 30)
    @Log(module = "我的知识库", type = "上传", desc = "上传知识库文件")
    @Operation(summary = "上传文件到知识库（自动 Tika 提取 + 异步向量化）")
    public Result<KnowledgeBaseFileVO> uploadFile(
            @PathVariable @Positive Long kbId,
            @RequestParam("file") MultipartFile file) {
        return Result.success(kbService.uploadFile(kbId, file));
    }

    @DeleteMapping("/files/{fileId}")
    @Log(module = "我的知识库", type = "删除", desc = "删除知识库文件")
    @Operation(summary = "删除知识库文件（含向量）")
    public Result<Void> deleteFile(@PathVariable @Positive Long fileId) {
        kbService.deleteFile(fileId);
        return Result.success();
    }
}
