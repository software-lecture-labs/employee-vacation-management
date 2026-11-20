package com.dogus.vacation.infrastructure.persistence;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import com.dogus.vacation.domain.model.LeaveRequestHistory;
import com.dogus.vacation.domain.model.enums.LeaveRequestStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

/**
 * Şirket izin geçmişi araması için dinamik filtre (FR-26).
 *
 * <p>Beş filtrenin her biri boş olabilir; yalnızca dolu olanlar {@code AND} ile birleştirilir.
 * Her kombinasyon için ayrı bir repository metodu (32 tane) ya da {@code :p IS NULL OR ...}
 * biçiminde tek bir JPQL sorgusu yerine Criteria API kullanılır: her filtre ayrı ve okunur
 * kalır, PostgreSQL'in boş parametre tipi sorunları da yaşanmaz.
 */
public final class LeaveRequestHistorySpecifications {

    private LeaveRequestHistorySpecifications() {
    }

    /**
     * @param occurredFrom bu andan itibaren (dahil); boşsa alt sınır yok
     * @param occurredTo   bu andan önce (hariç); boşsa üst sınır yok
     * @param departmentId talep sahibinin departmanı
     * @param status       geçişin yeni durumu
     * @param employeeId   talep sahibi
     */
    public static Specification<LeaveRequestHistory> matching(OffsetDateTime occurredFrom,
                                                               OffsetDateTime occurredTo,
                                                               Long departmentId,
                                                               LeaveRequestStatus status,
                                                               Long employeeId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (occurredFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), occurredFrom));
            }
            if (occurredTo != null) {
                predicates.add(cb.lessThan(root.get("occurredAt"), occurredTo));
            }
            if (departmentId != null) {
                predicates.add(cb.equal(
                        root.get("leaveRequest").get("employee").get("department").get("id"), departmentId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("toStatus"), status));
            }
            if (employeeId != null) {
                predicates.add(cb.equal(root.get("leaveRequest").get("employee").get("id"), employeeId));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
