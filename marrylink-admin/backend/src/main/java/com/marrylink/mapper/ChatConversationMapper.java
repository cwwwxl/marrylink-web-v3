package com.marrylink.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.marrylink.entity.ChatConversation;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatConversationMapper extends BaseMapper<ChatConversation> {
}
