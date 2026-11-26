package com.dogus.vacation.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import com.dogus.vacation.domain.model.Employee;
import com.dogus.vacation.domain.model.LeaveBalance;
import com.dogus.vacation.domain.model.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * Bir çalışanın bir yıldaki tüm bakiyeleri, izin türü sırasıyla (FR-32).
     *
     * <p>İzin türü {@code join fetch} ile aynı sorguda gelir; her satır için ayrı bir tür
     * sorgusu çalışmaz (N+1 yok).
     */
    @Query("""
            select b from LeaveBalance b
            join fetch b.leaveType t
            where b.employee.id = :employeeId and b.year = :year
            order by t.id
            """)
    List<LeaveBalance> findWithLeaveTypeByEmployeeIdAndYear(@Param("employeeId") Long employeeId,
                                                            @Param("year") Integer year);

    /**
     * Devir (BR-12): bir türün bir yıldaki bakiyeleri, yalnızca aktif çalışanlarınki, çalışan id
     * sırasıyla. Devirde önceki ve sonraki yılın bakiyeleri bu sorguyla birer kez okunur; çalışan
     * başına ayrı sorgu atılmaz.
     */
    @Query("""
            select b from LeaveBalance b
            join b.employee e
            where b.leaveType = :leaveType and b.year = :year and e.active = true
            order by e.id
            """)
    List<LeaveBalance> findForActiveEmployees(@Param("leaveType") LeaveType leaveType,
                                              @Param("year") Integer year);
}
