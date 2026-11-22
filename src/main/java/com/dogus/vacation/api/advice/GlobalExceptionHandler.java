package com.dogus.vacation.api.advice;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

import com.dogus.vacation.application.mediator.ValidationError;
import com.dogus.vacation.application.mediator.ValidationException;
import com.dogus.vacation.domain.exception.BusinessException;
import com.dogus.vacation.domain.exception.ErrorCategory;
import com.dogus.vacation.domain.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Bütün hataları {@link ErrorResponse} biçimine çevirir (NFR-05).
 *
 * <p>Hata türü → HTTP kodu eşlemesi sadece bu sınıftadır; alan katmanı HTTP bilmez.
 * <ul>
 *   <li>{@link ValidationException} → 400, {@code VALIDATION_FAILED} ve {@code fieldErrors}</li>
 *   <li>{@link BusinessException} → türüne göre 401 / 404 / 403 / 409 / 422, kod = {@link ErrorCode} adı
 *       (401: hatalı giriş bilgileri, {@code INVALID_CREDENTIALS})</li>
 *   <li>İyimser kilit çatışması → 409, {@code CONCURRENT_UPDATE} (NFR-08)</li>
 *   <li>Spring MVC hataları (bozuk JSON, yanlış metot, yanlış içerik tipi) → kendi kodları</li>
 *   <li>Beklenmeyen her şey → 500, genel mesaj; ayrıntı sadece loga yazılır</li>
 * </ul>
 *
 * <p>Token yok/geçersiz (401) ve rol yetkisi yok (403) hataları Spring Security filtresinde
 * oluşur ve buraya gelmez; aynı JSON biçimini {@code infrastructure.security} paketindeki
 * {@code RestAuthenticationEntryPoint} ve {@code RestAccessDeniedHandler} üretir;
 * {@code @PreAuthorize}'dan gelen Spring Security hataları da bilerek yakalanmaz,
 * Security'nin kendi akışına bırakılır.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    static final String VALIDATION_FAILED = "VALIDATION_FAILED";
    static final String INTERNAL_ERROR = "INTERNAL_ERROR";
    static final String VALIDATION_MESSAGE = "İstek geçersiz.";
    static final String INTERNAL_MESSAGE = "Beklenmeyen bir hata oluştu.";

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String SECURITY_PACKAGE = "org.springframework.security.";

    private final Clock clock;

    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, VALIDATION_FAILED, VALIDATION_MESSAGE,
                request.getRequestURI(), ex.getErrors());
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex, HttpServletRequest request) {
        ErrorCode code = ex.getErrorCode();
        log.debug("İş kuralı hatası {} {}: {}", request.getMethod(), request.getRequestURI(), code);
        return build(statusOf(code.category()), code.name(), ex.getMessage(),
                request.getRequestURI(), List.of());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleConcurrentUpdate(
            OptimisticLockingFailureException ex, HttpServletRequest request) {
        log.info("Eşzamanlı güncelleme çatışması {} {}", request.getMethod(), request.getRequestURI());
        ErrorCode code = ErrorCode.CONCURRENT_UPDATE;
        return build(statusOf(code.category()), code.name(), code.defaultMessage(),
                request.getRequestURI(), List.of());
    }

    /**
     * Beklenmeyen hata: ayrıntı loga yazılır, istemciye sadece genel mesaj döner.
     *
     * @throws Exception Spring Security hatası ise aynen fırlatılır (403/401'i Security üretir)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request)
            throws Exception {
        if (ex.getClass().getName().startsWith(SECURITY_PACKAGE)) {
            throw ex;
        }
        log.error("Beklenmeyen hata {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_ERROR, INTERNAL_MESSAGE,
                request.getRequestURI(), List.of());
    }

    /** Controller'da {@code @Valid} kullanılırsa alan hataları da aynı biçimde döner. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status,
            WebRequest request) {
        List<ValidationError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ValidationError(error.getField(),
                        error.getDefaultMessage() == null ? "Geçersiz değer." : error.getDefaultMessage()))
                .toList();
        ErrorResponse body = response(HttpStatus.BAD_REQUEST, VALIDATION_FAILED, VALIDATION_MESSAGE,
                pathOf(request), errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).headers(headers).body(body);
    }

    /** Spring MVC'nin kendi hataları (bozuk JSON, desteklenmeyen metot...) için ortak çıkış. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode,
            WebRequest request) {
        if (statusCode.is5xxServerError()) {
            log.error("İstek işlenemedi {}", pathOf(request), ex);
        }
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        String code = (status != null) ? status.name() : "HTTP_" + statusCode.value();
        ErrorResponse error = response(statusCode, code, mvcMessage(statusCode), pathOf(request), List.of());
        return ResponseEntity.status(statusCode).headers(headers).body(error);
    }

    static HttpStatusCode statusOf(ErrorCategory category) {
        return switch (category) {
            case UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case CONFLICT -> HttpStatus.CONFLICT;
            case BUSINESS_RULE -> HttpStatusCode.valueOf(422);
        };
    }

    private static String mvcMessage(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> "İstek okunamadı; gövde veya parametreler hatalı.";
            case 404 -> "İstenen adres bulunamadı.";
            case 405 -> "Bu adres bu HTTP metodunu desteklemiyor.";
            case 406 -> "İstenen cevap biçimi desteklenmiyor.";
            case 415 -> "Gönderilen içerik tipi desteklenmiyor; application/json kullanın.";
            default -> status.is5xxServerError() ? INTERNAL_MESSAGE : "İstek işlenemedi.";
        };
    }

    private ResponseEntity<ErrorResponse> build(HttpStatusCode status, String code, String message,
                                                String path, List<ValidationError> fieldErrors) {
        return ResponseEntity.status(status).body(response(status, code, message, path, fieldErrors));
    }

    private ErrorResponse response(HttpStatusCode status, String code, String message, String path,
                                   List<ValidationError> fieldErrors) {
        return new ErrorResponse(OffsetDateTime.now(clock), status.value(), code, message, path, fieldErrors);
    }

    private static String pathOf(WebRequest request) {
        return (request instanceof ServletWebRequest servletRequest)
                ? servletRequest.getRequest().getRequestURI()
                : null;
    }
}
