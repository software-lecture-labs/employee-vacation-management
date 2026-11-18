package com.dogus.vacation.infrastructure.config;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Entity'lerdeki {@code created_at} ve {@code updated_at} alanlarını otomatik doldurur.
 *
 * <p>Saat doğrudan sistemden değil uygulamanın {@link Clock} bean'inden alınır; böylece
 * testlerde "şimdi" sabitlenebilir. Ayar ana sınıfa değil buraya konur: {@code @WebMvcTest}
 * gibi dilim testleri bu sınıfı yüklemediği için JPA olmadan açılabilir.
 */
@Configuration(proxyBeanMethods = false)
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
public class JpaAuditingConfig {

    @Bean
    public DateTimeProvider auditingDateTimeProvider(Clock clock) {
        return () -> Optional.of(OffsetDateTime.now(clock));
    }
}
