package com.yupi.yupicturebackend.message.consumer;

import com.yupi.yupicturebackend.api.outpainting.impl.TencentOutPaintingService;
import com.yupi.yupicturebackend.api.outpainting.model.OutPaintingRequest;
import com.yupi.yupicturebackend.api.outpainting.model.OutPaintingResponse;
import com.yupi.yupicturebackend.config.RabbitMQConfig;
import com.yupi.yupicturebackend.message.dto.TaskCreateMessage;
import com.yupi.yupicturebackend.message.producer.OutPaintingTaskProducer;
import com.yupi.yupicturebackend.model.entity.Picture;
import com.yupi.yupicturebackend.service.PictureService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 腾讯云扩图任务消息消费者
 */
@Slf4j
@Component
public class TencentOutPaintingTaskConsumer {

    @Resource
    private TencentOutPaintingService tencentOutPaintingService;

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
    @RabbitListener(queues = RabbitMQConfig.TASK_CREATE_QUEUE, concurrency = "5")
    public void handleTaskCreateMessage(TaskCreateMessage message) {
        log.info("腾讯云消费者接收到任务创建消息: {}", message);

        try {
            // 1. 获取图片信息
            Picture picture = pictureService.getById(message.getPictureId());
            if (picture == null) {
                log.error("图片不存在: {}", message.getPictureId());
                return;
            }

            // 2. 构建扩图请求
            OutPaintingRequest request = new OutPaintingRequest();
            request.setInputUrl(picture.getUrl());

            TaskCreateMessage.Parameters msgParams = message.getParameters();
            if (msgParams != null) {
                request.setXScale(msgParams.getXScale());
                request.setYScale(msgParams.getYScale());
                request.setTopOffset(msgParams.getTopOffset());
                request.setBottomOffset(msgParams.getBottomOffset());
                request.setLeftOffset(msgParams.getLeftOffset());
                request.setRightOffset(msgParams.getRightOffset());
            }

            // 3. 调用腾讯云AI接口执行扩图操作
            log.info("腾讯云消费者调用AI扩图接口: {}", message.getTaskId());
            OutPaintingResponse response = tencentOutPaintingService.outpaint(request);

            // 4. 处理扩图结果
            log.info("扩图操作完成: {}, 状态: {}", 
                    message.getTaskId(), response.getStatus());

            outPaintingTaskProducer.sendTaskResultMessage(
                    message.getTaskId(),
                    response.getStatus(),
                    response.getOutputImageUrl(),
                    message.getUserId()
            );

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
     * 监听任务结果队列
     * @param message 任务结果消息
     */
    @RabbitListener(queues = RabbitMQConfig.TASK_RESULT_QUEUE)
    public void handleTaskResultMessage(com.yupi.yupicturebackend.message.dto.TaskResultMessage message) {
        log.info("腾讯云消费者接收到任务结果消息: {}", message);

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