package com.dogus.vacation.infrastructure.security;

import java.io.IOException;

import com.dogus.vacation.domain.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Giriş yapılmış ama rol yetmiyorsa (örneğin çalışan, İK adresini çağırdı) 403 ve
 * {@link ErrorCode#ACTION_NOT_ALLOWED} döner (FR-03).
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(RestAccessDeniedHandler.class);

    private final SecurityErrorWriter errorWriter;

    public RestAccessDeniedHandler(SecurityErrorWriter errorWriter) {
        this.errorWriter = errorWriter;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException exception) throws IOException {
        log.info("Yetkisiz erişim denemesi {} {}", request.getMethod(), request.getRequestURI());
        errorWriter.write(request, response, HttpStatus.FORBIDDEN, ErrorCode.ACTION_NOT_ALLOWED);
    }
}
