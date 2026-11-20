package com.dogus.vacation.application.common.mapper;

import com.dogus.vacation.application.common.view.DepartmentView;
import com.dogus.vacation.domain.model.Department;

/**
 * {@link Department} entity'sini dışarı verilen {@link DepartmentView}'a çevirir.
 * Entity Controller'dan dışarı çıkmaz; cevaplar yalnızca view taşır.
 */
public final class DepartmentMapper {

    private DepartmentMapper() {
    }

    public static DepartmentView toView(Department department) {
        return new DepartmentView(department.getId(), department.getName());
    }
}
