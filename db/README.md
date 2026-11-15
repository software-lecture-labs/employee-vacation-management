# Veritabanı

Bu klasör projenin SQL scriptlerini ve bu scriptlerin ortak Supabase
veritabanında ne zaman çalıştırıldığının kaydını tutar.
Veritabanı sorumlusu: Zehranur Burmaoğlu.

## Ortam

| Öğe | Değer |
|---|---|
| Sağlayıcı | Supabase (yönetilen PostgreSQL) |
| PostgreSQL sürümü | 17.11 |
| Bölge | Northeast Asia (Seoul), `ap-northeast-2`. Proje oluşturulurken bölge "Auto" bırakıldı; Supabase'de bölge sonradan değiştirilemez. |
| Şema | `leave_mgmt` (public değil) |
| Data API | Proje oluşturulurken kapatılamadı. `leave_mgmt` şeması Data API'nin "Exposed schemas" listesinde yoktur ve eklenmeyecektir. |

Uygulama veritabanına JDBC ile, Session pooler (port 5432) üzerinden ve
`sslmode=require` ile bağlanır. Bağlantı bilgileri ve şifre bu repoya
yazılmaz; ortam değişkenlerinden okunur (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`).

## Scriptler ve çalıştırma sırası

| Sıra | Dosya | Ne yapar |
|---|---|---|
| 1 | `scripts/01_schema.sql` | `leave_mgmt` şeması, 9 tablo, CHECK/tekil kısıtlar, indeksler |
| 2 | `scripts/02_seed_leave_types.sql` | 4 hazır izin türü: ANNUAL, EXCUSE (5 gün), SICK, UNPAID (FR-13) |
| 3 | `scripts/03_security.sql` | 9 tabloda Row Level Security (politikasız) |

Aynı dosyalar Supabase SQL Editor'da, Testcontainers testlerinde ve
Docker compose'daki `local-db` servisinde aynı sırayla çalışır.

## Kurallar

- Çalıştırılmış bir script bir daha değiştirilmez. Her şema değişikliği yeni
  numaralı bir scripttir (`04_...sql`) ve entity değişikliğiyle aynı PR'da gelir.
- Supabase SQL Editor'da tablo değiştiren komutu yalnızca veritabanı sorumlusu
  çalıştırır. İhtiyacı olan issue açar.
- Scriptler tekrar çalıştırılabilir yazılır (`if not exists`, `on conflict do nothing`)
  ve her biri tek transaction içindedir (`begin; ... commit;`).
- Supabase'de çalıştırılan içerik, repodaki dosyanın birebir aynısıdır.
- Şifre ve bağlantı bilgisi bu repoya, issue'lara ve grup sohbetine yazılmaz.

## Çalıştırma kaydı (Supabase)

| Tarih (UTC+3) | Script | Çalıştıran | Sonuç ve kontrol |
|---|---|---|---|
| 15.11.2026, ~23:15–23:20 | `01_schema.sql` | Zehranur Burmaoğlu | Başarılı. 9 tablo; kısıtlar: 9 PK, 14 FK, 8 CHECK, 6 UNIQUE; 21 index |
| 15.11.2026, ~23:15–23:20 | `02_seed_leave_types.sql` | Zehranur Burmaoğlu | Başarılı. 4 izin türü eklendi, değerler doğrulandı |
| 15.11.2026, ~23:15–23:20 | `03_security.sql` | Zehranur Burmaoğlu | Başarılı. 9 tablonun tamamında RLS açık |

### Notlar

- `01_schema.sql` çalıştırılırken SQL Editor "RLS açılmadan tablo oluşturuluyor"
  uyarısı verdi. "Run without RLS" seçildi: RLS bilinçli olarak `03_security.sql`
  ile açılıyor ve çalıştırılan içeriğin repodaki dosyayla aynı kalması gerekiyor.
  03 birkaç dakika içinde çalıştırıldı; bu sürede tablolar boştu ve şema Data
  API'ye açık değildi.
- Security Advisor (15.11.2026, 23:27): uyarı (WARN/ERROR) yok. 9 tabloda
  INFO seviyesinde "RLS Enabled No Policy" kaydı var. Bu bilinçli bir tercih:
  politika olmadığı için Data API rolleri hiçbir satırı göremez, uygulama ise
  tabloların sahibi olan kullanıcıyla bağlandığı için RLS'ten etkilenmez.

## Kontrol sorguları

Scriptlerden sonra veritabanının doğru kurulduğunu doğrulamak için:

```sql
-- Beklenen: 9
select count(*) as tablo_sayisi
from information_schema.tables where table_schema = 'leave_mgmt';

-- Beklenen: c 8, f 14, p 9, u 6
select contype, count(*)
from pg_constraint where connamespace = 'leave_mgmt'::regnamespace
group by contype order by contype;

-- Beklenen: 21
select count(*) as index_sayisi
from pg_indexes where schemaname = 'leave_mgmt';

-- Beklenen: 4 satır (ANNUAL, EXCUSE, SICK, UNPAID)
select code, deducts_from_balance, annual_limit_days
from leave_mgmt.leave_types order by id;

-- Beklenen: 9 satır, hepsi true
select relname, relrowsecurity
from pg_class where relnamespace = 'leave_mgmt'::regnamespace and relkind = 'r';
```
