package com.yupi.yupicturebackend.controller;

import com.yupi.yupicturebackend.api.ollama.AiCustomerClient;
import com.yupi.yupicturebackend.common.BaseResponse;
import com.yupi.yupicturebackend.common.ResultUtils;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.exception.ThrowUtils;
import com.yupi.yupicturebackend.model.dto.ai.AiChatRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI客服接口
 */
@RestController
public class AiCustomerController {

    /**
     * AI 聊天接口（完全对标注册接口格式）
     */
    @PostMapping("/ai/customer")
    public BaseResponse<String> chat(@RequestBody AiChatRequest aiChatRequest) throws Exception {
        // 1. 参数非空校验（和注册接口完全一致）
        ThrowUtils.throwIf(aiChatRequest == null, ErrorCode.PARAMS_ERROR);

        // 2. 获取参数
        String userId = aiChatRequest.getUserId();
        String message = aiChatRequest.getMessage();

        // 3. 业务参数校验
        ThrowUtils.throwIf(message == null || message.trim().isEmpty(), ErrorCode.PARAMS_ERROR, "消息不能为空");


        // 5. 调用 AI 服务
        String result = AiCustomerClient.chat(userId, message);

        // 6. 统一返回成功结果（和注册接口完全一致）
        return ResultUtils.success(result);
    }
}