package com.dogus.vacation.application.usecase.employee;

import com.dogus.vacation.domain.exception.BusinessException;
import com.dogus.vacation.domain.exception.ErrorCode;
import com.dogus.vacation.domain.model.Employee;
import com.dogus.vacation.domain.model.enums.Role;
import com.dogus.vacation.infrastructure.persistence.EmployeeRepository;

/**
 * BR-13: şirkette her zaman en az 2 aktif İK bulunmalı. Bir İK pasife alınırken ya da rolü
 * değiştirilirken çağrılır; güncelleme ve pasife alma Handler'ları birlikte kullanır.
 */
public final class HrMinimumRule {

    /** En az aktif İK sayısı (BR-13). */
    public static final int MINIMUM_ACTIVE_HR = 2;

    private HrMinimumRule() {
    }

    /**
     * Çalışan aktif bir İK ise ve işlemden sonra aktif İK sayısı ikinin altına düşecekse durdurur.
     *
     * @throws BusinessException {@link ErrorCode#MINIMUM_ACTIVE_HR_REQUIRED} (422)
     */
    public static void ensureAnotherHrRemains(Employee employee, EmployeeRepository employeeRepository) {
        if (employee.isActive() && employee.getRole() == Role.HR
                && employeeRepository.countByRoleAndActiveTrue(Role.HR) <= MINIMUM_ACTIVE_HR) {
            throw new BusinessException(ErrorCode.MINIMUM_ACTIVE_HR_REQUIRED);
        }
    }
}
