package com.dogus.vacation.application.common.mapper;

import com.dogus.vacation.application.common.view.EmployeeRef;
import com.dogus.vacation.application.common.view.EmployeeView;
import com.dogus.vacation.domain.model.Department;
import com.dogus.vacation.domain.model.Employee;

/**
 * {@link Employee} entity'sini {@link EmployeeView}'a çevirir.
 *
 * <p>Departman ve yönetici tembel yüklendiği için Handler içinde, transaction açıkken
 * çağrılır.
 */
public final class EmployeeMapper {

    private EmployeeMapper() {
    }

    public static EmployeeView toView(Employee employee) {
        Department department = employee.getDepartment();
        return new EmployeeView(
                employee.getId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getEmail(),
                employee.getRole(),
                employee.isActive(),
                employee.getHireDate(),
                employee.getBirthDate(),
                department.getId(),
                department.getName(),
                EmployeeRef.of(employee.getManager()));
    }
}
