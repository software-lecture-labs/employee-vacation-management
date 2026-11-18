package com.dogus.vacation.infrastructure.persistence;

import com.dogus.vacation.domain.model.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * {@code leave_requests} tablosu için Spring Data repository'si.
 *
 * <p>Temel kaydetme, bulma ve listeleme metotları {@link JpaRepository}'den gelir;
 * özel sorgular onları kullanan use case ile birlikte eklenir.
 */
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
}
