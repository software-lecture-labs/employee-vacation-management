package com.dogus.vacation.application.usecase.department.list;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.dogus.vacation.application.common.PageResponse;
import com.dogus.vacation.application.common.view.DepartmentView;
import com.dogus.vacation.domain.model.Department;
import com.dogus.vacation.infrastructure.persistence.DepartmentRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Handler'ın repository'ye doğru sayfa ve sıralamayı verdiğini ve sonucu view'a
 * çevirdiğini doğrular. Repository sahtedir (Mockito); Spring ve veritabanı gerekmez.
 */
class ListDepartmentsHandlerTest {

    private final DepartmentRepository departmentRepository = mock(DepartmentRepository.class);
    private final ListDepartmentsHandler handler = new ListDepartmentsHandler(departmentRepository);

    private static Department department(long id, String name) {
        Department department = new Department(name);
        ReflectionTestUtils.setField(department, "id", id);
        return department;
    }

    @Test
    void asksTheRepositoryForTheRequestedPageSortedByName() {
        when(departmentRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        handler.handle(new ListDepartmentsRequest(2, 5));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(departmentRepository).findAll(pageable.capture());
        assertThat(pageable.getValue()).isEqualTo(PageRequest.of(2, 5, ListDepartmentsHandler.ORDER));
    }

    @Test
    void mapsTheDepartmentsAndThePagingInformation() {
        PageRequest firstPage = PageRequest.of(0, 2, ListDepartmentsHandler.ORDER);
        when(departmentRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(
                List.of(department(3, "Finans"), department(1, "Pazarlama")), firstPage, 3));

        PageResponse<DepartmentView> response = handler.handle(new ListDepartmentsRequest(0, 2));

        assertThat(response.content()).containsExactly(
                new DepartmentView(3L, "Finans"), new DepartmentView(1L, "Pazarlama"));
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(3);
        assertThat(response.totalPages()).isEqualTo(2);
    }
}
