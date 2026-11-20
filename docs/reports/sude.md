# Çalışma Raporu: Sude

- **GitHub:** @sudedereli
- **Modül:** Organizasyon ve iptal akışı (`employee`, `department`, `leave-cancellation`)
- **Sorumlu olduğum gereksinimler:** FR-06, FR-07, FR-08, FR-09, FR-10, FR-23, FR-25, FR-28, FR-29 · bonus FR-38 · BR-02, BR-08, BR-09, BR-13, BR-15, BR-17 · NFR-02, NFR-09 · T2, T4
- **Altyapı işleri:** GitHub Projects panosu, etiketler, milestone'lar ve issue'lar; SonarQube kurulumu ve taramalar; DataSeeder; referans use case (ListDepartments)

---

## 18 Kasım 2026 — build(sonar): yerel SonarQube ve JaCoCo kapsam raporunu ekle (#<PR no>)
**İlgili gereksinimler:** NFR-02, NFR-03 (altyapı; notların A olması ve %80 kapsam 25–26 Kasım'da ölçülecek)
**Use case(ler):** Yok (altyapı)
**Ne yaptım:**
- `docker-compose.sonar.yml`: `sonarqube:community` ve ona ait `postgres:17-alpine`, port 9000; veriler volume'larda kalıcı.
- `pom.xml`: `jacoco-maven-plugin` 0.8.14 (`prepare-agent` + `verify` aşamasında `report`), sürümü sabitlenmiş `sonar-maven-plugin` 5.8.0.7211; `sonar.projectKey`, `sonar.projectName`, `sonar.host.url` property olarak.
- `.env.example`: `SONAR_DB_PASSWORD` ve `SONAR_TOKEN` satırları.
- README "SonarQube Analysis": kurulum, analiz komutu, Quality Gate ölçütleri ve tarama geçmişi tablosu.
- Proje yönetimi (kod dışı): GitHub Projects panosu (Yapılacak / Yapılıyor / İncelemede / Bitti), 7 etiket, 8 aşama milestone'u ve plandaki tüm görevler için 81 issue.
**Validator'a koyduğum kurallar:**
- Yok (kod yok)
**Handler'daki kurallar ve transaction:**
- Yok
**Neden böyle yaptım (tasarım kararları):**
- SonarQube'un kendi PostgreSQL'i var: gömülü H2 sadece deneme içindir, sürüm yükseltmede veriler taşınamaz.
- Sonar veritabanı şifresi compose dosyasına yazılmadı, `.env`'den okunuyor; tanımlı değilse compose açık bir hata mesajıyla durur (NFR-04).
- Sonar eklentisinin sürümü sabit: Sonar sürümsüz `sonar:sonar` kısayolunu kaldırdı, sabit sürüm yükseltmeleri bilinçli yapmamızı sağlar.
- JaCoCo raporu `verify` aşamasında üretiliyor; böylece CI'daki `./mvnw verify` de kapsam raporunu çıkarıyor, Sonar ayrı bir adım istemiyor.
**Testler:**
- Yeni test yok; `./mvnw verify` ile JaCoCo raporunun `target/site/jacoco/` altında oluştuğu doğrulanır.
**Karşılaştığım sorun ve çözümü:**
- Bilgisayarımda Docker kurulumu WSL hatası verdi; ayar dosyaları önce hazırlandı, ilk tarama Docker kurulunca yapılacak.
- 20 Kasım: WSL özellikleri açılıp Docker kuruldu ve ilk tarama yapıldı: Security A, Maintainability A, kapsam %89.7, tekrar %0.0, Quality Gate geçti; Reliability D (`WeekdayWorkingDayCalculator`'da tek uyarı, düzeltme Zehra'ya iletildi). Sonuç README tablosunda, ekran görüntüsü `docs/sonar/` altında (#<PR no>).
**Sunumda anlatacağım tek cümle:**
Kod kalitesini ilk günden yerel SonarQube ile ölçtük; her taramanın sonucu README'deki tabloda, kapsamı da JaCoCo her `verify`'da raporluyor.
**Açık kalanlar:**
- İlk tarama (temel çizgi) ve sonucun README tablosuna eklenmesi.
