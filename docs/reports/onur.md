# Çalışma Raporu: Onur

- **GitHub:** @onurkya5149
- **Modül:** Kimlik, güvenlik ve iptal süresi (`auth`, `security`; `leave-cancellation` içinde gece görevi ve acil uyarısı)
- **Sorumlu olduğum gereksinimler:** FR-01, FR-02, FR-03, FR-04, FR-11, FR-30, FR-31 · bonus FR-05 · BR-10, BR-18 · NFR-04 · T5
- **Altyapı işleri:** proje iskeleti, CI, Dockerfile ve compose, Swagger ayarı, `CONTRIBUTING.md` ve şablonlar, tüm endpoint'lerin yetki testleri

---

## 17 Kasım 2026 — chore(skeleton): proje iskeleti, profiller ve Supabase bağlantısı (#2)
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

## 17 Kasım 2026 — docs(contributing): katkı kuralları, şablonlar ve gereksinim listesi (#3, dev'e #103 ile)
**İlgili gereksinimler:** Doğrudan bir FR/BR karşılanmadı; bu PR bütün kodların sözlüğünü (`docs/REQUIREMENTS.md`) ve commit/PR kurallarını getiriyor.
**Use case(ler):** Yok (doküman ve ayar dosyaları)
**Ne yaptım:**
- `CONTRIBUTING.md`: altın kurallar, görev yaşam döngüsü, branch adları, commit başlık + açıklama biçimi, PR kuralları, GitHub ayarları ve günlük ritim.
- `.gitmessage`: `git config commit.template .gitmessage` ile açılan commit şablonu; tür ve kapsam listesi yorum satırlarında.
- `.github/pull_request_template.md`: Özet, İlgili iş, Değişiklikler, Nasıl test edildi, Kontrol listesi (REQUIREMENTS ve rapor kutucukları dahil).
- `.github/ISSUE_TEMPLATE/`: `task.md` (görev), `bug.md` (hata), `config.yml` (boş issue kapalı).
- `docs/REQUIREMENTS.md`: 38 FR, 18 BR, 11 NFR ve T1–T5 tek tabloda; hepsi ⬜, PR sütunu boş.
**Validator'a koyduğum kurallar:**
- Yok (kod yok)
**Handler'daki kurallar ve transaction:**
- Yok (kod yok)
**Neden böyle yaptım (tasarım kararları):**
- Gereksinim kodları commit başlığından çıkarılıp `Refs` satırına alındı; başlık kod listesini bilmeyen biri için de anlaşılır olmalı.
- `REQUIREMENTS.md` satırları yalnızca PR birleşince değişiyor; böylece ekip içi Görev Takibi tablosuyla çelişmiyor.
- Boş issue kapatıldı; her iş görev veya hata şablonuyla açılıyor.
**Testler:**
- Yok (sadece doküman). `.gitmessage` bir deneme reposunda denendi: yorum satırları commit'e girmiyor.
**Karşılaştığım sorun ve çözümü:**
- Yok
**Sunumda anlatacağım tek cümle:**
Commit, branch ve PR kurallarımızı `CONTRIBUTING.md`'ye yazdık; her commit'in sonundaki kodların ne olduğu ve hangi PR ile karşılandığı `docs/REQUIREMENTS.md`'den izleniyor.
**Açık kalanlar:**
- README'deki `docs/REQUIREMENTS.md` linki Zehra'nın README iskeleti PR'ında geliyor.
- Branch koruması ve "Rebase and merge" ayarı repo yöneticisinin yetkisinde; rica edilecek.

## 17 Kasım 2026 — ci(ci): GitHub Actions ile derleme ve testler (#4, dev'e #103 ile)
**İlgili gereksinimler:** NFR-03 (testlerin her PR'da koşması), NFR-04 (iş akışı gizli değer kullanmıyor)
**Use case(ler):** Yok (altyapı)
**Ne yaptım:**
- `.github/workflows/build.yml`: `dev` ve `main`'e açılan her PR'da ve bu branch'lere her birleşmede `./mvnw verify` koşuyor.
- Java 21 (Temurin) kuruluyor, Maven bağımlılıkları önbelleğe alınıyor.
- Test başarısız olursa `target/surefire-reports/` indirilebilir dosya olarak saklanıyor; birleşmelerde WAR dosyası 7 gün saklanıyor.
**Validator'a koyduğum kurallar:**
- Yok (kod yok)
**Handler'daki kurallar ve transaction:**
- Yok
**Neden böyle yaptım (tasarım kararları):**
- Testler Testcontainers ile geçici PostgreSQL açtığı için CI'da Supabase şifresine gerek yok; iş akışı hiçbir secret kullanmıyor ve yetkisi `contents: read` ile sınırlı.
- Aynı branch'e art arda push edilirse eski çalışma iptal ediliyor (`concurrency`).
- SonarQube yerelde çalıştığı için CI'da Sonar adımı yok (plan 10.6).
**Testler:**
- İş akışı `actionlint` ile denetlendi; ilk gerçek çalışma bu PR açıldığında GitHub'da görülecek.
**Karşılaştığım sorun ve çözümü:**
- Yok
**Sunumda anlatacağım tek cümle:**
Her PR birleşmeden önce GitHub Actions'ta derleniyor ve bütün testler gerçek bir PostgreSQL'e karşı koşuyor; kırmızı PR birleşmiyor.
**Açık kalanlar:**
- Branch korumasında "CI yeşil" şartının açılması repo yöneticisinin yetkisinde.

## 22 Kasım 2026 — docs(contributing): katkı kuralları ve CI'ı dev'e taşı (#103)
**İlgili gereksinimler:** NFR-01, NFR-03 (kurallar ve CI ilk kez dev'de)
**Use case(ler):** Yok
**Ne yaptım:**
- #3 ve #4'ün 8 commit'ini güncel `dev`'in üzerine taşıdım; içerik değişmedi.
- `CONTRIBUTING.md`: base'i `dev` olmayan PR birleştirilmez; zincirli PR'da önce rebase ve base değişimi.
- `CONTRIBUTING.md` ve PR şablonu: `dev`'e birleşen PR issue'yu kapatmadığı için issue elle kapatılır.
- Bu rapordaki PR numaralarını ve GitHub kullanıcı adını doldurdum.
**Validator'a koyduğum kurallar:**
- Yok (kod yok)
**Handler'daki kurallar ve transaction:**
- Yok
**Neden böyle yaptım (tasarım kararları):**
- #3 ve #4 zincirli açılmıştı ve base'leri `dev`'e çevrilmeden birleştirildi; değişiklikler önceki branch'lerde kaldı, `dev`'e ulaşmadı. Aynı hata tekrarlanmasın diye kural `CONTRIBUTING.md`'ye yazıldı.
**Testler:**
- Yok (sadece doküman ve CI dosyası). CI iş akışı bu PR'da ilk kez gerçek projede çalışıyor.
**Karşılaştığım sorun ve çözümü:**
- Sorun: kurallar, şablonlar ve CI 5 gün boyunca `dev`'de yoktu; ekip PR şablonunu ve `REQUIREMENTS.md`'yi kullanamadı. Çözüm: commit'ler `dev`'in üzerine yeniden uygulandı ve yeni PR ile birleştirildi.
**Sunumda anlatacağım tek cümle:**
Zincirli PR'larda base'in `dev`'e çevrilmesi unutulunca değişiklik ana dala ulaşmadı; bunu fark edip düzelttik ve kuralı CONTRIBUTING'e ekledik.
**Açık kalanlar:**
- Ekip üyeleri birleşmiş gereksinimlerini sonraki PR'larında `REQUIREMENTS.md`'de ✅ yapacak.
