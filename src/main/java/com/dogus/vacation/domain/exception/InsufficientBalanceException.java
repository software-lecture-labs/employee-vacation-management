package com.dogus.vacation.domain.exception;

/**
 * İzin bakiyesi talep edilen günlere yetmiyorsa fırlatılır (REST: 422; FR-16, BR-05).
 *
 * <p>Onayda (T1) bakiye düşümü sırasında atıldığında transaction geri alınır; talep
 * PENDING kalır, geçmiş satırı ve bakiye hareketi oluşmaz.
 */
public class InsufficientBalanceException extends BusinessException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message talep edilen ve kalan günleri içeren Türkçe mesaj, örneğin
     *                "Yetersiz izin bakiyesi: talep 5 gün, kalan 3 gün."
     */
    public InsufficientBalanceException(String message) {
        super(ErrorCode.INSUFFICIENT_BALANCE, message);
    }
}
