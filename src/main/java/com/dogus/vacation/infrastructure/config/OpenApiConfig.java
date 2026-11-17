package com.dogus.vacation.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI'da ({@code /swagger-ui.html}) görünen API bilgileri.
 *
 * <p>Bearer token ("Authorize") ayarı kimlik doğrulama eklendiğinde buraya gelir.
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    public OpenAPI employeeVacationOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Employee Vacation Management API")
                .description("Çalışan izin talebi, onay, iptal ve bakiye yönetimi")
                .version("v1"));
    }
}
