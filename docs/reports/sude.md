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
**Sunumda anlatacağım tek cümle:**
Kod kalitesini ilk günden yerel SonarQube ile ölçtük; her taramanın sonucu README'deki tabloda, kapsamı da JaCoCo her `verify`'da raporluyor.
**Açık kalanlar:**
- İlk tarama (temel çizgi) ve sonucun README tablosuna eklenmesi.

## 20 Kasım 2026 — feat(department): departmanları sayfalı listelemeyi ekle (#<PR no>)
**İlgili gereksinimler:** FR-10 (listeleme; ekleme ve güncelleme ayrı PR'da), NFR-01, NFR-07
**Use case(ler):** `ListDepartments`
**Ne yaptım:**
- `GET /api/departments?page&size`: departmanlar ada göre sıralı, varsayılan 20 kayıtlık sayfalarla döner.
- `ListDepartmentsRequest` (sayfalama sınırları anotasyonla), `ListDepartmentsHandler` (`@Transactional(readOnly = true)`), `DepartmentController`.
- Ortak `DepartmentView` ve `DepartmentMapper`: ekleme ve güncelleme cevapları da aynı görünümü kullanacak.
**Validator'a koyduğum kurallar:**
- Ayrı Validator yok; `page ≥ 0` ve `size` 1–100 kuralları istek record'undaki anotasyonlarla mediator'da denetleniyor (alanlar arası kural yok).
**Handler'daki kurallar ve transaction:**
- Salt okunur transaction; sıralama `name`, ardından `id` (aynı sayfa her istekte aynı sırada gelsin diye).
**Neden böyle yaptım (tasarım kararları):**
- Cevap ayrı bir Response record'u değil, ortak `PageResponse<DepartmentView>`: Zehra'nın geçmiş araması da sayfalı listeyi böyle dönüyor, istemci bütün sayfalı adreslerde aynı JSON'u görüyor.
- Spring'in `Page` nesnesi dışarı verilmiyor; JSON biçimi Spring sürümüne bağlı kalmıyor.
- Controller'da iş kuralı yok: sadece isteği kurup `mediator.send(...)` diyor.
**Testler:**
- `ListDepartmentsHandlerTest` — repository'ye doğru sayfa ve sıralamanın gitmesi, view'a çevirme (Mockito)
- `ListDepartmentsRequestTest` — boş sayfalamada 0. sayfa ve 20 kayıt
- `DepartmentControllerTest` — 200, sorgu parametreleri ve 400 hata biçimi (MockMvc)
- `ListDepartmentsIntegrationTest` — gerçek PostgreSQL ile ada göre sıra, ikinci sayfa ve geçersiz sayfalamada 400
**Karşılaştığım sorun ve çözümü:**
- Güvenlik altyapısı henüz dev'de olmadığı için "giriş yapmış herkes" kontrolü eklenemedi; güvenlik PR'ı birleşince eklenecek.
**Sunumda anlatacağım tek cümle:**
Departman listesi Controller → Mediator → Handler → Repository yolunu en sade haliyle gösteren referans use case'tir; sayfalama ve hata biçimi bütün listelerle aynıdır.
**Açık kalanlar:**
- Kimlik kontrolü (güvenlik PR'ından sonra), Swagger örnekleri (24 Kasım).
