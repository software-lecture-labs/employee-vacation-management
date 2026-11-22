package com.dogus.vacation.application.common.view;

import java.time.LocalDate;

import com.dogus.vacation.domain.model.enums.Role;

/**
 * Bir çalışanın dışarı verilen hali (profil, giriş cevabı; çalışan listesi ve detayı da
 * bunu kullanabilir). Şifre hash'i ve version dışarı çıkmaz.
 *
 * @param id             çalışan id'si
 * @param firstName      ad
 * @param lastName       soyad
 * @param email          e-posta (giriş adı)
 * @param role           rol
 * @param active         aktif mi; pasif çalışan giriş yapamaz
 * @param hireDate       işe giriş tarihi
 * @param birthDate      doğum tarihi
 * @param departmentId   departman id'si
 * @param departmentName departman adı
 * @param manager        yöneticisi; yoksa {@code null}
 */
public record EmployeeView(
        Long id,
        String firstName,
        String lastName,
        String email,
        Role role,
        boolean active,
        LocalDate hireDate,
        LocalDate birthDate,
        Long departmentId,
        String departmentName,
        EmployeeRef manager) {
}
