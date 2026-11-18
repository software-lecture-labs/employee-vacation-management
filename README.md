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

## Kullanılan Teknolojiler

_Doldurulacak._

## Mimari

_Katman diyagramı ve paket yapısı. Ayrıntı: `docs/ARCHITECTURE.md`._

## Veritabanı Şeması

_Tablo diyagramı ve kararlar. Ayrıntı: `docs/DATABASE_DESIGN.md`; script kaydı: [db/README.md](db/README.md)._

## Kurulum ve Çalıştırma

_Ön koşullar, `.env` ayarı, Docker Compose ve uygulamayı başlatma. Ayrıntı: `docs/DEPLOYMENT.md`._

## Demo Kullanıcıları

_Rol, e-posta ve şifre tablosu._

## API Dokümantasyonu

_Swagger UI adresi ve örnek istekler. Ayrıntı: `docs/API_DOCUMENTATION.md`._

## Transaction Senaryoları

_Beş transactional işlem (T1–T5) ve rollback testleri._

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
| _ilk tarama bekleniyor_ | | | | | | | | |

## Teknik Kararlar

_SOAP/gRPC'ye uyarlanabilirlik ve diğer kararlar. Ayrıntı: `docs/TECHNICAL_DECISIONS.md`._

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
