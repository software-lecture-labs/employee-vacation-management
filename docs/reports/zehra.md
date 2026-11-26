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

## 26 Kasım 2026 — refactor: use case yapısını tutarlı hâle getir (#106)
**İlgili gereksinimler:** NFR-01 · Issue #105
**Use case(ler):** Yapı düzeltmesi; etkilenenler `SearchLeaveHistory`, `ListDepartments`, `ListLeaveTypes`, `UpdateLeaveType`, `Login`, `GetMyProfile`, `SubmitLeave` (paket adı)
**Ne yaptım:**
- Repodaki tüm use case paketlerini plan 4.2'deki "her use case dört dosya (Request, Response, Handler, Validator)" kuralına göre tek tek inceledim; yanlış yerde duran bir Handler yoktu, ama dosyası eksik use case'ler vardı.
- Eksik Response'ları ekledim: `SearchLeaveHistoryResponse(history)` ve `ListDepartmentsResponse(departments)`. İkisi de Handler'dan doğrudan sayfa (`PageResponse`) dönüyordu; artık diğer use case'ler gibi sayfayı tek alanlı bir cevapla sarıyorlar.
- Eksik Validator'ları ekledim: `LoginValidator`, `GetMyProfileValidator`, `ListDepartmentsValidator`, `ListLeaveTypesValidator`, `UpdateLeaveTypeValidator`. Alanlar arası kuralı olmayanlar boş liste dönüyor; javadoc'larında kuralların nerede olduğu (istek anotasyonu ya da Handler) yazıyor.
- `usecase/leave/submit` paketini `usecase/leaverequest/submit` olarak yeniden adlandırdım (`git mv` ile, dosya geçmişi korunarak); IntelliJ şablon rehberindeki örnek yolu da güncelledim.
- `LeaveBalanceServiceTest`'in adını `LeaveBalanceServiceIntegrationTest` yaptım.
- Kuralı her derlemede denetleyen `architecture/UseCaseStructureTest` mimari testini yazdım.
- Bu iş için issue #105'i açtım; değişiklikleri 7 commit'e böldüm (her biri tek bir use case ya da tek bir düzeltme, her commit kendi testleriyle yeşil).
**Validator'a koyduğum kurallar:**
- Yeni kural yok; eklenen Validator'lar mevcut kuralların yerini değiştirmedi.
**Handler'daki kurallar ve transaction:**
- Değişmedi. Handler'lar yalnızca cevabı yeni Response record'una sarıyor; transaction sınırları ve iş kuralları aynı.
**Neden böyle yaptım (tasarım kararları):**
- Tutarlılık: plan ve Bilge'nin IntelliJ şablonu her use case için dört dosya öngörüyor; kodu okuyan biri her use case'te aynı dört dosyayı bulunca neyin nerede olduğunu aramıyor.
- Liste cevabını sarmak (`{history: {...}}`): diğer cevaplar da tek alanlı (`{leaveTypes: [...]}`, `{leaveType: {...}}`); cevaba ileride ek bilgi (örneğin filtre özeti) eklenirse JSON biçimi bozulmuyor. Bu uç noktaları kullanan bir istemci henüz olmadığı için değişikliğin en ucuz olduğu an şimdiydi.
- Boş Validator yazmak: dosya sayısı artıyor ama yer sabit kalıyor; yeni bir alanlar arası kural eklenecekse nereye yazılacağı belli. Alternatif ("Validator yalnızca kural varsa") daha az dosya demekti ama planla çelişiyordu.
- `leaverequest` adı: `leave` ile `leavetype` yan yana karışıyordu; commit kapsamı da zaten `leave-request`.
- Mimari testi: kural yalnızca bu PR'da düzelmekle kalmasın, korunsun diye. Test classpath'ten tüm `XxxRequest`'leri bulur ve her biri için aynı pakette `XxxResponse`, `XxxHandler`, `XxxValidator`'ın var olduğunu ve generic tiplerin (`Request<XxxResponse>`, `RequestHandler<XxxRequest, XxxResponse>`, `RequestValidator<XxxRequest>`) doğru bağlandığını, paketin `usecase.<alan>.<işlem>` biçiminde olduğunu denetler. Kurala uymayan yeni bir use case CI'da kırmızı olur. Ek kütüphane (ArchUnit gibi) eklemedim; Spring'in classpath tarayıcısı yetti.
- Handler'ı henüz yazılmamış `SubmitLeave` testte açıkça yazılmış bir istisna listesinde; Handler'ı yazmak Bilge'nin iş kurallarını yazmak demek olduğu için ona dokunmadım.
- Domain servislerinin persistence repository'lerini kullanması değiştirilmedi: plan bağımlılık yönünü `api → application → domain → infrastructure.persistence` olarak belirlemiş; bu bilinçli bir tercih ve `TECHNICAL_DECISIONS.md`'de gerekçesiyle anlatılacak.
**Testler:**
- `UseCaseStructureTest`: 1 sabit test (taramanın boş olmadığı) ve her use case için 1 dinamik test.
- Departman ve geçmiş araması testleri (Handler, entegrasyon, Controller) yeni cevap biçimine göre güncellendi; diğer testler değişmeden yeşil.
- `./mvnw verify`: BUILD SUCCESS.
**Karşılaştığım sorun ve çözümü:**
- `git mv` ile taşınan dosyalar otomatik olarak commit'e hazırlandığı için ilk commit'e karışacaktı → önce `git restore --staged .` ile hepsi hazırlıktan çıkarıldı, sonra her commit'e yalnızca kendi dosyaları eklendi; Git yeniden adlandırmaları yine tanıdı.
- Değişiklik başka üyelerin dosyalarına da dokunuyordu → cevap biçimi değişikliği ve paket adı PR açıklamasında ayrıca yazıldı; Bilge'ye yerel çalışmasını nasıl güncelleyeceği not edildi.
**Sunumda anlatacağım tek cümle:**
Her use case aynı dört dosyadan oluşuyor ve bu kural bir mimari testiyle her derlemede denetleniyor; yapıya uymayan kod CI'dan geçemiyor.

## 26 Kasım 2026 — feat(balance): bakiye görüntüleme (#107)
**İlgili gereksinimler:** FR-32 · Issue #34
**Use case(ler):** `GetMyBalances`, `GetEmployeeBalances`
**Ne yaptım:**
- `GET /api/leave-balances/mine?year` (200): giriş yapan çalışanın o yıldaki bakiyeleri; her satırda izin türü, hak edilen, kullanılan ve kalan gün.
- `GET /api/employees/{id}/leave-balances?year` (200 / 403 / 404): bir çalışanın bakiyeleri; yönetici yalnızca kendi ekibini, İK herkesi görür.
- `application/common/view/BalanceView` ve `mapper/BalanceMapper`; kalan gün entity'nin `remainingDays()` metodundan geliyor, hesap tek yerde.
- `LeaveBalanceRepository.findWithLeaveTypeByEmployeeIdAndYear`: bakiyeler izin türüyle birlikte `join fetch` ile tek sorguda, türe göre sıralı.
- `api/LeaveBalanceController`. İki use case de baştan dört dosyalı (Request, Response, Handler, Validator) yazıldı; #106'daki mimari testi bunları da denetliyor.
- Onur'un `CurrentUserProvider`'ı (#104) birleşince bu iş yazılabilir hale geldi; ondan önce bloklu kalmıştı.
- `docs/REQUIREMENTS.md`'de uç noktası olmayan, testleri `dev`'de olan 7 satırımı ✅ yaptım: FR-15 (#8), FR-27 (#98), FR-33 (#96), FR-35 (#97), BR-01 (#8), BR-03 (#97), BR-16 (#98). Uç noktası olan satırlar (FR-12, FR-13, FR-26, FR-32) Swagger denemesinden sonra işaretlenecek.
- Değişiklikleri 5 commit'e böldüm (görünüm ve sorgu, kendi bakiyeleri, çalışanın bakiyeleri, Controller, testler).
**Validator'a koyduğum kurallar:**
- İstek anotasyonları (400): yıl 2000–2100; çalışan id'si zorunlu ve pozitif.
- `GetMyBalancesValidator`, `GetEmployeeBalancesValidator`: alanlar arası kural yok, boş liste dönüyor; kayda bağlı kurallar Handler'da (javadoc'ta yazıyor).
**Handler'daki kurallar ve transaction:**
- `GetMyBalancesHandler` (`@Transactional(readOnly = true)`): yalnızca token'daki çalışan id'si kullanılıyor (`currentEmployeeId()`), çalışan kaydı ayrıca yüklenmiyor. Yıl verilmezse `Clock`'a göre bu yıl.
- `GetEmployeeBalancesHandler` (`@Transactional(readOnly = true)`): çalışan yoksa `EMPLOYEE_NOT_FOUND` (404). İK herkesi, yönetici yalnızca kendisine doğrudan bağlı çalışanları (`employee.getManager()` kendisi olanlar) görür; diğer durumlarda `ForbiddenActionException(ACTION_NOT_ALLOWED)` (403).
**Neden böyle yaptım (tasarım kararları):**
- Yetki iki katmanlı: rol kontrolü bir anotasyonla Controller'da (`@PreAuthorize("hasAnyRole('MANAGER','HR')")`); "bu yönetici bu kişiyi görebilir mi?" ise veritabanındaki yönetici ilişkisine bağlı olduğu için anotasyonla çözülemez, Handler'da. Normal çalışan kendi id'siyle bile ikinci uç noktaya 403 alıyor (plana göre); kendi bakiyesini `/mine`'dan görüyor.
- `/mine`'da istekte çalışan id'si yok: kimin bakiyesi olduğu yalnızca token'dan geliyor, böylece biri başkasının id'sini yazarak onun bakiyesini göremez (plan 9.2).
- `join fetch` ile tek sorgu: her bakiye satırında izin türünün adı ve kodu gösteriliyor; ilişki LAZY olduğu için önlem alınmasa her satır ayrı bir tür sorgusu olurdu.
- Boş liste hata değil: o yıl için bakiye açılmamışsa (örneğin yıllık hak ediş henüz çalıştırılmamışsa) 200 ve boş liste dönüyor.
- Cevaplar yılı da taşıyor: istekte yıl yoksa hangi yılın gösterildiği cevaptan anlaşılıyor.
**Testler:**
- `LeaveBalanceControllerIntegrationTest` (10 test, uçtan uca: gerçek giriş ve JWT, güvenlik filtreleri, mediator, Handler ve Testcontainers PostgreSQL birlikte; Onur'un güvenlik testindeki kalıp):
  - Kendi bakiyesi ve varsayılan yıl (yıllık 14 / kullanılan 3 / kalan 11, mazeret 5), yıl filtresi, bakiyesi olmayan çalışan için boş liste, 1999 yılı (400), token yok (401).
  - İK başka bir çalışanı görür (200); yönetici kendi ekibini görür (200), başka ekipten birini göremez (403 `ACTION_NOT_ALLOWED`); çalışan rolü kendi id'siyle bile 403; bulunamayan çalışan 404.
- `UseCaseStructureTest`: iki yeni use case için de geçti.
- `./mvnw verify`: 262 test, 0 hata.
**Karşılaştığım sorun ve çözümü:**
- Bu iş `CurrentUserProvider` olmadan yazılamıyordu → Onur'un güvenlik PR'ı (#104) birleşince başlandı.
- `REQUIREMENTS.md`'deki ✅ tanımı "Swagger'da çalışıyor" şartını da içeriyor; demo verisi (DataSeeder) olmadan Swagger'da giriş yapılamıyor → uç noktası olmayan satırlar şimdi işaretlendi; uç noktası olanlar, ekip işleri bitince hazırlanacak test senaryoları dokümanıyla Swagger'da denendikten sonra işaretlenecek.
**Sunumda anlatacağım tek cümle:**
Bakiye görüntülemede rolü Spring Security, "bu yönetici bu çalışanı görebilir mi?" sorusunu ise Handler veritabanındaki ilişkiye bakarak cevaplıyor; çalışan kendi bakiyesini token'dan, başkasının id'sini yazamadan görüyor.

## 26 Kasım 2026 — feat(balance): yıllık hak ediş (T3) ve rollback testi (#108)
**İlgili gereksinimler:** FR-33, FR-34 · BR-10, BR-11 · T3 · Issue #47
**Use case(ler):** `RunAnnualAccrual`
**Ne yaptım:**
- `POST /api/leave-balances/accruals?year=` (201 / 400 / 403 / 409): İK bir yıl için bütün aktif çalışanların yıllık bakiyelerini açar. Cevapta yıl, hak yazılan ve atlanan çalışan sayısı, açılan bakiye sayısı ve çalıştırma zamanı var.
- `LeaveBalanceService.openYearBalances`: bir çalışana bir yılın bakiyelerini açan ortak kural. Yıllık izin kıdem ve yaşa göre (`EntitlementPolicy`, #96), bakiyeden düşen diğer türler yıllık sınırı kadar (mazeret 5 gün) açılıyor; hastalık ve ücretsiz izne bakiye açılmıyor (BR-03). O yıl için bakiyesi zaten olan tür ve işe henüz başlamamış çalışan hata vermeden atlanıyor.
- `usecase/balance/accrual`: dört dosya (Request, Response, Handler, Validator).
- Repository'ler: aktif çalışanlar (`findAllByActiveTrueOrderByIdAsc`), aktif ve bakiyeden düşen türler, `AnnualAccrualRunRepository.existsByYear`.
- `docs/REQUIREMENTS.md`'de T3 satırını rollback testinin adıyla ✅ yaptım. FR-34 ve BR-11 uç noktayla görülebildiği için Swagger denemesinden sonra işaretlenecek.
- Değişiklikleri 4 commit'e böldüm (ortak bakiye açma kuralı, use case, Controller, uç nokta testleri).
**Validator'a koyduğum kurallar:**
- İstek anotasyonu (400): yıl zorunlu.
- `RunAnnualAccrualValidator` (400): yalnızca bu yıl veya gelecek yıl kabul ediliyor; "bu yıl" `Clock`'tan alınıyor. İK yeni yılın bakiyelerini Aralık'ta hazırlayabiliyor; 2062 gibi bir yazım hatası ise reddediliyor, çünkü BR-11 yüzünden o yıl bir daha çalıştırılamazdı.
**Handler'daki kurallar ve transaction:**
- `RunAnnualAccrualHandler` (`@Transactional`) T3'ün transaction sınırı. `LeaveBalanceService` kendi transaction'ını açmıyor (`MANDATORY`), buna katılıyor; bir çalışanda hata çıkarsa önceki çalışanların bakiyeleri, ACCRUAL hareketleri ve çalıştırma kaydı birlikte geri alınıyor.
- BR-11: yıl `annual_accrual_runs` tablosunda varsa `ACCRUAL_ALREADY_RUN` (409).
- BR-10: işlemi yapan İK pasifse `INACTIVE_EMPLOYEE`.
- Aynı anda iki çalıştırma: ikisi de ilk kontrolü geçse bile veritabanındaki `uq_annual_accrual_runs_year` ikinci kaydı reddediyor; kayıt `saveAndFlush` ile yazıldığı için hata Handler'da yakalanıp aynı 409'a çevriliyor ve ikinci işlemin açtığı bakiyeler geri alınıyor.
- İşlem en sonda çalıştırma kaydını yazıyor (yıl, zaman, İK, hak yazılan çalışan sayısı).
**Neden böyle yaptım (tasarım kararları):**
- Bakiye açma kuralı Handler'da değil `LeaveBalanceService`'te: yeni yıl (T3) ve yeni çalışan (T4) aynı kuralı kullanıyor; iki yerde yazılsa zamanla birbirinden ayrılırdı. T4'ün de bu metodu çağırması gerektiği servisin javadoc'unda yazıyor.
- BR-11 kontrolü bakiyelere değil çalıştırma kaydına bakıyor: yıl içinde eklenip T4 ile bakiyesi açılan bir çalışan, o yılın hak edişini yanlışlıkla engellemiyor. Böyle bir çalışan T3'te hata vermeden atlanıyor; atlanmasaydı 409 alır ve bütün işlem geri alınırdı.
- İzin türleri çalışan başına değil, bir kez okunuyor.
- Mazeret izni de açılıyor: açılmasaydı yeni yılda mazeret izni talebi `LEAVE_BALANCE_NOT_FOUND` verirdi.
**Testler:**
- `RunAnnualAccrualIntegrationTest` (7 test, mediator ve Testcontainers PostgreSQL; İK `@MockitoBean CurrentUserProvider` ile, `EntitlementPolicy` `@MockitoSpyBean` ile veriliyor):
  - Kıdem ve yaş: 0 yıl → 0, 3 yıl → 14, 10 yıl → 20, 20 yıl → 26, 18 yaş ve 55 yaş → en az 20; mazeret 5 gün; hastalık ve ücretsiz izne bakiye yok.
  - ACCRUAL hareketleri ve çalıştırma kaydı; 0 günlük yıllık izinde satır var, hareket yok.
  - Pasif, henüz işe başlamamış ve bakiyesi zaten açık çalışanlar atlanıyor; yalnızca yıllık izni açık olana eksik mazeret açılıyor.
  - Aktif olmayan ve yıllık sınırı olmayan türlere bakiye açılmıyor; yeni eklenen sınırlı tür açılıyor.
  - Aynı yıl ikinci kez → 409, hiçbir şey değişmiyor; pasif İK → `INACTIVE_EMPLOYEE`.
  - **Rollback testi:** 4 çalışandan 3'üncüsünde `EntitlementPolicy` hata fırlatıyor; bakiyeler, hareketler ve çalıştırma kaydı sıfır kalıyor. Kimlikler veritabanında üretildiği için ilk iki çalışanın satırları hata anında veritabanına gönderilmiş oluyor; sıfır kalmaları rollback'in gerçekten olduğunu gösteriyor. Hata giderilince aynı yıl yeniden çalıştırılabiliyor.
- `RunAnnualAccrualValidatorTest` (6 test): bu yıl ve gelecek yıl kabul, diğer yıllar red; İstanbul'da yeni yıl başladığında "bu yıl" saatin bölgesinden alınıyor.
- `LeaveBalanceServiceIntegrationTest` (+3 test): `openYearBalances` yıllık ve mazeret açıyor, açık türü atlıyor, işe girişten önceki yıl için hiçbir şey açmıyor.
- `LeaveBalanceControllerIntegrationTest` (+5 test, gerçek giriş ve JWT): İK 201 ve bakiyesi açık çalışanın atlanması, yönetici 403, aynı yıl 409, geçersiz yıl 400, yıl yok 400.
- `UseCaseStructureTest`: yeni use case için de geçti.
- `./mvnw verify`: 284 test, 0 hata.
**Karşılaştığım sorun ve çözümü:**
- T4 ile yıl içinde bakiyesi açılmış bir çalışan, `openBalance`'ın tekrar kontrolü yüzünden bütün T3'ü 409 ile geri aldırabilirdi → bakiyesi olan tür hata vermeden atlanıyor ve atlanan çalışan sayısı cevapta dönüyor.
- Aynı anda iki çalıştırmada ikinci kayıt veritabanı kısıtına takılıyor ve bu hata 500 olarak dönecekti → kayıt `saveAndFlush` ile yazılıp hata 409'a çevrildi.
**Sunumda anlatacağım tek cümle:**
Yıllık hak ediş bütün çalışanları tek transaction'da işliyor; testte 3. çalışanda hata çıkartıyorum ve önceki iki çalışanın bakiyeleri de veritabanından geri alınmış oluyor.

## 26 Kasım 2026 — perf(db): sorgu incelemesi, giriş sorgusu indeksi ve N+1 testleri (#109)
**İlgili gereksinimler:** FR-01, FR-32 · Issue #71 (Refs; ölçümler DATABASE_DESIGN.md'ye girecek)
**Use case(ler):** `Login` (sorgusu), `GetMyBalances`, `GetEmployeeBalances` (testleri)
**Ne yaptım:**
- Planın "Sorgu incelemesi (N+1, indeks)" maddesi. Yerel bir PostgreSQL'de `db/scripts` ile şemayı kurdum; gerçek projeden çok büyük bir veri yükledim (20.000 çalışan, 120.000 bakiye, 100.000 talep, 175.000 geçmiş satırı). Mevcut sorguları Hibernate'in ürettiği biçimde `EXPLAIN ANALYZE` ile çalıştırdım.
- Bulunan tek sorun: girişteki `findByEmailIgnoreCase` indeksi kullanmıyordu. `EmployeeRepository`'ye `lower()` kullanan bir `@Query` ekledim. Metodun adı ve davranışı aynı kaldı, `LoginHandler` değişmedi. Dosya Onur'un; PR'da inceleyici olarak eklendi.
- Bakiye uç noktalarına N+1 koruma testleri ekledim.
- Değişiklikleri 2 commit'e böldüm (giriş sorgusu ve testleri, sorgu sayısı testleri).

**Ölçümler:**

| Sorgu | Plan | Süre | Sonuç |
|---|---|---|---|
| Giriş, `upper(email)` (eski hâl) | Seq Scan | 6,4 ms | düzeltildi |
| Giriş, `lower(email)` (yeni hâl) | Index Scan `uq_employees_email_lower` | 0,07 ms | uygun |
| Bakiyeler (çalışan + yıl, join fetch) | Index Scan `uq_leave_balances_employee_type_year` | 0,08 ms | uygun |
| Geçmiş araması, çalışan filtresi | `ix_leave_requests_employee_status` + `ix_leave_request_history_request` | 0,3 ms | uygun |
| Geçmiş araması, departman filtresi | Index Scan Backward `ix_leave_request_history_occurred_at` | 35 ms | uygun |
| T3, aktif çalışanlar | Index Scan `employees_pkey` | 5 ms | uygun |

**Neden böyle yaptım (tasarım kararları):**
- Spring Data metot adındaki `IgnoreCase`'ten `upper(email) = upper(?)` üretiyor; tekil indeks ise `lower(email)` üzerinde. PostgreSQL bir ifade indeksini yalnızca sorgu aynı ifadeyi kullanırsa kullanabiliyor. `lower()` ile hem indeks kullanılıyor hem de giriş, tekillik kısıtıyla aynı kuralla karşılaştırıyor.
- Metodun adını değiştirmedim: `LoginHandler` ve Onur'un testleri bu adı kullanıyor; davranış (harf duyarsız arama) aynı.
- Yeni indeks eklemedim. `employees(department_id)` departman filtresinde fark yaratmadı (35 → 42 ms; planlayıcı yine `occurred_at` indeksini seçti). `employees(manager_id)` 2,2 ms'yi 0,5 ms'ye indiriyor ama bu kolonu sorgulayan kod henüz yok. Her indeks her eklemede ve güncellemede ek iş demek; indeksi kullanacak sorgu yazılınca eklenmeli.
- N+1 açısından proje baştan korumalı: `open-in-view: false` olduğu için Controller'da ya da JSON'a çevirirken gizli lazy-load olamıyor; geçmiş araması `@EntityGraph`, bakiyeler `join fetch` kullanıyor.
- Sorgu planı testinde `enable_seqscan` kapatılıyor: testteki tablo küçük olduğu için PostgreSQL normalde tabloyu taramayı seçer; tarama kapatılınca kullanılabilecek indeks varsa onu seçer. Test böylece "bu sorgu bu indeksi kullanabilir mi?" sorusunu veri miktarından bağımsız cevaplıyor.
**Testler:**
- `EmployeeRepositoryIntegrationTest` (yeni, 4 test, Testcontainers PostgreSQL): harf duyarsız bulma; repository'nin `lower` sorgusunu çalıştırması (Hibernate Statistics); `lower` ile `uq_employees_email_lower` indeksinin kullanılabilmesi; eski `upper` biçiminin kullanamaması (`EXPLAIN`).
- `LeaveBalanceControllerIntegrationTest` (+2 test, gerçek giriş ve JWT, Hibernate Statistics; giriş isteği ölçüme dahil değil): `/mine` tam 1 sorgu; çalışanın bakiyeleri en fazla 3 sorgu (hedef çalışan, işlemi yapan, bakiyeler).
- `./mvnw verify`: 290 test, 0 hata.
**Karşılaştığım sorun ve çözümü:**
- Testteki küçük tablolarda PostgreSQL indeks olsa da tabloyu taramayı seçiyor; plan "indeks kullanılıyor mu?" sorusunu cevaplamıyordu → plan testi tablo taraması kapalı bir transaction'da (`set local enable_seqscan = off`) çalıştırıldı.
- Bu madde için ayrı bir issue yok → PR, ölçümlerin gireceği DATABASE_DESIGN.md issue'suna (#71) `Refs` ile bağlandı.
**Sunumda anlatacağım tek cümle:**
Sorguları büyük bir veriyle `EXPLAIN` ile inceledim; girişteki sorgunun indeksi kullanmadığını bulup düzelttim, kullanılmayacak indeksleri ölçerek eklemedim ve N+1'e karşı sorgu sayısı testleri yazdım.

## 26 Kasım 2026 — docs(api): bakiye, izin türü ve geçmiş uç noktalarına Swagger örnekleri (#110)
**İlgili gereksinimler:** FR-12, FR-26, FR-32, FR-34 · Issue #57
**Use case(ler):** Swagger açıklamaları: `ListLeaveTypes`, `CreateLeaveType`, `UpdateLeaveType`, `SearchLeaveHistory`, `GetMyBalances`, `GetEmployeeBalances`, `RunAnnualAccrual`
**Ne yaptım:**
- Modülümün 7 uç noktasına Swagger açıklamaları ekledim: özet, açıklama (kimin çağırabileceği dahil), parametre açıklamaları ve örnekleri, istek örnekleri, başarı ve hata cevaplarının (400, 401, 403, 404, 409) örnekleri, izin türü eklemede `Location` başlığı.
- `api/docs/LeaveTypeApi`, `LeaveHistoryApi`, `LeaveBalanceApi`: açıklamalar bu arayüzlerde; üç controller bunları uyguluyor (`implements XxxApi`, `@Override`), davranışları değişmedi.
- `api/docs/ApiExamples`: bütün örnek JSON'lar tek yerde. Alan adları Response record'larıyla, hata mesajları kodun döndürdüğü metinlerle aynı. Ortak 401 ve 403 örnekleri diğer modüller için de kullanılabilir.
- Değişiklikleri 3 commit'e böldüm (açıklamalar ve örnekler, controller bağlantısı, test).
**Neden böyle yaptım (tasarım kararları):**
- Açıklamalar controller'da değil, ayrı arayüzlerde: Request ve Response record'ları application katmanında; `@Schema` oraya yazılsaydı application katmanı Swagger kütüphanesine bağımlı olurdu (plan, use case'lerin ileride SOAP/gRPC'den de kullanılabilmesini istiyor). Bütün açıklamalar controller'a yazılsaydı da controller'lar yüzlerce satır olurdu.
- Bedeli, açıklama ile kodun iki ayrı dosyada durması; bunu örneklerin kodla uyumunu denetleyen bir testle karşıladım.
- `JSON` içerik türü sabiti arayüzlerde değil `ApiExamples`'ta: arayüzde sabit tanımlamak bir kod kokusu sayılıyor (Sonar kuralı S1214).
**Testler:**
- `OpenApiDocumentationTest` (23 test): uygulamanın ürettiği `/v3/api-docs` belgesini okuyor.
  - 7 uç noktada özet, etiket ve her parametrenin açıklaması ile örneği var.
  - Başarı örneklerinin alanları Response record'larıyla birebir aynı; iç içe görünümler de (`LeaveTypeView`, `BalanceView`, `LeaveTypeRef`, `EmployeeRef`, `PageResponse`, `HistoryEntryView`) denetleniyor.
  - Bütün hata örnekleri `ErrorResponse` biçiminde ve `status` alanı HTTP koduyla tutarlı.
  - İstek örnekleri Request record'larıyla uyumlu (güncellemede yoldan gelen `id` hariç).
  - Örnek geçerli bir JSON değilse ya da bir record'a alan eklenip örnek güncellenmezse test kırılıyor.
- `./mvnw verify`: 313 test, 0 hata.
**Karşılaştığım sorun ve çözümü:**
- Swagger örnekleri elle yazılan metinler; Response'lar değiştikçe sessizce eskiyebilirler → örnekleri record alanlarıyla karşılaştıran test yazıldı.
- Projede daha önce Swagger anotasyonu yoktu; diğer modüller de aynı kalıbı kullanabilsin diye ortak örnekler ayrı bir sınıfta toplandı.
**Sunumda anlatacağım tek cümle:**
Swagger açıklamalarını controller'dan ayrı arayüzlere yazdım ve örneklerin kodla uyumunu bir testle güvenceye aldım; bir cevaba alan eklenip örnek güncellenmezse build kırılıyor.
