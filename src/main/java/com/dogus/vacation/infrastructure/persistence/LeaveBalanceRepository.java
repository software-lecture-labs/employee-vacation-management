package com.dogus.vacation.infrastructure.persistence;

import com.dogus.vacation.domain.model.LeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * {@code leave_balances} tablosu için Spring Data repository'si.
 *
 * <p>Temel kaydetme, bulma ve listeleme metotları {@link JpaRepository}'den gelir;
 * özel sorgular onları kullanan use case ile birlikte eklenir.
 */
public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {
}
