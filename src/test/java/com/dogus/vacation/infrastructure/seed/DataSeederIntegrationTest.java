package com.dogus.vacation.infrastructure.seed;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;

import com.dogus.vacation.AbstractIntegrationTest;
import com.dogus.vacation.domain.model.Employee;
import com.dogus.vacation.domain.model.enums.Role;
import com.dogus.vacation.domain.service.LeaveBalanceService;
import com.dogus.vacation.infrastructure.persistence.DepartmentRepository;
import com.dogus.vacation.infrastructure.persistence.EmployeeRepository;
import com.dogus.vacation.support.TestData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Demo verisini gerçek PostgreSQL'e yükler ve sonucu SQL ile okur.
 *
 * <p>{@link DataSeeder} yalnızca {@code dev} ve {@code local} profillerinde bean olur; test
 * profilinde olmadığı burada da doğrulanır. Testler onu aynı bağımlılıklarla elle kurar.
 */
class DataSeederIntegrationTest extends AbstractIntegrationTest {

    private static final String DEMO_PASSWORD = "DemoSifre1!";

    @Autowired
    private ApplicationContext context;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private LeaveBalanceService balanceService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private Clock clock;

    private DataSeeder seeder(String password) {
        return new DataSeeder(departmentRepository, employeeRepository, balanceService, passwordEncoder,
                transactionManager, clock, password);
    }

    private BigDecimal annualDays(String emailName) {
        return jdbcTemplate.queryForObject("""
                select b.entitled_days from leave_mgmt.leave_balances b
                join leave_mgmt.employees e on e.id = b.employee_id
                join leave_mgmt.leave_types t on t.id = b.leave_type_id
                where e.email = ? and t.code = 'ANNUAL' and b.year = ?""",
                BigDecimal.class, emailName + DataSeeder.DOMAIN, LocalDate.now(clock).getYear());
    }

    @Test
    void isNotABeanInTheTestProfile() {
        assertThat(context.getBeansOfType(DataSeeder.class)).isEmpty();
    }

    @Test
    void loadsTwoHrTwoManagersSixEmployeesAndTheirBalances() {
        assertThat(seeder(DEMO_PASSWORD).seed()).isTrue();

        assertThat(countRows("departments")).isEqualTo(3);
        assertThat(employeeRepository.countByRoleAndActiveTrue(Role.HR)).isEqualTo(2);
        assertThat(employeeRepository.countByRoleAndActiveTrue(Role.MANAGER)).isEqualTo(2);
        assertThat(employeeRepository.countByRoleAndActiveTrue(Role.EMPLOYEE)).isEqualTo(6);
        // Her çalışana yıllık ve mazeret bakiyesi; hastalık ve ücretsiz izne bakiye yok (BR-03).
        assertThat(countRows("leave_balances")).isEqualTo(20);
        // Yıllık hak ediş kaydı yazılmaz; yazılsaydı bu yıl T3 çalıştırılamazdı.
        assertThat(countRows("annual_accrual_runs")).isZero();
    }

    @Test
    void entitlementsShowSeniorityAndAgeRules() {
        seeder(DEMO_PASSWORD).seed();

        assertThat(annualDays("ahmet.yilmaz")).isEqualByComparingTo("26");   // 16 yıl
        assertThat(annualDays("can.ozturk")).isEqualByComparingTo("20");     // 7 yıl
        assertThat(annualDays("ayse.demir")).isEqualByComparingTo("14");     // 3 yıl
        assertThat(annualDays("burak.sahin")).isEqualByComparingTo("20");    // 2 yıl, 18 yaş
        assertThat(annualDays("selin.koc")).isEqualByComparingTo("20");      // 5 yıl, 52 yaş
        assertThat(annualDays("ece.arslan")).isEqualByComparingTo("0");      // bu yıl girdi
    }

    @Test
    void everyoneLogsInWithTheDemoPasswordAndOneEmployeeHasNoManager() {
        seeder(DEMO_PASSWORD).seed();

        Employee hr = employeeRepository.findByEmailIgnoreCase("zeynep.aydin" + DataSeeder.DOMAIN).orElseThrow();
        assertThat(passwordEncoder.matches(DEMO_PASSWORD, hr.getPasswordHash())).isTrue();
        assertThat(employeeRepository.findByEmailIgnoreCase("deniz.aksoy" + DataSeeder.DOMAIN).orElseThrow()
                .getManager()).isNull();
        assertThat(jdbcTemplate.queryForList(
                "select distinct created_by from leave_mgmt.balance_transactions", Long.class))
                .containsExactly(hr.getId());
    }

    @Test
    void doesNothingWhenEmployeesAlreadyExist() {
        employeeRepository.save(TestData.employee(departmentRepository.save(TestData.department())));

        assertThat(seeder(DEMO_PASSWORD).seed()).isFalse();

        assertThat(countRows("employees")).isEqualTo(1);
        assertThat(countRows("leave_balances")).isZero();
    }

    @Test
    void runningTwiceLoadsOnlyOnce() {
        seeder(DEMO_PASSWORD).seed();

        assertThat(seeder(DEMO_PASSWORD).seed()).isFalse();
        assertThat(countRows("employees")).isEqualTo(10);
    }

    @Test
    void doesNothingWithoutADemoPassword() {
        assertThat(seeder("").seed()).isFalse();
        assertThat(seeder(null).seed()).isFalse();

        assertThat(countRows("employees")).isZero();
    }
}
