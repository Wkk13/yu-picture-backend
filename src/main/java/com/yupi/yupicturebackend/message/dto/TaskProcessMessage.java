package com.yupi.yupicturebackend.message.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 任务处理消息
 */
@Data
public class TaskProcessMessage implements Serializable {
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
     * AI任务ID
     */
    private String aiTaskId;

    /**
     * 更新时间
     */
    private Date updatedAt;
}
