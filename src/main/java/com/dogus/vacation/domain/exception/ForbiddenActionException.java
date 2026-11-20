package com.dogus.vacation.domain.exception;

/**
 * İşlemi yapan kişinin bu kayıt üzerinde yetkisi yoksa fırlatılır (REST: 403).
 *
 * <p>Rol kontrolü endpoint'te yapılır; bu hata "kayıt ona ait mi, onun ekibinden mi,
 * kendi talebi mi" gibi Handler'daki sahiplik kontrolleri içindir (FR-24).
 */
public class ForbiddenActionException extends BusinessException {

    private static final long serialVersionUID = 1L;

    /** Genel "yetkiniz yok" hatası ({@link ErrorCode#ACTION_NOT_ALLOWED}). */
    public ForbiddenActionException() {
        this(ErrorCode.ACTION_NOT_ALLOWED);
    }

    /**
     * @param errorCode türü {@link ErrorCategory#FORBIDDEN} olan bir kod
     * @throws IllegalArgumentException kodun türü FORBIDDEN değilse
     */
    public ForbiddenActionException(ErrorCode errorCode) {
        super(requireCategory(errorCode, ErrorCategory.FORBIDDEN), null);
    }
}
