package com.dogus.vacation.application.usecase.auth.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dogus.vacation.application.common.CurrentUserProvider;
import com.dogus.vacation.domain.model.Department;
import com.dogus.vacation.domain.model.Employee;
import com.dogus.vacation.domain.model.enums.Role;
import com.dogus.vacation.support.TestData;
import org.junit.jupiter.api.Test;

class GetMyProfileHandlerTest {

    @Test
    void returnsTheSignedInEmployeeWithDepartmentAndManager() {
        Department it = TestData.department();
        Employee manager = TestData.employee(it, Role.MANAGER);
        Employee ayse = TestData.employee(it, Role.EMPLOYEE);
        ayse.setManager(manager);
        CurrentUserProvider currentUser = mock(CurrentUserProvider.class);
        when(currentUser.currentEmployee()).thenReturn(ayse);

        GetMyProfileResponse response = new GetMyProfileHandler(currentUser).handle(new GetMyProfileRequest());

        assertThat(response.employee().email()).isEqualTo(ayse.getEmail());
        assertThat(response.employee().role()).isEqualTo(Role.EMPLOYEE);
        assertThat(response.employee().departmentName()).isEqualTo(it.getName());
        assertThat(response.employee().manager().fullName())
                .isEqualTo(manager.getFirstName() + " " + manager.getLastName());
        assertThat(response.employee().active()).isTrue();
    }
}
