package com.hollysys.ppa.module.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hollysys.ppa.module.agent.entity.ChatSession;
import org.apache.ibatis.annotations.Mapper;

/**
 * Agent 会话 Mapper
 *
 * @author PPA Team
 */
@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {
}
