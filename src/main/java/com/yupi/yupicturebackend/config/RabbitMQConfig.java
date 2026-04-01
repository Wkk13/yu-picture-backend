package com.yupi.yupicturebackend.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ配置类
 */
@Configuration
public class RabbitMQConfig {

    // 队列名称
    public static final String TASK_CREATE_QUEUE = "image-outpainting-task-create";
    public static final String TASK_PROCESS_QUEUE = "image-outpainting-task-process";
    public static final String TASK_RESULT_QUEUE = "image-outpainting-task-result";
    public static final String TASK_DEAD_QUEUE = "image-outpainting-task-dead";

    // 交换机名称
    public static final String TASK_EXCHANGE = "image-outpainting-task-exchange";
    public static final String DEAD_EXCHANGE = "image-outpainting-dead-exchange";

    // 路由键
    public static final String TASK_CREATE_ROUTING_KEY = "task.create";
    public static final String TASK_PROCESS_ROUTING_KEY = "task.process";
    public static final String TASK_RESULT_ROUTING_KEY = "task.result";
    public static final String TASK_DEAD_ROUTING_KEY = "task.dead";

    /**
     * 创建任务交换机
     */
    @Bean
    public DirectExchange taskExchange() {
        return new DirectExchange(TASK_EXCHANGE, true, false);
    }

    /**
     * 创建死信交换机
     */
    @Bean
    public DirectExchange deadExchange() {
        return new DirectExchange(DEAD_EXCHANGE, true, false);
    }

    /**
     * 创建任务创建队列
     */
    @Bean
    public Queue taskCreateQueue() {
        return QueueBuilder.durable(TASK_CREATE_QUEUE)
                .withArgument("x-dead-letter-exchange", DEAD_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", TASK_DEAD_ROUTING_KEY)
                .withArgument("x-message-ttl", 60000) // 60秒过期
                .build();
    }

    /**
     * 创建任务处理队列
     */
    @Bean
    public Queue taskProcessQueue() {
        return QueueBuilder.durable(TASK_PROCESS_QUEUE)
                .withArgument("x-dead-letter-exchange", DEAD_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", TASK_DEAD_ROUTING_KEY)
                .withArgument("x-message-ttl", 300000) // 5分钟过期
                .build();
    }

    /**
     * 创建任务结果队列
     */
    @Bean
    public Queue taskResultQueue() {
        return QueueBuilder.durable(TASK_RESULT_QUEUE)
                .build();
    }

    /**
     * 创建死信队列
     */
    @Bean
    public Queue taskDeadQueue() {
        return QueueBuilder.durable(TASK_DEAD_QUEUE)
                .build();
    }

    /**
     * 绑定任务创建队列
     */
    @Bean
    public Binding taskCreateBinding(Queue taskCreateQueue, DirectExchange taskExchange) {
        return BindingBuilder.bind(taskCreateQueue)
                .to(taskExchange)
                .with(TASK_CREATE_ROUTING_KEY);
    }

    /**
     * 绑定任务处理队列
     */
    @Bean
    public Binding taskProcessBinding(Queue taskProcessQueue, DirectExchange taskExchange) {
        return BindingBuilder.bind(taskProcessQueue)
                .to(taskExchange)
                .with(TASK_PROCESS_ROUTING_KEY);
    }

    /**
     * 绑定任务结果队列
     */
    @Bean
    public Binding taskResultBinding(Queue taskResultQueue, DirectExchange taskExchange) {
        return BindingBuilder.bind(taskResultQueue)
                .to(taskExchange)
                .with(TASK_RESULT_ROUTING_KEY);
    }

    /**
     * 绑定死信队列
     */
    @Bean
    public Binding taskDeadBinding(Queue taskDeadQueue, DirectExchange deadExchange) {
        return BindingBuilder.bind(taskDeadQueue)
                .to(deadExchange)
                .with(TASK_DEAD_ROUTING_KEY);
    }
}
