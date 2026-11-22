package com.dogus.vacation.infrastructure.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

import com.dogus.vacation.api.advice.ErrorResponse;
import com.dogus.vacation.domain.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Security filtresinde oluşan 401 ve 403 hatalarını, {@code GlobalExceptionHandler}'ın
 * ürettiği {@link ErrorResponse} biçiminde yazar (NFR-05). Bu hatalar Controller'a
 * ulaşmadan oluştuğu için {@code @RestControllerAdvice} onları göremez.
 */
@Component
public class SecurityErrorWriter {

    private final JsonMapper jsonMapper;
    private final Clock clock;

    public SecurityErrorWriter(JsonMapper jsonMapper, Clock clock) {
        this.jsonMapper = jsonMapper;
        this.clock = clock;
    }

    void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status, ErrorCode code)
            throws IOException {
        ErrorResponse body = new ErrorResponse(OffsetDateTime.now(clock), status.value(), code.name(),
                code.defaultMessage(), request.getRequestURI(), List.of());
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        jsonMapper.writeValue(response.getOutputStream(), body);
    }
}
