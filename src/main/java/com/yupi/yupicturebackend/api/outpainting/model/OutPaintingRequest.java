package com.yupi.yupicturebackend.api.outpainting.model;

import lombok.Data;

/**
 * 扩图请求参数
 * 统一的扩图请求格式，适用于所有扩图接口
 */
@Data
public class OutPaintingRequest {

    /**
     * 输入图片URL
     */
    private String inputUrl;

    /**
     * 扩展比例（宽:高）
     * 支持：1:1、4:3、3:4、16:9、9:16
     */
    private String ratio;

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
