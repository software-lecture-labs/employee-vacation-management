package com.dogus.vacation.api;

import java.net.URI;

import com.dogus.vacation.api.docs.LeaveTypeApi;
import com.dogus.vacation.application.mediator.Mediator;
import com.dogus.vacation.application.usecase.leavetype.create.CreateLeaveTypeRequest;
import com.dogus.vacation.application.usecase.leavetype.create.CreateLeaveTypeResponse;
import com.dogus.vacation.application.usecase.leavetype.list.ListLeaveTypesRequest;
import com.dogus.vacation.application.usecase.leavetype.list.ListLeaveTypesResponse;
import com.dogus.vacation.application.usecase.leavetype.update.UpdateLeaveTypeRequest;
import com.dogus.vacation.application.usecase.leavetype.update.UpdateLeaveTypeResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * İzin türü endpoint'leri (FR-12, FR-13).
 *
 * <p>Controller iş kuralı içermez: isteği Request record'una çevirir, {@link Mediator}'a
 * verir ve HTTP kodunu seçer. Doğrulama mediator'da, kurallar Handler'dadır.
 *
 * <p><b>Yetki:</b> Listeleme giriş yapmış herkese, ekleme ve güncelleme yalnızca İK'ya açıktır
 * ({@code @PreAuthorize("hasRole('HR')")}); başka rol 403 alır.
 *
 * <p>Swagger açıklamaları ve örnekleri {@link LeaveTypeApi}'dedir.
 */
@RestController
@RequestMapping("/api/leave-types")
public class LeaveTypeController implements LeaveTypeApi {

    private final Mediator mediator;

    public LeaveTypeController(Mediator mediator) {
        this.mediator = mediator;
    }

    /** Tüm izin türleri (giriş yapmış herkes). */
    @Override
    @GetMapping
    public ListLeaveTypesResponse list() {
        return mediator.send(new ListLeaveTypesRequest());
    }

    /** Yeni izin türü (İK). 201 ve yeni kaydın adresi {@code Location} başlığında döner. */
    @Override
    @PostMapping
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<CreateLeaveTypeResponse> create(@RequestBody CreateLeaveTypeRequest request) {
        CreateLeaveTypeResponse response = mediator.send(request);
        URI location = URI.create("/api/leave-types/" + response.leaveType().id());
        return ResponseEntity.created(location).body(response);
    }

    /** İzin türünün adı, yıllık sınırı ve aktifliği (İK). Kod ve bakiye davranışı değişmez. */
    @Override
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('HR')")
    public UpdateLeaveTypeResponse update(@PathVariable Long id, @RequestBody UpdateLeaveTypeRequest request) {
        return mediator.send(request.withId(id));
    }
}
