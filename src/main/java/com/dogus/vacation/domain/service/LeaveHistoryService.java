package com.dogus.vacation.domain.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Objects;

import com.dogus.vacation.domain.model.Employee;
import com.dogus.vacation.domain.model.LeaveRequest;
import com.dogus.vacation.domain.model.LeaveRequestHistory;
import com.dogus.vacation.domain.model.enums.LeaveRequestStatus;
import com.dogus.vacation.infrastructure.persistence.LeaveRequestHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * İzin talebinin her durum değişikliğini {@code leave_request_history} tablosuna yazan
 * alan servisi (FR-27, BR-16).
 *
 * <p><b>Kullanım:</b> Handler, geçiş metodunu çağırmadan önce eski durumu alır; geçişten
 * sonra bu servisi çağırır. Yeni durum parametre olarak verilmez, talebin o anki
 * durumundan okunur; böylece çağıranın yanlış "yeni durum" yazması imkânsızdır.
 *
 * <pre>{@code
 * LeaveRequestStatus before = request.getStatus();
 * request.approve(approver, today, now);
 * historyService.record(request, before, approver, null);
 * }</pre>
 *
 * <p><b>Transaction:</b> {@link LeaveBalanceService} gibi kendi transaction'ını açmaz
 * ({@link Propagation#MANDATORY}); Handler'ın transaction'ına katılır. Kayıt
 * {@code saveAndFlush} ile yazılır, yani INSERT o anda veritabanına gider. Geçmiş
 * yazılamazsa hata Handler'ın transaction'ını geri aldırır ve durum değişikliği de
 * kaybolur (BR-16); rollback senaryosu B bunu gösterir.
 */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class LeaveHistoryService {

    /** {@code leave_request_history.note} sütununun uzunluğu. */
    static final int MAX_NOTE_LENGTH = 500;

    private final LeaveRequestHistoryRepository historyRepository;
    private final Clock clock;

    public LeaveHistoryService(LeaveRequestHistoryRepository historyRepository, Clock clock) {
        this.historyRepository = historyRepository;
        this.clock = clock;
    }

    /**
     * Talebin şu anki durumuna geçişi geçmişe yazar.
     *
     * @param request    durumu yeni değişmiş talep
     * @param fromStatus geçişten önceki durum; {@code null} ise talep yeni açılmıştır
     *                   (o zaman şu anki durum PENDING olmalıdır)
     * @param actor      işlemi yapan kişi; {@code null} ise sistem (örneğin gece görevi, T5)
     * @param note       gerekçe (red, iptal) ya da açıklama; boşsa kaydedilmez
     * @return kaydedilen geçmiş satırı
     * @throws IllegalArgumentException durum değişmemişse, yeni talep PENDING değilse
     *                                  ya da not 500 karakterden uzunsa
     */
    public LeaveRequestHistory record(LeaveRequest request, LeaveRequestStatus fromStatus,
                                      Employee actor, String note) {
        Objects.requireNonNull(request, "request");
        LeaveRequestStatus toStatus = Objects.requireNonNull(request.getStatus(), "request.status");

        if (fromStatus == null && toStatus != LeaveRequestStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Önceki durum boşsa talep yeni açılmış olmalı (PENDING), şu anki durum: " + toStatus);
        }
        if (fromStatus == toStatus) {
            throw new IllegalArgumentException(
                    "Durum değişmemiş (" + toStatus + "); geçmiş, geçiş metodundan sonra yazılmalı.");
        }

        LeaveRequestHistory entry = new LeaveRequestHistory(request, fromStatus, toStatus, actor,
                normalize(note), OffsetDateTime.now(clock));
        return historyRepository.saveAndFlush(entry);
    }

    /** Baştaki ve sondaki boşlukları atar; boş not {@code null} olarak saklanır. */
    private static String normalize(String note) {
        if (note == null || note.isBlank()) {
            return null;
        }
        String stripped = note.strip();
        if (stripped.length() > MAX_NOTE_LENGTH) {
            throw new IllegalArgumentException("Not en fazla " + MAX_NOTE_LENGTH + " karakter olabilir.");
        }
        return stripped;
    }
}
