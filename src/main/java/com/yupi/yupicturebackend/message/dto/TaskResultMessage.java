package com.yupi.yupicturebackend.message.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 任务结果消息
 */
@Data
public class TaskResultMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 任务状态
     */
    private String status;

    /**
     * 输出图片URL
     */
    private String outputImageUrl;

    /**
     * 完成时间
     */
    private Date completedAt;

    /**
     * 用户ID
     */
    private Long userId;
}
