package com.example.blog.modules.agent.tools;

import cn.hutool.core.date.DateTime;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

/**
 * @author: qingqiugeek
 * @date: 2026/5/11 18:34
 * @description: DateTime agent tool
 */

@Component
public class DateTimeTool implements ITool {

    /** 工具名常量，便于在 ChatService 中识别并解析结果累积 sources */
    public static final String TOOL_NAME = "dateTime";

    @Override
    public String getName() {
        return TOOL_NAME;
    }

    @Override
    public String getDescription() {
        return "Get current dateTime";
    }

    @Tool(name = "dateTimeTool", value = "Get current dateTime")
    public String dateTimeTool() {
        return DateTime.now().toString();
    }
}
