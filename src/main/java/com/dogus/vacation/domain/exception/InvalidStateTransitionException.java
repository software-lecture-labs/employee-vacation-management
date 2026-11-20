package com.dogus.vacation.domain.exception;

/**
 * İzin talebi mevcut durumundayken istenen işleme izin vermiyorsa fırlatılır
 * (REST: 409). Örneğin reddedilmiş bir talebi onaylamak.
 *
 * <p>Durum geçişlerini {@code LeaveRequest} entity'sinin kendisi korur ve geçersiz
 * geçişte bu hatayı atar.
 */
public class InvalidStateTransitionException extends BusinessException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message hangi durumdan hangi işlemin yapılamadığını anlatan Türkçe mesaj;
     *                boşsa varsayılan mesaj kullanılır
     */
    public InvalidStateTransitionException(String message) {
        super(ErrorCode.INVALID_STATUS_TRANSITION, message);
    }
}
