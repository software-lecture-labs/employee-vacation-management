package com.dogus.vacation.application.usecase.employee;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.dogus.vacation.application.mediator.ValidationError;

/**
 * Çalışanın tarih kuralları; ekleme ve güncelleme Validator'ları birlikte kullanır.
 * <ul>
 *   <li>İşe giriş tarihi gelecekte olamaz.</li>
 *   <li>Doğum tarihi en az 15 yıl önce olmalı (çalışma yaşı).</li>
 *   <li>İşe giriş doğumdan sonra olmalı.</li>
 * </ul>
 */
public final class EmployeeDateRules {

    /** En küçük çalışma yaşı. */
    public static final int MINIMUM_AGE = 15;

    private EmployeeDateRules() {
    }

    /** Hatalar alan adı sırasıyla döner (önce {@code birthDate}, sonra {@code hireDate}). */
    public static List<ValidationError> check(LocalDate hireDate, LocalDate birthDate, LocalDate today) {
        List<ValidationError> errors = new ArrayList<>();
        boolean oldEnough = !birthDate.isAfter(today.minusYears(MINIMUM_AGE));
        if (!oldEnough) {
            errors.add(new ValidationError("birthDate", "Çalışan en az " + MINIMUM_AGE + " yaşında olmalı."));
        }
        if (hireDate.isAfter(today)) {
            errors.add(new ValidationError("hireDate", "İşe giriş tarihi gelecekte olamaz."));
        } else if (oldEnough && !hireDate.isAfter(birthDate)) {
            errors.add(new ValidationError("hireDate", "İşe giriş tarihi doğum tarihinden sonra olmalı."));
        }
        return errors;
    }
}
