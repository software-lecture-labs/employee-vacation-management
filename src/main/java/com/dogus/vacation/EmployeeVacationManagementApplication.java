package com.dogus.vacation;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Uygulamanın giriş noktası.
 *
 * <p>{@link SpringBootServletInitializer}'dan türediği için aynı WAR dosyası hem
 * {@code java -jar} ile gömülü Tomcat'te hem de bir sunucuya (Payara Micro) deploy
 * edilerek çalışır.
 */
@SpringBootApplication
public class EmployeeVacationManagementApplication extends SpringBootServletInitializer {

    /** Uygulamanın ve JVM'in saat dilimi; iş günü ve tarih hesapları buna göre yapılır. */
    public static final String TIME_ZONE = "Europe/Istanbul";

    static {
        // Sınıf her iki çalıştırma yolunda da (java -jar ve sunucuya deploy) yüklenir.
        TimeZone.setDefault(TimeZone.getTimeZone(TIME_ZONE));
    }

    public static void main(String[] args) {
        SpringApplication.run(EmployeeVacationManagementApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(EmployeeVacationManagementApplication.class);
    }
}
