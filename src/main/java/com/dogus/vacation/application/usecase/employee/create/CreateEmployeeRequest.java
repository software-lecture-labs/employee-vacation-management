package com.dogus.vacation.application.usecase.employee.create;

import java.time.LocalDate;

import com.dogus.vacation.application.mediator.Request;
import com.dogus.vacation.domain.model.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Yeni çalışan ekleme isteği (FR-06; yalnızca İK). Çalışan, ilk bakiyeleriyle birlikte
 * tek transaction'da oluşturulur (T4).
 *
 * @param firstName    ad, en fazla 50 karakter
 * @param lastName     soyad, en fazla 50 karakter
 * @param email        giriş adı; şirkette tekil (büyük/küçük harf fark etmez)
 * @param password     ilk şifre, 8–72 karakter; BCrypt ile saklanır, cevapta dönmez
 * @param hireDate     işe giriş tarihi; gelecekte olamaz
 * @param birthDate    doğum tarihi; en az 15 yıl önce ve işe girişten önce
 * @param role         EMPLOYEE, MANAGER veya HR
 * @param departmentId departman
 * @param managerId    yöneticisi; boşsa talepleri İK onaylar (BR-07)
 */
public record CreateEmployeeRequest(
        @NotBlank(message = "Ad zorunlu.")
        @Size(max = 50, message = "Ad en fazla 50 karakter olabilir.")
        String firstName,

        @NotBlank(message = "Soyad zorunlu.")
        @Size(max = 50, message = "Soyad en fazla 50 karakter olabilir.")
        String lastName,

        @NotBlank(message = "E-posta zorunlu.")
        @Email(message = "Geçerli bir e-posta adresi girilmeli.")
        @Size(max = 120, message = "E-posta en fazla 120 karakter olabilir.")
        String email,

        @NotBlank(message = "İlk şifre zorunlu.")
        @Size(min = 8, max = 72, message = "Şifre 8–72 karakter olmalı.")
        String password,

        @NotNull(message = "İşe giriş tarihi zorunlu.")
        LocalDate hireDate,

        @NotNull(message = "Doğum tarihi zorunlu.")
        LocalDate birthDate,

        @NotNull(message = "Rol zorunlu.")
        Role role,

        @NotNull(message = "Departman zorunlu.")
        @Positive(message = "Geçerli bir departman seçilmeli.")
        Long departmentId,

        @Positive(message = "Geçerli bir yönetici seçilmeli.")
        Long managerId)
        implements Request<CreateEmployeeResponse> {

    /** Şifre loglara ve hata mesajlarına düşmesin diye {@code toString}'de gizlenir. */
    @Override
    public String toString() {
        return "CreateEmployeeRequest[email=" + email + ", role=" + role + ", departmentId=" + departmentId + "]";
    }
}
