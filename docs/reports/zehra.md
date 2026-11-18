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
