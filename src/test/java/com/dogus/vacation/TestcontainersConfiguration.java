package com.dogus.vacation;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Testlerin bağlandığı geçici PostgreSQL. Testler Supabase'e asla bağlanmaz.
 *
 * <p>İmaj, Supabase'deki PostgreSQL ana sürümüyle (17) aynıdır. {@code db/scripts}
 * dosyalarını konteynere yükleyen ortak test tabanı veritabanı işleriyle birlikte gelir.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    static final String POSTGRES_IMAGE = "postgres:17-alpine";

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(POSTGRES_IMAGE);
    }
}
