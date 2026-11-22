# Katkı Kuralları

Bu dosya ekibin dört üyesi için bağlayıcıdır. Özet: **her görev bir issue, her issue yeni bir branch, her branch bir PR; her commit başlık + açıklama taşır.**

Gereksinim kodlarının (`FR-21`, `BR-05`, `NFR-03`, `T1` …) ne anlama geldiği ve hangisinin hangi PR ile karşılandığı [`docs/REQUIREMENTS.md`](docs/REQUIREMENTS.md) dosyasındadır.

---

## 1. Altın kurallar

1. Her iş bir GitHub issue'dur; issue'su olmayan kod yazılmaz.
2. Her yeni görev, `dev`'in en güncel halinden açılan yeni bir branch'te başlar. Bir branch = bir görev = bir PR. Biten branch tekrar kullanılmaz.
3. `main` ve `dev`'e doğrudan push yoktur; her değişiklik PR ile gelir (küçük bir yazım hatası bile).
4. Her commit tek bir mantıksal değişikliktir, derlenir ve testleri geçer.
5. Her commit'in başlığı ve açıklaması vardır (biçim [4. bölümde](#4-commit-mesajı-başlık--açıklama)). Açıklamasız commit yoktur.
6. PR'lar **"Rebase and merge"** ile birleşir. "Squash and merge" yasaktır: küçük ve anlamlı commit'leri tek commit'e ezer ve katkıyı görünmez yapar.
7. Her PR en az bir onay alır; entity veya SQL script değiştiren PR'da veritabanı sorumlusunun (Zehra) onayı şarttır.
8. Kimse başkasının branch'ine commit atmaz. Birlikte çalışıldıysa commit'e `Co-authored-by:` satırı eklenir; böylece GitHub katkıyı ikisine de yazar.
9. Herkes her çalışma günü en az bir anlamlı commit atar ve günü açık bir PR ya da PR'a eklenmiş commit'lerle kapatır. Sayı şişirmek için anlamsız commit atılmaz.
10. `.env`, şifreler, token'lar, `target/`, `.idea/` asla commit edilmez.

## 2. Bir görevin yaşam döngüsü

```bash
# 0) İlk gün bir kez: kimlik ve commit şablonu
git config user.name "Ad Soyad"
git config user.email "github-hesabindaki@eposta.com"   # GitHub'daki e-postayla aynı olmalı
git config commit.template .gitmessage

# 1) Güncel dev'i al
git switch dev
git pull --ff-only origin dev

# 2) Görev için yeni branch aç
git switch -c feature/leave-approval-with-balance-deduction

# 3) Küçük adımlarla çalış; her adımda commit (editör şablonla açılır)
git add -p
git commit

# 4) PR açmadan önce dev'in son halini al ve testleri koştur
git fetch origin
git rebase origin/dev
./mvnw verify

# 5) Push ve PR
git push -u origin feature/leave-approval-with-balance-deduction
#    GitHub'da base: dev olacak şekilde PR aç, şablonu doldur, inceleyiciyi ekle

# 6) Birleştiği gün temizlik ve yeni görev
git switch dev && git pull --ff-only origin dev
git branch -d feature/leave-approval-with-balance-deduction
```

- `git commit -m "..."` tek satırlık kullanımı yasaktır, çünkü açıklama yazmayı atlatır.
- Rebase sonrası push reddedilirse **sadece kendi branch'inde** `git push --force-with-lease` kullanılır.
- `dev`'den kendi branch'ine `Merge branch 'dev' into ...` commit'i alınmaz; rebase kullanılır.

## 3. Branch adları

Biçim: `<tür>/<işi-anlatan-ad>`. Branch adında numara, FR kodu veya issue numarası olmaz; ad tek başına okunduğunda ne yapıldığını anlatmalıdır. Ad 3–6 kelimelik, küçük harfli, kelimeleri tireyle ayrılmış ve İngilizcedir.

| Tür | Ne için | Örnek |
|---|---|---|
| `feature/` | Yeni FR veya endpoint | `feature/leave-approval-with-balance-deduction` |
| `fix/` | Hata düzeltme | `fix/same-day-leave-overlap-check` |
| `test/` | Sadece test ekleyen iş | `test/concurrent-approval-optimistic-locking` |
| `db/` | SQL script ve scaffold | `db/initial-schema-and-seed-scripts` |
| `refactor/` | Davranışı değiştirmeyen düzenleme | `refactor/approval-extract-resolver` |
| `docs/` | README ve dokümanlar | `docs/readme-installation` |
| `chore/`, `ci/` | Ayar, bağımlılık, pipeline | `ci/github-actions-build-and-test` |

Bir branch en fazla 1–2 gün yaşar; daha uzun yaşayan branch çakışma üretir.

## 4. Commit mesajı: başlık + açıklama

Repodaki [`.gitmessage`](.gitmessage) şablonu `git config commit.template .gitmessage` ile açılır:

```text
<tür>(<kapsam>): <ne yapılıyor, emir kipinde, en fazla 72 karakter>

Neden: <bu değişiklik neden gerekli; kodu bilmeyen birinin anlayacağı cümle>
Ne değişti:
- <madde>
- <madde>
Test: <hangi test eklendi veya nasıl doğrulandı>

Refs: #<issue> · <karşılanan FR/BR/NFR kodları>
```

### Başlık kuralları

- **Tür** (Conventional Commits): `feat`, `fix`, `test`, `refactor`, `docs`, `build`, `ci`, `chore`, `perf`.
- **Kapsam**: işin yapıldığı modülün kısa İngilizce adı. Gereksinim kodu (`FR-21` gibi) başlığa yazılmaz; başlık tek başına anlaşılmalıdır, kodlar `Refs` satırına gider.

  | Alan | Kapsamlar |
  |---|---|
  | Kimlik (Onur) | `auth` (giriş, profil, şifre) |
  | Organizasyon (Sude) | `employee`, `department` |
  | Bakiye ve geçmiş (Zehra) | `leave-type`, `balance`, `accrual`, `leave-history` |
  | Talep ve onay (Bilge) | `leave-request`, `leave-approval` |
  | İptal (Sude; gece görevi ve acil uyarısı Onur) | `leave-cancellation` |
  | Bonus | `holiday`, `team-calendar`, `usage-report` |
  | Altyapı | `skeleton`, `db`, `mediator`, `error-handling`, `security`, `ci`, `docker`, `sonar`, `readme`, `swagger`, `contributing`, `requirements`, `report` |

- **Özet** Türkçe, emir kipinde ve küçük harfle başlar: "ekle", "düzelt", "kaldır", "taşı", "güncelle". Sonunda nokta yok, en fazla 72 karakter. Kod adları İngilizce kalır.
- Başlık "ne" sorusunu cevaplar ve tek başına okunabilir olmalıdır: commit listesinde sadece başlık görünür.

### Açıklama kuralları

- Başlıktan sonra bir boş satır; satırlar 72 karakterde kırılır.
- **Neden** satırı zorunludur: kodu okuyan "ne"yi görür, "neden"i sadece buradan öğrenir. Gereksinim kodlarını bilmeyen birinin de anlayacağı düz bir cümledir.
- **Ne değişti** 1–4 madde; sınıf veya metot adları yazılır.
- **Test** satırı `feat` ve `fix` için zorunludur; diğerlerinde `Test: gerekmez (sadece doküman)` yazılabilir.
- Son satır `Refs: #<issue> · FR-21, BR-05`: issue numarası ve karşılanan gereksinim kodları. Kodların anlamı [`docs/REQUIREMENTS.md`](docs/REQUIREMENTS.md)'dedir.
- Birlikte çalışıldıysa en alta `Co-authored-by: Ad Soyad <eposta>` eklenir.

### İyi örnek

```text
feat(leave-approval): izin onayında bakiye düşümünü ekle

Neden: Onaylanan talebin günleri yıllık izin bakiyesinden düşülmeli;
talebin durumu, geçmiş kaydı ve bakiye tek transaction'da değişmeli.
Ne değişti:
- ApproveLeaveHandler.handle() talebi APPROVED yapar, geçmiş yazar
- LeaveBalanceService.deduct() çağrılır; metot @Transactional
Test: ApproveLeaveHandlerTest'e 4 birim testi eklendi, ./mvnw verify yeşil.

Refs: #23 · FR-21, BR-05
```

### Yasak örnekler

| Commit başlığı | Sorun |
|---|---|
| `update`, `fix`, `düzeltme` | Neyin değiştiği anlaşılmıyor |
| `son hali`, `çalışıyor artık`, `asdf` | Profesyonel değil |
| `feat(FR-21): onay` | Başlıkta gereksinim kodu var; kod `Refs` satırına yazılır |
| `feat: onay akışı tamamlandı (42 dosya)` | Bir günlük iş tek commit'e gömülmüş; parçalanmalı |
| `Merge branch 'dev' into feature/...` | Gereksiz merge; rebase kullanılır |
| `WIP` | Yarım iş `dev`'e gidemez; branch'te bile anlamlı ad verilir |

### Yanlış mesajı düzeltme

- Henüz push edilmediyse: `git commit --amend`
- Push edildi ama PR birleşmediyse: `git rebase -i origin/dev` ile `reword`
- `dev`'e girmiş commit'in geçmişi yeniden yazılmaz.

## 5. Pull Request kuralları

- **Başlık**: commit başlığıyla aynı biçim, işin bütününü anlatır: `feat(leave-approval): izin onayı ve bakiye düşümü`.
- **Base**: her zaman `dev`. Sadece sürüm PR'ları `dev → main` yönündedir.
- **Birbirine bağlı PR'lar**: Bir branch başka bir açık PR'ın branch'i üzerine kurulduysa PR'ın base'i geçici olarak o branch olabilir; ama **birleştirmeden önce base mutlaka `dev`'e çevrilir** (önceki PR birleşince `git rebase origin/dev` + `git push --force-with-lease`). Base'i `dev` olmayan PR birleştirilmez; aksi halde değişiklik `dev`'e hiç ulaşmaz.
- **Boyut**: en fazla ~400 satır değişiklik; büyükse bölünür. İş yarımken görünürlük için Draft PR açılabilir.
- **Açıklama**: [`.github/pull_request_template.md`](.github/pull_request_template.md) şablonu doldurulur.
- **İnceleme sırası**: Onur'un PR'ını Sude, Sude'ninkini Bilge, Bilge'ninkini Zehra, Zehra'nınkini Onur inceler. Veritabanına dokunan her PR'a ayrıca Zehra eklenir (`CODEOWNERS` bunu otomatik yapar).
- **İnceleme süresi**: en geç 4 saat. İnceleyen en az bir anlamlı yorum ya da "Onay: şunları kontrol ettim …" yazar; boş onay verilmez.
- **Düzeltmeler** yeni commit olarak gelir ve onlar da kurala uyar: `refactor(leave-approval): bakiye kontrolünü ayrı metoda taşı`.
- **Birleştirme**: CI yeşil + onay sonrası PR sahibi base'in `dev` olduğunu kontrol eder, "Rebase and merge" yapar ve branch'i siler.
- **Issue'yu kapatma**: GitHub `Closes #` anahtar kelimesini yalnızca varsayılan dala (`main`) birleşen PR'larda işletir; `dev`'e birleşen PR issue'yu kapatmaz. PR birleşince issue elle kapatılır ve PR numarası yazılır: "#97 ile tamamlandı." `Closes #` satırı yine yazılır: sürüm PR'ı `main`'e geçtiğinde bağlantıyı gösterir.
- **Gereksinim ve rapor**: PR'ın son commit'i rapor commit'idir (`docs(report): izin onayı raporunu ekle, gereksinimi işaretle`). Bu commit'te `docs/reports/<ad>.md`'ye bölüm eklenir ve [`docs/REQUIREMENTS.md`](docs/REQUIREMENTS.md)'de karşılanan satırlar ⬜'dan ✅'a çevrilip PR numarası yazılır. Ayrı PR açılmaz.
- **Sürüm PR'ları** (`dev → main`): 16 Kasım `v0.1-setup` (Onur), 24 Kasım `v0.5-features` (Sude), 26 Kasım `v0.9-quality` (Bilge), 30 Kasım `v1.0` (Zehra). Başlık `release: v0.5-features`, açıklamada o sürümde kapanan FR listesi; "Create a merge commit" ile birleştirilir ve etiketlenir.

## 6. GitHub ayarları ve günlük ritim

- **CODEOWNERS**: `db/scripts/` ve `**/domain/model/` için Zehra.
- **Branch koruması** (`main`, `dev`): doğrudan push yok, CI yeşil, 1 onay, "Rebase and merge" dışındaki yöntemler kapalı. Ayar açılamazsa kural ekip içinde uygulanır.
- **Etiketler**: `modul:kimlik` (Onur), `modul:organizasyon-iptal` (Sude), `modul:talep-onay` (Bilge), `modul:bakiye-gecmis` (Zehra), `db`, `bonus`, `docs`. Milestone'lar aşamalardır.
- **GitHub Projects panosu**: Yapılacak / Yapılıyor / İncelemede / Bitti. Issue'yu başlatan kişi kendini atar ve kartı taşır.
- **Günlük ritim**: 10:00'da grupta yazılı durum (dün / bugün / engel); günün PR'ları en geç 21:00'de açık; inceleme aynı akşam.
- **SonarQube** yerelde çalıştığı için CI'da Sonar adımı yoktur; herkes PR açmadan önce IDE'deki uyarıları sıfırlar.
