package com.dogus.vacation.infrastructure.seed;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import com.dogus.vacation.domain.model.Department;
import com.dogus.vacation.domain.model.Employee;
import com.dogus.vacation.domain.model.enums.Role;
import com.dogus.vacation.domain.service.LeaveBalanceService;
import com.dogus.vacation.infrastructure.persistence.DepartmentRepository;
import com.dogus.vacation.infrastructure.persistence.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Demo verisini yükler: 3 departman, 2 İK, 2 yönetici, 6 çalışan ve hepsinin bu yılki bakiyeleri.
 *
 * <p><b>Ne zaman çalışır:</b> yalnızca {@code dev} ve {@code local} profillerinde, uygulama açılırken
 * ve çalışan tablosu boşsa. Ortak Supabase'de elle eklenmiş kayıtlar varsa hiçbir şey yazılmaz;
 * testlerde ({@code test}) ve teslim profilinde demo verisi yoktur.
 *
 * <p><b>Şifre:</b> bütün demo kullanıcıları {@code DEMO_PASSWORD} ortam değişkenindeki (veya
 * {@code .env}) şifreyle girer; şifre koda yazılmaz (NFR-04). Boşsa veri yüklenmez, uyarı loglanır.
 *
 * <p><b>Bakiyeler:</b> T4 ile aynı yoldan, {@link LeaveBalanceService#openYearBalances} ile açılır
 * (yıllık izin kıdem ve yaşa göre, mazeret 5 gün). {@code annual_accrual_runs} tablosuna kayıt
 * yazılmaz; yazılsaydı bu yıl için yıllık hak ediş (T3) çalıştırılamazdı. T3 bakiyesi olan çalışanı
 * zaten atlar.
 *
 * <p>Bütün yükleme tek transaction'dır: yarıda hata çıkarsa yarım demo verisi kalmaz.
 */
@Component
@Profile({"dev", "local"})
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    /** Demo e-postalarının alan adı. */
    static final String DOMAIN = "@demo.local";

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveBalanceService balanceService;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;
    private final String demoPassword;

    public DataSeeder(DepartmentRepository departmentRepository, EmployeeRepository employeeRepository,
                      LeaveBalanceService balanceService, PasswordEncoder passwordEncoder,
                      PlatformTransactionManager transactionManager, Clock clock,
                      @Value("${app.demo.password:}") String demoPassword) {
        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
        this.balanceService = balanceService;
        this.passwordEncoder = passwordEncoder;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.clock = clock;
        this.demoPassword = demoPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        seed();
    }

    /**
     * @return yüklendiyse {@code true}; tablo dolu olduğu ya da şifre verilmediği için atlandıysa
     *         {@code false}
     */
    public boolean seed() {
        if (demoPassword == null || demoPassword.isBlank()) {
            log.warn("Demo verisi yüklenmedi: DEMO_PASSWORD tanımlı değil (.env, bkz. README 'Demo Kullanıcıları').");
            return false;
        }
        if (employeeRepository.count() > 0) {
            log.info("Demo verisi yüklenmedi: çalışan tablosu boş değil.");
            return false;
        }
        Integer employees = transactionTemplate.execute(status -> load());
        log.info("Demo verisi yüklendi: 3 departman, {} çalışan, {} yılı bakiyeleri.", employees,
                LocalDate.now(clock).getYear());
        return true;
    }

    private int load() {
        int year = LocalDate.now(clock).getYear();
        String hash = passwordEncoder.encode(demoPassword);
        Department hrDepartment = departmentRepository.save(new Department("İnsan Kaynakları"));
        Department software = departmentRepository.save(new Department("Yazılım Geliştirme"));
        Department sales = departmentRepository.save(new Department("Satış"));

        // Kıdem ve yaş çeşitli seçildi: hak 14 / 20 / 26 gün, 18 yaş ve 50 yaş koruması, bu yıl
        // giren (yıllık izni 0) ve yöneticisi olmayan (talebini İK onaylar, BR-07) çalışan.
        Employee hr1 = person("Zeynep", "Aydın", "zeynep.aydin", Role.HR, hrDepartment, null,
                LocalDate.of(year - 12, 2, 1), LocalDate.of(year - 38, 5, 14), hash);
        Employee hr2 = person("Murat", "Çelik", "murat.celik", Role.HR, hrDepartment, null,
                LocalDate.of(year - 4, 9, 1), LocalDate.of(year - 31, 11, 3), hash);
        Employee softwareManager = person("Ahmet", "Yılmaz", "ahmet.yilmaz", Role.MANAGER, software, null,
                LocalDate.of(year - 16, 3, 15), LocalDate.of(year - 45, 1, 20), hash);
        Employee salesManager = person("Elif", "Kaya", "elif.kaya", Role.MANAGER, sales, null,
                LocalDate.of(year - 9, 6, 1), LocalDate.of(year - 40, 7, 8), hash);
        List<Employee> employees = List.of(hr1, hr2, softwareManager, salesManager,
                person("Ayşe", "Demir", "ayse.demir", Role.EMPLOYEE, software, softwareManager,
                        LocalDate.of(year - 3, 4, 1), LocalDate.of(year - 28, 2, 10), hash),
                person("Can", "Öztürk", "can.ozturk", Role.EMPLOYEE, software, softwareManager,
                        LocalDate.of(year - 7, 10, 1), LocalDate.of(year - 33, 9, 25), hash),
                person("Ece", "Arslan", "ece.arslan", Role.EMPLOYEE, software, softwareManager,
                        LocalDate.of(year, 1, 2), LocalDate.of(year - 24, 12, 5), hash),
                person("Burak", "Şahin", "burak.sahin", Role.EMPLOYEE, sales, salesManager,
                        LocalDate.of(year - 2, 7, 1), LocalDate.of(year - 18, 3, 3), hash),
                person("Selin", "Koç", "selin.koc", Role.EMPLOYEE, sales, salesManager,
                        LocalDate.of(year - 5, 2, 15), LocalDate.of(year - 52, 8, 30), hash),
                person("Deniz", "Aksoy", "deniz.aksoy", Role.EMPLOYEE, sales, null,
                        LocalDate.of(year - 1, 5, 2), LocalDate.of(year - 26, 6, 18), hash));

        for (Employee employee : employees) {
            balanceService.openYearBalances(employee, year, hr1);
        }
        return employees.size();
    }

    private Employee person(String firstName, String lastName, String emailName, Role role, Department department,
                            Employee manager, LocalDate hireDate, LocalDate birthDate, String passwordHash) {
        Employee employee = new Employee(firstName, lastName, emailName + DOMAIN, hireDate, birthDate, role,
                department);
        employee.setManager(manager);
        employee.setPasswordHash(passwordHash);
        return employeeRepository.save(employee);
    }
}
