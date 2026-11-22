package com.dogus.vacation.api;

import com.dogus.vacation.application.mediator.Mediator;
import com.dogus.vacation.application.usecase.auth.login.LoginRequest;
import com.dogus.vacation.application.usecase.auth.login.LoginResponse;
import com.dogus.vacation.application.usecase.auth.profile.GetMyProfileRequest;
import com.dogus.vacation.application.usecase.auth.profile.GetMyProfileResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Giriş ve kendi profili (FR-01, FR-11).
 *
 * <p>Controller iş kuralı içermez: isteği {@link Mediator}'a verir. Giriş adresi
 * token istemez ({@code SecurityConfig}); diğer bütün adresler ister.
 */
@RestController
public class AuthController {

    private final Mediator mediator;

    public AuthController(Mediator mediator) {
        this.mediator = mediator;
    }

    /**
     * E-posta ve şifreyle giriş. Başarılıysa 200 ve token; hatalı e-posta/şifre veya pasif
     * çalışan 401, eksik alan 400.
     */
    @PostMapping("/api/auth/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return mediator.send(request);
    }

    /** Giriş yapan çalışanın kendi profili (giriş yapmış herkes). Token yoksa 401. */
    @GetMapping("/api/me")
    public GetMyProfileResponse me() {
        return mediator.send(new GetMyProfileRequest());
    }
}
