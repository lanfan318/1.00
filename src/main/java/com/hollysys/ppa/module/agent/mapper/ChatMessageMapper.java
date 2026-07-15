package com.hollysys.ppa.module.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hollysys.ppa.module.agent.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * Agent 消息 Mapper
 *
 * @author PPA Team
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}
