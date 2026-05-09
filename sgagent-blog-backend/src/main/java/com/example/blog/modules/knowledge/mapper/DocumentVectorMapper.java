package com.example.blog.modules.knowledge.mapper;

import com.example.blog.modules.knowledge.model.entity.DocumentVector;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 知识库文档向量表 Mapper（每 chunk 一行）
 * <p>通过 STRING_TO_VECTOR / VECTOR_TO_STRING 在字符串与 VECTOR 列之间转换
 */
@Mapper
public interface DocumentVectorMapper {

    /**
     * 插入一条 chunk 向量
     */
    @Insert("""
            INSERT INTO document_vector (kb_id, file_id, chunk_index, chunk_text,
                                         embedding, dimension, model_name, source_hash,
                                         create_time, update_time)
            VALUES (#{kbId}, #{fileId}, #{chunkIndex}, #{chunkText},
                    STRING_TO_VECTOR(#{embeddingStr}), #{dimension}, #{modelName}, #{sourceHash},
                    NOW(), NOW())
            """)
    int insertOne(DocumentVector vector);

    /**
     * 删除某文件的全部 chunk 向量（文件被删除时调用）
     */
    @Delete("DELETE FROM document_vector WHERE file_id = #{fileId}")
    int deleteByFileId(@Param("fileId") Long fileId);

    /**
     * 删除某知识库下的全部向量（知识库被删除时调用）
     */
    @Delete("DELETE FROM document_vector WHERE kb_id = #{kbId}")
    int deleteByKbId(@Param("kbId") Long kbId);

    /**
     * 拉取指定知识库范围内的所有 chunk 向量，余弦相似度在 Java 端计算。
     * <p>kbIds 至少包含一个；为空将返回空集合（由调用方保证）。
     */
    @Select("""
            <script>
            SELECT dv.id           AS id,
                   dv.kb_id        AS kbId,
                   dv.file_id      AS fileId,
                   dv.chunk_index  AS chunkIndex,
                   dv.chunk_text   AS chunkText,
                   VECTOR_TO_STRING(dv.embedding) AS embeddingStr,
                   f.file_name     AS fileName,
                   kb.name         AS kbName
            FROM document_vector dv
            JOIN knowledge_base_file f ON f.id = dv.file_id AND f.is_deleted = 0
            JOIN knowledge_base     kb ON kb.id = dv.kb_id  AND kb.is_deleted = 0
            WHERE dv.kb_id IN
              <foreach collection="kbIds" item="kid" open="(" separator="," close=")">#{kid}</foreach>
            </script>
            """)
    List<Map<String, Object>> listForSearchByKbIds(@Param("kbIds") List<Long> kbIds);
}
