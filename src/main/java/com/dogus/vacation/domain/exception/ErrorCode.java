package com.dogus.vacation.domain.exception;

/**
 * Alan hatalarının sabit kodları.
 *
 * <p>Kod, hata cevabındaki {@code code} alanında aynen görünür; istemci mesaja değil
 * koda bakarak karar verir. Her kodun bir türü ({@link ErrorCategory}) ve varsayılan
 * Türkçe mesajı vardır. Yeni bir kural için buraya yeni bir sabit eklenir; HTTP
 * eşlemesi değişmez.
 */
public enum ErrorCode {

    // 401
    INVALID_CREDENTIALS(ErrorCategory.UNAUTHENTICATED, "E-posta veya şifre hatalı."),
    AUTHENTICATION_REQUIRED(ErrorCategory.UNAUTHENTICATED,
            "Bu işlem için giriş yapılmalı; token eksik, geçersiz veya süresi dolmuş."),

    // 404
    EMPLOYEE_NOT_FOUND(ErrorCategory.NOT_FOUND, "Çalışan bulunamadı."),
    DEPARTMENT_NOT_FOUND(ErrorCategory.NOT_FOUND, "Departman bulunamadı."),
    LEAVE_TYPE_NOT_FOUND(ErrorCategory.NOT_FOUND, "İzin türü bulunamadı."),
    LEAVE_REQUEST_NOT_FOUND(ErrorCategory.NOT_FOUND, "İzin talebi bulunamadı."),
    LEAVE_BALANCE_NOT_FOUND(ErrorCategory.NOT_FOUND, "İzin bakiyesi bulunamadı."),

    // 403
    ACTION_NOT_ALLOWED(ErrorCategory.FORBIDDEN, "Bu işlem için yetkiniz yok."),
    SELF_APPROVAL_NOT_ALLOWED(ErrorCategory.FORBIDDEN, "Kendi talebiniz üzerinde karar veremezsiniz."),

    // 409
    INVALID_STATUS_TRANSITION(ErrorCategory.CONFLICT, "Talep bu durumdayken bu işlem yapılamaz."),
    OVERLAPPING_LEAVE_REQUEST(ErrorCategory.CONFLICT, "Bu tarihlerle çakışan bir izin talebiniz var."),
    DUPLICATE_VALUE(ErrorCategory.CONFLICT, "Bu değer zaten kullanılıyor."),
    CONCURRENT_UPDATE(ErrorCategory.CONFLICT, "Kayıt aynı anda başka bir işlemle değişti; lütfen tekrar deneyin."),
    ACCRUAL_ALREADY_RUN(ErrorCategory.CONFLICT, "Bu yılın hak edişi zaten çalıştırıldı."),
    ACCRUAL_NOT_RUN(ErrorCategory.CONFLICT, "Önce yeni yılın hak edişi çalıştırılmalı."),

    // 422
    INSUFFICIENT_BALANCE(ErrorCategory.BUSINESS_RULE, "Yetersiz izin bakiyesi."),
    EXCUSE_LEAVE_LIMIT_EXCEEDED(ErrorCategory.BUSINESS_RULE, "Mazeret izni yılda en fazla 5 gün kullanılabilir."),
    MINIMUM_ACTIVE_HR_REQUIRED(ErrorCategory.BUSINESS_RULE, "Şirkette en az 2 aktif İK çalışanı bulunmalı."),
    INACTIVE_EMPLOYEE(ErrorCategory.BUSINESS_RULE, "Pasif bir çalışan bu işlemi yapamaz.");

    private final ErrorCategory category;
    private final String defaultMessage;

    ErrorCode(ErrorCategory category, String defaultMessage) {
        this.category = category;
        this.defaultMessage = defaultMessage;
    }

    /** @return hatanın türü; HTTP kodu buna göre seçilir */
    public ErrorCategory category() {
        return category;
    }

    /** @return ayrıntı verilmediğinde kullanılan Türkçe mesaj */
    public String defaultMessage() {
        return defaultMessage;
    }
}
