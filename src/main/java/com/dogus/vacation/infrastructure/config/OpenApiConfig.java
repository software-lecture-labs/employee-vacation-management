package com.dogus.vacation.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI'da ({@code /swagger-ui.html}) görünen API bilgileri ve Bearer token girişi.
 *
 * <p>Kullanım: {@code POST /api/auth/login} ile token al, sağ üstteki "Authorize"
 * düğmesine token'ı yapıştır; sonraki bütün istekler {@code Authorization: Bearer ...}
 * başlığıyla gider.
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI employeeVacationOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Employee Vacation Management API")
                        .description("Çalışan izin talebi, onay, iptal ve bakiye yönetimi")
                        .version("v1"))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("POST /api/auth/login cevabındaki accessToken")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
