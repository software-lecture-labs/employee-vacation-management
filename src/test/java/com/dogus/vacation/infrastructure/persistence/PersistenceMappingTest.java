package com.dogus.vacation.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.dogus.vacation.TestcontainersConfiguration;
import com.dogus.vacation.domain.model.Department;
import com.dogus.vacation.domain.model.LeaveType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Entity'lerin test veritabanına (db/scripts ile kurulan PostgreSQL) bağlandığını doğrular.
 *
 * <p>Uygulamanın açılması, {@code ddl-auto: validate} sayesinde 9 entity'nin tablolarla
 * uyumlu olduğunu zaten kanıtlar; bu testler ayrıca seed verisini ve auditing'i dener.
 * Çalışması için Docker açık olmalıdır.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class PersistenceMappingTest {

    @Autowired
    private LeaveTypeRepository leaveTypeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Test
    void seedScriptLoadsFourLeaveTypes() {
        assertThat(leaveTypeRepository.findAll())
                .extracting(LeaveType::getCode)
                .containsExactlyInAnyOrder("ANNUAL", "EXCUSE", "SICK", "UNPAID");
    }

    @Test
    void savingEntityFillsIdAndTimestamps() {
        Department saved = departmentRepository.save(new Department("Persistence Mapping Test"));
        try {
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getUpdatedAt()).isNotNull();
        } finally {
            departmentRepository.delete(saved);
        }
    }
}
