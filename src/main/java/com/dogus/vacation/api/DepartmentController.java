package com.dogus.vacation.api;

import java.net.URI;

import com.dogus.vacation.api.docs.DepartmentApi;
import com.dogus.vacation.application.mediator.Mediator;
import com.dogus.vacation.application.usecase.department.create.CreateDepartmentRequest;
import com.dogus.vacation.application.usecase.department.create.CreateDepartmentResponse;
import com.dogus.vacation.application.usecase.department.list.ListDepartmentsRequest;
import com.dogus.vacation.application.usecase.department.list.ListDepartmentsResponse;
import com.dogus.vacation.application.usecase.department.update.UpdateDepartmentRequest;
import com.dogus.vacation.application.usecase.department.update.UpdateDepartmentResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Departman endpoint'leri (FR-10).
 *
 * <p>Controller iş kuralı içermez: isteği Request record'una çevirir, {@link Mediator}'a
 * verir ve HTTP kodunu seçer. Doğrulama mediator'da, kurallar Handler'dadır.
 *
 * <p><b>Yetki:</b> Listeleme giriş yapmış herkese, ekleme ve güncelleme yalnızca İK'ya açıktır
 * ({@code @PreAuthorize("hasRole('HR')")}); başka rol 403 alır.
 *
 * <p>Swagger açıklamaları ve örnekleri {@link DepartmentApi}'dedir.
 */
@RestController
@RequestMapping("/api/departments")
public class DepartmentController implements DepartmentApi {

    private final Mediator mediator;

    public DepartmentController(Mediator mediator) {
        this.mediator = mediator;
    }

    /**
     * Ada göre sıralı, sayfalı departman listesi (giriş yapmış herkes).
     * Örnek: {@code /api/departments?page=0&size=20}
     */
    @Override
    @GetMapping
    public ListDepartmentsResponse list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return mediator.send(new ListDepartmentsRequest(page, size));
    }

    /** Yeni departman (İK). 201 ve yeni kaydın adresi {@code Location} başlığında döner. */
    @Override
    @PostMapping
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<CreateDepartmentResponse> create(@RequestBody CreateDepartmentRequest request) {
        CreateDepartmentResponse response = mediator.send(request);
        URI location = URI.create("/api/departments/" + response.department().id());
        return ResponseEntity.created(location).body(response);
    }

    /** Departmanın adı (İK). Hangi departmanın güncelleneceği yoldaki id'den alınır. */
    @Override
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('HR')")
    public UpdateDepartmentResponse update(@PathVariable Long id, @RequestBody UpdateDepartmentRequest request) {
        return mediator.send(request.withId(id));
    }
}
