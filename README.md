# İzin Yönetim Sistemi (Employee Vacation Management)

Kolay İK'nın izin modülünün REST API klonu. Çalışan izin talebi açar, yöneticisi ya da
İK onaylar; yıllık izin hakkı kıdem ve yaşa göre hesaplanır, her bakiye ve durum
değişikliği kayıt altına alınır. Arayüz yoktur; bütün işlemler Swagger UI üzerinden
gösterilir.

**Gereksinimler ve ilerleme:** [docs/REQUIREMENTS.md](docs/REQUIREMENTS.md)

> Bu README proje ilerledikçe doldurulan bir iskelettir. Her bölümün altında kimin,
> ne zaman dolduracağı yazar.

## Özellikler

_Her ekip üyesi kendi modülünün özelliklerini 27 Kasım'da ekler._

### Bakiye, hak ediş ve izin geçmişi

| Özellik | Uç nokta | Kim kullanır |
|---|---|---|
| İzin türlerini listeleme | `GET /api/leave-types` | Giriş yapmış herkes |
| İzin türü ekleme ve güncelleme | `POST /api/leave-types`, `PUT /api/leave-types/{id}` | İK |
| Kendi bakiyelerini görme | `GET /api/leave-balances/mine?year=` | Giriş yapmış herkes |
| Bir çalışanın bakiyelerini görme | `GET /api/employees/{id}/leave-balances?year=` | Yönetici (yalnızca kendi ekibi), İK |
| Yıllık hak edişi çalıştırma (T3) | `POST /api/leave-balances/accruals` | İK |
| Kullanılmayan yıllık izni devretme (bonus) | `POST /api/leave-balances/carry-overs?fromYear=` | İK |
| Şirket izin geçmişinde arama | `GET /api/leave-requests/history` | İK |

- **4 hazır izin türü:** Yıllık (bakiyeden düşer), Mazeret (yılda en fazla 5 gün, bakiyeden düşer), Hastalık ve
  Ücretsiz (bakiyeden düşmez). İK yeni tür ekleyebilir; türler silinmez, pasife alınır.
- **Yıllık izin hakkı** kıdem ve yaşa göre hesaplanır: ilk yıl hak yoktur; 1–5 yıl 14 gün, 6–14 yıl 20 gün,
  15 yıl ve üzeri 26 gün; 18 yaş ve altı ile 50 yaş ve üstü en az 20 gün (ilk yıl hariç). Kıdem yalnızca yıl
  farkıyla hesaplanır.
- **İzin günü** başlangıç ve bitiş dahil, yalnızca Pazartesi–Cuma sayılarak hesaplanır.
- **Her bakiye değişikliği** (hak ediş, düşüm, iade, devir) `balance_transactions` tablosuna bir hareket olarak
  yazılır; kalan gün saklanmaz, hesaplanır.
- **Her durum değişikliği** aynı transaction içinde izin geçmişine yazılır; geçmiş yazılamazsa değişiklik de
  geri alınır.
- **Yıllık hak ediş** bütün aktif çalışanlara tek transaction'da yazılır ve aynı yıl için ikinci kez
  çalıştırılamaz. Bir çalışanda hata olursa hiçbir çalışana hak yazılmaz.
- **Devir** yalnızca biten yıldan bu yıla yapılır; aynı gün iki yılda kullanılamaz ve işlem tekrar
  çalıştırılsa da bir şey değişmez.
- **Geçmiş araması** tarih aralığı, departman, durum ve çalışana göre süzülür; sayfa başına en fazla 100 kayıt.

Bütün uç noktaların istek ve cevap örnekleri Swagger UI'dadır.

## Kullanılan Teknolojiler

_Doldurulacak._

## Mimari

_Katman diyagramı ve paket yapısı. Ayrıntı: `docs/ARCHITECTURE.md`._

## Veritabanı Şeması

Veritabanı Supabase'de yönetilen **PostgreSQL 17**; tablolar `leave_mgmt` şemasındadır. Şema
`db/scripts` altındaki SQL scriptleriyle kurulur; uygulama tablo oluşturmaz, açılışta yalnızca entity'lerle
uyumu denetler (`ddl-auto: validate`). Testler aynı scriptlerle açılan geçici bir PostgreSQL kullanır
(Testcontainers).

| Grup | Tablolar |
|---|---|
| Organizasyon | `departments`, `employees` |
| İzin tanımları | `leave_types`, `public_holidays` |
| İzin işlemleri | `leave_requests`, `leave_balances` |
| Kayıt (yalnızca eklenir) | `leave_request_history`, `balance_transactions`, `annual_accrual_runs` |

- 9 tablo; 14 yabancı anahtar, 8 CHECK ve 6 UNIQUE kısıtı, 21 indeks.
- Hiçbir tabloda satır silinmez; çalışan ve izin türü pasife alınır.
- Aynı kayıt aynı anda iki işlemle değişirse iyimser kilit (`@Version`) ikincisini durdurur ve 409 döner.
- Gün sayıları `numeric(4,1)` (yarım gün desteklenir).

ER diyagramı, her kısıtın koruduğu iş kuralı, sorgu incelemesi ve indeks kararları:
[docs/DATABASE_DESIGN.md](docs/DATABASE_DESIGN.md). Scriptlerin sırası ve çalıştırma kaydı:
[db/README.md](db/README.md).

## Kurulum ve Çalıştırma

_Ön koşullar, `.env` ayarı, Docker Compose ve uygulamayı başlatma. Ayrıntı: `docs/DEPLOYMENT.md`._

## Demo Kullanıcıları

`dev` ve `local` profillerinde uygulama açılırken, **çalışan tablosu boşsa** demo verisi yüklenir
(`DataSeeder`): 3 departman, 2 İK, 2 yönetici, 6 çalışan ve hepsinin bu yılki bakiyeleri. Bütün
kullanıcıların şifresi `.env`'deki `DEMO_PASSWORD` değeridir (grupta değil, özel mesajla paylaşılır);
boşsa demo verisi yüklenmez. Testlerde ve teslim profilinde demo verisi yoktur.

| Ad Soyad | E-posta | Rol | Departman | Yönetici | Yıllık izin (bu yıl) | Neyi gösterir |
|---|---|---|---|---|---|---|
| Zeynep Aydın | `zeynep.aydin@demo.local` | HR | İnsan Kaynakları | — | 20 | İK-1 (demo 7: kendi talebini onaylayamaz) |
| Murat Çelik | `murat.celik@demo.local` | HR | İnsan Kaynakları | — | 14 | İK-2 (İK-1'in talebini onaylar) |
| Ahmet Yılmaz | `ahmet.yilmaz@demo.local` | MANAGER | Yazılım Geliştirme | — | 26 | 15+ yıl kıdem |
| Elif Kaya | `elif.kaya@demo.local` | MANAGER | Satış | — | 20 | |
| Ayşe Demir | `ayse.demir@demo.local` | EMPLOYEE | Yazılım Geliştirme | Ahmet Yılmaz | 14 | 1–5 yıl kıdem |
| Can Öztürk | `can.ozturk@demo.local` | EMPLOYEE | Yazılım Geliştirme | Ahmet Yılmaz | 20 | 6–14 yıl kıdem |
| Ece Arslan | `ece.arslan@demo.local` | EMPLOYEE | Yazılım Geliştirme | Ahmet Yılmaz | 0 | Bu yıl işe girdi |
| Burak Şahin | `burak.sahin@demo.local` | EMPLOYEE | Satış | Elif Kaya | 20 | 18 yaş koruması |
| Selin Koç | `selin.koc@demo.local` | EMPLOYEE | Satış | Elif Kaya | 20 | 50 yaş koruması |
| Deniz Aksoy | `deniz.aksoy@demo.local` | EMPLOYEE | Satış | — | 14 | Yöneticisi yok, talebini İK onaylar (BR-07) |

Herkesin ayrıca 5 günlük mazeret izni bakiyesi vardır. Bakiyeler yıllık hak edişle aynı kuralla
(`LeaveBalanceService.openYearBalances`) açılır; `annual_accrual_runs` tablosuna kayıt yazılmadığı için
bu yıl için T3 yine çalıştırılabilir.

## API Dokümantasyonu

_Swagger UI adresi ve örnek istekler. Ayrıntı: `docs/API_DOCUMENTATION.md`._

## Transaction Senaryoları

_Beş transactional işlem (T1–T5) ve rollback testleri._

| # | İşlem | Bir adım hata verirse | Rollback testi |
|---|---|---|---|
| T2 | Onaylı iznin iptali (şirket adına iptal veya iptal talebinin kabulü): durum, geçmiş satırı, gün iadesi | İzin APPROVED (veya CANCEL_REQUESTED) kalır, geçmiş satırı ve iade hareketi oluşmaz, gün iade edilmez | `LeaveCancellationIntegrationTest` |
| T3 | Yıllık hak ediş: bütün aktif çalışanlara hak yazılır, çalıştırma kaydedilir | Hiçbir çalışana hak yazılmaz, çalıştırma kaydı oluşmaz; yıl yeniden çalıştırılabilir | `RunAnnualAccrualIntegrationTest` |
| T4 | Çalışan ekleme: çalışan, ilk şifre ve bu yılın bakiyeleri | Çalışan da oluşmaz; e-posta yeniden kullanılabilir | `CreateEmployeeIntegrationTest` |

## Testleri Çalıştırma

_Komutlar ve test türleri. Ayrıntı: `docs/TEST_PLAN.md`._

## SOLID Uygulaması

_Her ilke için projedeki somut örnek._

## SonarQube Analysis

Statik analiz yerelde, Docker'da çalışan SonarQube Community Build ile yapılır; test kapsamını JaCoCo ölçer.
Analiz her zaman `dev` dalından, teslimden önce son kez `main` dalından alınır.

**İlk kurulum (bir kez)**

1. `.env` dosyasında `SONAR_DB_PASSWORD` değerini doldurun (sadece yerel Sonar veritabanının şifresi).
2. SonarQube'u başlatın; ilk açılış 1–2 dakika sürer:
   ```bash
   docker compose -f docker-compose.sonar.yml up -d
   ```
3. http://localhost:9000 adresine `admin` / `admin` ile girin ve istenen yeni şifreyi belirleyin.
4. **Create Project → Local project**: anahtar `employee-vacation-management`, ana dal `dev`.
5. **My Account → Security → Generate Tokens** ile bir analiz token'ı üretin ve `.env` içindeki `SONAR_TOKEN` satırına yazın. Token repoya girmez.

**Analiz**

```bash
./mvnw clean verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar -Dsonar.token=<SONAR_TOKEN>
```

`verify` testleri çalıştırır ve `target/site/jacoco/` altına kapsam raporunu yazar (HTML: `target/site/jacoco/index.html`);
ardından analiz SonarQube'a gönderilir. Sunucu adresi ve proje anahtarı `pom.xml`'de tanımlıdır.
Testler Testcontainers kullandığı için Docker'ın açık olması gerekir. SonarQube'u kapatmak için:
`docker compose -f docker-compose.sonar.yml down` (veriler silinmez).

**Quality Gate:** "Sonar way": yeni kodda kapsam en az %80, tekrar en fazla %3, güvenlik sıcak noktalarının
tamamı incelenmiş; ayrıca tüm kodda Reliability, Security ve Maintainability notları A.

**Tarama geçmişi** (her taramada bir satır eklenir; ekran görüntüleri `docs/sonar/` altında)

| Tarih | Dal / commit | Bug | Zafiyet | Kod kokusu | Kapsam | Tekrar | Notlar (R / S / M) | Quality Gate |
|---|---|---|---|---|---|---|---|---|
| 20.11.2026 | `dev` / `12cf0b0` | 2 | 0 | 0 | %89.7 | %0.0 | D / A / A | Passed |

İlk taramada (temel çizgi, 2.258 satır kod) Reliability notu D çıktı. Nedeni
`WeekdayWorkingDayCalculator` içindeki tek satır: `DayOfWeek` karşılaştırması `!=` ile yapılmış
(kural `java:S8696`; Sonar aynı satırı iki kez sayıyor). Enum için `!=` doğrudur, ama
satırın `switch` ile yeniden yazılmasıyla uyarı kapanacak. Ekran görüntüsü:
[`docs/sonar/sonar-2026-11-20.png`](docs/sonar/sonar-2026-11-20.png).

## Teknik Kararlar

**REST yerine SOAP ya da gRPC kullanılsaydı ne değişirdi?** Yalnızca giriş katmanı. Controller'lar iş
yapmaz; isteği bir Request nesnesine çevirip mediator'a verir. SOAP endpoint'i ya da gRPC servisi de aynı
nesneyi aynı mediator'a verir. Use case'ler, iş kuralları, veritabanı ve transaction'lar değişmez;
`application` ve `domain` paketlerinde HTTP'ye ya da Swagger'a ait hiçbir import yoktur.

| Dönüşüm | Kurulum | 8 temel izin işlemi | Bütün işlemler |
|---|---|---|---|
| SOAP (Spring-WS) | 1–1,5 kişi-gün | 3–4 kişi-gün | 8–12 kişi-gün |
| gRPC (Spring gRPC) | 1,5–2 kişi-gün | 4–5 kişi-gün | 9–13 kişi-gün |

Değişen bileşenler tablosu, katmanlar arası bağımlılık kararı ve 13 karar kaydı (karar, neden, alternatif,
bedeli): [docs/TECHNICAL_DECISIONS.md](docs/TECHNICAL_DECISIONS.md).

## Ekip

| Ad | GitHub | Sorumlu olduğu modül |
|---|---|---|
| Zehra | @Zehranur04 | Bakiye, hak ediş ve geçmiş; veritabanı |
| Sude | @sudedereli | Organizasyon ve iptal akışı |
| Bilge | @bilgeerol | İzin talebi ve onay akışı |
| Onur | @onurky5149 | Kimlik, güvenlik ve dağıtım |

## Team Contributions

_Kişisel raporlardan (`docs/reports/`) her üye için 5–8 maddelik özet; 28 Kasım._

## Ders Kriterleri

_Her ders kriterinin projedeki kanıtına bağlantı; 28 Kasım._
