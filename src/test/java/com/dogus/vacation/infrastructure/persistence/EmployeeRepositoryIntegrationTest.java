package com.dogus.vacation.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import com.dogus.vacation.AbstractIntegrationTest;
import com.dogus.vacation.domain.model.Department;
import com.dogus.vacation.domain.model.Employee;
import com.dogus.vacation.support.TestData;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Girişteki e-posta sorgusunu ({@link EmployeeRepository#findByEmailIgnoreCase}) gerçek
 * PostgreSQL üzerinde doğrular: harf duyarsız bulma davranışı ve {@code lower(email)}
 * indeksinin kullanılabilmesi.
 *
 * <p>Sorgu planı testlerinde {@code enable_seqscan} kapatılır. Testteki tablo küçük olduğu
 * için PostgreSQL normalde indeks yerine tabloyu taramayı seçer; tarama kapatılınca
 * kullanılabilecek bir indeks varsa onu seçer. Böylece test "bu sorgu bu indeksi
 * kullanabilir mi?" sorusunu veri miktarından bağımsız cevaplar.
 */
class EmployeeRepositoryIntegrationTest extends AbstractIntegrationTest {

    private static final String EMAIL_INDEX = "uq_employees_email_lower";

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private Employee ayse;

    @BeforeEach
    void createEmployee() {
        Department department = departmentRepository.save(TestData.department());
        Employee employee = TestData.employee(department);
        employee.setEmail("Ayse.Yilmaz@Firma.com");
        ayse = employeeRepository.save(employee);
    }

    @Test
    void findsTheEmployeeWhateverTheCaseOfTheEmail() {
        assertThat(employeeRepository.findByEmailIgnoreCase("ayse.yilmaz@firma.com"))
                .map(Employee::getId).contains(ayse.getId());
        assertThat(employeeRepository.findByEmailIgnoreCase("AYSE.YILMAZ@FIRMA.COM"))
                .map(Employee::getId).contains(ayse.getId());
        assertThat(employeeRepository.findByEmailIgnoreCase("baska@firma.com")).isEmpty();
    }

    @Test
    void theRepositoryComparesWithLowerLikeTheUniqueIndex() {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
        try {
            employeeRepository.findByEmailIgnoreCase("ayse.yilmaz@firma.com");

            assertThat(Arrays.asList(statistics.getQueries()))
                    .anyMatch(query -> query.contains("lower(e.email) = lower(:email)"));
        } finally {
            statistics.setStatisticsEnabled(false);
        }
    }

    @Test
    void lowerComparisonCanUseTheEmailIndex() {
        assertThat(planOf("select * from leave_mgmt.employees e"
                + " where lower(e.email) = lower('ayse.yilmaz@firma.com')"))
                .contains(EMAIL_INDEX);
    }

    /** Eski hâl: Spring Data'nın IgnoreCase için ürettiği upper() karşılaştırması indeksi kullanamaz. */
    @Test
    void upperComparisonCannotUseTheEmailIndex() {
        assertThat(planOf("select * from leave_mgmt.employees e"
                + " where upper(e.email) = upper('ayse.yilmaz@firma.com')"))
                .doesNotContain(EMAIL_INDEX)
                .contains("Seq Scan");
    }

    /** Tablo taraması kapalıyken sorgunun planı; {@code set local} yalnızca bu transaction'da geçerlidir. */
    private String planOf(String sql) {
        return transactionTemplate.execute(status -> {
            jdbcTemplate.execute("set local enable_seqscan = off");
            return String.join("\n", jdbcTemplate.queryForList("explain " + sql, String.class));
        });
    }
}
