package com.dogus.vacation.api;

import java.net.URI;

import com.dogus.vacation.api.docs.EmployeeApi;
import com.dogus.vacation.application.mediator.Mediator;
import com.dogus.vacation.application.usecase.employee.create.CreateEmployeeRequest;
import com.dogus.vacation.application.usecase.employee.create.CreateEmployeeResponse;
import com.dogus.vacation.application.usecase.employee.deactivate.DeactivateEmployeeRequest;
import com.dogus.vacation.application.usecase.employee.deactivate.DeactivateEmployeeResponse;
import com.dogus.vacation.application.usecase.employee.get.GetEmployeeRequest;
import com.dogus.vacation.application.usecase.employee.get.GetEmployeeResponse;
import com.dogus.vacation.application.usecase.employee.list.ListEmployeesRequest;
import com.dogus.vacation.application.usecase.employee.list.ListEmployeesResponse;
import com.dogus.vacation.application.usecase.employee.update.UpdateEmployeeRequest;
import com.dogus.vacation.application.usecase.employee.update.UpdateEmployeeResponse;
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
 * Çalışan yönetimi endpoint'leri (FR-06 – FR-09). Hepsi yalnızca İK'ya açıktır; başka rol 403 alır.
 *
 * <p>Controller iş kuralı içermez: isteği Request record'una çevirir ve {@link Mediator}'a verir.
 * Bir çalışanın bakiyeleri ({@code /api/employees/{id}/leave-balances}) bakiye modülündedir.
 *
 * <p>Swagger açıklamaları ve örnekleri {@link EmployeeApi}'dedir.
 */
@RestController
@RequestMapping("/api/employees")
@PreAuthorize("hasRole('HR')")
public class EmployeeController implements EmployeeApi {

    private final Mediator mediator;

    public EmployeeController(Mediator mediator) {
        this.mediator = mediator;
    }

    /**
     * Sayfalı ve filtreli çalışan listesi.
     * Örnek: {@code /api/employees?departmentId=2&active=true&page=0&size=20}
     */
    @Override
    @GetMapping
    public ListEmployeesResponse list(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return mediator.send(new ListEmployeesRequest(departmentId, active, page, size));
    }

    /**
     * Yeni çalışan ve bu yılın ilk bakiyeleri (T4). 201 ve yeni kaydın adresi {@code Location}
     * başlığında döner; şifre cevapta yer almaz.
     */
    @Override
    @PostMapping
    public ResponseEntity<CreateEmployeeResponse> create(@RequestBody CreateEmployeeRequest request) {
        CreateEmployeeResponse response = mediator.send(request);
        URI location = URI.create("/api/employees/" + response.employee().id());
        return ResponseEntity.created(location).body(response);
    }

    /** Bir çalışanın detayı; pasif çalışan da görüntülenir. */
    @Override
    @GetMapping("/{id}")
    public GetEmployeeResponse get(@PathVariable Long id) {
        return mediator.send(new GetEmployeeRequest(id));
    }

    /** Çalışanın bilgileri; şifre ve aktiflik burada değişmez. Yoldaki id geçerlidir. */
    @Override
    @PutMapping("/{id}")
    public UpdateEmployeeResponse update(@PathVariable Long id, @RequestBody UpdateEmployeeRequest request) {
        return mediator.send(request.withId(id));
    }

    /** Çalışanı pasife alır; pasif çalışan giriş yapamaz. Son iki aktif İK pasife alınamaz (BR-13). */
    @Override
    @PostMapping("/{id}/deactivate")
    public DeactivateEmployeeResponse deactivate(@PathVariable Long id) {
        return mediator.send(new DeactivateEmployeeRequest(id));
    }
}
