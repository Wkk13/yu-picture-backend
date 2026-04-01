package com.yupi.yupicturebackend.api.ollama;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.yupi.yupicturebackend.config.AiCustomerConfig;
import com.yupi.yupicturebackend.utils.AdvancedRAG;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AiCustomerClient {

    private static AiCustomerConfig aiCustomerConfig;

    @Autowired
    public void setAiCustomerConfig(AiCustomerConfig config) {
        AiCustomerClient.aiCustomerConfig = config;
    }

    public static String chat(String userId, String msg) throws Exception {
        List<Map<String, String>> messages = new ArrayList<>();
        // 添加系统提示
        messages.add(Map.of("role", "system", "content", AiCustomerConfig.SYSTEM_PROMPT));
        
        // RAG 检索知识库
        String knowledge = AdvancedRAG.getKnowledge(msg);
        String prompt = knowledge + "\n用户问题：" + msg;
        
        // 添加用户消息
        messages.add(Map.of("role", "user", "content", prompt));

        // 构建请求体
        Map<String, Object> requestBody = buildRequestBody(messages);

        // 发送POST请求
        try (HttpResponse response = HttpRequest.post(aiCustomerConfig.API_URL)
                .header("Authorization", "Bearer " + aiCustomerConfig.API_KEY)
                .header("Content-Type", "application/json")
                .body(JSONUtil.toJsonStr(requestBody))
                .timeout(30000)
                .execute()) {

            // 打印响应内容，以便调试
            System.out.println("Aliyun API Status Code: " + response.getStatus());
            System.out.println("Aliyun API Response: " + response.body());

            // 解析响应
            String reply;
            if (response.isOk()) {
                cn.hutool.json.JSONObject jsonObject = JSONUtil.parseObj(response.body());
                if (jsonObject.containsKey("output")) {
                    cn.hutool.json.JSONObject output = jsonObject.getJSONObject("output");
                    if (output.containsKey("choices")) {
                        cn.hutool.json.JSONArray choices = output.getJSONArray("choices");
                        if (!choices.isEmpty()) {
                            cn.hutool.json.JSONObject choice = choices.getJSONObject(0);
                            if (choice.containsKey("message")) {
                                cn.hutool.json.JSONObject message = choice.getJSONObject("message");
                                if (message.containsKey("content")) {
                                    reply = message.getStr("content");
                                } else {
                                    reply = "抱歉，我暂时无法回答您的问题，请稍后再试。";
                                }
                            } else {
                                reply = "抱歉，我暂时无法回答您的问题，请稍后再试。";
                            }
                        } else {
                            reply = "抱歉，我暂时无法回答您的问题，请稍后再试。";
                        }
                    } else if (output.containsKey("text")) {
                        // 兼容旧版响应格式
                        reply = output.getStr("text");
                    } else {
                        reply = "抱歉，我暂时无法回答您的问题，请稍后再试。";
                    }
                } else {
                    reply = "抱歉，我暂时无法回答您的问题，请稍后再试。";
                }
            } else {
                reply = "抱歉，我暂时无法回答您的问题，请稍后再试。";
            }

            return reply;
        } catch (Exception e) {
            e.printStackTrace();
            return "抱歉，我暂时无法回答您的问题，请稍后再试。";
        }
    }

    /**
     * 构建请求参数
     */
    private static Map<String, Object> buildRequestBody(List<Map<String, String>> messages) {
        Map<String, Object> params = new HashMap<>();
        
        // 模型名称
        params.put("model", aiCustomerConfig.MODEL_CODE);
        
        // input 节点
        Map<String, Object> input = new HashMap<>();
        input.put("messages", messages);
        params.put("input", input);
        
        // parameters 节点
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("result_format", "message");
        params.put("parameters", parameters);
        
        return params;
    }
}