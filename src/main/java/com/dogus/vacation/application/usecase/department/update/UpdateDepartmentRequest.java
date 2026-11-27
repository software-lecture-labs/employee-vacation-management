package com.dogus.vacation.application.usecase.department.update;

import com.dogus.vacation.application.mediator.Request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Departman adını güncelleme isteği (FR-10; yalnızca İK).
 *
 * <p>{@code id} yoldan gelir; Controller gövdeyi {@link #withId(Long)} ile tamamlar.
 *
 * @param id   güncellenecek departman
 * @param name yeni ad, en fazla 100 karakter
 */
public record UpdateDepartmentRequest(
        @NotNull(message = "Departman belirtilmeli.")
        @Positive(message = "Geçerli bir departman seçilmeli.")
        Long id,

        @NotBlank(message = "Departman adı zorunlu.")
        @Size(max = 100, message = "Departman adı en fazla 100 karakter olabilir.")
        String name)
        implements Request<UpdateDepartmentResponse> {

    /** Gövdeden gelen isteğe yoldaki id'yi ekler; gövdede id olsa bile yoldaki geçerlidir. */
    public UpdateDepartmentRequest withId(Long pathId) {
        return new UpdateDepartmentRequest(pathId, name);
    }
}
