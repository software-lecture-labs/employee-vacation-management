package com.dogus.vacation.infrastructure.persistence;

import com.dogus.vacation.domain.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * {@code departments} tablosu için Spring Data repository'si.
 *
 * <p>Temel kaydetme, bulma ve listeleme metotları {@link JpaRepository}'den gelir;
 * özel sorgular onları kullanan use case ile birlikte eklenir.
 */
public interface DepartmentRepository extends JpaRepository<Department, Long> {
}
