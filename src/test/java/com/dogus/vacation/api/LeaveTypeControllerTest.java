package com.dogus.vacation.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import com.dogus.vacation.api.advice.GlobalExceptionHandler;
import com.dogus.vacation.application.common.view.LeaveTypeView;
import com.dogus.vacation.application.mediator.Mediator;
import com.dogus.vacation.application.mediator.ValidationError;
import com.dogus.vacation.application.mediator.ValidationException;
import com.dogus.vacation.application.usecase.leavetype.create.CreateLeaveTypeRequest;
import com.dogus.vacation.application.usecase.leavetype.create.CreateLeaveTypeResponse;
import com.dogus.vacation.application.usecase.leavetype.list.ListLeaveTypesRequest;
import com.dogus.vacation.application.usecase.leavetype.list.ListLeaveTypesResponse;
import com.dogus.vacation.application.usecase.leavetype.update.UpdateLeaveTypeRequest;
import com.dogus.vacation.application.usecase.leavetype.update.UpdateLeaveTypeResponse;
import com.dogus.vacation.domain.exception.BusinessException;
import com.dogus.vacation.domain.exception.ErrorCode;
import com.dogus.vacation.domain.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * {@link LeaveTypeController}'ın yalnızca HTTP işini yaptığını doğrular: adresler, durum
 * kodları, {@code Location} başlığı ve JSON biçimi. Mediator sahtedir (Mockito); kurallar
 * {@code LeaveTypeUseCasesIntegrationTest}'te test edilir. Spring context açılmaz.
 */
class LeaveTypeControllerTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-11-20T10:00:00Z"), ZoneId.of("Europe/Istanbul"));

    private static final LeaveTypeView MARRIAGE =
            new LeaveTypeView(5L, "MARRIAGE_LEAVE", "Evlilik izni", true, new BigDecimal("3"), true);

    private Mediator mediator;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mediator = mock(Mediator.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new LeaveTypeController(mediator))
                .setControllerAdvice(new GlobalExceptionHandler(CLOCK))
                .build();
    }

    @Test
    void listReturns200WithAllLeaveTypes() throws Exception {
        when(mediator.send(any(ListLeaveTypesRequest.class)))
                .thenReturn(new ListLeaveTypesResponse(List.of(MARRIAGE)));

        mockMvc.perform(get("/api/leave-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leaveTypes[0].code").value("MARRIAGE_LEAVE"))
                .andExpect(jsonPath("$.leaveTypes[0].deductsFromBalance").value(true))
                .andExpect(jsonPath("$.leaveTypes[0].active").value(true));
    }

    @Test
    void createReturns201WithTheLocationOfTheNewType() throws Exception {
        when(mediator.send(any(CreateLeaveTypeRequest.class))).thenReturn(new CreateLeaveTypeResponse(MARRIAGE));

        mockMvc.perform(post("/api/leave-types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"MARRIAGE_LEAVE","name":"Evlilik izni",
                                 "deductsFromBalance":true,"annualLimitDays":3}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/leave-types/5"))
                .andExpect(jsonPath("$.leaveType.id").value(5))
                .andExpect(jsonPath("$.leaveType.name").value("Evlilik izni"));
        verify(mediator).send(new CreateLeaveTypeRequest("MARRIAGE_LEAVE", "Evlilik izni", true, new BigDecimal("3")));
    }

    @Test
    void createReturns400WithFieldErrorsWhenTheRequestIsInvalid() throws Exception {
        when(mediator.send(any(CreateLeaveTypeRequest.class))).thenThrow(
                new ValidationException(List.of(new ValidationError("code", "Kod zorunlu."))));

        mockMvc.perform(post("/api/leave-types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Evlilik izni\",\"deductsFromBalance\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("code"));
    }

    @Test
    void createReturns409WhenTheCodeIsTaken() throws Exception {
        when(mediator.send(any(CreateLeaveTypeRequest.class))).thenThrow(
                new BusinessException(ErrorCode.DUPLICATE_VALUE, "'ANNUAL' kodlu bir izin türü zaten var."));

        mockMvc.perform(post("/api/leave-types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"ANNUAL\",\"name\":\"X\",\"deductsFromBalance\":true}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_VALUE"))
                .andExpect(jsonPath("$.message").value("'ANNUAL' kodlu bir izin türü zaten var."));
    }

    @Test
    void updateUsesTheIdFromThePath() throws Exception {
        when(mediator.send(any(UpdateLeaveTypeRequest.class))).thenReturn(new UpdateLeaveTypeResponse(MARRIAGE));

        mockMvc.perform(put("/api/leave-types/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":99,\"name\":\"Evlilik izni\",\"annualLimitDays\":3,\"active\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leaveType.id").value(5));
        verify(mediator).send(new UpdateLeaveTypeRequest(5L, "Evlilik izni", new BigDecimal("3"), true));
    }

    @Test
    void updateReturns404ForAnUnknownLeaveType() throws Exception {
        when(mediator.send(any(UpdateLeaveTypeRequest.class)))
                .thenThrow(new NotFoundException(ErrorCode.LEAVE_TYPE_NOT_FOUND));

        mockMvc.perform(put("/api/leave-types/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Yok\",\"active\":true}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LEAVE_TYPE_NOT_FOUND"));
    }
}
