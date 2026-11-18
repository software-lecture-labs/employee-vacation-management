package com.dogus.vacation;

import javax.sql.DataSource;

import com.dogus.vacation.domain.model.LeaveType;
import com.dogus.vacation.infrastructure.persistence.LeaveTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.jdbc.JdbcTestUtils;

/**
 * Veritabanına giden bütün entegrasyon testlerinin ortak tabanı.
 *
 * <p>Testler Supabase'e değil, {@code db/scripts} ile kurulan geçici PostgreSQL'e
 * gider ({@link TestcontainersConfiguration}). Bu sınıftan türeyen bütün testler aynı
 * Spring bağlamını ve aynı konteyneri paylaşır.
 *
 * <p>Kurallar:
 * <ul>
 *   <li>Her test temiz bir veritabanıyla başlar: tablolar boşaltılır, 4 hazır izin
 *       türü yeniden yüklenir.</li>
 *   <li>Test sınıfına {@code @Transactional} konmaz. Konursa test kendi transaction'ı
 *       içinde koşar ve rollback testleri "veritabanında iz kaldı mı?" sorusunu
 *       ölçemez. Bunun yerine {@link #countRows(String)} kullanılır.</li>
 * </ul>
 *
 * <p>Örnek:
 * <pre>{@code
 * class ApproveRollbackIT extends AbstractIntegrationTest {
 *     @Autowired DepartmentRepository departments;
 *     @Autowired EmployeeRepository employees;
 *
 *     @Test
 *     void ... {
 *         Department it = departments.save(TestData.department());
 *         Employee ayse = employees.save(TestData.employee(it, Role.EMPLOYEE));
 *         ...
 *         assertThat(countRows("leave_request_history")).isZero();
 *     }
 * }
 * }</pre>
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public abstract class AbstractIntegrationTest {

    /** Bütün tablolar; CASCADE foreign key sırasını kendisi çözer. */
    private static final String TRUNCATE_ALL_TABLES = """
            TRUNCATE TABLE
                leave_mgmt.balance_transactions,
                leave_mgmt.leave_request_history,
                leave_mgmt.leave_requests,
                leave_mgmt.leave_balances,
                leave_mgmt.annual_accrual_runs,
                leave_mgmt.public_holidays,
                leave_mgmt.employees,
                leave_mgmt.departments,
                leave_mgmt.leave_types
            RESTART IDENTITY CASCADE
            """;

    /** Supabase'de çalıştırılan seed scriptinin aynısı (proje köküne göre yol). */
    private static final FileSystemResource LEAVE_TYPE_SEED =
            new FileSystemResource("db/scripts/02_seed_leave_types.sql");

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private LeaveTypeRepository leaveTypeRepository;

    /**
     * Testten önce çalışır (sonra değil): önceki bir test yarıda çökse bile
     * her test temiz başlar. {@code RESTART IDENTITY} id'leri 1'den başlatır.
     */
    @BeforeEach
    protected void resetDatabase() {
        jdbcTemplate.execute(TRUNCATE_ALL_TABLES);
        new ResourceDatabasePopulator(LEAVE_TYPE_SEED).execute(dataSource);
    }

    /** Hazır izin türlerinden birini koduyla döner: ANNUAL, EXCUSE, SICK, UNPAID. */
    protected LeaveType leaveType(String code) {
        return leaveTypeRepository.findByCode(code)
                .orElseThrow(() -> new IllegalStateException("Hazır izin türü bulunamadı: " + code));
    }

    /** {@code leave_mgmt} şemasındaki bir tablonun satır sayısı; rollback testleri için. */
    protected int countRows(String table) {
        return JdbcTestUtils.countRowsInTable(jdbcTemplate, "leave_mgmt." + table);
    }
}
