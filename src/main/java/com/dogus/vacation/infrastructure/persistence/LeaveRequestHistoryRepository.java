package com.dogus.vacation.infrastructure.persistence;

import com.dogus.vacation.domain.model.LeaveRequestHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * {@code leave_request_history} tablosu için Spring Data repository'si.
 *
 * <p>Temel kaydetme, bulma ve listeleme metotları {@link JpaRepository}'den gelir;
 * özel sorgular onları kullanan use case ile birlikte eklenir.
 */
public interface LeaveRequestHistoryRepository
        extends JpaRepository<LeaveRequestHistory, Long>, JpaSpecificationExecutor<LeaveRequestHistory> {

    /**
     * Filtreli ve sayfalı geçmiş araması (FR-26).
     *
     * <p>Her satır için talep, talep sahibi, departmanı, izin türü ve işlemi yapan kişi
     * gösterilir. Bu ilişkiler LAZY olduğu için normalde her biri ayrı sorgu olurdu
     * (20 satırlık sayfada 1 + 100 sorgu, "N+1 sorunu"). {@link EntityGraph} bunları ana
     * sorguya join ile ekler; sayfa tek SELECT ile gelir, gerekirse bir de COUNT sorgusu çalışır.
     */
    @Override
    @EntityGraph(attributePaths = {
            "leaveRequest",
            "leaveRequest.employee",
            "leaveRequest.employee.department",
            "leaveRequest.leaveType",
            "actor"})
    Page<LeaveRequestHistory> findAll(Specification<LeaveRequestHistory> spec, Pageable pageable);
}
