package com.dogus.vacation.domain.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dogus.vacation.domain.exception.ErrorCode;
import com.dogus.vacation.domain.exception.ForbiddenActionException;
import com.dogus.vacation.domain.model.Department;
import com.dogus.vacation.domain.model.Employee;
import com.dogus.vacation.domain.model.enums.Role;
import com.dogus.vacation.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@link ApproverResolver} için birim testleri (BR-07, FR-24). Veritabanı gerekmez.
 *
 * <p>Kadro: yönetici Mehmet, onun ekibinden Ayşe, yöneticisi olmayan Can, İK'dan Selin ve Deniz.
 */
class ApproverResolverTest {

    private final ApproverResolver resolver = new ApproverResolver();

    private Employee mehmetManager;
    private Employee otherManager;
    private Employee ayse;
    private Employee can;
    private Employee selinHr;
    private Employee denizHr;

    @BeforeEach
    void setUp() {
        Department department = TestData.department();
        mehmetManager = TestData.employee(department, Role.MANAGER);
        otherManager = TestData.employee(department, Role.MANAGER);
        selinHr = TestData.employee(department, Role.HR);
        denizHr = TestData.employee(department, Role.HR);
        ayse = TestData.employee(department);
        ayse.setManager(mehmetManager);
        can = TestData.employee(department);
    }

    @Test
    void employeeWithActiveManagerIsDecidedByThatManagerOnly() {
        ApprovalRoute route = resolver.resolve(ayse);

        assertThat(route.kind()).isEqualTo(ApprovalRoute.Kind.MANAGER);
        assertThat(route.manager()).isSameAs(mehmetManager);
        assertThat(resolver.canDecide(mehmetManager, ayse)).isTrue();
        assertThat(resolver.canDecide(otherManager, ayse)).isFalse();
        assertThat(resolver.canDecide(selinHr, ayse)).as("İK, yöneticisi olan çalışana karar veremez").isFalse();
    }

    @Test
    void employeeWithoutManagerIsDecidedByAnyHr() {
        assertThat(resolver.resolve(can).kind()).isEqualTo(ApprovalRoute.Kind.HR);
        assertThat(resolver.canDecide(selinHr, can)).isTrue();
        assertThat(resolver.canDecide(denizHr, can)).isTrue();
        assertThat(resolver.canDecide(mehmetManager, can)).isFalse();
    }

    @Test
    void inactiveManagerCountsAsNoManager() {
        mehmetManager.setActive(false);

        assertThat(resolver.resolve(ayse).kind()).isEqualTo(ApprovalRoute.Kind.HR);
        assertThat(resolver.canDecide(mehmetManager, ayse)).isFalse();
        assertThat(resolver.canDecide(selinHr, ayse)).isTrue();
    }

    @Test
    void hrRequestIsDecidedByAnotherHrEvenIfHrHasManager() {
        selinHr.setManager(mehmetManager);

        assertThat(resolver.resolve(selinHr).kind()).isEqualTo(ApprovalRoute.Kind.HR);
        assertThat(resolver.canDecide(denizHr, selinHr)).isTrue();
        assertThat(resolver.canDecide(selinHr, selinHr)).isFalse();
        assertThat(resolver.canDecide(mehmetManager, selinHr)).isFalse();
    }

    @Test
    void inactiveHrCannotDecide() {
        denizHr.setActive(false);

        assertThat(resolver.canDecide(denizHr, can)).isFalse();
        assertThat(resolver.canDecide(denizHr, selinHr)).isFalse();
    }

    @Test
    void ownRequestIsForbiddenWithSelfApprovalCode() {
        assertThatThrownBy(() -> resolver.checkCanDecide(selinHr, selinHr))
                .isInstanceOf(ForbiddenActionException.class)
                .extracting(e -> ((ForbiddenActionException) e).getErrorCode())
                .isEqualTo(ErrorCode.SELF_APPROVAL_NOT_ALLOWED);
    }

    @Test
    void wrongApproverIsForbiddenWithGeneralCode() {
        assertThatThrownBy(() -> resolver.checkCanDecide(otherManager, ayse))
                .isInstanceOf(ForbiddenActionException.class)
                .extracting(e -> ((ForbiddenActionException) e).getErrorCode())
                .isEqualTo(ErrorCode.ACTION_NOT_ALLOWED);
    }

    @Test
    void rightApproverPassesCheck() {
        assertThatCode(() -> resolver.checkCanDecide(mehmetManager, ayse)).doesNotThrowAnyException();
        assertThatCode(() -> resolver.checkCanDecide(denizHr, selinHr)).doesNotThrowAnyException();
    }
}
