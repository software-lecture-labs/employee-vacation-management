package com.dogus.vacation.infrastructure.persistence;

import com.dogus.vacation.domain.model.AnnualAccrualRun;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * {@code annual_accrual_runs} tablosu için Spring Data repository'si.
 *
 * <p>Temel kaydetme, bulma ve listeleme metotları {@link JpaRepository}'den gelir;
 * özel sorgular onları kullanan use case ile birlikte eklenir.
 */
public interface AnnualAccrualRunRepository extends JpaRepository<AnnualAccrualRun, Long> {
}
