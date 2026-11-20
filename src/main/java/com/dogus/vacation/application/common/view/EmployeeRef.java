package com.dogus.vacation.application.common.view;

import com.dogus.vacation.domain.model.Employee;

/**
 * Cevaplarda bir çalışanın kısa gösterimi: sadece id ve görünen ad.
 * Şifre hash'i, rol veya version dışarı çıkmaz.
 *
 * @param id       çalışan id'si
 * @param fullName "Ad Soyad"
 */
public record EmployeeRef(Long id, String fullName) {

    /** @return çalışan boşsa {@code null} (örneğin henüz karar verilmemiş talepte onaylayan) */
    public static EmployeeRef of(Employee employee) {
        if (employee == null) {
            return null;
        }
        return new EmployeeRef(employee.getId(), employee.getFirstName() + " " + employee.getLastName());
    }
}
