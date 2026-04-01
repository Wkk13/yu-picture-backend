package com.yupi.yupicturebackend.message.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 任务创建消息
 */
@Data
public class TaskCreateMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 图片ID
     */
    private Long pictureId;

    /**
     * 扩图参数
     */
    private Parameters parameters;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 扩图参数类
     */
    @Data
    public static class Parameters implements Serializable {
        private static final long serialVersionUID = 1L;
        /**
         * X轴缩放比例
         */
        private Float xScale;

        /**
         * Y轴缩放比例
         */
        private Float yScale;

        /**
         * 顶部偏移像素
         */
        private Integer topOffset;

        /**
         * 底部偏移像素
         */
        private Integer bottomOffset;

        /**
         * 左侧偏移像素
         */
        private Integer leftOffset;

        /**
         * 右侧偏移像素
         */
        private Integer rightOffset;

        /**
         * 旋转角度
         */
        private Integer angle;

        /**
         * 输出比例
         */
        private String outputRatio;

        /**
         * 是否开启最佳质量
         */
        private Boolean bestQuality;

        /**
         * 是否限制输出尺寸
         */
        private Boolean limitImageSize;

        /**
         * 是否添加水印
         */
        private Boolean addWatermark;
    }
}
