package com.example.blog.modules.article.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.blog.common.base.Result;
import com.example.blog.common.constants.MessageConstants;
import com.example.blog.common.enums.ResultCode;
import com.example.blog.core.annotation.AuthCheck;
import com.example.blog.core.annotation.Log;
import com.example.blog.core.exception.CustomerException;
import com.example.blog.core.security.UserContext;
import com.example.blog.modules.article.model.dto.ArticleAddDTO;
import com.example.blog.modules.article.model.dto.ArticleGenerateSummaryDTO;
import com.example.blog.modules.article.model.dto.ArticleQueryDTO;
import com.example.blog.modules.article.model.dto.ArticleUpdateDTO;
import com.example.blog.modules.article.model.entity.Article;
import com.example.blog.modules.article.model.vo.AdminArticleVO;
import com.example.blog.modules.article.service.AiSummaryService;
import com.example.blog.modules.article.service.ArticleService;
import com.example.blog.modules.user.model.dto.UserPayloadDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 前台「我的文章」控制器
 * <p>面向所有登录用户：可查看 / 创建 / 编辑 / 删除自己发布的文章。
 * <p>与 {@link AdminArticleController} 的区别：
 * <ul>
 *   <li>列表查询强制按当前登录用户过滤（仅看到自己的）</li>
 *   <li>编辑、删除前先做 ownership 校验，防止越权修改他人文章</li>
 *   <li>新增 / 更新 / 删除 复用 {@link ArticleService} 业务逻辑，无需重写</li>
 * </ul>
 */
@RestController
@Validated
@RequestMapping("/api/v1/user/articles")
@AuthCheck
@Tag(name = "前台/我的文章")
public class UserArticleController {

    @Resource
    private ArticleService articleService;

    @Resource
    private AiSummaryService aiSummaryService;

    /**
     * 分页查询「我的文章」列表
     * <p>不管前端有没有传 userId，都会被强制覆盖为当前登录用户。
     */
    @GetMapping
    @Operation(summary = "分页查询我的文章列表")
    public Result<IPage<AdminArticleVO>> pageMyArticles(@Valid ArticleQueryDTO queryDTO) {
        queryDTO.setUserId(currentUserId());
        IPage<AdminArticleVO> pageResult = articleService.pageAdminArticles(queryDTO);
        return Result.success(pageResult);
    }

    /**
     * 获取我的某篇文章详情（用于编辑回显）
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取我的文章详情")
    public Result<AdminArticleVO> getMyArticle(@PathVariable @Positive(message = "文章ID非法") Long id) {
        requireMine(id);
        return Result.success(articleService.getArticleForEdit(id));
    }

    /**
     * 新增文章（作者强制为当前登录用户，由 Service 层基于 UserContext 注入）
     */
    @PostMapping
    @Log(module = "我的文章", type = "新增", desc = "用户发布了新文章")
    @Operation(summary = "发布新文章")
    public Result<Void> addArticle(@Valid @RequestBody ArticleAddDTO addDTO) {
        articleService.addArticle(addDTO);
        return Result.success();
    }

    /**
     * 修改我的文章
     */
    @PutMapping
    @Log(module = "我的文章", type = "修改", desc = "用户修改了自己的文章")
    @Operation(summary = "修改我的文章")
    public Result<Void> updateArticle(@Valid @RequestBody ArticleUpdateDTO updateDTO) {
        requireMine(updateDTO.getId());
        articleService.updateArticle(updateDTO);
        return Result.success();
    }

    /**
     * 删除我的某篇文章
     */
    @DeleteMapping("/{id}")
    @Log(module = "我的文章", type = "删除", desc = "用户删除了自己的文章")
    @Operation(summary = "删除我的文章")
    public Result<Void> deleteArticle(@PathVariable @Positive(message = "文章ID非法") Long id) {
        requireMine(id);
        articleService.deleteArticleById(id);
        return Result.success();
    }

    /**
     * 批量删除我的文章
     */
    @DeleteMapping("/batch")
    @Log(module = "我的文章", type = "批量删除", desc = "用户批量删除了自己的文章")
    @Operation(summary = "批量删除我的文章")
    public Result<Void> batchDelete(
            @RequestBody
            @NotEmpty(message = "请选择要删除的文章")
            List<
                    @NotNull(message = "ID不能为null")
                    @Positive(message = "ID必须为正数")
                            Long
                    > ids
    ) {
        // 任意一条不属于当前用户即拒绝整个批量操作
        Long currentUid = currentUserId();
        for (Long id : ids) {
            Article article = articleService.getById(id);
            if (article == null) {
                throw new CustomerException(ResultCode.NOT_FOUND, MessageConstants.MSG_ARTICLE_NOT_EXIST);
            }
            if (!currentUid.equals(article.getUserId())) {
                throw new CustomerException(ResultCode.FORBIDDEN, "无权删除他人文章");
            }
        }
        articleService.batchDeleteArticles(ids);
        return Result.success();
    }

    /**
     * AI 生成文章摘要（与后台共用同一份能力）
     */
    @PostMapping("/summary-generation")
    @Operation(summary = "AI 生成摘要")
    public Result<String> generateSummary(@RequestBody @Validated ArticleGenerateSummaryDTO dto) {
        String summary = aiSummaryService.generateSummary(dto.getTitle(), dto.getContent());
        return Result.success(summary);
    }

    // =================== 私有工具方法 ===================

    /** 取当前登录用户 ID；未登录抛 401 */
    private Long currentUserId() {
        UserPayloadDTO current = UserContext.get();
        if (current == null) {
            throw new CustomerException(ResultCode.UNAUTHORIZED, MessageConstants.MSG_NOT_LOGIN);
        }
        return current.getId();
    }

    /** 校验文章存在且属于当前登录用户，否则抛 403/404 */
    private void requireMine(Long articleId) {
        Article article = articleService.getById(articleId);
        if (article == null) {
            throw new CustomerException(ResultCode.NOT_FOUND, MessageConstants.MSG_ARTICLE_NOT_EXIST);
        }
        if (!currentUserId().equals(article.getUserId())) {
            throw new CustomerException(ResultCode.FORBIDDEN, "无权操作他人文章");
        }
    }
}
