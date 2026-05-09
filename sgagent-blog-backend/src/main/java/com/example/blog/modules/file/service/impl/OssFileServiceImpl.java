package com.example.blog.modules.file.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.PutObjectResult;
import com.example.blog.common.constants.Constants;
import com.example.blog.common.constants.MessageConstants;
import com.example.blog.common.enums.ResultCode;
import com.example.blog.core.exception.CustomerException;
import com.example.blog.modules.file.service.FileService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.Set;

@Service
@Slf4j
public class OssFileServiceImpl implements FileService {

    @Value("${oss.accessKey}")
    private String accessKey;

    @Value("${oss.secretKey}")
    private String secretKey;

    @Value("${oss.bucket}")
    private String bucket;

    @Value("${oss.url}")
    private String endpoint;

    @Override
    public String upload(MultipartFile file, String dir) {
        Assert.notNull(file, "上传文件对象不能为空");
        if (file.isEmpty()) {
            throw new CustomerException(ResultCode.PARAM_ERROR, MessageConstants.MSG_FILE_IS_EMPTY);
        }

        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKey, secretKey);
        try {
            // 1. 生成文件名
            String originalFilename = file.getOriginalFilename();
            String extName = FileUtil.extName(originalFilename);
            String safeName = FileUtil.mainName(originalFilename);

            String currentPrefix = Constants.OSS_DIR_PREFIX;
            if (StrUtil.isNotBlank(dir)) {
                currentPrefix = currentPrefix + StrUtil.SLASH + dir;
            }
            String fileName = currentPrefix + StrUtil.SLASH + System.currentTimeMillis() + StrUtil.UNDERLINE + safeName + StrUtil.DOT + extName;

            // 2. 上传到阿里云 OSS
            PutObjectResult putObjectResult = ossClient.putObject(bucket, fileName,
                file.getInputStream());
            // 3. 返回带有 / 前缀的相对路径
            String url = "https://" + bucket + "." + endpoint + "/" + fileName;
            return url;

        } catch (Exception e) {
            log.error("阿里云OSS文件上传失败", e);
            throw new CustomerException(ResultCode.INTERNAL_SERVER_ERROR, MessageConstants.MSG_UPLOAD_FAILURE);
        } finally {
            ossClient.shutdown();
        }
    }

    @Override
    public String upload(byte[] fileData, String originalFilename, String dir) {
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKey, secretKey);
        try {
            String extName = FileUtil.extName(originalFilename);
            String safeName = FileUtil.mainName(originalFilename);

            String currentPrefix = Constants.OSS_DIR_PREFIX;
            if (StrUtil.isNotBlank(dir)) {
                currentPrefix = currentPrefix + StrUtil.SLASH + dir;
            }
            String fileName = currentPrefix + StrUtil.SLASH + System.currentTimeMillis() + StrUtil.UNDERLINE + safeName + StrUtil.DOT + extName;

            // 阿里云 OSS 支持 InputStream，将字节数组包装为流
            ossClient.putObject(bucket, fileName, new ByteArrayInputStream(fileData));

            // 拼接完整访问 URL：https://bucket.endpoint/fileName
            // 注意：application.yml 中 oss.url 配置的是裸域名（不含协议头），所以直接拼 https://
            return "https://" + bucket + "." + endpoint + "/" + fileName;
        } catch (Exception e) {
            log.error("阿里云OSS字节流上传失败", e);
            throw new CustomerException(ResultCode.INTERNAL_SERVER_ERROR, MessageConstants.MSG_UPLOAD_FAILURE);
        } finally {
            ossClient.shutdown();
        }
    }

    @Override
    public void download(String fileName, HttpServletResponse response) {
        // 云存储文件请直接使用URL访问，后端不做流转发
        throw new CustomerException(ResultCode.PARAM_ERROR, MessageConstants.MSG_OSS_DIRECT_ACCESS_REQUIRED);
    }

    @Override
    public int clearOrphanFiles(Set<String> activeFileNames) {
        // 阿里云OSS通常不在业务服务器上做高频文件比对和删除
        log.info("阿里云OSS模式下暂不执行本地孤儿文件清理逻辑");
        return 0;
    }
}
