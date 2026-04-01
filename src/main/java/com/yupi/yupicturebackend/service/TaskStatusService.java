package com.yupi.yupicturebackend.service;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 任务状态管理服务
 */
@Slf4j
@Service
public class TaskStatusService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // 任务状态前缀
    private static final String TASK_STATUS_PREFIX = "yupicture:task:status:";

    // 任务状态有效期（24小时）
    private static final long TASK_STATUS_EXPIRE = 24 * 60 * 60;

    /**
     * 保存任务状态
     * @param taskId 任务ID
     * @param status 任务状态
     * @param outputImageUrl 输出图片URL
     */
    public void saveTaskStatus(String taskId, String status, String outputImageUrl) {
        try {
            Map<String, Object> statusMap = new HashMap<>();
            statusMap.put("status", status);
            statusMap.put("outputImageUrl", outputImageUrl);
            statusMap.put("updatedAt", System.currentTimeMillis());

            String key = TASK_STATUS_PREFIX + taskId;
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(statusMap), TASK_STATUS_EXPIRE, TimeUnit.SECONDS);
            log.info("保存任务状态成功: {}, status: {}", taskId, status);
        } catch (Exception e) {
            log.error("保存任务状态失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 获取任务状态
     * @param taskId 任务ID
     * @return 任务状态信息
     */
    public Map<String, Object> getTaskStatus(String taskId) {
        try {
            String key = TASK_STATUS_PREFIX + taskId;
            String statusJson = stringRedisTemplate.opsForValue().get(key);
            if (statusJson != null) {
                return JSONUtil.toBean(statusJson, Map.class);
            }
        } catch (Exception e) {
            log.error("获取任务状态失败: {}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * 更新任务状态
     * @param taskId 任务ID
     * @param status 任务状态
     */
    public void updateTaskStatus(String taskId, String status) {
        Map<String, Object> statusMap = getTaskStatus(taskId);
        if (statusMap != null) {
            statusMap.put("status", status);
            statusMap.put("updatedAt", System.currentTimeMillis());
            String key = TASK_STATUS_PREFIX + taskId;
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(statusMap), TASK_STATUS_EXPIRE, TimeUnit.SECONDS);
            log.info("更新任务状态成功: {}, status: {}", taskId, status);
        }
    }

    /**
     * 删除任务状态
     * @param taskId 任务ID
     */
    public void deleteTaskStatus(String taskId) {
        try {
            String key = TASK_STATUS_PREFIX + taskId;
            stringRedisTemplate.delete(key);
            log.info("删除任务状态成功: {}", taskId);
        } catch (Exception e) {
            log.error("删除任务状态失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 检查任务是否存在
     * @param taskId 任务ID
     * @return 是否存在
     */
    public boolean existsTask(String taskId) {
        try {
            String key = TASK_STATUS_PREFIX + taskId;
            return stringRedisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error("检查任务是否存在失败: {}", e.getMessage(), e);
            return false;
        }
    }
}
