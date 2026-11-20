package com.dogus.vacation.domain.exception;

/**
 * Aranan kayıt yoksa fırlatılır (REST: 404).
 *
 * <pre>{@code
 * LeaveRequest leave = repository.findById(id)
 *         .orElseThrow(() -> new NotFoundException(ErrorCode.LEAVE_REQUEST_NOT_FOUND));
 * }</pre>
 */
public class NotFoundException extends BusinessException {

    private static final long serialVersionUID = 1L;

    /**
     * @param errorCode türü {@link ErrorCategory#NOT_FOUND} olan bir kod
     * @throws IllegalArgumentException kodun türü NOT_FOUND değilse
     */
    public NotFoundException(ErrorCode errorCode) {
        super(requireCategory(errorCode, ErrorCategory.NOT_FOUND), null);
    }
}
