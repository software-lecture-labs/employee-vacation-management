package com.dogus.vacation.application.usecase.employee.update;

import java.time.LocalDate;

import com.dogus.vacation.application.mediator.Request;
import com.dogus.vacation.domain.model.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Çalışan güncelleme isteği (FR-07; yalnızca İK). Şifre burada değişmez; çalışan kendi
 * şifresini değiştirir. Aktiflik de burada değil, pasife alma uç noktasında değişir.
 *
 * <p>{@code id} yoldan gelir; Controller gövdeyi {@link #withId(Long)} ile tamamlar.
 *
 * @param id           güncellenecek çalışan
 * @param firstName    ad, en fazla 50 karakter
 * @param lastName     soyad, en fazla 50 karakter
 * @param email        giriş adı; şirkette tekil
 * @param hireDate     işe giriş tarihi; gelecekte olamaz
 * @param birthDate    doğum tarihi; en az 15 yıl önce ve işe girişten önce
 * @param role         rol; son iki aktif İK'dan birinin rolü değiştirilemez (BR-13)
 * @param departmentId departman
 * @param managerId    yöneticisi; boşsa yöneticisiz; kendisi veya kendi ekibinden biri olamaz
 */
public record UpdateEmployeeRequest(
        @NotNull(message = "Çalışan belirtilmeli.")
        @Positive(message = "Geçerli bir çalışan seçilmeli.")
        Long id,

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
        implements Request<UpdateEmployeeResponse> {

    /** Gövdeden gelen isteğe yoldaki id'yi ekler; gövdede id olsa bile yoldaki geçerlidir. */
    public UpdateEmployeeRequest withId(Long pathId) {
        return new UpdateEmployeeRequest(pathId, firstName, lastName, email, hireDate, birthDate, role,
                departmentId, managerId);
    }
}
