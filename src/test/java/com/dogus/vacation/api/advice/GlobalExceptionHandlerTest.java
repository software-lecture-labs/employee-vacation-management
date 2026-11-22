package com.dogus.vacation.api.advice;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import com.dogus.vacation.application.mediator.ValidationError;
import com.dogus.vacation.application.mediator.ValidationException;
import com.dogus.vacation.domain.exception.BusinessException;
import com.dogus.vacation.domain.exception.ErrorCode;
import com.dogus.vacation.domain.exception.InsufficientBalanceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@link GlobalExceptionHandler}'ın her hatayı tek biçimli JSON'a çevirdiğini doğrular.
 * Spring context ve veritabanı açılmaz; hata fırlatan küçük bir test Controller'ı kullanılır.
 */
class GlobalExceptionHandlerTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2027-01-20T07:05:31Z"), ZoneId.of("Europe/Istanbul"));

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler(CLOCK))
                .build();
    }

    @Test
    void validationExceptionBecomes400WithFieldErrors() throws Exception {
        mockMvc.perform(get("/test/validation"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("İstek geçersiz."))
                .andExpect(jsonPath("$.path").value("/test/validation"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("reason"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("Gerekçe boş olamaz."));
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "INVALID_CREDENTIALS,       401",
            "LEAVE_REQUEST_NOT_FOUND,   404",
            "SELF_APPROVAL_NOT_ALLOWED, 403",
            "INVALID_STATUS_TRANSITION, 409",
            "INSUFFICIENT_BALANCE,      422"
    })
    void businessExceptionStatusFollowsItsCategory(ErrorCode code, int expectedStatus) throws Exception {
        mockMvc.perform(get("/test/business/" + code.name()))
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.status").value(expectedStatus))
                .andExpect(jsonPath("$.code").value(code.name()))
                .andExpect(jsonPath("$.message").value(code.defaultMessage()))
                .andExpect(jsonPath("$.fieldErrors").isEmpty());
    }

    @Test
    void detailedBusinessMessageIsReturned() throws Exception {
        mockMvc.perform(get("/test/balance"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_BALANCE"))
                .andExpect(jsonPath("$.message").value("Yetersiz izin bakiyesi: talep 5 gün, kalan 3 gün."));
    }

    @Test
    void optimisticLockConflictBecomes409() throws Exception {
        mockMvc.perform(get("/test/concurrent"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONCURRENT_UPDATE"));
    }

    @Test
    void unexpectedErrorHidesDetails() throws Exception {
        mockMvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("Beklenmeyen bir hata oluştu."))
                .andExpect(content().string(not(containsString("gizli ayrıntı"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))));
    }

    @Test
    void malformedJsonBecomes400InSameFormat() throws Exception {
        mockMvc.perform(post("/test/body").contentType(MediaType.APPLICATION_JSON).content("{bozuk"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.path").value("/test/body"));
    }

    @Test
    void wrongHttpMethodBecomes405InSameFormat() throws Exception {
        mockMvc.perform(delete("/test/validation"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"))
                .andExpect(jsonPath("$.message").value("Bu adres bu HTTP metodunu desteklemiyor."));
    }

    @Test
    void timestampComesFromClock() throws Exception {
        mockMvc.perform(get("/test/validation"))
                .andExpect(jsonPath("$.timestamp", startsWith("2027-01-20T10:05:31")));
    }

    /** Sadece hata fırlatan test Controller'ı. */
    @RestController
    @RequestMapping("/test")
    public static class ThrowingController {

        @GetMapping("/validation")
        public String validation() {
            throw new ValidationException(List.of(new ValidationError("reason", "Gerekçe boş olamaz.")));
        }

        @GetMapping("/business/{code}")
        public String business(@PathVariable("code") ErrorCode code) {
            throw new BusinessException(code);
        }

        @GetMapping("/balance")
        public String balance() {
            throw new InsufficientBalanceException("Yetersiz izin bakiyesi: talep 5 gün, kalan 3 gün.");
        }

        @GetMapping("/concurrent")
        public String concurrent() {
            throw new OptimisticLockingFailureException("version değişti");
        }

        @GetMapping("/unexpected")
        public String unexpected() {
            throw new IllegalStateException("gizli ayrıntı");
        }

        @PostMapping("/body")
        public String body(@RequestBody Map<String, Object> body) {
            return "ok";
        }
    }
}
