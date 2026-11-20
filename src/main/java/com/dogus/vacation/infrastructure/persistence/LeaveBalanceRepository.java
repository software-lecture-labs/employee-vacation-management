package com.dogus.vacation.infrastructure.persistence;

import java.util.Optional;

import com.dogus.vacation.domain.model.Employee;
import com.dogus.vacation.domain.model.LeaveBalance;
import com.dogus.vacation.domain.model.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * {@code leave_balances} tablosu için Spring Data repository'si.
 *
 * <p>Temel kaydetme, bulma ve listeleme metotları {@link JpaRepository}'den gelir;
 * özel sorgular onları kullanan use case ile birlikte eklenir.
 */
public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {

    /**
     * Bir çalışanın bir izin türündeki yıllık bakiyesi. Üçlü tekil kısıt
     * ({@code uq_leave_balances_employee_type_year}) en fazla bir satır olmasını sağlar.
     */
    Optional<LeaveBalance> findByEmployeeAndLeaveTypeAndYear(Employee employee, LeaveType leaveType, Integer year);

    /** Aynı yıl için ikinci bakiyenin açılmasını tekil kısıta takılmadan önce yakalamak için. */
    boolean existsByEmployeeAndLeaveTypeAndYear(Employee employee, LeaveType leaveType, Integer year);
}
