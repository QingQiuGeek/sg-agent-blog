package com.sg.blog.modules.knowledge.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sg.blog.common.constants.Constants;
import com.sg.blog.common.constants.MessageConstants;
import com.sg.blog.common.enums.ResultCode;
import com.sg.blog.core.exception.CustomerException;
import com.sg.blog.core.security.UserContext;
import com.sg.blog.modules.agent.service.FileExtractService;
import com.sg.blog.modules.file.service.FileService;
import com.sg.blog.modules.knowledge.mapper.KnowledgeBaseFileMapper;
import com.sg.blog.modules.knowledge.mapper.KnowledgeBaseMapper;
import com.sg.blog.modules.knowledge.model.dto.KnowledgeBaseAddDTO;
import com.sg.blog.modules.knowledge.model.dto.KnowledgeBaseUpdateDTO;
import com.sg.blog.modules.knowledge.model.entity.KnowledgeBase;
import com.sg.blog.modules.knowledge.model.entity.KnowledgeBaseFile;
import com.sg.blog.modules.knowledge.model.vo.KnowledgeBaseFileVO;
import com.sg.blog.modules.knowledge.model.vo.KnowledgeBaseVO;
import com.sg.blog.modules.knowledge.service.KnowledgeBaseService;
import com.sg.blog.modules.knowledge.service.KnowledgeBaseVectorService;
import com.sg.blog.modules.user.model.dto.UserPayloadDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 我的知识库业务实现：所有操作都隐式按当前登录用户过滤；
 * 文件上传时同步落 OSS + Tika 提取文本 + 触发异步向量化（status 字段反馈进度）。
 */
@Slf4j
@Service
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    /** 单文件大小上限：10 MB（文档型文件，比 agent 附件略宽松） */
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    /**
     * 允许上传的文件扩展名（与 AgentFileController 保持一致：以文档类为主）。
     * 二进制 / 媒体 / 压缩包不允许：Tika 抽不到文本，向量化无意义。
     */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "md", "txt", "docx", "doc", "rtf", "odt", "pdf",
            "xlsx", "xls", "csv", "tsv",
            "json", "xml", "yaml", "yml", "toml", "ini", "properties", "log",
            "html", "htm", "css",
            "java", "kt", "scala", "groovy",
            "js", "mjs", "cjs", "ts", "tsx", "jsx", "vue", "svelte",
            "py", "rb", "php", "go", "rs",
            "c", "cc", "cpp", "cxx", "h", "hpp",
            "cs", "swift", "m", "mm",
            "sh", "bash", "zsh", "ps1", "bat", "cmd",
            "sql", "graphql", "gql", "proto"
    );

    @Resource
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Resource
    private KnowledgeBaseFileMapper knowledgeBaseFileMapper;

    @Resource
    private KnowledgeBaseVectorService kbVectorService;

    @Resource
    private FileService fileService;

    @Resource
    private FileExtractService fileExtractService;

    /* ================= 知识库 CRUD ================= */

    @Override
    public List<KnowledgeBaseVO> listMyKbs() {
        Long uid = currentUserId();
        List<KnowledgeBase> rows = knowledgeBaseMapper.selectList(
                new LambdaQueryWrapper<KnowledgeBase>()
                        .eq(KnowledgeBase::getUserId, uid)
                        .orderByDesc(KnowledgeBase::getUpdateTime));
        if (rows == null || rows.isEmpty()) return Collections.emptyList();
        List<KnowledgeBaseVO> result = new ArrayList<>(rows.size());
        for (KnowledgeBase kb : rows) {
            result.add(toVO(kb));
        }
        return result;
    }

    @Override
    public List<KnowledgeBaseVO> listMyKbsBrief() {
        // 与详情列表共用，前端展示时只取需要的字段即可
        return listMyKbs();
    }

    @Override
    public KnowledgeBaseVO getMyKb(Long kbId) {
        return toVO(requireMine(kbId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addKb(KnowledgeBaseAddDTO dto) {
        Long uid = currentUserId();
        KnowledgeBase kb = KnowledgeBase.builder()
                .userId(uid)
                .name(dto.getName().trim())
                .description(StrUtil.trimToNull(dto.getDescription()))
                .fileCount(0)
                .isDeleted(0)
                .build();
        knowledgeBaseMapper.insert(kb);
        return kb.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateKb(KnowledgeBaseUpdateDTO dto) {
        KnowledgeBase kb = requireMine(dto.getId());
        if (StrUtil.isNotBlank(dto.getName())) {
            kb.setName(dto.getName().trim());
        }
        if (dto.getDescription() != null) {
            // 允许置空（清空描述）
            kb.setDescription(StrUtil.trimToNull(dto.getDescription()));
        }
        knowledgeBaseMapper.updateById(kb);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteKb(Long kbId) {
        KnowledgeBase kb = requireMine(kbId);
        // 1. 物理删除全部 chunk 向量
        kbVectorService.deleteByKbId(kb.getId());
        // 2. 逻辑删除该知识库下的全部文件
        List<KnowledgeBaseFile> files = knowledgeBaseFileMapper.selectList(
                new LambdaQueryWrapper<KnowledgeBaseFile>()
                        .eq(KnowledgeBaseFile::getKbId, kb.getId()));
        if (files != null && !files.isEmpty()) {
            List<Long> fileIds = files.stream().map(KnowledgeBaseFile::getId).toList();
            knowledgeBaseFileMapper.deleteByIds(fileIds);
        }
        // 3. 逻辑删除知识库本身
        knowledgeBaseMapper.deleteById(kb.getId());
    }

    /* ================= 文件管理 ================= */

    @Override
    public List<KnowledgeBaseFileVO> listFiles(Long kbId) {
        requireMine(kbId);
        List<KnowledgeBaseFile> rows = knowledgeBaseFileMapper.selectList(
                new LambdaQueryWrapper<KnowledgeBaseFile>()
                        .eq(KnowledgeBaseFile::getKbId, kbId)
                        .orderByDesc(KnowledgeBaseFile::getCreateTime));
        if (rows == null || rows.isEmpty()) return Collections.emptyList();
        List<KnowledgeBaseFileVO> result = new ArrayList<>(rows.size());
        for (KnowledgeBaseFile f : rows) {
            result.add(toFileVO(f));
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeBaseFileVO uploadFile(Long kbId, MultipartFile file) {
        Long uid = currentUserId();
        KnowledgeBase kb = requireMine(kbId);

        // 1. 基础校验
        if (file == null || file.isEmpty()) {
            throw new CustomerException(ResultCode.PARAM_ERROR, "文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new CustomerException(ResultCode.PARAM_ERROR, "单个文件不能超过 10MB");
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

        // 2. 提前用 Tika 抽文本（消耗 InputStream 一次）；失败不阻断上传，仅 chunkCount=0
        String content = "";
        try (InputStream in = file.getInputStream()) {
            content = fileExtractService.extract(in, originalName);
        } catch (IOException e) {
            log.warn("读取上传流失败 name={}", originalName, e);
            throw new CustomerException(ResultCode.INTERNAL_SERVER_ERROR, "文件读取失败");
        } catch (RuntimeException e) {
            log.warn("Tika 解析失败 name={} 仍允许上传：{}", originalName, e.getMessage());
        }

        // 3. 上传 OSS
        String url;
        try {
            url = fileService.upload(file, Constants.UPLOAD_DIR_KB);
        } catch (Exception e) {
            log.error("知识库文件上传 OSS 失败 kbId={} name={}", kbId, originalName, e);
            throw new CustomerException(ResultCode.INTERNAL_SERVER_ERROR, MessageConstants.MSG_UPLOAD_FAILURE);
        }

        // 4. 插入文件元信息（status=PENDING）
        KnowledgeBaseFile entity = KnowledgeBaseFile.builder()
                .kbId(kb.getId())
                .userId(uid)
                .fileName(originalName)
                .fileUrl(url)
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .ext(ext)
                .status(KnowledgeBaseFile.STATUS_PENDING)
                .chunkCount(0)
                .isDeleted(0)
                .build();
        knowledgeBaseFileMapper.insert(entity);

        // 5. 知识库 fileCount + 1
        kb.setFileCount((kb.getFileCount() == null ? 0 : kb.getFileCount()) + 1);
        knowledgeBaseMapper.updateById(kb);

        // 6. 触发异步向量化
        kbVectorService.embedFileAsync(entity.getId(), content);

        return toFileVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFile(Long fileId) {
        Long uid = currentUserId();
        KnowledgeBaseFile file = knowledgeBaseFileMapper.selectById(fileId);
        if (file == null) {
            throw new CustomerException(ResultCode.NOT_FOUND, "文件不存在");
        }
        if (!uid.equals(file.getUserId())) {
            throw new CustomerException(ResultCode.FORBIDDEN, "无权操作他人文件");
        }
        // 1. 物理删除全部 chunk 向量
        kbVectorService.deleteByFileId(file.getId());
        // 2. 逻辑删除文件元信息
        knowledgeBaseFileMapper.deleteById(file.getId());
        // 3. 知识库 fileCount - 1（不小于 0）
        KnowledgeBase kb = knowledgeBaseMapper.selectById(file.getKbId());
        if (kb != null) {
            int next = Math.max(0, (kb.getFileCount() == null ? 0 : kb.getFileCount()) - 1);
            kb.setFileCount(next);
            knowledgeBaseMapper.updateById(kb);
        }
    }

    /* ================= 私有工具方法 ================= */

    private Long currentUserId() {
        UserPayloadDTO user = UserContext.get();
        if (user == null) {
            throw new CustomerException(ResultCode.UNAUTHORIZED, MessageConstants.MSG_NOT_LOGIN);
        }
        return user.getId();
    }

    /** 校验知识库存在且属于当前登录用户 */
    private KnowledgeBase requireMine(Long kbId) {
        if (kbId == null) {
            throw new CustomerException(ResultCode.PARAM_ERROR, "知识库ID不能为空");
        }
        KnowledgeBase kb = knowledgeBaseMapper.selectById(kbId);
        if (kb == null) {
            throw new CustomerException(ResultCode.NOT_FOUND, "知识库不存在");
        }
        if (!currentUserId().equals(kb.getUserId())) {
            throw new CustomerException(ResultCode.FORBIDDEN, "无权访问该知识库");
        }
        return kb;
    }

    private KnowledgeBaseVO toVO(KnowledgeBase kb) {
        return KnowledgeBaseVO.builder()
                .id(kb.getId())
                .name(kb.getName())
                .description(kb.getDescription())
                .fileCount(kb.getFileCount())
                .createTime(kb.getCreateTime())
                .updateTime(kb.getUpdateTime())
                .build();
    }

    private KnowledgeBaseFileVO toFileVO(KnowledgeBaseFile f) {
        return KnowledgeBaseFileVO.builder()
                .id(f.getId())
                .kbId(f.getKbId())
                .fileName(f.getFileName())
                .fileUrl(f.getFileUrl())
                .fileSize(f.getFileSize())
                .contentType(f.getContentType())
                .ext(f.getExt())
                .status(f.getStatus())
                .chunkCount(f.getChunkCount())
                .errorMessage(f.getErrorMessage())
                .createTime(f.getCreateTime())
                .build();
    }
}
