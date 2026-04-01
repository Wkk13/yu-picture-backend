package com.yupi.yupicturebackend.utils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 对话上下文缓存（多轮对话记忆）
 */
public class ChatHistoryUtil {
    private static final Cache<String, List<Map<String, String>>> CACHE =
            CacheBuilder.newBuilder()
                    .expireAfterAccess(10, TimeUnit.MINUTES)
                    .build();

    public static List<Map<String, String>> get(String userId) {
        return CACHE.getIfPresent(userId);
    }

    public static void save(String userId, List<Map<String, String>> history) {
        CACHE.put(userId, history);
    }
}