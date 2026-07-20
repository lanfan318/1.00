package com.hollysys.ppa.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j (Swagger 3) 配置
 *
 * @author PPA Team
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("电厂智能预警与故障诊断 Agent 系统 API")
                        .version("1.0.0")
                        .description("电厂监盘智能化系统后端服务接口文档")
                        .contact(new Contact()
                                .name("PPA Team")
                                .email("ppa@hollysys.com"))
                        .license(new License()
                                .name("内部使用")
                                .url("https://www.hollysys.com")));
    }
}
