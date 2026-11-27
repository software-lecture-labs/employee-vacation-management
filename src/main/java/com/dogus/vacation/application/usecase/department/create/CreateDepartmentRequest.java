package com.dogus.vacation.application.usecase.department.create;

import com.dogus.vacation.application.mediator.Request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Yeni departman ekleme isteği (FR-10; yalnızca İK).
 *
 * @param name departman adı, en fazla 100 karakter; baştaki ve sondaki boşluklar atılır
 */
public record CreateDepartmentRequest(
        @NotBlank(message = "Departman adı zorunlu.")
        @Size(max = 100, message = "Departman adı en fazla 100 karakter olabilir.")
        String name)
        implements Request<CreateDepartmentResponse> {
}
