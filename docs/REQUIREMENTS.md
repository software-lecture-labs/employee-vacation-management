# Gereksinim Listesi

Bu dosya, analiz dokümanındaki bütün gereksinimlerin projede nerede karşılandığını gösterir. Commit açıklamalarının son satırındaki kodlar (`Refs: #23 · FR-21, BR-05`) bu dosyadaki satırlara karşılık gelir.

- **FR**: fonksiyonel gereksinim · **BR**: iş kuralı · **NFR**: kalite gereksinimi
- ⬜ henüz karşılanmadı · ✅ karşılandı (kod `dev`'e birleşti, testi var, Swagger'da çalışıyor)
- Bir satır yalnızca onu karşılayan PR birleştiğinde, o PR'ın son commit'inde ✅ yapılır ve PR numarası yazılır.

**Özet:** Zorunlu FR 5 / 34 · Bonus FR 0 / 4 · İş kuralı 0 / 18 · Kalite gereksinimi 0 / 11

---

## 1. Fonksiyonel gereksinimler

### 1.1 Kimlik ve erişim

| Durum | Kod | Gereksinim | Endpoint / bileşen | Use case | Sorumlu | PR |
|---|---|---|---|---|---|---|
| ✅ | FR-01 | E-posta ve şifreyle giriş | `POST /api/auth/login` | `Login` | Onur | #104 |
| ✅ | FR-02 | Şifreler BCrypt ile saklanır | `PasswordEncoder` (BCrypt) | `Login` | Onur | #104 |
| ✅ | FR-03 | Rol bazlı koruma, 401 / 403 | `SecurityConfig`, `@PreAuthorize` | Tüm endpoint'ler | Onur | #104 |
| ✅ | FR-04 | JWT üretimi ve doğrulama | `SecurityConfig` (OAuth2 Resource Server, HS256) | `Login` | Onur | #104 |
| ✅ | FR-11 | Kendi profilini görme | `GET /api/me` | `GetMyProfile` | Onur | #104 |

### 1.2 Organizasyon

| Durum | Kod | Gereksinim | Endpoint / bileşen | Use case | Sorumlu | PR |
|---|---|---|---|---|---|---|
| ⬜ | FR-06 | Çalışan ekleme ve ilk bakiyeler (T4) | `POST /api/employees` | `CreateEmployee` | Sude | |
| ⬜ | FR-07 | Çalışan güncelleme | `PUT /api/employees/{id}` | `UpdateEmployee` | Sude | |
| ⬜ | FR-08 | Çalışanı pasife alma | `POST /api/employees/{id}/deactivate` | `DeactivateEmployee` | Sude | |
| ⬜ | FR-09 | Çalışan listesi (sayfalı, filtreli) ve detayı | `GET /api/employees`, `GET /api/employees/{id}` | `ListEmployees`, `GetEmployee` | Sude | |
| ⬜ | FR-10 | Departman ekleme, güncelleme, listeleme | `GET`, `POST /api/departments`, `PUT /api/departments/{id}` | `ListDepartments`, `CreateDepartment`, `UpdateDepartment` | Sude | |

### 1.3 İzin türleri ve bakiye

| Durum | Kod | Gereksinim | Endpoint / bileşen | Use case | Sorumlu | PR |
|---|---|---|---|---|---|---|
| ⬜ | FR-12 | İzin türü tanımlama | `GET`, `POST /api/leave-types`, `PUT /api/leave-types/{id}` | `ListLeaveTypes`, `CreateLeaveType`, `UpdateLeaveType` | Zehra | |
| ⬜ | FR-13 | 4 hazır izin türü | `db/scripts/02_seed_leave_types.sql` | `ListLeaveTypes` | Zehra | |
| ⬜ | FR-15 | İş günü hesabı | `WorkingDayCalculator` | `SubmitLeave` içinde | Zehra | |
| ⬜ | FR-32 | Bakiye görüntüleme | `GET /api/leave-balances/mine`, `GET /api/employees/{id}/leave-balances` | `GetMyBalances`, `GetEmployeeBalances` | Zehra | |
| ⬜ | FR-33 | Kıdem ve yaşa göre hak hesabı | `EntitlementPolicy` | `CreateEmployee`, `RunAnnualAccrual` içinde | Zehra | |
| ⬜ | FR-34 | Yıllık hak ediş (T3) | `POST /api/leave-balances/accruals` | `RunAnnualAccrual` | Zehra | |
| ⬜ | FR-35 | Bakiye hareket kaydı | `LeaveBalanceService`, `balance_transactions` tablosu | Onay, iptal, hak ediş içinde | Zehra | |

### 1.4 İzin talebi ve onay

| Durum | Kod | Gereksinim | Endpoint / bileşen | Use case | Sorumlu | PR |
|---|---|---|---|---|---|---|
| ⬜ | FR-14 | İzin talebi açma | `POST /api/leave-requests` | `SubmitLeave` | Bilge | |
| ⬜ | FR-16 | Bakiye yetmezse talebin reddi | `POST /api/leave-requests` (422) | `SubmitLeave` | Bilge | |
| ⬜ | FR-17 | Çakışan tarih engeli | `POST /api/leave-requests` (409) | `SubmitLeave` | Bilge | |
| ⬜ | FR-18 | Bekleyen talebi geri çekme | `POST /api/leave-requests/{id}/withdraw` | `WithdrawLeave` | Bilge | |
| ⬜ | FR-19 | Kendi taleplerini listeleme ve detay | `GET /api/leave-requests/mine`, `GET /api/leave-requests/{id}` | `GetMyLeaves`, `GetLeaveDetail` | Bilge | |
| ⬜ | FR-20 | Onay bekleyenler listesi | `GET /api/leave-requests/pending` | `GetPendingApprovals` | Bilge | |
| ⬜ | FR-21 | Onay ve bakiye düşümü (T1) | `POST /api/leave-requests/{id}/approve` | `ApproveLeave` | Bilge | |
| ⬜ | FR-22 | Gerekçeli red | `POST /api/leave-requests/{id}/reject` | `RejectLeave` | Bilge | |
| ⬜ | FR-24 | Kendi talebini onaylayamama | `approve` ve `reject` (403) | `ApproveLeave`, `RejectLeave` | Bilge | |

### 1.5 İptal

| Durum | Kod | Gereksinim | Endpoint / bileşen | Use case | Sorumlu | PR |
|---|---|---|---|---|---|---|
| ⬜ | FR-23 | Şirket adına iptal (T2) | `POST /api/leave-requests/{id}/cancel` | `CancelLeave` | Sude | |
| ⬜ | FR-25 | İptal talebi açma | `POST /api/leave-requests/{id}/cancel-request` | `RequestLeaveCancellation` | Sude | |
| ⬜ | FR-28 | İptal talebini kabul ve red (T2) | `POST .../cancel-request/approve`, `POST .../cancel-request/reject` | `ApproveLeaveCancellation`, `RejectLeaveCancellation` | Sude | |
| ⬜ | FR-29 | İptal talebinden vazgeçme | `POST .../cancel-request/withdraw` | `WithdrawLeaveCancellation` | Sude | |
| ⬜ | FR-30 | Yaklaşan iptal talebi uyarısı | `GET /api/leave-requests/pending` içinde acil bayrağı | `CancellationUrgencyPolicy` | Onur | |
| ⬜ | FR-31 | Gece zamanlanmış görev: süresi dolan iptal talebi sonuçsuz olur (T5) | Zamanlayıcı, her gece 00:05 | `ExpireCancellationRequests` | Onur | |

### 1.6 İzin geçmişi

| Durum | Kod | Gereksinim | Endpoint / bileşen | Use case | Sorumlu | PR |
|---|---|---|---|---|---|---|
| ⬜ | FR-26 | İK için şirket izin geçmişi | `GET /api/leave-requests/history` | `SearchLeaveHistory` | Zehra | |
| ⬜ | FR-27 | Durum geçmişi aynı transaction'da yazılır | `LeaveHistoryService`, `leave_request_history` tablosu | Tüm durum değişiklikleri | Zehra | |

### 1.7 Bonus

| Durum | Kod | Gereksinim | Endpoint / bileşen | Use case | Sorumlu | PR |
|---|---|---|---|---|---|---|
| ⬜ | FR-05 | Kendi şifresini değiştirme | `PUT /api/me/password` | `ChangeMyPassword` | Onur | |
| ⬜ | FR-36 | Ekip izin takvimi | `GET /api/reports/team-calendar` | `GetTeamCalendar` | Bilge | |
| ⬜ | FR-37 | Departman izin raporu | `GET /api/reports/department-usage` | `GetDepartmentUsageReport` | Bilge | |
| ⬜ | FR-38 | Resmi tatiller | `GET`, `POST /api/public-holidays` | `ListPublicHolidays`, `CreatePublicHoliday` | Sude | |

---

## 2. İş kuralları

| Durum | Kod | Kural | Uygulandığı yer | Sorumlu | PR |
|---|---|---|---|---|---|
| ⬜ | BR-01 | İzin gününe başlangıç ve bitiş dahildir; sadece Pazartesi–Cuma sayılır | `WorkingDayCalculator` | Zehra | |
| ⬜ | BR-02 | Resmi tatiller izin gününden düşülür (bonus) | `HolidayAwareWorkingDayCalculator` | Sude | |
| ⬜ | BR-03 | Yıllık ve mazeret izni bakiyeden düşer; hastalık ve ücretsiz izin düşmez | `LeaveBalanceService` | Zehra | |
| ⬜ | BR-04 | Mazeret izni yılda en fazla 5 gün | `SubmitLeaveHandler` | Bilge | |
| ⬜ | BR-05 | Bakiye negatife düşmez; talepte ve onayda iki kez kontrol edilir | `SubmitLeaveHandler`, `LeaveBalanceService.deduct` | Bilge, Zehra | |
| ⬜ | BR-06 | Geçmiş tarihe talep yok, bitiş başlangıçtan önce olamaz, en fazla 30 iş günü | `SubmitLeaveValidator` | Bilge | |
| ⬜ | BR-07 | Onaylayıcı sırası: İK'nın talebini başka bir İK, diğerlerini yönetici, yöneticisi yoksa İK onaylar | `ApproverResolver` | Bilge | |
| ⬜ | BR-08 | Onaylı izin sadece başlangıçtan önce iptal edilebilir | `CancelLeaveHandler`, `RequestLeaveCancellationHandler` | Sude | |
| ⬜ | BR-09 | Red ve iptalde gerekçe zorunlu | `RejectLeaveValidator`, `CancelLeaveValidator`, `RequestLeaveCancellationValidator` | Bilge, Sude | |
| ⬜ | BR-10 | Pasif çalışan giriş yapamaz, talep açamaz | `LoginHandler`, `SubmitLeaveHandler` | Onur, Bilge | |
| ⬜ | BR-11 | Yıllık hak ediş aynı yıl iki kez çalıştırılamaz | `RunAnnualAccrualHandler`, `annual_accrual_runs` tablosu | Zehra | |
| ⬜ | BR-12 | Kullanılmayan yıllık izin sonraki yıla devreder (bonus) | `RunAnnualAccrualHandler` | Zehra | |
| ⬜ | BR-13 | En az 2 aktif İK bulunmalı | `UpdateEmployeeHandler`, `DeactivateEmployeeHandler` | Sude | |
| ⬜ | BR-14 | Sadece talep sahibi, sadece bekleyen talebini geri çekebilir | `WithdrawLeaveHandler` | Bilge | |
| ⬜ | BR-15 | İptal yetkisi: yönetici; yöneticisi yoksa İK; İK'nın iznini başka bir İK | `CancellationAuthorizationPolicy` | Sude | |
| ⬜ | BR-16 | Her durum değişikliği aynı transaction'da geçmişe yazılır; yazılamazsa değişiklik geri alınır | `LeaveHistoryService` | Zehra | |
| ⬜ | BR-17 | İptal talebine onaylayıcı karar verir; günler sadece kabul edilince iade edilir | `ApproveLeaveCancellationHandler`, `RejectLeaveCancellationHandler` | Sude | |
| ⬜ | BR-18 | Başlangıca 3 gün veya daha az kalan iptal talebi acil gösterilir; başlangıç günü gelirse sonuçsuz olur | `CancellationUrgencyPolicy`, `ExpireCancellationRequestsHandler` | Onur | |

---

## 3. Kalite gereksinimleri

| Durum | Kod | Gereksinim | Kanıt | Sorumlu | PR |
|---|---|---|---|---|---|
| ⬜ | NFR-01 | Katmanlı mimari: Controller'da iş kuralı yok, her use case Request / Response / Handler / Validator | `ARCHITECTURE.md`, kod incelemesi | Herkes | |
| ⬜ | NFR-02 | Statik analizde Reliability, Security, Maintainability notları A; Quality Gate yeşil | Yerel SonarQube panosu, README'deki tarama tablosu | Sude | |
| ⬜ | NFR-03 | Yeni kodda test kapsamı en az %80 | JaCoCo raporu, SonarQube | Herkes | |
| ⬜ | NFR-04 | Gizli bilgiler koda yazılmaz, ortam değişkeninden okunur | `.env.example`, Sonar'da "hard-coded password" uyarısı yok | Onur | |
| ⬜ | NFR-05 | Tüm hatalar tek biçimde JSON döner; stack trace gösterilmez | `GlobalExceptionHandler` | Bilge | |
| ⬜ | NFR-06 | Gelen veriler doğrulanır | Bean Validation + `XxxValidator` sınıfları ve testleri | Herkes | |
| ⬜ | NFR-07 | Liste dönen adresler sayfalı (varsayılan 20 kayıt) | `PageResponse<T>`, Swagger | Herkes | |
| ⬜ | NFR-08 | Aynı talep aynı anda iki kez onaylanamaz (iyimser kilit) | `@Version`, eşzamanlı onay entegrasyon testi | Bilge | |
| ⬜ | NFR-09 | Proje yeni bir bilgisayarda README adımlarıyla 15 dakikada açılır | Temiz kurulum provası | Sude | |
| ⬜ | NFR-10 | Önemli işlemler SLF4J ile loglanır; `System.out.println` yok | Sonar'da uyarı yok | Herkes | |
| ⬜ | NFR-11 | Kod isimleri İngilizce; README, hata mesajları ve sunum Türkçe | Kod incelemesi | Herkes | |

> NFR-02 ve NFR-03 analiz dokümanında SonarCloud olarak yazılmıştı; hocanın isteğiyle analiz yerel Docker'daki SonarQube ile yapılır.

---

## 4. Transactional işlemler

| Durum | Kod | İşlem | Use case | Geri alınan durum (rollback senaryosu) | Rollback testi | Sorumlu | PR |
|---|---|---|---|---|---|---|---|
| ⬜ | T1 | İzin onayı: durum, geçmiş, bakiye düşümü | `ApproveLeave` | Bakiye yetmezse talep PENDING kalır, geçmiş satırı ve bakiye hareketi oluşmaz | | Bilge | |
| ⬜ | T2 | Onaylı iznin iptali: durum, geçmiş, bakiye iadesi | `CancelLeave`, `ApproveLeaveCancellation` | Herhangi bir adım hata verirse izin APPROVED kalır, gün iade edilmez | | Sude | |
| ⬜ | T3 | Yıllık hak ediş: tüm aktif çalışanlar | `RunAnnualAccrual` | Bir çalışanda hata olursa hiçbir çalışana hak yazılmaz | | Zehra | |
| ⬜ | T4 | Çalışan ekleme ve ilk bakiyeler | `CreateEmployee` | Bakiye adımı hata verirse çalışan da oluşmaz | | Sude | |
| ⬜ | T5 | Süresi dolan iptal taleplerini sonuçsuz yapma | `ExpireCancellationRequests` | Her talep kendi transaction'ında; biri hata verirse diğerleri etkilenmez | | Onur | |

---

## Nasıl güncellenir

1. Gereksinimi karşılayan PR'ın son commit'inde ilgili satırın durumunu ⬜'dan ✅'a çevir ve PR sütununa numarayı yaz (`#23`). Birden çok PR ile karşılanan satıra hepsini yaz (`#18, #31`).
2. En üstteki özet sayısını güncelle.
3. Commit örneği: `docs(report): izin onayı raporunu ekle, gereksinimi işaretle`
4. Endpoint veya sınıf adı değiştiyse aynı commit'te bu dosyadaki adı da düzelt.
