package com.dogus.vacation.application.usecase.department.list;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import com.dogus.vacation.AbstractIntegrationTest;
import com.dogus.vacation.application.common.PageResponse;
import com.dogus.vacation.application.common.view.DepartmentView;
import com.dogus.vacation.application.mediator.Mediator;
import com.dogus.vacation.application.mediator.ValidationError;
import com.dogus.vacation.application.mediator.ValidationException;
import com.dogus.vacation.domain.model.Department;
import com.dogus.vacation.infrastructure.persistence.DepartmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * ListDepartments'ı mediator üzerinden, gerçek PostgreSQL ile çalıştırır: istek
 * anotasyonları, Handler, sıralama ve sayfalama birlikte test edilir.
 */
class ListDepartmentsIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private Mediator mediator;

    @Autowired
    private DepartmentRepository departmentRepository;

    @BeforeEach
    void saveDepartments() {
        departmentRepository.saveAll(List.of(
                new Department("Yazılım"), new Department("Finans"), new Department("Pazarlama")));
    }

    @Test
    void listsAllDepartmentsSortedByNameOnTheDefaultPage() {
        PageResponse<DepartmentView> response = mediator.send(new ListDepartmentsRequest(null, null)).departments();

        assertThat(response.content()).extracting(DepartmentView::name)
                .containsExactly("Finans", "Pazarlama", "Yazılım");
        assertThat(response.content()).allMatch(view -> view.id() != null);
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalElements()).isEqualTo(3);
        assertThat(response.totalPages()).isEqualTo(1);
    }

    @Test
    void returnsTheRequestedPage() {
        PageResponse<DepartmentView> secondPage = mediator.send(new ListDepartmentsRequest(1, 2)).departments();

        assertThat(secondPage.content()).extracting(DepartmentView::name).containsExactly("Yazılım");
        assertThat(secondPage.totalElements()).isEqualTo(3);
        assertThat(secondPage.totalPages()).isEqualTo(2);
    }

    @Test
    void rejectsInvalidPagingBeforeReachingTheHandler() {
        assertThatThrownBy(() -> mediator.send(new ListDepartmentsRequest(-1, 0)))
                .isInstanceOf(ValidationException.class)
                .extracting(e -> ((ValidationException) e).getErrors().stream().map(ValidationError::field).toList())
                .isEqualTo(List.of("page", "size"));
    }

    @Test
    void rejectsAPageSizeAboveOneHundred() {
        assertThatThrownBy(() -> mediator.send(new ListDepartmentsRequest(0, 101)))
                .isInstanceOf(ValidationException.class);
    }
}
