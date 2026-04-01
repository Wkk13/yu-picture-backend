package com.yupi.yupicturebackend.message.producer;

import com.yupi.yupicturebackend.config.RabbitMQConfig;
import com.yupi.yupicturebackend.message.dto.TaskCreateMessage;
import com.yupi.yupicturebackend.message.dto.TaskProcessMessage;
import com.yupi.yupicturebackend.message.dto.TaskResultMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 扩图任务消息生产者
 */
@Slf4j
@Component
public class OutPaintingTaskProducer {

    @Resource
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送任务创建消息
     * @param message 任务创建消息
     */
    public void sendTaskCreateMessage(TaskCreateMessage message) {
        try {
            log.info("发送任务创建消息: {}", message);
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.TASK_EXCHANGE,
                    RabbitMQConfig.TASK_CREATE_ROUTING_KEY,
                    message
            );
            log.info("任务创建消息发送成功: {}", message.getTaskId());
        } catch (Exception e) {
            log.error("发送任务创建消息失败: {}", e.getMessage(), e);
            throw new RuntimeException("发送任务创建消息失败", e);
        }
    }

    /**
     * 发送任务处理消息
     * @param taskId 任务ID
     * @param status 任务状态
     * @param aiTaskId AI任务ID
     */
    public void sendTaskProcessMessage(String taskId, String status, String aiTaskId) {
        try {
            com.yupi.yupicturebackend.message.dto.TaskProcessMessage message = new com.yupi.yupicturebackend.message.dto.TaskProcessMessage();
            message.setTaskId(taskId);
            message.setStatus(status);
            message.setAiTaskId(aiTaskId);
            message.setUpdatedAt(new java.util.Date());
            
            log.info("发送任务处理消息: {}", message);
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.TASK_EXCHANGE,
                    RabbitMQConfig.TASK_PROCESS_ROUTING_KEY,
                    message
            );
            log.info("任务处理消息发送成功: {}", taskId);
        } catch (Exception e) {
            log.error("发送任务处理消息失败: {}", e.getMessage(), e);
            throw new RuntimeException("发送任务处理消息失败", e);
        }
    }

    /**
     * 发送任务结果消息
     * @param taskId 任务ID
     * @param status 任务状态
     * @param outputImageUrl 输出图片URL
     * @param userId 用户ID
     */
    public void sendTaskResultMessage(String taskId, String status, String outputImageUrl, Long userId) {
        try {
            com.yupi.yupicturebackend.message.dto.TaskResultMessage message = new com.yupi.yupicturebackend.message.dto.TaskResultMessage();
            message.setTaskId(taskId);
            message.setStatus(status);
            message.setOutputImageUrl(outputImageUrl);
            message.setCompletedAt(new java.util.Date());
            message.setUserId(userId);
            
            log.info("发送任务结果消息: {}", message);
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.TASK_EXCHANGE,
                    RabbitMQConfig.TASK_RESULT_ROUTING_KEY,
                    message
            );
            log.info("任务结果消息发送成功: {}", taskId);
        } catch (Exception e) {
            log.error("发送任务结果消息失败: {}", e.getMessage(), e);
            throw new RuntimeException("发送任务结果消息失败", e);
        }
    }


}
