package com.yupi.yupicturebackend.api.imagesearch.sub;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.json.JSONUtil;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class GetImagePageUrlApi {

    public static String getImagePageUrl(String imageUrl) {
        // 前置校验：图片URL不能为空且必须是HTTP/HTTPS开头
        if (StrUtil.isBlank(imageUrl) || !imageUrl.startsWith("http")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片URL为空或非合法HTTP地址");
        }

        // 1. 准备请求参数（核心修复：对imageUrl做URL编码）
        Map<String, Object> formData = new HashMap<>();
        // 关键修复1：图片URL必须UTF-8编码，这是解决Params illegal的核心
        formData.put("image", URLUtil.encode(imageUrl, StandardCharsets.UTF_8));
        formData.put("tn", "pc");
        formData.put("from", "pc");
        formData.put("image_source", "PC_UPLOAD_URL");
        // 补充百度要求的隐含参数，避免参数缺失导致非法
        formData.put("sdkParams", "");
        formData.put("range", "0");

        // 获取当前时间戳（确保参数新鲜）
        long uptime = System.currentTimeMillis();
        // 请求地址
        String url = "https://graph.baidu.com/upload?uptime=" + uptime;
        log.info("调用百度上传接口，URL：{}，编码后图片URL：{}", url, formData.get("image"));

        try {
            // 2. 发送请求（核心修复2：补充浏览器请求头，模拟真实请求）
            HttpResponse httpResponse = HttpRequest.post(url)
                    // 模拟Chrome浏览器请求头，避免被判定为爬虫
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    // 必须设置Referer为百度域名，否则参数上下文非法
                    .header("Referer", "https://graph.baidu.com/")
                    // 设置Content-Type，确保参数解析格式正确
                    .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                    .form(formData)
                    // 延长超时时间，避免网络波动
                    .timeout(10000)
                    .execute();

            // 打印响应状态和原始内容，方便排查
            int status = httpResponse.getStatus();
            String body = httpResponse.body();
            log.info("百度上传接口响应状态：{}，原始响应：{}", status, body);

            if (status != HttpStatus.HTTP_OK) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败，状态码：" + status);
            }

            // 解析响应
            Map<String, Object> result = JSONUtil.toBean(body, Map.class);
            if (result == null) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口返回空响应");
            }

            // 处理百度返回的错误提示（比如Params illegal）
            Object statusObj = result.get("status");
            int respStatus = statusObj instanceof String ? Integer.parseInt((String) statusObj) : (Integer) statusObj;
            if (respStatus != 0) {
                String msg = (String) result.get("msg");
                log.error("百度 API 返回错误：{}", msg);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败：" + StrUtil.blankToDefault(msg, "未知错误"));
            }

            // 逐层校验，避免空指针
            Object dataObj = result.get("data");
            if (!(dataObj instanceof Map)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口返回data格式错误");
            }
            Map<String, Object> data = (Map<String, Object>) dataObj;

            String rawUrl = (String) data.get("url");
            if (StrUtil.isBlank(rawUrl)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "未返回有效的结果地址");
            }

            // 对URL解码
            String searchResultUrl = URLUtil.decode(rawUrl, StandardCharsets.UTF_8);
            if (StrUtil.isBlank(searchResultUrl)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "URL解码后为空");
            }

            return searchResultUrl;
        } catch (BusinessException e) {
            // 已知业务异常，直接抛出
            throw e;
        } catch (Exception e) {
            log.error("调用百度以图搜图接口失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "搜索失败：" + e.getMessage());
        }
    }

    public static void main(String[] args) {
        // 测试修复后的功能
        String imageUrl = "https://www.codefather.cn/logo.png";
        try {
            String searchResultUrl = getImagePageUrl(imageUrl);
            System.out.println("搜索成功，结果 URL：" + searchResultUrl);
        } catch (Exception e) {
            System.out.println("搜索失败：" + e.getMessage());
        }
    }
}