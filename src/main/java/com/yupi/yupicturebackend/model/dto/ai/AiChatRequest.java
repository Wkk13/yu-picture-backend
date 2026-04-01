package com.yupi.yupicturebackend.model.dto.ai;

import lombok.Data;

import java.io.Serializable;

/**
 * AI 客服聊天请求参数封装类
 * 对标 UserRegisterRequest，统一请求参数格式
 *
 * @author wk
 * */
@Data
public class AiChatRequest implements Serializable {

    /**
     * 用户标识（可选，默认匿名）
     */
    private String userId;

    /**
     * 发送给 AI 的消息内容（必填）
     */
    private String message;

    /**
     * 序列化版本号（固定写法，防止反序列化报错）
     */
    private static final long serialVersionUID = 1L;
}