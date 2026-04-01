package com.yupi.yupicturebackend.api.outpainting.impl;

import com.yupi.yupicturebackend.api.outpainting.OutPaintingService;
import com.yupi.yupicturebackend.api.outpainting.model.OutPaintingRequest;
import com.yupi.yupicturebackend.api.outpainting.model.OutPaintingResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * 腾讯云扩图服务实现
 */
@Service
@Slf4j
public class TencentOutPaintingService implements OutPaintingService {

    @Value("${tencent.ai.secretId}")
    private String secretId;

    @Value("${tencent.ai.secretKey}")
    private String secretKey;

    @Value("${tencent.ai.region:ap-southeast-1}")
    private String region;

    private final String API_ENDPOINT = "https://aiart.%s.tencentcloudapi.com";
    private final String API_VERSION = "2022-12-29";
    private final String API_ACTION = "ImageOutpainting";

    private final RestTemplate restTemplate = new RestTemplate();
    private int weight = 100;
    private final int concurrencyLimit = 10;

    @Override
    public OutPaintingResponse outpaint(OutPaintingRequest request) {
        long startTime = System.currentTimeMillis();
        OutPaintingResponse response = new OutPaintingResponse();
        response.setServiceName(getServiceName());

        try {
            // 构建业务参数
            Map<String, Object> businessParams = new HashMap<>();
            businessParams.put("InputUrl", request.getInputUrl());
            businessParams.put("Ratio", request.getRatio() != null ? request.getRatio() : "4:3");
            businessParams.put("RspImgType", "url");
            businessParams.put("LogoAdd", 1);

            // 构建请求参数（包含公共参数）
            Map<String, Object> params = new HashMap<>();
            params.put("Action", API_ACTION);
            params.put("Version", API_VERSION);
            params.put("Region", region);

            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            String host = String.format("aiart.%s.tencentcloudapi.com", region);
            headers.add("Host", host);
            headers.add("Content-Type", "application/json");
            headers.add("X-TC-Action", API_ACTION);
            headers.add("X-TC-Version", API_VERSION);
            headers.add("X-TC-Region", region);
            headers.add("X-TC-Timestamp", String.valueOf(System.currentTimeMillis() / 1000));
            headers.add("X-TC-SecretId", secretId);
            headers.add("X-TC-Signature", generateSignature(params));

            // 构建请求实体
            HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(businessParams, headers);
            // 发送请求
            String endpoint = String.format(API_ENDPOINT, region);
            log.info("Tencent API request: endpoint={}, headers={}, params={}", endpoint, headers, businessParams);
            ResponseEntity<Map> apiResponse = restTemplate.exchange(
                    endpoint, HttpMethod.POST, httpEntity, Map.class);
            log.info("Tencent API response: status={}, body={}", apiResponse.getStatusCode(), apiResponse.getBody());
            // 处理响应
            handleResponse(apiResponse.getBody(), response);
        } catch (Exception e) {
            log.error("Tencent outpainting failed: {}", e.getMessage(), e);
            response.setStatus("FAILED");
            response.setErrorCode("INTERNAL_ERROR");
            response.setErrorMessage(e.getMessage());
        }

        response.setProcessingTime(System.currentTimeMillis() - startTime);
        return response;
    }

    @Override
    public String getServiceName() {
        return "tencent";
    }

    @Override
    public boolean healthCheck() {
        try {
            // 简单检查配置是否存在
            if (secretId == null || secretId.equals("your-secret-id") || secretKey == null || secretKey.equals("your-secret-key")) {
                log.warn("Tencent AI API not configured");
                return false;
            }
            // 暂时返回true，避免因为API密钥未配置而导致服务不可用
            return true;
        } catch (Exception e) {
            log.warn("Tencent outpainting health check failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public int getConcurrencyLimit() {
        return concurrencyLimit;
    }

    @Override
    public int getWeight() {
        return weight;
    }

    @Override
    public void setWeight(int weight) {
        this.weight = weight;
    }

    /**
     * 生成签名
     */
    private String generateSignature(Map<String, Object> params) throws Exception {
        // 1. 对参数进行排序
        TreeMap<String, Object> sortedParams = new TreeMap<>(params);
        // 2. 构建签名字符串
        StringBuilder sb = new StringBuilder();
        sb.append("POST").append("\n");
        String host = String.format("aiart.%s.tencentcloudapi.com", region);
        sb.append(host).append("\n");
        sb.append("/").append("\n");
        // 构建查询字符串
        boolean first = true;
        for (Map.Entry<String, Object> entry : sortedParams.entrySet()) {
            if (entry.getValue() != null) {
                if (!first) {
                    sb.append("&");
                }
                sb.append(entry.getKey()).append("=").append(entry.getValue());
                first = false;
            }
        }
        String signStr = sb.toString();
        // 3. 使用HMAC-SHA256算法生成签名
        return hmacSha256(signStr, secretKey);
    }

    /**
     * HMAC-SHA256加密
     */
    private String hmacSha256(String data, String key) throws Exception {
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        javax.crypto.spec.SecretKeySpec secretKeySpec = new javax.crypto.spec.SecretKeySpec(key.getBytes(), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] bytes = mac.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * 处理API响应
     */
    private void handleResponse(Map<String, Object> apiResponse, OutPaintingResponse response) {
        if (apiResponse == null) {
            response.setStatus("FAILED");
            response.setErrorCode("EMPTY_RESPONSE");
            response.setErrorMessage("Empty response from Tencent API");
            return;
        }

        // 检查响应结构 - 腾讯云官方格式
        if (apiResponse.containsKey("Response")) {
            Map<String, Object> responseData = (Map<String, Object>) apiResponse.get("Response");
            if (responseData.containsKey("Error")) {
                Map<String, Object> error = (Map<String, Object>) responseData.get("Error");
                response.setStatus("FAILED");
                response.setErrorCode((String) error.get("Code"));
                response.setErrorMessage((String) error.get("Message"));
            } else {
                response.setStatus("SUCCEEDED");
                if (responseData.containsKey("ResultImage")) {
                    response.setOutputImageUrl((String) responseData.get("ResultImage"));
                }
                if (responseData.containsKey("RequestId")) {
                    response.setTaskId((String) responseData.get("RequestId"));
                    response.setRequestId((String) responseData.get("RequestId"));
                }
            }
        } else {
            response.setStatus("FAILED");
            response.setErrorCode("INVALID_RESPONSE");
            response.setErrorMessage("Invalid response format from Tencent API");
        }
    }
}