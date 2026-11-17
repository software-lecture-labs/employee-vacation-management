# Çalışma Raporu: Onur

- **GitHub:** @<kullanıcı-adı>
- **Modül:** Kimlik, güvenlik ve iptal süresi (`auth`, `security`; `leave-cancellation` içinde gece görevi ve acil uyarısı)
- **Sorumlu olduğum gereksinimler:** FR-01, FR-02, FR-03, FR-04, FR-11, FR-30, FR-31 · bonus FR-05 · BR-10, BR-18 · NFR-04 · T5
- **Altyapı işleri:** proje iskeleti, CI, Dockerfile ve compose, Swagger ayarı, `CONTRIBUTING.md` ve şablonlar, tüm endpoint'lerin yetki testleri

---

## 17 Kasım 2026 — chore(skeleton): proje iskeleti, profiller ve Supabase bağlantısı (#<PR no>)
**İlgili gereksinimler:** NFR-01, NFR-04, NFR-09 (doğrudan bir FR karşılanmadı; bütün FR'ler bu iskeletin üzerine yazılacak)
**Use case(ler):** Yok (altyapı)
**Ne yaptım:**
- `pom.xml`: Java 21, Spring Boot 4.1.2, `<packaging>war</packaging>`, `finalName` sürümsüz (`employee-vacation-management.war`).
- Maven Wrapper (`mvnw`, `mvnw.cmd`, Maven 3.9.16): herkes aynı Maven sürümüyle derliyor.
- `EmployeeVacationManagementApplication`: `SpringBootServletInitializer`'dan türüyor; JVM saat dilimi `Europe/Istanbul`.
- Katman paketleri: `api`, `application`, `domain`, `infrastructure` (her birinde `package-info.java` ile katmanın kuralı).
- `application.yml`, `application-dev.yml`, `application-local.yml`; test için `application-test.yml`.
- Supabase bağlantısı `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` ortam değişkenlerinden; `.env.example` eklendi, `.env` `.gitignore`'da.
- `ClockConfig` (`Clock` bean'i) ve `OpenApiConfig` (Swagger UI başlığı).
**Validator'a koyduğum kurallar:**
- Yok (kod yok)
**Handler'daki kurallar ve transaction:**
- Yok. `spring.jpa.open-in-view=false` ile transaction ve lazy yükleme Handler içinde biter.
**Neden böyle yaptım (tasarım kararları):**
- İlk günden WAR: aynı dosya `java -jar` ile gömülü Tomcat'te, bonus olarak Payara Micro'da çalışacak; sonradan çevirmek Dockerfile ve kurulum dokümanını değiştirirdi.
- `ddl-auto: validate`: şema SQL scriptleriyle kuruluyor, Hibernate sadece entity'lerin şemayla uyumunu denetliyor.
- Hikari havuzu 5: dört kişi aynı Supabase'e bağlanıyor.
- Tarih tek bir `Clock`'tan geliyor: testlerde "bugün" sabitlenebiliyor.
- Testler Supabase'e bağlanmıyor; Testcontainers ile `postgres:17-alpine` açılıyor (Supabase PostgreSQL 17).
**Testler:**
- `ClockConfigTest.clockUsesIstanbulTimeZone` — `Clock` bean'inin saat dilimi
- `EmployeeVacationManagementApplicationTests.contextLoads` — uygulama bağlamı test veritabanıyla açılıyor
- `EmployeeVacationManagementApplicationTests.clockAndJvmUseIstanbulTimeZone` — `Clock` ve JVM saat dilimi
**Karşılaştığım sorun ve çözümü:**
- Spring Boot 4'te web starter'ı `spring-boot-starter-webmvc` oldu ve Testcontainers 2'de modül adları değişti (`testcontainers-postgresql`, `org.testcontainers.postgresql.PostgreSQLContainer`); bağımlılıklar buna göre yazıldı.
**Sunumda anlatacağım tek cümle:**
Proje ilk günden çalıştırılabilir WAR olarak kuruldu; aynı dosya gömülü Tomcat'te ve Payara'da çalışıyor, bağlantı bilgileri koda değil ortam değişkenlerine bağlı.
**Açık kalanlar:**
- `db/scripts` dosyalarını test veritabanına yükleyen ortak test tabanı (Zehra, 18 Kasım).
- `@EnableScheduling` ve test profilinde kapatılması gece göreviyle (FR-31) gelecek.
- Güvenlik (`SecurityConfig`, JWT) ve Swagger'da "Authorize" kimlik doğrulama PR'ında.
