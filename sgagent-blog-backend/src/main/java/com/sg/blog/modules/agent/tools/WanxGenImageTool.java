package com.sg.blog.modules.agent.tools;

import com.alibaba.dashscope.aigc.imagegeneration.ImageGeneration;
import com.alibaba.dashscope.aigc.imagegeneration.ImageGenerationMessage;
import com.alibaba.dashscope.aigc.imagegeneration.ImageGenerationOutput;
import com.alibaba.dashscope.aigc.imagegeneration.ImageGenerationParam;
import com.alibaba.dashscope.aigc.imagegeneration.ImageGenerationResult;
import com.alibaba.dashscope.utils.JsonUtils;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import com.sg.blog.common.constants.Constants;
import com.sg.blog.common.enums.ResultCode;
import com.sg.blog.core.exception.CustomerException;
import com.sg.blog.modules.file.service.FileService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * @author: qingqiugeek
 * @date: 2026/5/4 16:40
 * @description: WanxGenImage agent tool
 */
@Slf4j
@Component
public class WanxGenImageTool implements ITool {

  public static final String TOOL_NAME = "wanxGenerateImage";

  /** 默认尺寸：DashScope wanx 系列使用 W*H 表示（注意中间是星号，不是 x）。 */
  private static final String DEFAULT_SIZE = "1024*1024";

  /** 文生图模型名，例如 wan2.7-image-pro 等。 */
  @Value("${image-model.wanx.model-name:}")
  private String imageModelName;

  /** DashScope API Key。可在 application-local.yaml 或环境变量 DASHSCOPE_API_KEY 中配置。 */
  @Value("${image-model.wanx.api-key:}")
  private String apiKey;

  @Resource
  private FileService fileService;

  @Override
  public String getName() {
    return TOOL_NAME;
  }

  @Override
  public String getDescription() {
    return "通义万相文生图";
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
      @P(value = "image size, format must be `WIDTH*HEIGHT`! "
          + "Common: 1:1 is 1280*1280, 3:4 is 1104*1472, 4:3 is 1472*1104, 9:16 is 960*1696, 16:9 is 1696*960, 1280*720, 720*1280. Default 1280*1280.",
          required = false)
      String size
  ) {
    if (StrUtil.isBlank(prompt)) {
      return "Error: prompt cannot be empty.";
    }
    if (StrUtil.isBlank(apiKey)) {
      log.warn("[WanxGenImageTool] apiKey is blank; configure image-model.api-key or DASHSCOPE_API_KEY env var");
      return "图片生成失败：DashScope API Key 未配置，请在 application-local.yaml 设置 image-model.api-key 或环境变量 DASHSCOPE_API_KEY。";
    }
    // size 是可选参数，AI 不传时为 null/空，需要兜底；否则 WanxImageSize.of(null) 会 NPE。
    String chosenSize = StrUtil.isBlank(size) ? DEFAULT_SIZE : size.trim();

    ImageGenerationMessage message = ImageGenerationMessage.builder()
        .role("user")
        .content(Collections.singletonList(
            Collections.singletonMap("text",prompt)
        )).build();

    ImageGenerationParam param = ImageGenerationParam.builder()
        .apiKey(apiKey)
        .model(imageModelName)
        .n(1)
        .size(chosenSize)
        .negativePrompt("低分辨率，低画质，肢体畸形，手指畸形，画面过饱和，蜡像感，人脸无细节，过度光滑，画面具有AI感。构图混乱。文字模糊，扭曲。")
        .promptExtend(true)
        .watermark(false)
        .messages(Collections.singletonList(message))
        .build();

    ImageGeneration imageGeneration = new ImageGeneration();
    try {
      ImageGenerationResult result =  imageGeneration.call(param);
      log.info("[WanxGenImageTool] model={} size={} result={}", imageModelName, chosenSize, JsonUtils.toJson(result));
      ImageGenerationOutput output = result.getOutput();
      if(output == null){
        throw new CustomerException(ResultCode.INTERNAL_SERVER_ERROR, "通义万相图片生成响应为空");
      }
      String upstreamUrl = output.getChoices().get(0).getMessage().getContent().get(0).get("image")
          .toString();
      log.info("[WanxGenImageTool] upstream={}", upstreamUrl);

      // 上游 URL 是带签名的临时链接（24h 后失效），转存到自己的 OSS 拿永久 URL
      String ossUrl = transferToOssQuietly(upstreamUrl);
      String htmlImg = "<img src=\"" + ossUrl + "\" alt=\"" + escapeAttr(prompt) + "\" style=\"max-width:100%;border-radius:8px;\" />";
      return htmlImg
          + "\n\nINSTRUCTION FOR THE ASSISTANT: copy the <img> tag above VERBATIM into your final answer; "
          + "do NOT convert it to markdown, do NOT wrap it in code blocks, then add any extra description you like.";
    } catch (Exception e) {
      log.error("[WanxGenImageTool] generate failed, model={} size={}", imageModelName, chosenSize, e);
      String msg = e.getMessage();
      return "图片生成异常：" + (StrUtil.isBlank(msg) ? e.getClass().getSimpleName() : msg);
    }
  }

  /** alt 属性需转义双引号，避免破坏 HTML 结构 */
  private static String escapeAttr(String s) {
    if (s == null) return "";
    return s.replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
  }

  /** 把上游临时 URL 转存到自己的 OSS；失败时降级返回上游 URL 保证流程不中断。 */
  private String transferToOssQuietly(String upstreamUrl) {
    try {
      byte[] bytes = HttpRequest.get(upstreamUrl).timeout(20_000).execute().bodyBytes();
      if (bytes == null || bytes.length == 0) {
        return upstreamUrl;
      }
      return fileService.upload(bytes, "gen.png", Constants.UPLOAD_DIR_GEN_IMAGE);
    } catch (Exception ex) {
      log.warn("[WanxGenImageTool] OSS 转存失败，降级返回上游 URL: {}", ex.getMessage());
      return upstreamUrl;
    }
  }
}
