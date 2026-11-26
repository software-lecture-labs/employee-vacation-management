package com.dogus.vacation.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import com.dogus.vacation.api.advice.GlobalExceptionHandler;
import com.dogus.vacation.application.common.PageResponse;
import com.dogus.vacation.application.common.view.DepartmentView;
import com.dogus.vacation.application.mediator.Mediator;
import com.dogus.vacation.application.mediator.ValidationError;
import com.dogus.vacation.application.mediator.ValidationException;
import com.dogus.vacation.application.usecase.department.list.ListDepartmentsRequest;
import com.dogus.vacation.application.usecase.department.list.ListDepartmentsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * {@link DepartmentController}'ın yalnızca HTTP işini yaptığını doğrular: adres, sorgu
 * parametreleri, durum kodları ve JSON biçimi. Mediator sahtedir (Mockito); kurallar
 * {@code ListDepartmentsIntegrationTest}'te test edilir. Spring context açılmaz.
 */
class DepartmentControllerTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-11-20T10:00:00Z"), ZoneId.of("Europe/Istanbul"));

    private Mediator mediator;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mediator = mock(Mediator.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new DepartmentController(mediator))
                .setControllerAdvice(new GlobalExceptionHandler(CLOCK))
                .build();
    }

    @Test
    void listReturns200WithAPageOfDepartments() throws Exception {
        when(mediator.send(any(ListDepartmentsRequest.class))).thenReturn(new ListDepartmentsResponse(
                new PageResponse<>(List.of(new DepartmentView(1L, "Finans")), 0, 20, 1, 1)));

        mockMvc.perform(get("/api/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departments.content[0].id").value(1))
                .andExpect(jsonPath("$.departments.content[0].name").value("Finans"))
                .andExpect(jsonPath("$.departments.page").value(0))
                .andExpect(jsonPath("$.departments.size").value(20))
                .andExpect(jsonPath("$.departments.totalElements").value(1));
        verify(mediator).send(new ListDepartmentsRequest(null, null));
    }

    @Test
    void listPassesThePagingParameters() throws Exception {
        when(mediator.send(any(ListDepartmentsRequest.class)))
                .thenReturn(new ListDepartmentsResponse(new PageResponse<>(List.of(), 2, 5, 0, 0)));

        mockMvc.perform(get("/api/departments").param("page", "2").param("size", "5"))
                .andExpect(status().isOk());
        verify(mediator).send(new ListDepartmentsRequest(2, 5));
    }

    @Test
    void listReturns400WhenThePagingIsInvalid() throws Exception {
        when(mediator.send(any(ListDepartmentsRequest.class))).thenThrow(new ValidationException(
                List.of(new ValidationError("size", "Sayfa büyüklüğü en fazla 100 olabilir."))));

        mockMvc.perform(get("/api/departments").param("size", "500"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("size"));
    }
}
