package com.dogus.vacation.infrastructure.persistence;

import java.util.Optional;

import com.dogus.vacation.domain.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * {@code employees} tablosu için Spring Data repository'si.
 *
 * <p>Temel kaydetme, bulma ve listeleme metotları {@link JpaRepository}'den gelir;
 * özel sorgular onları kullanan use case ile birlikte eklenir.
 */
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    /**
     * Girişte kullanılır. E-posta büyük/küçük harfe duyarsız tekildir
     * ({@code uq_employees_email_lower}), bu yüzden en fazla bir kayıt döner.
     */
    Optional<Employee> findByEmailIgnoreCase(String email);
}
