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

    /** Departman eklerken ad kontrolü (FR-10); "Finans" ile "finans" aynı ad sayılır. */
    boolean existsByNameIgnoreCase(String name);

    /** Departman güncellerken ad kontrolü: kendisi dışında aynı adda departman var mı. */
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
