package com.yupi.yupicturebackend.api.outpainting;

import com.yupi.yupicturebackend.api.outpainting.model.OutPaintingRequest;
import com.yupi.yupicturebackend.api.outpainting.model.OutPaintingResponse;

/**
 * 扩图服务接口
 * 定义统一的扩图操作方法，所有扩图接口实现都需要实现此接口
 */
public interface OutPaintingService {

    /**
     * 执行扩图操作
     * @param request 扩图请求参数
     * @return 扩图响应结果
     */
    OutPaintingResponse outpaint(OutPaintingRequest request);

    /**
     * 获取服务名称
     * @return 服务名称
     */
    String getServiceName();

    /**
     * 健康检查
     * @return 是否健康
     */
    boolean healthCheck();

    /**
     * 获取并发限制
     * @return 并发限制数
     */
    int getConcurrencyLimit();

    /**
     * 获取权重
     * @return 权重值
     */
    int getWeight();

    /**
     * 设置权重
     * @param weight 权重值
     */
    void setWeight(int weight);
}
