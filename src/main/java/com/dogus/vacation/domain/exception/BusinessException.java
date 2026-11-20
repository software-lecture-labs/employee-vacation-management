package com.dogus.vacation.domain.exception;

import java.util.Objects;

/**
 * Bir iş kuralı ihlal edildiğinde Handler'ların ve alan servislerinin fırlattığı hata.
 *
 * <p>HTTP kodu taşımaz, bir {@link ErrorCode} taşır. {@link RuntimeException}'dan
 * türediği için {@code @Transactional} metottan çıkınca transaction geri alınır;
 * bu yüzden Handler içinde yakalanıp yutulmamalıdır.
 *
 * <p>Sık kullanılan türler için alt sınıflar vardır: {@link NotFoundException} (404),
 * {@link ForbiddenActionException} (403), {@link InvalidStateTransitionException} (409),
 * {@link InsufficientBalanceException} (422). Diğer kurallar için bu sınıf doğrudan
 * kullanılır:
 *
 * <pre>{@code
 * throw new BusinessException(ErrorCode.OVERLAPPING_LEAVE_REQUEST);
 * throw new BusinessException(ErrorCode.EXCUSE_LEAVE_LIMIT_EXCEEDED,
 *         "Mazeret izni yılda en fazla 5 gün: kullanılan 4, talep 2.");
 * }</pre>
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorCode errorCode;

    /**
     * Kodun varsayılan mesajıyla hata oluşturur.
     *
     * @param errorCode hata kodu
     */
    public BusinessException(ErrorCode errorCode) {
        this(errorCode, null);
    }

    /**
     * @param errorCode hata kodu
     * @param message   kullanıcıya gösterilecek ayrıntılı Türkçe mesaj; boşsa kodun
     *                  varsayılan mesajı kullanılır
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(messageOrDefault(errorCode, message));
        this.errorCode = errorCode;
    }

    /** @return hata kodu */
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * Alt sınıfların, kendilerine uygun olmayan bir kodla oluşturulmasını engeller.
     *
     * @return verilen kod
     * @throws IllegalArgumentException kodun türü beklenen tür değilse
     */
    protected static ErrorCode requireCategory(ErrorCode errorCode, ErrorCategory expected) {
        Objects.requireNonNull(errorCode, "errorCode boş olamaz");
        if (errorCode.category() != expected) {
            throw new IllegalArgumentException(
                    errorCode + " kodunun türü " + errorCode.category() + ", beklenen " + expected);
        }
        return errorCode;
    }

    private static String messageOrDefault(ErrorCode errorCode, String message) {
        Objects.requireNonNull(errorCode, "errorCode boş olamaz");
        return (message == null || message.isBlank()) ? errorCode.defaultMessage() : message;
    }
}
