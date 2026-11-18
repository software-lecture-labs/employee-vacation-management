package com.dogus.vacation;

import static org.assertj.core.api.Assertions.assertThat;

import com.dogus.vacation.domain.model.LeaveType;
import com.dogus.vacation.infrastructure.persistence.DepartmentRepository;
import com.dogus.vacation.infrastructure.persistence.LeaveTypeRepository;
import com.dogus.vacation.support.TestData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * {@link AbstractIntegrationTest}'in her testten önce veritabanını sıfırladığını doğrular.
 *
 * <p>Testler ikişer ikişer aynı işi yapar; hangisi önce çalışırsa çalışsın, sıfırlama
 * bozuk olsaydı ikinci çalışan önceki testin verisini görür ve başarısız olurdu.
 */
class DatabaseResetTest extends AbstractIntegrationTest {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private LeaveTypeRepository leaveTypeRepository;

    @Test
    void firstTestSeesOnlyItsOwnDepartment() {
        departmentRepository.save(TestData.department());

        assertThat(countRows("departments")).isEqualTo(1);
    }

    @Test
    void secondTestSeesOnlyItsOwnDepartment() {
        departmentRepository.save(TestData.department());

        assertThat(countRows("departments")).isEqualTo(1);
    }

    @Test
    void renamingASeedLeaveTypeAffectsOnlyThisTest() {
        LeaveType annual = leaveType("ANNUAL");
        annual.setName("Değiştirilmiş ad");
        leaveTypeRepository.save(annual);

        assertThat(leaveType("ANNUAL").getName()).isEqualTo("Değiştirilmiş ad");
    }

    @Test
    void seedLeaveTypesStartFromTheirOriginalState() {
        assertThat(countRows("leave_types")).isEqualTo(4);
        assertThat(leaveType("ANNUAL").getName()).isEqualTo("Yıllık izin");
        assertThat(leaveType("ANNUAL").getId()).isEqualTo(1L);
    }
}
