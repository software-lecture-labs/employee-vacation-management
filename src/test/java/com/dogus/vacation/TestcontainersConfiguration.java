package com.dogus.vacation;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/**
 * Testlerin bağlandığı geçici PostgreSQL. Testler Supabase'e asla bağlanmaz.
 *
 * <p>İmaj, Supabase'deki PostgreSQL ana sürümüyle (17) aynıdır. {@code db/scripts}
 * klasöründeki scriptler konteynerin {@code /docker-entrypoint-initdb.d/} klasörüne
 * kopyalanır; PostgreSQL ilk açılışta bunları ada göre sırayla (01, 02, 03) çalıştırır.
 * Böylece test veritabanı, Supabase'de çalıştırılan scriptlerin aynısıyla kurulur.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    static final String POSTGRES_IMAGE = "postgres:17-alpine";

    /** Proje köküne göre yol; Maven testleri proje kökünden çalıştırır. */
    static final String DB_SCRIPTS_DIR = "db/scripts";

    static final String INIT_SCRIPTS_DIR = "/docker-entrypoint-initdb.d/";

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(POSTGRES_IMAGE)
                .withCopyFileToContainer(MountableFile.forHostPath(DB_SCRIPTS_DIR), INIT_SCRIPTS_DIR);
    }
}
