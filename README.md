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

_Her taramanın sonucu bir satır olarak eklenir: tarih, commit, bug, zafiyet, kod kokusu, kapsam, tekrar, notlar. Ekran görüntüleri: `docs/sonar/`._

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
