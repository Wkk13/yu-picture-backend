package com.yupi.yupicturebackend.api.outpainting.impl;

import com.yupi.yupicturebackend.api.aliyunai.AliYunAiApi;
import com.yupi.yupicturebackend.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.yupi.yupicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.yupi.yupicturebackend.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.yupi.yupicturebackend.api.outpainting.OutPaintingService;
import com.yupi.yupicturebackend.api.outpainting.model.OutPaintingRequest;
import com.yupi.yupicturebackend.api.outpainting.model.OutPaintingResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 阿里云扩图服务实现
 */
@Service
@Slf4j
public class AliyunOutPaintingService implements OutPaintingService {

    @Autowired
    private AliYunAiApi aliYunAiApi;

    private int weight = 100;
    private final int concurrencyLimit = 10;

    @Override
    public OutPaintingResponse outpaint(OutPaintingRequest request) {
        long startTime = System.currentTimeMillis();
        OutPaintingResponse response = new OutPaintingResponse();
        response.setServiceName(getServiceName());

        try {
            // 构建阿里云扩图请求
            CreateOutPaintingTaskRequest aliyunRequest = buildAliyunRequest(request);
            // 创建扩图任务
            CreateOutPaintingTaskResponse createResponse = aliYunAiApi.createOutPaintingTask(aliyunRequest);
            // 获取任务ID
            String taskId = createResponse.getOutput().getTaskId();
            response.setTaskId(taskId);
            response.setStatus(createResponse.getOutput().getTaskStatus());
            
            // 轮询任务状态
            GetOutPaintingTaskResponse taskResponse = pollTaskStatus(taskId);
            if (taskResponse != null) {
                String taskStatus = taskResponse.getOutput().getTaskStatus();
                response.setStatus(taskStatus);
                if ("SUCCEEDED".equals(taskStatus)) {
                    response.setOutputImageUrl(taskResponse.getOutput().getOutputImageUrl());
                } else if ("FAILED".equals(taskStatus)) {
                    response.setErrorCode("ALIYUN_ERROR");
                    response.setErrorMessage("Aliyun outpainting failed");
                }
            }
        } catch (Exception e) {
            log.error("Aliyun outpainting failed: {}", e.getMessage(), e);
            response.setStatus("FAILED");
            response.setErrorCode("INTERNAL_ERROR");
            response.setErrorMessage(e.getMessage());
        }

        response.setProcessingTime(System.currentTimeMillis() - startTime);
        return response;
    }

    @Override
    public String getServiceName() {
        return "aliyun";
    }

    @Override
    public boolean healthCheck() {
        try {
            // 简单检查API密钥是否配置
            if (aliYunAiApi == null) {
                log.warn("Aliyun AI API not initialized");
                return false;
            }
            // 暂时返回true，避免因为API密钥未配置而导致服务不可用
            return true;
        } catch (Exception e) {
            log.warn("Aliyun outpainting health check failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public int getConcurrencyLimit() {
        return concurrencyLimit;
    }

    @Override
    public int getWeight() {
        return weight;
    }

    @Override
    public void setWeight(int weight) {
        this.weight = weight;
    }

    /**
     * 构建阿里云扩图请求
     */
    private CreateOutPaintingTaskRequest buildAliyunRequest(OutPaintingRequest request) {
        CreateOutPaintingTaskRequest aliyunRequest = new CreateOutPaintingTaskRequest();
        
        // 设置输入图像信息
        CreateOutPaintingTaskRequest.Input input = new CreateOutPaintingTaskRequest.Input();
        input.setImageUrl(request.getInputUrl());
        aliyunRequest.setInput(input);
        
        // 设置参数
        CreateOutPaintingTaskRequest.Parameters parameters = new CreateOutPaintingTaskRequest.Parameters();
        parameters.setXScale(request.getXScale());
        parameters.setYScale(request.getYScale());
        parameters.setTopOffset(request.getTopOffset());
        parameters.setBottomOffset(request.getBottomOffset());
        parameters.setLeftOffset(request.getLeftOffset());
        parameters.setRightOffset(request.getRightOffset());
        
        aliyunRequest.setParameters(parameters);
        return aliyunRequest;
    }

    /**
     * 轮询任务状态
     */
    private GetOutPaintingTaskResponse pollTaskStatus(String taskId) throws Exception {
        int maxRetries = 30;
        int retryInterval = 2000; // 2秒

        for (int i = 0; i < maxRetries; i++) {
            GetOutPaintingTaskResponse response = aliYunAiApi.getOutPaintingTask(taskId);
            String taskStatus = response.getOutput().getTaskStatus();
            
            if ("SUCCEEDED".equals(taskStatus) || "FAILED".equals(taskStatus)) {
                return response;
            }
            
            Thread.sleep(retryInterval);
        }
        
        throw new Exception("Task timeout after max retries");
    }
}