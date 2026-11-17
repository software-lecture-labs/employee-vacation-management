package com.dogus.vacation.infrastructure.config;

import java.time.Clock;
import java.time.ZoneId;

import com.dogus.vacation.EmployeeVacationManagementApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tarih gerektiren her sınıf "bugün"ü bu {@link Clock}'tan alır.
 *
 * <p>{@code LocalDate.now()} doğrudan çağrılmaz; testlerde sabit bir saat verilerek
 * tarihe bağlı kurallar (geçmiş tarih, iptal süresi) tekrarlanabilir şekilde denenir.
 */
@Configuration(proxyBeanMethods = false)
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of(EmployeeVacationManagementApplication.TIME_ZONE));
    }
}
