package com.dogus.vacation.domain.model.enums;

/**
 * İzin talebinin durumu (analiz dokümanındaki 7 durum).
 *
 * <p>Veritabanında metin olarak saklanır ({@code leave_requests.status}) ve
 * {@code ck_leave_requests_status} kısıtı yalnızca bu değerlere izin verir.
 * Bir değer eklenirse yeni bir SQL scriptiyle kısıt da güncellenmelidir.
 *
 * <p>Geçişleri {@code LeaveRequest}'in metotları yapar; geçersiz geçişte hata atılır.
 */
public enum LeaveRequestStatus {
    /** Bekliyor. */
    PENDING("Bekliyor", false),
    /** Onaylandı. */
    APPROVED("Onaylandı", false),
    /** Reddedildi (son durum). */
    REJECTED("Reddedildi", true),
    /** Geri çekildi (son durum). */
    WITHDRAWN("Geri çekildi", true),
    /** İptal talebinde. */
    CANCEL_REQUESTED("İptal talebinde", false),
    /** İptal edildi (son durum). */
    CANCELLED("İptal edildi", true),
    /** İptal talebi sonuçsuz (son durum). */
    CANCEL_REQUEST_EXPIRED("İptal talebi sonuçsuz", true);

    private final String label;
    private final boolean finalStatus;

    LeaveRequestStatus(String label, boolean finalStatus) {
        this.label = label;
        this.finalStatus = finalStatus;
    }

    /** @return hata mesajlarında ve cevaplarda kullanılan Türkçe ad */
    public String label() {
        return label;
    }

    /** @return bu durumdan başka bir duruma geçilemiyorsa {@code true} */
    public boolean isFinal() {
        return finalStatus;
    }
}
