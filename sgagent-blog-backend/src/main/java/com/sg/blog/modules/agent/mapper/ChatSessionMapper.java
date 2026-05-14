package com.sg.blog.modules.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sg.blog.modules.agent.model.entity.ChatSession;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {
}
