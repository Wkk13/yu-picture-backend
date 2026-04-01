package com.yupi.yupicturebackend.message.consumer;

import com.yupi.yupicturebackend.api.aliyunai.AliYunAiApi;
import com.yupi.yupicturebackend.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.yupi.yupicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.yupi.yupicturebackend.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.yupi.yupicturebackend.config.RabbitMQConfig;
import com.yupi.yupicturebackend.message.dto.TaskCreateMessage;
import com.yupi.yupicturebackend.message.producer.OutPaintingTaskProducer;
import com.yupi.yupicturebackend.model.entity.Picture;
import com.yupi.yupicturebackend.service.PictureService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * 阿里云扩图任务消息消费者
 */
@Slf4j
@Component
public class AliyunOutPaintingTaskConsumer {

    @Resource
    private AliYunAiApi aliYunAiApi;

    @Resource
    private PictureService pictureService;

    @Resource
    private OutPaintingTaskProducer outPaintingTaskProducer;

    @Resource
    private com.yupi.yupicturebackend.service.TaskStatusService taskStatusService;

    /**
     * 监听任务创建队列
     * @param message 任务创建消息
     */
    @RabbitListener(queues = RabbitMQConfig.TASK_CREATE_QUEUE, concurrency = "1")
    public void handleTaskCreateMessage(TaskCreateMessage message) {
        log.info("阿里云消费者接收到任务创建消息: {}", message);

        try {
            // 1. 获取图片信息
            Picture picture = pictureService.getById(message.getPictureId());
            if (picture == null) {
                log.error("图片不存在: {}", message.getPictureId());
                return;
            }

            // 2. 构建阿里云AI扩图请求
            CreateOutPaintingTaskRequest request = new CreateOutPaintingTaskRequest();
            request.setModel("image-out-painting");

            CreateOutPaintingTaskRequest.Input input = new CreateOutPaintingTaskRequest.Input();
            input.setImageUrl(picture.getUrl());
            request.setInput(input);

            CreateOutPaintingTaskRequest.Parameters parameters = new CreateOutPaintingTaskRequest.Parameters();
            TaskCreateMessage.Parameters msgParams = message.getParameters();
            if (msgParams != null) {
                parameters.setXScale(msgParams.getXScale());
                parameters.setYScale(msgParams.getYScale());
                parameters.setTopOffset(msgParams.getTopOffset());
                parameters.setBottomOffset(msgParams.getBottomOffset());
                parameters.setLeftOffset(msgParams.getLeftOffset());
                parameters.setRightOffset(msgParams.getRightOffset());
                parameters.setAngle(msgParams.getAngle());
                parameters.setOutputRatio(msgParams.getOutputRatio());
                parameters.setBestQuality(msgParams.getBestQuality());
                parameters.setLimitImageSize(msgParams.getLimitImageSize());
                parameters.setAddWatermark(msgParams.getAddWatermark());
            }
            request.setParameters(parameters);

            // 3. 调用阿里云AI接口创建任务
            log.info("阿里云消费者调用AI扩图接口: {}", message.getTaskId());
            CreateOutPaintingTaskResponse createResponse = aliYunAiApi.createOutPaintingTask(request);

            if (createResponse == null || createResponse.getOutput() == null) {
                log.error("创建AI任务失败: {}", message.getTaskId());
                outPaintingTaskProducer.sendTaskResultMessage(
                        message.getTaskId(),
                        "FAILED",
                        null,
                        message.getUserId()
                );
                return;
            }

            String aiTaskId = createResponse.getOutput().getTaskId();
            log.info("AI任务创建成功: {}, AI任务ID: {}", message.getTaskId(), aiTaskId);

            // 4. 轮询任务状态
            pollTaskStatus(message.getTaskId(), aiTaskId, message.getUserId());

        } catch (Exception e) {
            log.error("处理任务创建消息失败: {}", e.getMessage(), e);
            outPaintingTaskProducer.sendTaskResultMessage(
                    message.getTaskId(),
                    "FAILED",
                    null,
                    message.getUserId()
            );
        }
    }

    /**
     * 轮询任务状态
     * @param taskId 任务ID
     * @param aiTaskId AI任务ID
     * @param userId 用户ID
     */
    private void pollTaskStatus(String taskId, String aiTaskId, Long userId) {
        int maxRetries = 30; // 最大轮询次数
        int retryInterval = 2000; // 初始轮询间隔（毫秒）

        for (int i = 0; i < maxRetries; i++) {
            try {
                // 调用阿里云AI接口查询任务状态
                GetOutPaintingTaskResponse response = aliYunAiApi.getOutPaintingTask(aiTaskId);

                if (response == null || response.getOutput() == null) {
                    log.error("查询任务状态失败: {}", taskId);
                    TimeUnit.MILLISECONDS.sleep(retryInterval);
                    continue;
                }

                String status = response.getOutput().getTaskStatus();
                log.info("任务状态查询结果: {} - {}", taskId, status);

                // 根据状态处理
                switch (status) {
                    case "SUCCEEDED":
                        // 任务成功，获取输出图片URL
                        String outputImageUrl = response.getOutput().getOutputImageUrl();
                        log.info("任务完成成功: {}, 输出图片URL: {}", taskId, outputImageUrl);
                        outPaintingTaskProducer.sendTaskResultMessage(
                                taskId,
                                "SUCCEEDED",
                                outputImageUrl,
                                userId
                        );
                        return;
                    case "FAILED":
                    case "UNKNOWN":
                        // 任务失败或状态未知
                        log.error("任务执行失败: {}, 状态: {}", taskId, status);
                        outPaintingTaskProducer.sendTaskResultMessage(
                                taskId,
                                "FAILED",
                                null,
                                userId
                        );
                        return;
                    case "PENDING":
                    case "RUNNING":
                    case "SUSPENDED":
                        // 任务进行中，继续轮询
                        log.info("任务进行中: {} - {}", taskId, status);
                        break;
                    default:
                        log.warn("未知任务状态: {} - {}", taskId, status);
                        break;
                }

                // 指数退避策略
                retryInterval = Math.min(retryInterval * 2, 30000); // 最大30秒
                TimeUnit.MILLISECONDS.sleep(retryInterval);

            } catch (Exception e) {
                log.error("轮询任务状态失败: {}", e.getMessage(), e);
                try {
                    TimeUnit.MILLISECONDS.sleep(retryInterval);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        // 轮询超时
        log.error("任务轮询超时: {}", taskId);
        outPaintingTaskProducer.sendTaskResultMessage(
                taskId,
                "TIMEOUT",
                null,
                userId
        );
    }

    /**
     * 监听任务结果队列
     * @param message 任务结果消息
     */
    @RabbitListener(queues = RabbitMQConfig.TASK_RESULT_QUEUE)
    public void handleTaskResultMessage(com.yupi.yupicturebackend.message.dto.TaskResultMessage message) {
        log.info("阿里云消费者接收到任务结果消息: {}", message);

        try {
            // 保存任务状态到TaskStatusService
            taskStatusService.saveTaskStatus(
                    message.getTaskId(),
                    message.getStatus(),
                    message.getOutputImageUrl()
            );
            
            // 这里可以添加任务结果的后续处理逻辑
            // 例如：更新任务状态到数据库、通知用户等
            log.info("任务结果处理完成: {}, 状态: {}", message.getTaskId(), message.getStatus());

        } catch (Exception e) {
            log.error("处理任务结果消息失败: {}", e.getMessage(), e);
        }
    }


}