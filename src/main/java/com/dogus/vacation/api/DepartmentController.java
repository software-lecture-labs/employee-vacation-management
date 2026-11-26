package com.dogus.vacation.api;

import com.dogus.vacation.application.mediator.Mediator;
import com.dogus.vacation.application.usecase.department.list.ListDepartmentsRequest;
import com.dogus.vacation.application.usecase.department.list.ListDepartmentsResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Departman endpoint'leri (FR-10).
 *
 * <p>Controller iş kuralı içermez: isteği Request record'una çevirir ve {@link Mediator}'a
 * verir. Doğrulama mediator'da, kurallar Handler'dadır.
 *
 * <p><b>Yetki:</b> Listeleme giriş yapmış herkese açık olacak. Güvenlik altyapısı (JWT,
 * roller) henüz projede olmadığı için kimlik kontrolü güvenlik PR'ı birleşince gelir.
 */
@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    private final Mediator mediator;

    public DepartmentController(Mediator mediator) {
        this.mediator = mediator;
    }

    /**
     * Ada göre sıralı, sayfalı departman listesi.
     * Örnek: {@code /api/departments?page=0&size=20}
     */
    @GetMapping
    public ListDepartmentsResponse list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return mediator.send(new ListDepartmentsRequest(page, size));
    }
}
