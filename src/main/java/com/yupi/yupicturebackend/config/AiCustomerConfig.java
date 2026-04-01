package com.yupi.yupicturebackend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * AI客服配置（修改模型名称/客服话术即可）
 */
@Component
public class AiCustomerConfig {
    // 阿里云百炼配置
    @Value("${aliYunAi.apiUrl}")
    public String API_URL;
    
    @Value("${aliYunAi.modelCode}")
    public String MODEL_CODE;
    
    @Value("${aliYunAi.apiKey}")
    public String API_KEY;

    // 云图库AI客服角色
    public static final String SYSTEM_PROMPT = """
        你是云图库平台的专业AI客服。
        请严格根据参考资料回答用户问题，不编造、不扩展。
        不知道就说"暂无相关说明，可转人工客服"。
        回答简洁、礼貌、专业，只回答与云图库相关问题。
        """;
}