# Çalışma Raporu: Zehra

- **GitHub:** @Zehranur04
- **Modül:** Bakiye, hak ediş ve geçmiş (`leave-type`, `balance`, `accrual`, `leave-history`) + veritabanı sorumlusu (`db`)
- **Sorumlu olduğum gereksinimler:** FR-12, FR-13, FR-15, FR-26, FR-27, FR-32, FR-33, FR-34, FR-35 · bonus BR-12 · T3
- **Altyapı işleri:** Supabase projesi, SQL scriptleri, JPA entity'leri ve repository'ler, Testcontainers test tabanı, README iskeleti ve birleştirme, demo kaydı
- **Dokümanlar:** `db/README.md`, `DATABASE_DESIGN.md`, `TECHNICAL_DECISIONS.md`

---

## 15–16 Kasım 2026 — db/initial-schema-and-seed-scripts: veritabanı şeması, başlangıç verisi ve güvenlik scriptleri (#1)
**İlgili gereksinimler:** FR-13 · BR-03, BR-04, BR-05, BR-11 · NFR-04, NFR-08
**Use case(ler):** Yok (veritabanı altyapısı)
**Ne yaptım:**
- Supabase'de `employee-vacation-team` organizasyonunu (Free plan) ve `employee-vacation-management` projesini açtım; Sude, Bilge ve Onur'u Developer rolüyle davet ettim. PostgreSQL sürümü 17.11.
- `db/scripts/01_schema.sql`: `leave_mgmt` şeması ve 9 tablo (departments, employees, leave_types, leave_balances, leave_requests, leave_request_history, balance_transactions, public_holidays, annual_accrual_runs); CHECK, tekil ve foreign key kısıtları, indeksler.
- `db/scripts/02_seed_leave_types.sql`: 4 hazır izin türü — ANNUAL, EXCUSE (yıllık 5 gün), SICK, UNPAID (FR-13).
- `db/scripts/03_security.sql`: 9 tabloda Row Level Security.
- Scriptleri 15 Kasım 2026, yaklaşık 23:15–23:20 arasında Supabase SQL Editor'da sırayla çalıştırdım ve her birinden sonra kontrol sorgusuyla doğruladım.
- `db/README.md`: ortam bilgisi, script sırası ve kuralları, çalıştırma kaydı, kontrol sorguları.
- `.github/CODEOWNERS`: `db/scripts/` ve `**/domain/model/` değiştiren PR'lara veritabanı sorumlusu otomatik inceleyici olur.
- Değişiklikleri 5 commit'e böldüm (şema, seed, güvenlik, README, CODEOWNERS); PR Sude'nin onayıyla "Rebase and merge" yöntemiyle `dev`'e birleşti. Bu bölümün raporu bir sonraki PR'ın (#5) rapor commit'iyle eklendi.
**Validator'a koyduğum kurallar:**
- Yok (kod yok)
**Handler'daki kurallar ve transaction:**
- Handler yok. Her script tek transaction içinde (`begin; ... commit;`): bir hata olursa script hiç uygulanmamış sayılır, veritabanı yarım kurulmaz.
**Neden böyle yaptım (tasarım kararları):**
- Tablolar `public` yerine ayrı `leave_mgmt` şemasında; bu şema Supabase Data API'nin dışa açtığı şemalar arasında değil. Ek olarak 03 ile tüm tablolarda politikasız RLS açık; böylece Data API rolleri hiçbir satırı göremez, uygulama ise tablo sahibi kullanıcıyla bağlandığı için etkilenmez.
- Scriptler tekrar çalıştırılabilir (`create ... if not exists`, `on conflict do nothing`), çünkü aynı dosyalar Testcontainers ve yerel veritabanında da her açılışta çalışacak.
- Kısıtlar isimlendirildi (`uq_`, `ck_`, `fk_`, `ix_`); hata mesajında hangi kuralın ihlal edildiği okunabiliyor.
- E-posta tekilliği `lower(email)` üzerinde unique index ile; büyük-küçük harf farkı aynı kişi sayılıyor.
- `ck_leave_balances_used_days` (BR-05): Handler bakiye kontrolünü kaçırsa bile veritabanı negatif bakiyeyi reddeder; rollback senaryosu D'nin dayanağı.
- `ck_leave_requests_status`: planda yoktu, ekledim; durum değerleri Plan 6.1'deki 7 değerle sınırlı. Bedeli: yeni bir durum yeni bir script gerektirir.
- `employees.department_id` zorunlu (İK kullanıcıları da bir departmana bağlı), `leave_requests.reason` boş olabilir, not alanları `varchar(500)`.
- Foreign key'lerde `on delete cascade` yok: sistemde silme değil pasife alma var; denetim kaydı kaybolmamalı.
- `version` sütunları iyimser kilit içindir (NFR-08).
**Testler:**
- Kontrol sorguları (Supabase): 9 tablo; 9 primary key, 14 foreign key, 8 check, 6 unique kısıt; 21 index; 4 izin türü; 9 tabloda RLS açık.
- Scriptler yerel bir test PostgreSQL'inde iki kez arka arkaya çalıştırıldı, ikincisi hatasız bitti (tekrar çalıştırılabilirlik).
- Aynı test veritabanında kısıtlar denendi: aynı e-postanın farklı harf büyüklüğüyle eklenmesi, geçersiz durum değeri (`'APROVED'`) ve kullanılan günün hak edilen günü aşması reddedildi.
- Security Advisor: uyarı (WARN/ERROR) yok; 9 tabloda INFO seviyesinde "RLS Enabled No Policy" var, bu bilinçli tercih.
**Karşılaştığım sorun ve çözümü:**
- Proje oluşturulurken Data API kapatılamadı → tablolar Data API'ye açık olmayan `leave_mgmt` şemasına kuruldu ve RLS açıldı.
- SQL Editor "RLS olmadan tablo oluşturuluyor" uyarısı verdi → "Run without RLS" seçtim, çünkü RLS bilinçli olarak 03 ile açılıyor ve Supabase'de çalışan içerik repodaki dosyanın birebir aynısı olmalı; 03 birkaç dakika içinde çalıştırıldı.
- Bölge "Auto" bırakıldığı için proje `ap-northeast-2` (Seul) bölgesinde oluştu → yeniden kurmamaya karar verdim ve `db/README.md`'ye not düştüm; gecikme nedeniyle sorgu incelemesi (N+1) daha önemli hale geldi.
- Git e-postası ilk denemede yanlış yazıldı → commit atılmadan GitHub noreply adresiyle düzeltildi.
- Claude masaüstü uygulamasının "Claude outputs" klasörü repoda göründü → silindi ve yalnızca yerelde geçerli `.git/info/exclude` dosyasına eklendi.
**Sunumda anlatacağım tek cümle:**
Veritabanı önce SQL scriptleriyle tasarlandı; aynı üç script Supabase'de, testlerde ve yerel yedekte birebir aynı şemayı kuruyor ve bakiye gibi kritik kurallar veritabanında da kısıtla korunuyor.
## 18 Kasım 2026 — feat(db): JPA entity'leri, repository'ler ve test veritabanı kurulumu (#5)
**İlgili gereksinimler:** FR-03, FR-06, FR-10, FR-12, FR-13, FR-14, FR-27, FR-32, FR-35, FR-38 · BR-05, BR-11 · NFR-04, NFR-08
**Use case(ler):** Yok (veritabanı altyapısı)
**Ne yaptım:**
- Geliştirme ortamını tamamladım: `JAVA_HOME` (JDK 21) kalıcı olarak ayarlandı, Docker Desktop çalıştırıldı; değişiklikten önce `./mvnw verify` ile Onur'un iskeletinin 3 testinin geçtiğini doğruladım.
- `domain/model`: 9 entity — `Department`, `Employee`, `LeaveType`, `LeaveBalance`, `LeaveRequest`, `LeaveRequestHistory`, `BalanceTransaction`, `PublicHoliday`, `AnnualAccrualRun` — ve ortak `BaseEntity` (`@MappedSuperclass`: `id`, `createdAt`, `updatedAt`).
- `domain/model/enums`: `Role`, `LeaveRequestStatus` (7 durum), `BalanceTransactionType`.
- `infrastructure/config/JpaAuditingConfig`: `created_at` / `updated_at` alanları `Clock` bean'inden otomatik doluyor.
- `infrastructure/persistence`: 9 `JpaRepository` arayüzü.
- `TestcontainersConfiguration`: `db/scripts` konteynerin `/docker-entrypoint-initdb.d/` klasörüne kopyalanıyor; test veritabanı Supabase'deki scriptlerin aynısıyla kuruluyor.
- Değişiklikleri 8 commit'e böldüm: test veritabanı, BaseEntity ve auditing, enum'lar, üç grup entity, repository'ler, test.
**Validator'a koyduğum kurallar:**
- Yok (use case yok)
**Handler'daki kurallar ve transaction:**
- Handler yok. Entity'lerde `@Version` ile iyimser kilit: `Employee`, `LeaveBalance`, `LeaveRequest` (NFR-08).
**Neden böyle yaptım (tasarım kararları):**
- Entity'ler planda yazan IntelliJ "Create JPA Entities from DB" yerine `01_schema.sql`'e bakılarak VS Code'da elle yazıldı: üretilen kodun büyük kısmı zaten temizlenecekti (enum, `@Version`, `BaseEntity`, gereksiz koleksiyonlar). Şemayla uyumu `ddl-auto: validate` denetliyor.
- Zaman damgaları Spring Data JPA auditing ile ve uygulamanın `Clock` bean'inden; testlerde "şimdi" sabitlenebilir. Ayar ana sınıfa değil ayrı bir yapılandırma sınıfına kondu, böylece web katmanı testleri JPA olmadan açılabilir.
- Enum'lar `@Enumerated(EnumType.STRING)`: veritabanında sıra numarası değil ad saklanıyor, enum sırası değişse bile veri bozulmuyor.
- Bütün ilişkiler `@ManyToOne(fetch = LAZY)` ve tek yönlü; hiçbir entity'de liste alanı yok. İlişkili kayıt yalnızca kullanıldığında yüklenir.
- Setter politikası: `id`, `version` ve zaman damgalarının setter'ı yok; `LeaveType.code` değiştirilemez (`updatable = false`); sadece eklenen tablolarda (geçmiş, hareket, hak ediş kaydı) hiç setter yok; `LeaveRequest` durum alanları Bilge'nin durum geçiş metotlarına, `LeaveBalance` gün alanları `LeaveBalanceService`'e bırakıldı.
- `year` sütunları `SMALLINT`; Java'da `Integer` + `@JdbcTypeCode(SqlTypes.SMALLINT)`, kodda tip dönüşümü gerekmiyor.
- `equals`/`hashCode` `BaseEntity`'de: aynı türde ve aynı id'ye sahip kayıtlar eşit; Hibernate proxy'leri `Hibernate.getClass` ile doğru karşılaştırılıyor.
- Test veritabanı ayarı bu PR'a alındı: entity'ler eklenince `validate` test veritabanında da tabloları aradığı için, ayar olmadan mevcut `contextLoads` testi bozulurdu.
- PR ~400 satır sınırını aşıyor (yorumlar hariç ~750); ekip entity'leri beklediği için tek PR açıldı, commit commit incelenebilir.
**Testler:**
- `PersistenceMappingTest.seedScriptLoadsFourLeaveTypes` — test veritabanında 4 hazır izin türünün yüklü olduğunu, yani scriptlerin çalıştığını doğruluyor.
- `PersistenceMappingTest.savingEntityFillsIdAndTimestamps` — kaydedilen departmanda `id`, `createdAt`, `updatedAt` alanlarının otomatik dolduğunu doğruluyor.
- `./mvnw verify`: 5 test, 0 hata (3 mevcut + 2 yeni).
- `java -jar target/employee-vacation-management.war --spring.profiles.active=dev`: uygulama Supabase'e bağlandı, `ddl-auto: validate` 9 entity'yi tablolarla uyumlu buldu, 9 repository yüklendi.
**Karşılaştığım sorun ve çözümü:**
- `JAVA_HOME` tanımlı değildi → kullanıcı ortam değişkeni olarak ayarlandı, VS Code yeniden başlatıldı.
- Docker Desktop kapalıydı ("cannot find the file specified") → başlatıldı; Testcontainers çalışan bir Docker gerektiriyor.
- Supabase'e ilk bağlantıda "password authentication failed" → `.env` dosyasındaki bağlantı bilgisi düzeltildi.
- Bilgisayarın dil ayarı `tr_TR`: `"i".toUpperCase()` Türkçe'de `"İ"` döndürür → kodda büyük-küçük harf dönüşümlerinde `Locale.ROOT` kullanılacak.
**Sunumda anlatacağım tek cümle:**
Entity'ler veritabanı şemasından yazıldı ve uygulama her açılışta `ddl-auto: validate` ile Java sınıflarının tablolarla birebir uyuştuğunu kendisi kontrol ediyor.

## 18 Kasım 2026 — test(db): entegrasyon test tabanı ve test verisi oluşturucuları (#6)
**İlgili gereksinimler:** T1–T5 · FR-13, FR-32 · NFR-08
**Use case(ler):** Yok (test altyapısı)
**Ne yaptım:**
- `AbstractIntegrationTest`: veritabanına giden bütün entegrasyon testlerinin ortak tabanı. Her testten önce 9 tabloyu `TRUNCATE ... RESTART IDENTITY CASCADE` ile boşaltıp `02_seed_leave_types.sql`'i yeniden çalıştırıyor; `leaveType("ANNUAL")` ve `countRows("tablo")` yardımcılarını sağlıyor.
- `support/TestData`: test verisi oluşturucuları — `department()`, `employee(departman, rol)`, `balance(çalışan, tür, yıl, gün)`, `leaveRequest(...)`. Departman adı ve e-posta her çağrıda farklı üretiliyor.
- `DatabaseResetTest`: sıfırlamanın çalıştığını kanıtlayan 4 test.
- `LeaveTypeRepository.findByCode`: izin türünü değişmeyen koduyla bulan türetilmiş sorgu.
- `PersistenceMappingTest`'i yeni tabana taşıdım; elle yazılmış `try/finally` temizliği kalktı, çalışan ve bakiye kaydını deneyen bir test ekledim.
- Değişiklikleri 4 commit'e böldüm; PR 266 satır, plandaki sınırın altında.
**Validator'a koyduğum kurallar:**
- Yok (use case yok)
**Handler'daki kurallar ve transaction:**
- Handler yok. Test sınıflarına bilerek `@Transactional` konmadı: test kendi transaction'ında koşsaydı her şey test sonunda zaten geri alınır, rollback testleri "veritabanında iz kaldı mı?" sorusunu ölçemezdi. Bu yüzden ölçüm `countRows` ile doğrudan veritabanına soruluyor.
**Neden böyle yaptım (tasarım kararları):**
- H2 yerine gerçek PostgreSQL (Testcontainers): şema PostgreSQL'e özgü özellikler kullanıyor (`leave_mgmt` şeması, `lower(email)` üzerinde tekil index, CHECK kısıtları, `timestamptz`, RLS); H2'de bu scriptler aynen çalışmaz ve rollback testleri gerçek veritabanının davranışını ölçmemiş olurdu. Testler Supabase'deki scriptlerin aynısıyla kurulan PostgreSQL 17'ye gidiyor.
- Temizlik testten sonra değil önce (`@BeforeEach`): önceki bir test yarıda çökse bile her test temiz başlıyor.
- Hazır izin türleri her testten önce yeniden yükleniyor: bir test izin türünü değiştirse bile sonraki test aynı 4 türle ve aynı id'lerle (1–4) başlıyor.
- `TestData` kaydedilmemiş nesne döndürüyor: aynı oluşturucular veritabanlı testlerde repository ile kaydediliyor, Mockito'lu birim testlerinde doğrudan kullanılabiliyor.
- Bütün entegrasyon testleri aynı ayarlarla (`@SpringBootTest`, `test` profili, `TestcontainersConfiguration`) çalıştığı için tek Spring bağlamını ve tek konteyneri paylaşıyor; testler hızlı kalıyor.
- Diğerlerinin kullanımı:
  `class ApproveRollbackIT extends AbstractIntegrationTest { ... employees.save(TestData.employee(departments.save(TestData.department()))); ... assertThat(countRows("leave_request_history")).isZero(); }`
**Testler:**
- `DatabaseResetTest.firstTestSeesOnlyItsOwnDepartment` / `secondTestSeesOnlyItsOwnDepartment` — iki test aynı departmanı ekleyip tabloda 1 satır görüyor; sıfırlama çalışmasaydı ikinci çalışan 2 satır görürdü.
- `DatabaseResetTest.renamingASeedLeaveTypeAffectsOnlyThisTest` / `seedLeaveTypesStartFromTheirOriginalState` — bir testte değiştirilen izin türü sonraki testte özgün adıyla ve id 1 ile geri geliyor.
- `PersistenceMappingTest.balanceIsSavedWithItsEmployeeAndRemainingDaysAreCalculated` — çalışan ve bakiye kaydediliyor, kalan gün 14 olarak hesaplanıyor.
- `./mvnw verify`: 10 test, 0 hata (3 mevcut + 3 eşleme + 4 sıfırlama).
**Karşılaştığım sorun ve çözümü:**
- Yok.
**Sunumda anlatacağım tek cümle:**
Testler H2 gibi taklit bir veritabanında değil, Supabase'deki scriptlerle kurulan gerçek PostgreSQL'de koşuyor ve her test temiz bir veritabanıyla başlıyor; bu yüzden rollback testleri gerçek davranışı ölçüyor.

## 18 Kasım 2026 — docs(readme): 15 başlıklı README iskeleti (#7)
**İlgili gereksinimler:** Kriter 3 (Kapsamlı README.md)
**Use case(ler):** Yok (doküman)
**Ne yaptım:**
- Kök dizindeki 2 satırlık `README.md`'yi, proje boyunca doldurulacak 15 başlıklı iskeletle değiştirdim.
- En üste proje tanımını ve "Gereksinimler ve ilerleme → `docs/REQUIREMENTS.md`" bağlantısını koydum (Plan 10.8).
- Her başlığın altına o bölüme ne yazılacağını ve ilgili dokümanı (`ARCHITECTURE.md`, `DATABASE_DESIGN.md`, `API_DOCUMENTATION.md`, `TEST_PLAN.md`, `DEPLOYMENT.md`, `TECHNICAL_DECISIONS.md`, `db/README.md`) yazdım.
- Ekip tablosuna dört üyenin GitHub kullanıcı adını ve modülünü ekledim; kullanıcı adlarını commit geçmişinden ve PR inceleyici listesinden doğruladım.
**Validator'a koyduğum kurallar:**
- Yok (kod yok)
**Handler'daki kurallar ve transaction:**
- Yok
**Neden böyle yaptım (tasarım kararları):**
- Başlıkların 11'i analiz dokümanındaki README şablonundan (bölüm 16.1): Özellikler, Kullanılan Teknolojiler, Mimari, Veritabanı Şeması, Kurulum ve Çalıştırma, Demo Kullanıcıları, API Dokümantasyonu, Transaction Senaryoları, Testleri Çalıştırma, SOLID Uygulaması, Ekip.
- Üçü plandan: SonarQube Analysis (her taramanın tablosu), Team Contributions (kişisel raporların özeti), Ders Kriterleri (her kriterin kanıtına bağlantı). On beşincisi Teknik Kararlar: hocanın SOAP/gRPC sorusunun cevabı olan `TECHNICAL_DECISIONS.md`'ye bağlanıyor. Böylece hocanın README'de arayacağı her kriterin bir başlığı var.
- Şablondaki SonarCloud rozetleri konmadı, çünkü plan SonarCloud yerine yerel SonarQube kullanıyor.
- Ekip tablosunda e-posta yok, yalnızca GitHub kullanıcı adı var: repo herkese açık.
**Testler:**
- Gerekmez (sadece doküman); `git diff --stat` ile yalnızca `README.md`'nin değiştiği doğrulandı.
**Karşılaştığım sorun ve çözümü:**
- Görev Takibi'nde bu madde yanlışlıkla "Bitti" işaretlenmişti, repoda iskelet yoktu → görev yapıldı ve durum PR'a göre güncellendi.
- Dosya kopyalanırken hedef olarak yanlış ad (`README..md`) verildi, repoda ikinci bir dosya oluştu → fazla dosya silindi, içerik asıl `README.md`'nin üzerine kopyalandı.
**Sunumda anlatacağım tek cümle:**
README, hocanın ders kriterlerinin her biri için bir başlıkla ilk günden repoda; her ekip üyesi kendi bölümünü proje ilerledikçe dolduruyor.

## 18 Kasım 2026 — feat(working-day): iş günü hesabı ve sözleşme testleri (#8)
**İlgili gereksinimler:** FR-15 · BR-01
**Use case(ler):** Yok (alan bileşeni; `SubmitLeave` use case'i tarafından kullanılacak)
**Ne yaptım:**
- `domain/policy/WorkingDayCalculator`: iki tarih arasındaki iş günü sayısını veren arayüz, `int count(LocalDate startDate, LocalDate endDate)`.
- `domain/policy/WeekdayWorkingDayCalculator` (`@Component`): başlangıç ve bitiş dahil Pazartesi–Cuma günlerini sayıyor; bitiş başlangıçtan önceyse `IllegalArgumentException`, tarih boşsa `NullPointerException` fırlatıyor.
- `WorkingDayCalculatorContractTest`: her uygulamanın geçmesi gereken ortak testleri içeren soyut test sınıfı; `WeekdayWorkingDayCalculatorTest` bu sınıftan türüyor.
- Değişiklikleri 2 commit'e böldüm (uygulama, testler).
**Validator'a koyduğum kurallar:**
- Yok (use case yok). Bitişin başlangıçtan önce olmaması kullanıcıya 400 dönecek şekilde `SubmitLeave` Validator'ında kontrol edilecek; bileşendeki kontrol ikinci savunma hattı.
**Handler'daki kurallar ve transaction:**
- Yok. Bileşen veritabanına gitmiyor, saf Java hesaplaması.
**Neden böyle yaptım (tasarım kararları):**
- Arayüz + uygulama: talep açan kod somut sınıfa değil arayüze bağlanıyor (DIP). Resmi tatilleri de düşen bonus hesap (FR-38) aynı arayüzü uygulayıp eskisinin yerine geçebiliyor; mevcut kod değişmiyor (OCP).
- Soyut sözleşme testi (LSP): testler arayüz için bir kez yazıldı; her yeni uygulama bu sınıftan türeyen bir testle aynı 8 testten geçmek zorunda. Bu, "alt tür üst türün yerine sorunsuz kullanılabilir" ilkesinin testle gösterilmesi.
- `isWorkingDay` metodu `protected`: tatil hesabı isterse bu sınıftan türeyip yalnızca bu kuralı genişletebiliyor.
- Dönüş tipi `int`: iş günü her zaman tam sayı; veritabanındaki `DECIMAL(4,1)` alana çağıran taraf `BigDecimal` olarak yazıyor.
- Gün gün sayma (`datesUntil`): bir talep en fazla 30 iş günü (BR-06); formülle hesaplamaya göre okunması ve tatil kuralının eklenmesi daha kolay.
**Testler:**
- `WorkingDayCalculatorContractTest.countsWorkingDaysIncludingStartAndEnd` (6 durum, `@ParameterizedTest`): aynı gün hafta içi → 1, Pazartesi–Cuma → 5, Cuma–Pazartesi → 2, Cumartesi–Pazar → 0, aynı gün Cumartesi → 0, iki tam hafta → 10.
- `rejectsEndDateBeforeStartDate` — bitiş başlangıçtan önceyse hata.
- `rejectsMissingDates` — boş tarih kabul edilmiyor.
- `./mvnw verify`: 18 test, 0 hata (8 yeni).
**Karşılaştığım sorun ve çözümü:**
- Yok.
**Sunumda anlatacağım tek cümle:**
İş günü hesabı bir arayüzün arkasında; resmi tatilleri de düşen bonus hesap aynı sözleşme testlerinden geçerek mevcut koda dokunmadan yerine takılabiliyor.

## 20 Kasım 2026 — feat(entitlement): kıdem ve yaşa göre yıllık izin hakkı (#96)
**İlgili gereksinimler:** FR-33 · T3, T4 (kullanacak olanlar)
**Use case(ler):** Yok (alan bileşeni; `CreateEmployee` (T4) ve `RunAnnualAccrual` (T3) tarafından kullanılacak)
**Ne yaptım:**
- `domain/policy/EntitlementPolicy`: verilen yıl için yıllık izin gününü veren arayüz, `int annualLeaveDays(LocalDate hireDate, LocalDate birthDate, int year)`.
- `domain/policy/SeniorityAndAgeEntitlementPolicy` (`@Component`): gereksinim dokümanı 4.1'deki tablo. 1 yıldan az kıdem → hak yok (0), 1–5 yıl → 14, 6–14 yıl → 20, 15 yıl ve üstü → 26 gün; o yıl 18 yaşında veya küçük ya da 50 yaşında veya büyük olanlarda sonuç 20 günden az olamaz.
- Hak ediş yılı işe giriş yılından önceyse `IllegalArgumentException`, tarih boşsa `NullPointerException`.
- Değişiklikleri 2 commit'e böldüm (uygulama, testler).
**Validator'a koyduğum kurallar:**
- Yok (use case yok).
**Handler'daki kurallar ve transaction:**
- Yok. Bileşen veritabanına gitmiyor; sonucu bakiyeye yazmak T3 ve T4 Handler'larının transaction'ı içinde yapılacak.
**Neden böyle yaptım (tasarım kararları):**
- Kıdem ve yaş, hak ediş yılından işe giriş ve doğum yılı çıkarılarak bulunuyor; ay ve gün dikkate alınmıyor. Dokümandaki örnek böyle (2027 hak edişinde 2021'de giren biri 6 yıl). Bu, 4857 sayılı İş Kanunu'nun 53. maddesinin projeye özel basitleştirilmiş hâli; sunumda bilinçli tercih olarak anlatılacak.
- Yaş kuralı yalnızca hakkı olan çalışana uygulanıyor: 1 yılını doldurmamış çalışanın yaşı ne olursa olsun hakkı yok. Dokümanda "yukarıdaki değer 20'den azsa 20 gün" deniyor; kanunda da önce en az 1 yıl çalışma şartı geliyor, yaş kuralı yalnızca alt sınırı belirliyor.
- Arayüz + uygulama: hakkı kullanan kod somut sınıfa değil arayüze bağlanıyor (DIP). Kural değişirse (örneğin şirkete özel ek gün) yeni bir uygulama yazılıp yerine takılabiliyor; T3 ve T4 değişmiyor (OCP).
- Parametre olarak `Employee` yerine tarihler: kural yalnızca iki tarihe bakıyor. Entity'ye bağlansaydı test için her durumda çalışan, departman ve rol kurmak gerekirdi; tarihlerle dokümandaki tablo satır satır test edilebiliyor.
- Dönüş tipi `int`: kural tam gün üretiyor; bakiyedeki `numeric(4,1)` alana çağıran taraf `BigDecimal.valueOf(gün)` ile yazıyor.
- Kademe ve yaş sınırları isimli sabitler (`JUNIOR_DAYS`, `OLDER_MIN_AGE` gibi): kod dokümandaki tabloyla eşleşiyor ve SonarQube'un "magic number" uyarısı çıkmıyor.
**Testler:**
- `SeniorityAndAgeEntitlementPolicyTest.calculatesAnnualLeaveDaysBySeniorityAndAge` (14 durum, `@ParameterizedTest` + `@CsvSource`, hak ediş yılı 2027):
  - Dokümandaki zorunlu örnekler: 0 yıl → 0, 5 yıl → 14, 6 yıl → 20, 15 yıl → 26, 51 yaş + 2 yıl → 20.
  - Sınırlar: 14 ve 15 yıl; 17, 18 ve 19 yaş; 49 ve 50 yaş; 31 Aralık'ta işe giren 1 yıl sayılıyor; 52 yaş + 27 yıl → 26 (yaş kuralı hakkı düşürmüyor); 57 yaş + 0 yıl → 0.
- `rejectsYearBeforeHireYear` — hak ediş yılı işe giriş yılından önceyse hata, mesajda iki yıl da yazıyor.
- `rejectsMissingDates` — boş tarih kabul edilmiyor.
- `./mvnw verify`: 126 test, 0 hata (16 yeni).
**Karşılaştığım sorun ve çözümü:**
- Dokümandaki yaş kuralı, 1 yılını doldurmamış 50 yaş üstü çalışan için açık değildi → kanundaki sıraya göre yorumlandı, kararı bir test satırıyla sabitledim ve PR açıklamasına yazdım.
**Sunumda anlatacağım tek cümle:**
Yıllık izin hakkı kanundaki kademelerin basitleştirilmiş hâliyle tek bir politika sınıfında hesaplanıyor; kural değişirse yeni bir politika takılıyor, çalışan ekleme ve yıllık hak ediş kodu değişmiyor.

## 20 Kasım 2026 — feat(balance): bakiye servisi ve hareket kaydı (#97)
**İlgili gereksinimler:** FR-32, FR-35 · BR-03, BR-05 · T1, T2, T3, T4 (kullanacak olanlar) · Issue #33
**Use case(ler):** Yok (alan servisi; `SubmitLeave`, `ApproveLeave` (T1), `CancelLeave` ve `ApproveLeaveCancellation` (T2), `CreateEmployee` (T4) ve `RunAnnualAccrual` (T3) tarafından kullanılacak)
**Ne yaptım:**
- `domain/model/LeaveBalance`: `requireAvailable(gün)`, `use(gün)` ve `restore(gün)` metotları. Yetmeyen bakiyede "Yetersiz izin bakiyesi: talep 5 gün, kalan 3 gün." mesajıyla `InsufficientBalanceException` (422); kullanılandan fazla iadede `IllegalStateException`. Gün alanlarına setter eklenmedi.
- `domain/service/LeaveBalanceService` (`@Service`):
  - `checkSufficient(çalışan, tür, yıl, gün)`: talep açılırken bakiye kontrolü, hiçbir şey yazmaz.
  - `deduct(talep, işlemi yapan)`: onayda kullanılan günü artırır, DEDUCTION hareketi yazar.
  - `refund(talep, işlemi yapan)`: iptalde günü iade eder, REFUND hareketi yazar.
  - `openBalance(çalışan, tür, yıl, hak, işlemi yapan)`: yeni çalışan ve yeni yıl için bakiye açar, gün > 0 ise ACCRUAL hareketi yazar; aynı yıl için ikinci bakiye `DUPLICATE_VALUE` (409).
- `LeaveBalanceRepository`: `findByEmployeeAndLeaveTypeAndYear` ve `existsByEmployeeAndLeaveTypeAndYear` (metot adından üretilen sorgular).
- İmzaları PR'dan önce Bilge ve Sude'ye ilettim; planda öngörülen imza görüşmesi yapılmamıştı.
- Değişiklikleri 3 commit'e böldüm (entity kuralları, servis, testler); PR şablonuna göre açıklama yazdım, `Closes #33` ve `modul:bakiye-gecmis` etiketi ekledim.
**Validator'a koyduğum kurallar:**
- Yok (use case yok).
**Handler'daki kurallar ve transaction:**
- Servis kendi transaction'ını açmaz: `@Transactional(propagation = MANDATORY)`. Çağıran Handler'ın transaction'ına katılır; transaction yoksa Spring hemen `IllegalTransactionStateException` fırlatır. Böylece bakiye düşümü ile talebin durum değişikliği ayrı ayrı kaydedilemez (T1, T2).
- Servisin fırlattığı hata Handler'ın transaction'ını geri aldırır: yetersiz bakiyede ne bakiye ne hareket ne de talep durumu değişir (rollback senaryosu A'nın dayanağı).
**Neden böyle yaptım (tasarım kararları):**
- `MANDATORY`, `REQUIRED` yerine: ilk önerimde `REQUIRED` vardı; planın katman tablosundaki "alan katmanı kendi transaction'ını açmaz" kuralına göre düzelttim. `REQUIRED` transaction yoksa yenisini açardı; `@Transactional` unutulmuş bir Handler'da bakiye düşer ama talep PENDING kalabilirdi. `MANDATORY` bu hatayı sessizce kabul etmek yerine ilk testte gösteriyor.
- BR-05 entity'de (zengin alan modeli): bakiyenin negatife düşmemesi bakiyenin kendi kuralı; `LeaveBalance`'ı hangi kod değiştirirse değiştirsin kontrolü atlayamıyor. Veritabanındaki `ck_leave_balances_used_days` kısıtı üçüncü savunma hattı.
- Bakiyeden düşmeyen türlerde (SICK, UNPAID; BR-03) `checkSufficient`, `deduct` ve `refund` hiçbir şey yapmıyor: kural tek yerde, çağıranlar tür kontrolü yazmıyor.
- `deduct` ve `refund` yılı ve günü talepten alıyor (`startDate`'in yılı, `workingDays`): çağıranın yanlış yıl veya gün vermesi mümkün değil.
- Hareket notu otomatik ("İzin talebi #12 onaylandı", "İzin talebi #12 iptal edildi", "2026 yılı hak edişi"): FR-35'teki "neden" her kayıtta aynı biçimde.
- 0 günlük hakta bakiye satırı açılıyor ama hareket yazılmıyor: hareket tablosunda `days > 0` kısıtı var ve kıdemi 0 olan yeni çalışanın yıllık hakkı 0.
- Arayüz yerine tek sınıf: ikinci bir uygulaması olmayacak; plan da `domain/service` altında tek sınıf tanımlıyor. Mockito sınıfları da taklit edebildiği için Bilge ve Sude'nin testleri etkilenmiyor.
**Testler:**
- `LeaveBalanceTest` (10 birim testi, veritabanı yok): düşüm ve iade, bakiyenin tamamını kullanma, yetersiz bakiye ve mesajı, fazla iade, 0/negatif/boş gün.
- `LeaveBalanceServiceTest` (11 entegrasyon testi, Testcontainers PostgreSQL): her çağrı Handler'ın yapacağı gibi `TransactionTemplate` içinde; sonuç transaction kapandıktan sonra doğrudan tablodan okunuyor.
  - Hareket tipi, gün, not, talep numarası ve "kim" sütunu doğru yazılıyor.
  - Yetersiz bakiyede bakiye ve hareket tablosu hiç değişmiyor (rollback).
  - Bakiye yoksa `LEAVE_BALANCE_NOT_FOUND`; aynı yıl ikinci bakiye `DUPLICATE_VALUE`; SICK ve UNPAID'de hiçbir şey yazılmıyor.
  - Transaction olmadan çağrı `IllegalTransactionStateException` ile reddediliyor.
- `./mvnw verify`: 147 test, 0 hata (21 yeni).
**Karşılaştığım sorun ve çözümü:**
- İlk önerideki `REQUIRED` planın katman kuralına uymuyordu → kod yazılmadan önce `MANDATORY` olarak düzeltildi ve kullananlara "Handler `@Transactional` olmalı" notu iletildi.
- PR şablonu ve `CONTRIBUTING.md` henüz birleşmemiş bir branch'te olduğu için önceki PR'larda kullanılmamıştı → bu PR'dan itibaren şablon, `Closes #` satırı ve modül etiketi kullanılıyor. Önceki PR'lara (#1, #5, #6, #7, #8, #96) etiketler sonradan eklendi; biten issue'lar (#9, #10, #17, #21, #26, #27) hangi PR ile tamamlandıkları yazılarak kapatıldı.
- `Closes #33` issue'yu otomatik kapatmadı, çünkü GitHub bu anahtar kelimeyi yalnızca ana dala (`main`) birleşen PR'larda işletiyor → #33 PR numarası yazılarak elle kapatıldı.
- PR, rapor commit'i eklenmeden birleşti → bu bölüm bir sonraki PR'ın rapor commit'iyle eklendi.
**Sunumda anlatacağım tek cümle:**
Bakiye yalnızca tek bir servisten ve her seferinde hareket kaydıyla değişiyor; servis kendi transaction'ını açmadığı için onay ya da iptal yarıda kalırsa bakiye de geri alınıyor.

## 20 Kasım 2026 — feat(leave-history): izin durum geçmişi servisi (#98)
**İlgili gereksinimler:** FR-27 · BR-16 · Issue #41
**Use case(ler):** Yok (alan servisi; onay, red, geri çekme ve iptal Handler'ları ile gece görevi (T5) tarafından kullanılacak)
**Ne yaptım:**
- `domain/service/LeaveHistoryService.record(talep, önceki durum, işlemi yapan, not)`: talebin şu anki durumuna geçişi `leave_request_history` tablosuna yazıyor ve kaydedilen satırı döndürüyor.
  - Yeni durum parametre olarak verilmiyor, talebin o anki durumundan okunuyor.
  - Önceki durum boşsa talep yeni açılmıştır (boş → PENDING); işlemi yapan boşsa işlem sistemindir (gece görevi).
  - Durum değişmemişse, yeni olmayan bir talepte önceki durum boşsa ya da not 500 karakteri aşarsa `IllegalArgumentException`.
  - Not baştaki ve sondaki boşluklardan arındırılıyor; boş not `null` olarak saklanıyor.
  - Zaman `Clock` bean'inden alınıyor (`OffsetDateTime.now(clock)`).
- Handler'da kullanım örneğini javadoc'a ve PR açıklamasına yazdım: geçiş metodundan önce eski durum alınır, geçişten sonra servis çağrılır.
- Değişiklikleri 2 commit'e böldüm (servis, testler); commit'lerin `Refs` satırına ilk kez issue numarasını (#41) yazdım. PR'ı rapor commit'i eklenmeden birleşmesin diye taslak (draft) olarak açtım.
**Validator'a koyduğum kurallar:**
- Yok (use case yok).
**Handler'daki kurallar ve transaction:**
- Servis kendi transaction'ını açmıyor (`@Transactional(propagation = MANDATORY)`), bakiye servisiyle aynı kural. Handler'ın transaction'ına katılıyor; transaction yoksa `IllegalTransactionStateException`.
- Kayıt `saveAndFlush` ile yazılıyor: INSERT o anda veritabanına gidiyor. Geçmiş yazılamazsa hata Handler'ın transaction'ını geri aldırıyor ve durum değişikliği de kayboluyor (BR-16).
**Neden böyle yaptım (tasarım kararları):**
- Yeni durumun talepten okunması: çağıranın yanlış "yeni durum" yazması imkânsız; Bilge'nin durum geçiş metotlarından sonra çağrılma düzenine de uyuyor.
- `save` yerine `saveAndFlush`: JPA normalde INSERT'leri transaction sonuna kadar bekletiyor. Plan 8.1'deki rollback senaryosu B'nin anlamlı olması için veritabanına gerçekten bir INSERT gitmiş ve geri alınmış olmalı; `saveAndFlush` bunu sağlıyor. Bedeli: her kayıtta veritabanına hemen bir istek gidiyor; talep başına bir satır olduğu için önemsiz.
- Önceki durumun boş olabilmesi: tablo buna izin veriyor (`from_status` boş olabilir); talebin açılışı da geçmişte görünürse İK'nın şirket izin geçmişi aramasında (#52) talebin tüm hayatı izlenebiliyor.
- İşlemi yapanın boş olabilmesi: gece görevi bir kişi değil; `actor_id` sütunu boş olabiliyor.
- Durum değişmemişse hata: geçiş metodu çağrılmadan geçmiş yazılmasını, yani bir programlama hatasını hemen yakalıyor.
**Testler:**
- `LeaveHistoryServiceTest` (8 birim testi, Mockito ile sahte repository ve `Clock.fixed` ile sabit saat; projede ilk Mockito kullanımı): geçişin tüm alanlarıyla yazılması ve `saveAndFlush` çağrısı, yeni talep, sistem işlemi, not temizleme, uzun not, durumun değişmemesi, yeni olmayan talepte boş önceki durum, boş talep.
- `LeaveHistoryServiceIntegrationTest` (4 test, Testcontainers PostgreSQL):
  - Satırın talep, önceki/yeni durum, işlemi yapan, not ve zaman sütunları doğru yazılıyor.
  - INSERT transaction bitmeden veritabanında görünüyor (`saveAndFlush`'ın kanıtı).
  - Geçmiş yazıldıktan sonra sonraki adım hata verirse geçmiş satırı kalmıyor (BR-16).
  - Transaction olmadan çağrı reddediliyor.
- `./mvnw verify`: 159 test, 0 hata (12 yeni).
**Karşılaştığım sorun ve çözümü:**
- Önceki PR (#97) rapor commit'i eklenmeden birleşmişti → bu PR taslak (draft) olarak açıldı ve rapor commit'i eklenmeden incelemeye açılmadı; #97'nin bölümü de bu rapor commit'inde.
**Sunumda anlatacağım tek cümle:**
Talebin her durum değişikliği aynı transaction'da ve hemen veritabanına yazılıyor; geçmiş yazılamazsa onay ya da iptal de geri alınıyor, yani geçmişi eksik bir talep olamıyor.

## 20 Kasım 2026 — feat(leave-type): izin türü listeleme, ekleme ve güncelleme (#99)
**İlgili gereksinimler:** FR-12, FR-13 · Issue #25 (İK kısıtı eklenince kapanacak)
**Use case(ler):** `ListLeaveTypes`, `CreateLeaveType`, `UpdateLeaveType`
**Ne yaptım:**
- `GET /api/leave-types` (200): aktif ve pasif tüm izin türleri, id sırasıyla; hazır 4 tür (FR-13) dahil.
- `POST /api/leave-types` (201 + `Location` başlığı): yeni tür aktif olarak kaydediliyor.
- `PUT /api/leave-types/{id}` (200): yalnızca ad, yıllık sınır ve aktiflik değişebiliyor; yoldaki id gövdedeki id'yi eziyor (`withId`).
- `application/common/view/LeaveTypeView` ve `mapper/LeaveTypeMapper`: cevaplar entity değil görünüm taşıyor.
- `api/LeaveTypeController`: projedeki ilk Controller'lardan; isteği Request record'una çevirip `mediator.send()` çağırıyor, iş kuralı içermiyor.
- `LeaveTypeRepository`: `existsByCode`, `existsByName`, `existsByNameAndIdNot`.
- `LeaveType.setDeductsFromBalance` kaldırıldı.
- Değişiklikleri 6 commit'e böldüm (görünüm ve eşleyici, listeleme, ekleme, güncelleme, Controller, testler); PR'ı taslak olarak açtım.
**Validator'a koyduğum kurallar:**
- İstek anotasyonları (400): kod zorunlu, büyük harfle başlar, yalnızca büyük harf ve alt çizgi, 2–30 karakter (`MARRIAGE_LEAVE`); ad zorunlu, en fazla 50; yıllık sınır 0–365, en fazla bir ondalık; bakiyeden düşme ve aktiflik bilgisi zorunlu.
- `CreateLeaveTypeValidator` (400): bakiyeden düşmeyen türde yıllık sınır boş olmalı.
**Handler'daki kurallar ve transaction:**
- `CreateLeaveTypeHandler` (`@Transactional`): aynı kod ya da ad varsa `DUPLICATE_VALUE` (409); ad baştaki ve sondaki boşluklardan arındırılıyor.
- `UpdateLeaveTypeHandler` (`@Transactional`): tür yoksa `LEAVE_TYPE_NOT_FOUND` (404); ad başka bir türde varsa 409; kayıtlı tür bakiyeden düşmüyorsa ve sınır verildiyse 400 (`ValidationException`).
- `ListLeaveTypesHandler` (`@Transactional(readOnly = true)`): salt okunur transaction.
**Neden böyle yaptım (tasarım kararları):**
- Kuralların yeri plan 8. bölümdeki tabloya göre: tek alanlık kurallar anotasyonda, alanlar arası kural Validator'da, veritabanı gerektiren kurallar Handler'da. Güncellemedeki "düşmeyen türde sınır boş" kuralı türün kayıtlı halini bilmeyi gerektirdiği için Handler'da, ama eklemeyle aynı 400 cevabını veriyor.
- Tekillik önceden kontrol ediliyor: veritabanının tekil kısıtı da reddederdi, ama o hata 500 "beklenmeyen hata" olarak dönerdi; önceden kontrol edilince anlamlı bir 409 ve mesaj dönüyor.
- Kod ve "bakiyeden düşer mi" sonradan değişmiyor: kod başka yerlerde kimlik olarak kullanılıyor (hak ediş yıllık izni koduyla buluyor). Bakiyeden düşen bir tür sonradan "düşmez" yapılırsa, onaylanmış bir izin iptal edildiğinde bakiye servisi bu türde iade yapmaz ve düşülmüş günler kaybolur. Kural hem güncelleme isteğinde (alan yok) hem entity'de (setter yok) korunuyor.
- Pasif türler de listeleniyor: pasif türle talep açılamıyor (talep açma kuralı), ama İK'nın türü yeniden aktif yapabilmesi için listede görmesi gerekiyor.
- Liste için Validator yazılmadı: doğrulanacak alan yok; mediator Validator'ı olmayan isteği kabul ediyor.
- İK kısıtı (`@PreAuthorize("hasRole('HR')")`) güvenlik altyapısı henüz `dev`'de olmadığı için eklenmedi; ekleme ve güncelleme şimdilik herkese açık. Güvenlik PR'ı birleşince kısıt ve 403 testleri küçük bir PR ile eklenecek; issue #25 bu yüzden `Refs` ile bağlandı, kapatılmadı.
**Testler:**
- `CreateLeaveTypeValidatorTest` (3 birim testi): sınırlı düşen tür, sınırsız düşmeyen tür, sınırlı düşmeyen tür (hata).
- `LeaveTypeUseCasesIntegrationTest` (11 test, mediator üzerinden, Testcontainers PostgreSQL; anotasyon, Validator, Handler ve veritabanı birlikte):
  - 4 hazır türün id sırasıyla listelenmesi.
  - Ekleme; aynı kod (409), aynı ad (409), hatalı kod (400, Handler'a ulaşmadan), düşmeyen türde sınır (400).
  - Güncelleme: ad, sınır ve aktiflik değişiyor, kod ve bakiye davranışı değişmiyor; aynı adı korumak serbest; bulunamayan tür (404); başka türün adı (409); düşmeyen türde sınır (400).
- `LeaveTypeControllerTest` (6 test, sahte mediator, Spring açılmadan): 200 ve JSON biçimi, 201 ve `Location`, 400/409/404 hata gövdeleri, yoldaki id'nin gövdedeki id'yi ezmesi.
- `./mvnw verify`: 179 test, 0 hata (20 yeni).
**Karşılaştığım sorun ve çözümü:**
- Güvenlik altyapısı (JWT, roller) planlanan tarihte gelmedi → use case'ler ve endpoint'ler yazıldı, rol kısıtı sonraya bırakıldı.
- PR yaklaşık 730 satırla önerilen ~400 satır sınırını aştı → commit'ler use case başına bölündü, PR açıklamasına not düşüldü.
**Sunumda anlatacağım tek cümle:**
İzin türleri üç ayrı use case olarak yazıldı; her kural kendi katmanında (anotasyon, Validator, Handler) ve bakiye davranışı gibi geçmiş kayıtları bozabilecek alanlar oluşturulduktan sonra değiştirilemiyor.

## 20 Kasım 2026 — feat(leave-history): şirket izin geçmişi araması (#100)
**İlgili gereksinimler:** FR-26 · NFR-07 · Issue #52 (İK kısıtı eklenince kapanacak)
**Use case(ler):** `SearchLeaveHistory`
**Ne yaptım:**
- `GET /api/leave-requests/history?from&to&departmentId&status&employeeId&page&size` (200): `leave_request_history` satırlarını filtreleyip sayfalı döndürüyor, en yeni olay önce. Her satır bir durum değişikliği: talep, talep sahibi, departmanı, izin türü, izin tarihleri, eski → yeni durum, işlemi yapan ve not.
- `application/common/PageResponse<T>` (`content, page, size, totalElements, totalPages`) ve `PageResponse.of(sayfa, eşleyici)`: projedeki ilk sayfalı liste olduğu için ortak sınıfı ben ekledim; ekibin sayfalı listeleri de bunu kullanacak.
- `view/HistoryEntryView` ve `mapper/HistoryEntryMapper`; Bilge'nin `EmployeeRef` ve `LeaveTypeRef` görünümlerini yeniden kullandım.
- `LeaveRequestHistorySpecifications.matching(...)`: beş isteğe bağlı filtre.
- `LeaveRequestHistoryRepository`: `JpaSpecificationExecutor` ve `@EntityGraph`.
- `api/LeaveHistoryController`: sorgu parametrelerini isteğe çevirip `mediator.send()` çağırıyor.
- Değişiklikleri 6 commit'e böldüm (ortak sayfa cevabı, görünüm ve eşleyici, sorgu katmanı, use case, Controller, testler); PR'ı taslak olarak açtım.
**Validator'a koyduğum kurallar:**
- İstek anotasyonları (400): `page ≥ 0`, `size` 1–100, departman ve çalışan id'si pozitif. Boş `page` 0, boş `size` 20 (NFR-07).
- `SearchLeaveHistoryValidator` (400): bitiş tarihi başlangıçtan önce olamaz.
- Controller (400): bilinmeyen durum (`APROVED`) ya da farklı biçimde tarih (`01.11.2026`) mediator'a ulaşmadan reddediliyor.
**Handler'daki kurallar ve transaction:**
- `@Transactional(readOnly = true)`: salt okunur transaction.
- Tarih filtresi gün olarak geliyor; Handler `Clock` bean'inin saat dilimiyle (Europe/Istanbul) `from` gününün başı ve `to`'nun ertesi gününün başı olarak aralığa çeviriyor. Böylece `to` günü 23:59'daki olay da dahil.
- Sıralama: olay zamanı, eşitse id; ikisi de azalan.
**Neden böyle yaptım (tasarım kararları):**
- Arama talepler değil geçmiş satırları üzerinde: FR-26 "onaylanan, reddedilen, geri çekilen ve iptal edilen talepler, işlemi yapan kişi ve gerekçesiyle" diyor; bu bilgi `leave_request_history`'de. Talebin açılışı da dahil tüm olaylar gösteriliyor, `status` filtresiyle daraltılabiliyor.
- Dinamik sorgu için Spring Data `Specification` (Criteria API): beş filtrenin her biri boş olabilir. Her kombinasyon için ayrı repository metodu 32 metot olurdu; `:p IS NULL OR ...` biçimindeki tek JPQL sorgusu ise okunması zor ve PostgreSQL'de boş parametrenin tipi yüzünden hata verebiliyor. Specification'da yalnızca dolu filtreler birleştiriliyor.
- N+1 sorununa karşı `@EntityGraph`: her satırda talep, çalışan, departman, tür ve işlemi yapan gösteriliyor; ilişkiler LAZY olduğu için önlem alınmasa 20 satırlık sayfa 1 + 100 sorgu olurdu. `@EntityGraph` bunları ana sorguya join ile ekliyor; sayfa en fazla 2 sorguyla (SELECT ve gerekirse COUNT) yükleniyor.
- `PageResponse<T>`: plan 6.2'ye göre Spring'in `Page` nesnesi dışarı verilmiyor; JSON biçimi Spring sürümüne bağlı kalmıyor ve SOAP/gRPC giriş katmanları da aynı yapıyı kullanabiliyor.
- Tarih parametreleri Controller'da `@DateTimeFormat` ile çözülüyor; istek record'unda Spring anotasyonu yok (plan 9.2: Handler ve istekler HTTP'ye özgü tip içermez).
- İK kısıtı (`@PreAuthorize("hasRole('HR')")`) güvenlik altyapısı `dev`'de olmadığı için eklenmedi; issue #52 `Refs` ile bağlandı, kapatılmadı.
**Testler:**
- `SearchLeaveHistoryValidatorTest` (3 birim testi): sıralı aralık, tek gün ve açık uçlu aralık geçerli; ters aralık hata; sayfalama varsayılanları 0 ve 20.
- `SearchLeaveHistoryIntegrationTest` (8 test, mediator üzerinden, Testcontainers PostgreSQL; iki departmanda iki çalışanın iki talebi için dört olay, ikisi gece yarısının iki yanında):
  - Filtresiz arama: dört olay, en yeni önce.
  - Bir olayın tüm sütunları (çalışan, departman, tür, tarih, durumlar, işlemi yapan, not, zaman).
  - Gün sınırı: 3 Kasım filtresi 23:30'daki olayı alıyor, ertesi gün 00:15'tekini almıyor.
  - Departman, çalışan ve durum filtreleri tek tek ve birlikte.
  - Sayfalama: 3'lük sayfalarda ilk sayfa 3, ikinci sayfa 1 kayıt; toplam 4, 2 sayfa.
  - Ters tarih aralığı ve 101'lik sayfa 400.
  - Sorgu sayısı: Hibernate istatistikleriyle 3 satırlık sayfanın en fazla 2 SQL ile geldiği doğrulanıyor (N+1 yok).
- `LeaveHistoryControllerTest` (4 test, sahte mediator, Spring açılmadan): tüm parametrelerin isteğe aktarılması ve sayfalı JSON biçimi, parametresiz varsayılanlar, bilinmeyen durum ve hatalı tarih biçimi için 400.
- `./mvnw verify`: 174 test, 0 hata (15 yeni).
**Karşılaştığım sorun ve çözümü:**
- Branch #99 birleşmeden önce açıldığı için rapor dosyasında #99'un bölümü yoktu → güncel `dev` branch'e birleştirildi (`git merge origin/dev`), rapor bölümü #99'un bölümünün arkasına eklendi; böylece birleştirmede çakışma çıkmadı.
**Sunumda anlatacağım tek cümle:**
İK'nın şirket izin geçmişi beş isteğe bağlı filtreyle tek bir dinamik sorguda aranıyor; ilişkiler join ile geldiği için sayfa ne kadar büyük olursa olsun en fazla iki SQL çalışıyor.
