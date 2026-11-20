package com.dogus.vacation.application.usecase.leavetype;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import com.dogus.vacation.AbstractIntegrationTest;
import com.dogus.vacation.application.common.view.LeaveTypeView;
import com.dogus.vacation.application.mediator.Mediator;
import com.dogus.vacation.application.mediator.ValidationError;
import com.dogus.vacation.application.mediator.ValidationException;
import com.dogus.vacation.application.usecase.leavetype.create.CreateLeaveTypeRequest;
import com.dogus.vacation.application.usecase.leavetype.list.ListLeaveTypesRequest;
import com.dogus.vacation.application.usecase.leavetype.update.UpdateLeaveTypeRequest;
import com.dogus.vacation.domain.exception.BusinessException;
import com.dogus.vacation.domain.exception.ErrorCode;
import com.dogus.vacation.domain.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * İzin türü use case'lerini mediator üzerinden, gerçek PostgreSQL ile çalıştırır: istek
 * anotasyonları, Validator, Handler ve veritabanı birlikte test edilir. Her testten önce
 * veritabanı 4 hazır türle (FR-13) sıfırlanır.
 */
class LeaveTypeUseCasesIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private Mediator mediator;

    // ---- ListLeaveTypes ----

    @Test
    void listsTheFourSeededTypesInIdOrder() {
        var leaveTypes = mediator.send(new ListLeaveTypesRequest()).leaveTypes();

        assertThat(leaveTypes).extracting(LeaveTypeView::code)
                .containsExactly("ANNUAL", "EXCUSE", "SICK", "UNPAID");
        assertThat(leaveTypes.get(1).annualLimitDays()).isEqualByComparingTo("5");
        assertThat(leaveTypes).allMatch(LeaveTypeView::active);
    }

    // ---- CreateLeaveType ----

    @Test
    void createsAnActiveLeaveType() {
        LeaveTypeView created = mediator.send(
                new CreateLeaveTypeRequest("MARRIAGE_LEAVE", "  Evlilik izni ", true, new BigDecimal("3"))).leaveType();

        assertThat(created.id()).isNotNull();
        assertThat(created.code()).isEqualTo("MARRIAGE_LEAVE");
        assertThat(created.name()).isEqualTo("Evlilik izni");
        assertThat(created.deductsFromBalance()).isTrue();
        assertThat(created.annualLimitDays()).isEqualByComparingTo("3");
        assertThat(created.active()).isTrue();
        assertThat(countRows("leave_types")).isEqualTo(5);
    }

    @Test
    void rejectsADuplicateCode() {
        assertThatThrownBy(() -> mediator.send(new CreateLeaveTypeRequest("ANNUAL", "Başka bir ad", true, null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_VALUE);
        assertThat(countRows("leave_types")).isEqualTo(4);
    }

    @Test
    void rejectsADuplicateName() {
        assertThatThrownBy(() -> mediator.send(new CreateLeaveTypeRequest("OTHER", "Yıllık izin", true, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Yıllık izin");
    }

    @Test
    void rejectsAnInvalidCodeBeforeReachingTheHandler() {
        assertThatThrownBy(() -> mediator.send(new CreateLeaveTypeRequest("evlilik", "Evlilik izni", true, null)))
                .isInstanceOf(ValidationException.class)
                .extracting(e -> ((ValidationException) e).getErrors().stream().map(ValidationError::field).toList())
                .isEqualTo(List.of("code"));
    }

    @Test
    void rejectsALimitForATypeThatDoesNotUseTheBalance() {
        assertThatThrownBy(() -> mediator.send(
                new CreateLeaveTypeRequest("BLOOD_DONATION", "Kan bağışı", false, BigDecimal.ONE)))
                .isInstanceOf(ValidationException.class);
        assertThat(countRows("leave_types")).isEqualTo(4);
    }

    // ---- UpdateLeaveType ----

    @Test
    void updatesNameLimitAndActiveButKeepsCodeAndBalanceBehaviour() {
        Long excuseId = leaveType("EXCUSE").getId();

        LeaveTypeView updated = mediator.send(
                new UpdateLeaveTypeRequest(excuseId, "Mazeret izni (günlük)", new BigDecimal("6"), false)).leaveType();

        assertThat(updated.code()).isEqualTo("EXCUSE");
        assertThat(updated.deductsFromBalance()).isTrue();
        assertThat(jdbcTemplate.queryForMap(
                "select name, annual_limit_days, active from leave_mgmt.leave_types where id = ?", excuseId))
                .containsEntry("name", "Mazeret izni (günlük)")
                .containsEntry("active", false)
                .hasEntrySatisfying("annual_limit_days",
                        value -> assertThat((BigDecimal) value).isEqualByComparingTo("6"));
    }

    @Test
    void updateCanKeepTheSameName() {
        Long annualId = leaveType("ANNUAL").getId();

        LeaveTypeView updated = mediator.send(new UpdateLeaveTypeRequest(annualId, "Yıllık izin", null, true)).leaveType();

        assertThat(updated.name()).isEqualTo("Yıllık izin");
    }

    @Test
    void updateRejectsAnUnknownLeaveType() {
        assertThatThrownBy(() -> mediator.send(new UpdateLeaveTypeRequest(999L, "Yok", null, true)))
                .isInstanceOf(NotFoundException.class)
                .extracting(e -> ((NotFoundException) e).getErrorCode())
                .isEqualTo(ErrorCode.LEAVE_TYPE_NOT_FOUND);
    }

    @Test
    void updateRejectsANameUsedByAnotherType() {
        Long sickId = leaveType("SICK").getId();

        assertThatThrownBy(() -> mediator.send(new UpdateLeaveTypeRequest(sickId, "Yıllık izin", null, true)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_VALUE);
    }

    @Test
    void updateRejectsALimitForATypeThatDoesNotUseTheBalance() {
        Long sickId = leaveType("SICK").getId();

        assertThatThrownBy(() -> mediator.send(new UpdateLeaveTypeRequest(sickId, "Hastalık izni", BigDecimal.TEN, true)))
                .isInstanceOf(ValidationException.class)
                .extracting(e -> ((ValidationException) e).getErrors())
                .isEqualTo(List.of(new ValidationError("annualLimitDays",
                        "Bakiyeden düşmeyen türde yıllık sınır boş olmalı.")));
    }
}
