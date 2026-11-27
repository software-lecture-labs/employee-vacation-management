package com.dogus.vacation.application.usecase.department;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import com.dogus.vacation.AbstractIntegrationTest;
import com.dogus.vacation.application.mediator.Mediator;
import com.dogus.vacation.application.mediator.ValidationError;
import com.dogus.vacation.application.mediator.ValidationException;
import com.dogus.vacation.application.usecase.department.create.CreateDepartmentRequest;
import com.dogus.vacation.application.usecase.department.create.CreateDepartmentResponse;
import com.dogus.vacation.application.usecase.department.update.UpdateDepartmentRequest;
import com.dogus.vacation.application.usecase.department.update.UpdateDepartmentResponse;
import com.dogus.vacation.domain.exception.BusinessException;
import com.dogus.vacation.domain.exception.ErrorCode;
import com.dogus.vacation.domain.exception.NotFoundException;
import com.dogus.vacation.domain.model.Department;
import com.dogus.vacation.infrastructure.persistence.DepartmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Departman ekleme ve güncellemeyi (FR-10) mediator üzerinden, gerçek PostgreSQL ile
 * çalıştırır: istek anotasyonları, Handler kuralları ve veritabanına yazılan değer birlikte
 * test edilir.
 */
class DepartmentUseCasesIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private Mediator mediator;

    @Autowired
    private DepartmentRepository departmentRepository;

    // ---- CreateDepartment ----

    @Test
    void createSavesTheDepartmentWithATrimmedName() {
        CreateDepartmentResponse response = mediator.send(new CreateDepartmentRequest("  Ar-Ge  "));

        assertThat(response.department().id()).isNotNull();
        assertThat(response.department().name()).isEqualTo("Ar-Ge");
        assertThat(departmentRepository.findById(response.department().id()))
                .get().extracting(Department::getName).isEqualTo("Ar-Ge");
    }

    @Test
    void createRejectsANameThatIsAlreadyUsedIgnoringCase() {
        departmentRepository.save(new Department("Finans"));

        assertThatThrownBy(() -> mediator.send(new CreateDepartmentRequest("finans")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_VALUE);
        assertThat(departmentRepository.count()).isEqualTo(1);
    }

    @Test
    void createRejectsABlankOrTooLongName() {
        assertThatThrownBy(() -> mediator.send(new CreateDepartmentRequest("  ")))
                .isInstanceOf(ValidationException.class)
                .extracting(e -> ((ValidationException) e).getErrors().stream().map(ValidationError::field).toList())
                .isEqualTo(List.of("name"));
        assertThatThrownBy(() -> mediator.send(new CreateDepartmentRequest("x".repeat(101))))
                .isInstanceOf(ValidationException.class);
    }

    // ---- UpdateDepartment ----

    @Test
    void updateRenamesTheDepartment() {
        Department department = departmentRepository.save(new Department("Pazarlama"));

        UpdateDepartmentResponse response =
                mediator.send(new UpdateDepartmentRequest(department.getId(), " Dijital Pazarlama "));

        assertThat(response.department().name()).isEqualTo("Dijital Pazarlama");
        assertThat(departmentRepository.findById(department.getId()))
                .get().extracting(Department::getName).isEqualTo("Dijital Pazarlama");
    }

    @Test
    void updateAllowsKeepingTheSameName() {
        Department department = departmentRepository.save(new Department("Satış"));

        UpdateDepartmentResponse response = mediator.send(new UpdateDepartmentRequest(department.getId(), "Satış"));

        assertThat(response.department().name()).isEqualTo("Satış");
    }

    @Test
    void updateRejectsANameUsedByAnotherDepartment() {
        departmentRepository.save(new Department("Finans"));
        Department other = departmentRepository.save(new Department("Muhasebe"));

        assertThatThrownBy(() -> mediator.send(new UpdateDepartmentRequest(other.getId(), "FINANS")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_VALUE);
        assertThat(departmentRepository.findById(other.getId()))
                .get().extracting(Department::getName).isEqualTo("Muhasebe");
    }

    @Test
    void updateOfAMissingDepartmentIsNotFound() {
        assertThatThrownBy(() -> mediator.send(new UpdateDepartmentRequest(999_999L, "Yok")))
                .isInstanceOf(NotFoundException.class)
                .extracting(e -> ((NotFoundException) e).getErrorCode())
                .isEqualTo(ErrorCode.DEPARTMENT_NOT_FOUND);
    }

    @Test
    void updateTakesTheIdFromThePath() {
        UpdateDepartmentRequest fromBody = new UpdateDepartmentRequest(1L, "Ad");

        assertThat(fromBody.withId(7L)).isEqualTo(new UpdateDepartmentRequest(7L, "Ad"));
    }
}
