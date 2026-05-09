package com.example.blog.modules.agent.tools;

import cn.hutool.core.util.StrUtil;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationOutput;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import cn.hutool.http.HttpRequest;
import com.example.blog.common.constants.Constants;
import com.example.blog.common.enums.ResultCode;
import com.example.blog.core.exception.CustomerException;
import com.example.blog.modules.file.service.FileService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author: qingqiugeek
 * @date: 2026/5/8 08:25
 * @description: QwenGenImage agent tool
 */
@Slf4j
@Component
public class QwenGenImageTool implements ITool {

  public static final String TOOL_NAME = "qwenGenerateImage";

  /** 默认尺寸：DashScope qwen 系列使用 W*H 表示（注意中间是星号，不是 x）。 */
  private static final String DEFAULT_SIZE = "1024*1024";

  /** 文生图模型名，例如 qwen-image-2.0-pro 等。 */
  @Value("${image-model.qwen.model-name:}")
  private String imageModelName;

  /** DashScope API Key。可在 application-local.yaml 或环境变量 DASHSCOPE_API_KEY 中配置。 */
  @Value("${image-model.qwen.api-key:}")
  private String apiKey;

  @Resource
  private FileService fileService;

  @Override
  public String getName() {
    return TOOL_NAME;
  }

  @Override
  public String getDescription() {
    return "通义千问文生图";
  }

  @Tool(
      name = TOOL_NAME,
      value = "Generate a high-quality png image. "
          + "Use this whenever the user asks to draw / paint / generate / produce an image, illustration, poster, "
          + "icon, avatar, or scene. Returns an HTML <img> tag. "
          + "IMPORTANT: in your final reply you MUST copy the returned <img> tag VERBATIM (do NOT convert it to markdown, "
          + "do NOT wrap it in code blocks); otherwise the user will only see text."
  )
  public String generateImage(
      @P(value = "Image description (prompt). 中英文均可，越具体越好；最多 800 个字符。")
      String prompt,
      @P(value = "image size, format must be `WIDTH*HEIGHT`!"
          + "Common: 16:9 is 2688*1536, 9:16 is 1536*2688, 1:1 is 2048*2048, 4:3 is 2368*1728, 3:4 is 1728*2368. Default 2048*2048",
          required = false)
      String size
  ) {
    if (StrUtil.isBlank(prompt)) {
      return "Error: prompt cannot be empty.";
    }
    if (StrUtil.isBlank(apiKey)) {
      log.warn("[QwenGenImageTool] apiKey is blank; configure image-model.api-key or DASHSCOPE_API_KEY env var");
      return "图片生成失败：DashScope API Key 未配置，请在 application-local.yaml 设置 image-model.api-key 或环境变量 DASHSCOPE_API_KEY。";
    }
    MultiModalConversation conv = new MultiModalConversation();

    MultiModalMessage userMessage = MultiModalMessage.builder().role(Role.USER.getValue())
        .content(Arrays.asList(
            Collections.singletonMap("text", prompt)
        )).build();

    Map<String, Object> parameters = new HashMap<>();
    parameters.put("watermark", false);
    parameters.put("prompt_extend", true);
    parameters.put("negative_prompt", "低分辨率，低画质，肢体畸形，手指畸形，画面过饱和，蜡像感，人脸无细节，过度光滑，画面具有AI感。构图混乱。文字模糊，扭曲。");
    
    // size 是可选参数，AI 不传时为 null/空，需要兜底；否则 WanxImageSize.of(null) 会 NPE。
    String chosenSize = StrUtil.isBlank(size) ? DEFAULT_SIZE : size.trim();

    MultiModalConversationParam param = MultiModalConversationParam.builder()
        .apiKey(apiKey)
        .model(imageModelName)
        .messages(Collections.singletonList(userMessage))
        .parameters(parameters)
        .size(chosenSize)
        .build();
    try {
      MultiModalConversationResult result = conv.call(param);
      MultiModalConversationOutput output = result.getOutput();
      if (output == null) {
        throw new CustomerException(ResultCode.INTERNAL_SERVER_ERROR, "通义千问图片生成响应为空");
      }
      String upstreamUrl = output.getChoices().get(0).getMessage().getContent().get(0)
          .get("image").toString();
      log.info("[QwenGenImageTool] upstream={}", upstreamUrl);

      // 上游 URL 是带签名的临时链接（24h 后失效），转存到自己的 OSS 拿永久 URL
      String ossUrl = transferToOssQuietly(upstreamUrl);
      String htmlImg = "<img src=\"" + ossUrl + "\" alt=\"" + escapeAttr(prompt) + "\" style=\"max-width:100%;border-radius:8px;\" />";
      return htmlImg
          + "\n\nINSTRUCTION FOR THE ASSISTANT: copy the <img> tag above VERBATIM into your final answer; "
          + "do NOT convert it to markdown, do NOT wrap it in code blocks, then add any extra description you like.";
    } catch (CustomerException be) {
      throw be;
    } catch (Exception e) {
      log.error("[QwenGenImageTool] generate failed", e);
      return "图片生成异常：" + e.getMessage();
    }
  }

  /**
   * 把上游临时图片链接转存到自己的 OSS，路径规则：
   * <pre>upload/generate-image/yyyy-MM-dd-HH-mm-ss/{ts}_{random}.{ext}</pre>
   * 转存失败时降级返回原始 URL（仍可在有效期内访问），保证流程不中断。
   */
  /** alt 属性需转义双引号，避免破坏 HTML 结构 */
  private static String escapeAttr(String s) {
    if (s == null) return "";
    return s.replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
  }

  private String transferToOssQuietly(String upstreamUrl) {
    try {
      byte[] bytes = HttpRequest.get(upstreamUrl).timeout(20_000).execute().bodyBytes();
      if (bytes == null || bytes.length == 0) {
        return upstreamUrl;
      }
      return fileService.upload(bytes, "gen.png", Constants.UPLOAD_DIR_GEN_IMAGE);
    } catch (Exception ex) {
      log.warn("[QwenGenImageTool] OSS 转存失败，降级返回上游 URL: {}", ex.getMessage());
      return upstreamUrl;
    }
  }
}
