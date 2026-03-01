package com.yupi.yupicturebackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2WebMvc;

/**
 * Knife4j 接口文档配置
 * 
 * @author yupi
 */
@Configuration
@EnableSwagger2WebMvc
public class Knife4jConfig {

    @Bean(value = "defaultApi2")
    public Docket defaultApi2() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                // 指定 Controller 扫描包路径
                .apis(RequestHandlerSelectors.basePackage("com.yupi.yupicturebackend.controller"))
                .paths(PathSelectors.any())
                .build();
    }

    /**
     * API 页面展示信息
     */
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("鱼图后端接口文档")
                .description("鱼图项目的后端接口文档")
                .version("1.0")
                .contact(new Contact("yupi", "https://github.com/liyupi", "liyupi@example.com"))
                .license("MIT")
                .licenseUrl("https://opensource.org/licenses/MIT")
                .build();
    }
}