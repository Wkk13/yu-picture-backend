package com.yupi.yupicturebackend.api.outpainting.model;

import lombok.Data;

/**
 * 扩图响应结果
 * 统一的扩图响应格式，适用于所有扩图接口
 */
@Data
public class OutPaintingResponse {

    /**
     * 扩图结果图片URL
     */
    private String outputImageUrl;

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 任务状态
     * SUCCEEDED: 成功
     * FAILED: 失败
     * PENDING: 处理中
     * RUNNING: 运行中
     * TIMEOUT: 超时
     */
    private String status;

    /**
     * 错误码
     */
    private String errorCode;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 处理时间（毫秒）
     */
    private Long processingTime;

    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 请求ID
     */
    private String requestId;
}
