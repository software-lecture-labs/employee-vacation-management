package com.dogus.vacation.domain.model.enums;

/**
 * İzin talebinin durumu (analiz dokümanındaki 7 durum).
 *
 * <p>Veritabanında metin olarak saklanır ({@code leave_requests.status}) ve
 * {@code ck_leave_requests_status} kısıtı yalnızca bu değerlere izin verir.
 * Bir değer eklenirse yeni bir SQL scriptiyle kısıt da güncellenmelidir.
 */
public enum LeaveRequestStatus {
    /** Bekliyor. */
    PENDING,
    /** Onaylandı. */
    APPROVED,
    /** Reddedildi (son durum). */
    REJECTED,
    /** Geri çekildi (son durum). */
    WITHDRAWN,
    /** İptal talebinde. */
    CANCEL_REQUESTED,
    /** İptal edildi (son durum). */
    CANCELLED,
    /** İptal talebi sonuçsuz (son durum). */
    CANCEL_REQUEST_EXPIRED
}
