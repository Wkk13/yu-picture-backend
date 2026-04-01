package com.yupi.yupicturebackend;

import com.yupi.yupicturebackend.utils.AdvancedRAG;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@MapperScan("com.yupi.yupicturebackend.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
public class YuPictureBackendApplication {

    public static void main(String[] args) throws Exception {
        SpringApplication.run(YuPictureBackendApplication.class, args);
        // 🔥 启动 RAG 知识库
        AdvancedRAG.init();
    }

}
