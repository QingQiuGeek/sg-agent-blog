package com.example.blog.modules.knowledge.service;

import com.example.blog.modules.knowledge.model.dto.KnowledgeBaseAddDTO;
import com.example.blog.modules.knowledge.model.dto.KnowledgeBaseUpdateDTO;
import com.example.blog.modules.knowledge.model.vo.KnowledgeBaseFileVO;
import com.example.blog.modules.knowledge.model.vo.KnowledgeBaseVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 我的知识库业务服务（前台用户私有，所有操作都隐式按当前登录用户过滤）
 */
public interface KnowledgeBaseService {

    /** 列出当前用户的全部知识库（按更新时间倒序） */
    List<KnowledgeBaseVO> listMyKbs();

    /** 列出当前用户的全部知识库简要信息（用于 SgAgent 输入框下拉多选） */
    List<KnowledgeBaseVO> listMyKbsBrief();

    /** 获取单个知识库（含 ownership 校验） */
    KnowledgeBaseVO getMyKb(Long kbId);

    /** 新建知识库 */
    Long addKb(KnowledgeBaseAddDTO dto);

    /** 修改知识库（仅名称 / 描述） */
    void updateKb(KnowledgeBaseUpdateDTO dto);

    /** 删除知识库（级联删除其下文件 + 向量；不删 OSS 文件以保留追溯） */
    void deleteKb(Long kbId);

    /** 列出某知识库下的全部文件 */
    List<KnowledgeBaseFileVO> listFiles(Long kbId);

    /** 上传文件到知识库：OSS 落盘 + Tika 提取文本 + 异步向量化 */
    KnowledgeBaseFileVO uploadFile(Long kbId, MultipartFile file);

    /** 删除知识库下的某个文件（含其全部 chunk 向量） */
    void deleteFile(Long fileId);
}
