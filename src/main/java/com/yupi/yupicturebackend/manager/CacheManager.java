package com.yupi.yupicturebackend.manager;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 三级缓存管理类
 */
@Component
public class CacheManager {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 本地 Caffeine 缓存
     */
    private final Cache<String, String> LOCAL_CACHE = Caffeine.newBuilder()
            .initialCapacity(1024)
            .maximumSize(10_000L)
            .expireAfterWrite(Duration.ofMinutes(5))
            .build();

    /**
     * 通用分页查询三级缓存方法
     *
     * @param cacheKeyPrefix  缓存 Key 前缀（如 "yupicture:listPictureVOByPage"）
     * @param queryCondition  查询条件对象（用于生成唯一缓存 Key）
     * @param dbQueryFunction 数据库查询逻辑（函数式接口，传入 Lambda 表达式）
     * @param resultClass     返回结果的 Class 类型
     * @param <T>             结果类型泛型
     * @return 查询结果
     */
    public <T> T getCachedPageResult(String cacheKeyPrefix,
                                      Object queryCondition,
                                      Supplier<T> dbQueryFunction,
                                      Class<T> resultClass) {
        // 1. 构建唯一缓存 Key
        String queryJson = JSONUtil.toJsonStr(queryCondition);
        String hashKey = DigestUtils.md5DigestAsHex(queryJson.getBytes());
        String cacheKey = String.format("%s:%s", cacheKeyPrefix, hashKey);

        // 2. 先查本地缓存
        String cachedValue = LOCAL_CACHE.getIfPresent(cacheKey);
        if (cachedValue != null) {
            return JSONUtil.toBean(cachedValue, resultClass);
        }

        // 3. 本地缓存未命中，查 Redis
        ValueOperations<String, String> opsForValue = stringRedisTemplate.opsForValue();
        cachedValue = opsForValue.get(cacheKey);
        if (cachedValue != null) {
            // 同步更新本地缓存
            LOCAL_CACHE.put(cacheKey, cachedValue);
            return JSONUtil.toBean(cachedValue, resultClass);
        }

        // 4. 都未命中，查数据库
        T result = dbQueryFunction.get();

        // 5. 更新两级缓存
        String resultJson = JSONUtil.toJsonStr(result);
        // Redis 过期时间：5-10 分钟随机（防止缓存雪崩）
        int expireTime = 300 + RandomUtil.randomInt(0, 300);
        opsForValue.set(cacheKey, resultJson, expireTime, TimeUnit.SECONDS);
        // 更新本地缓存
        LOCAL_CACHE.put(cacheKey, resultJson);

        return result;
    }
}