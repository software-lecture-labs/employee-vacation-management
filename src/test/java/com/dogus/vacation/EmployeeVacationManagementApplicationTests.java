package com.dogus.vacation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.ZoneId;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Uygulama bağlamının veritabanıyla birlikte açıldığını doğrular.
 * Çalışması için Docker açık olmalıdır (Testcontainers).
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class EmployeeVacationManagementApplicationTests {

    private static final ZoneId ISTANBUL = ZoneId.of(EmployeeVacationManagementApplication.TIME_ZONE);

    @Autowired
    private Clock clock;

    @Test
    void contextLoads() {
        assertThat(clock).isNotNull();
    }

    @Test
    void clockAndJvmUseIstanbulTimeZone() {
        assertThat(clock.getZone()).isEqualTo(ISTANBUL);
        assertThat(TimeZone.getDefault().toZoneId()).isEqualTo(ISTANBUL);
    }
}
