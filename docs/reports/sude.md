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

## 27 Kasım 2026 — feat(department): departman ekleme ve güncellemeyi ekle (#117)
**İlgili gereksinimler:** FR-10 (tamamlandı: listeleme #101, ekleme ve güncelleme bu PR)
**Use case(ler):** `CreateDepartment`, `UpdateDepartment`
**Ne yaptım:**
- `POST /api/departments` (201 ve `Location` başlığı) ve `PUT /api/departments/{id}`; ikisi de yalnızca İK (`@PreAuthorize("hasRole('HR')")`).
- Her use case dört dosya (Request, Response, Handler, Validator); cevaplar ortak `DepartmentView`'ı kullanıyor.
- `DepartmentRepository`: `existsByNameIgnoreCase`, `existsByNameIgnoreCaseAndIdNot`.
- `DepartmentController` artık listeleme, ekleme ve güncellemeyi birlikte sunuyor.
**Validator'a koyduğum kurallar:**
- Alanlar arası kural yok; ad zorunlu ve en fazla 100 karakter (istek anotasyonları).
**Handler'daki kurallar ve transaction:**
- Ad büyük/küçük harfe bakmadan tekil: çakışırsa 409 `DUPLICATE_VALUE` (veritabanının tekil kısıtına takılıp 500 dönmesin diye önceden kontrol).
- Olmayan departman güncellenirse 404 `DEPARTMENT_NOT_FOUND`; departman kendi adını koruyabilir.
- Baştaki ve sondaki boşluklar atılıyor; her işlem tek transaction.
**Neden böyle yaptım (tasarım kararları):**
- "Finans" ile "finans" iki ayrı departman olmasın diye ad kontrolü harf duyarsız; veritabanı kısıtı harfe duyarlı olduğu için bu kural uygulama katmanında.
- Departman silinmez (çalışanlar ona bağlı), bu yüzden silme uç noktası yok.
- Güncellenecek kaydın id'si yoldan alınır (`withId`); gövdedeki id yok sayılır. İzin türlerindeki yapıyla aynı.
**Testler:**
- `DepartmentUseCasesIntegrationTest` — ekleme, boşluk temizleme, harf duyarsız çakışma, boş ve uzun ad, güncelleme, aynı adı koruma, başka departmanın adı, olmayan departman
- `DepartmentControllerIntegrationTest` — uçtan uca: herkes listeler, token yoksa 401, İK ekler (201) ve günceller (200), çalışan 403, çakışma 409, boş ad 400, olmayan departman 404
**Karşılaştığım sorun ve çözümü:**
- Testte "Bilgi İşlem" ile "bilgi işlem" eşleşmedi: PostgreSQL `upper` Türkçe İ/i harflerini farklı çeviriyor. Testlerde bu harfleri içermeyen adlar kullanıldı; gerçek kullanımda da bu sınır README'de not edilebilir.
**Sunumda anlatacağım tek cümle:**
Departman yönetiminde yetki controller'da, ad tekilliği Handler'da, biçim kuralları istekte; her katman tek bir işi yapıyor.
**Açık kalanlar:**
- Swagger açıklamaları ve örnekleri.

## 27 Kasım 2026 — feat(employee): çalışan listesi ve detayını ekle (#118)
**İlgili gereksinimler:** FR-09, NFR-07
**Use case(ler):** `ListEmployees`, `GetEmployee`
**Ne yaptım:**
- `GET /api/employees?departmentId&active&page&size` ve `GET /api/employees/{id}`; ikisi de yalnızca İK (`EmployeeController` sınıf seviyesinde `@PreAuthorize("hasRole('HR')")`).
- Cevaplarda Onur'un profil için yazdığı ortak `EmployeeView` kullanıldı (departman adı ve yönetici dahil, şifre hash'i yok).
- `EmployeeSpecifications.matching(departmentId, active)`: boş filtreler sorguya girmiyor.
- `EmployeeRepository.findAll(Specification, Pageable)` üzerinde `@EntityGraph(department, manager)`.
**Validator'a koyduğum kurallar:**
- Alanlar arası kural yok; `departmentId` pozitif, `page ≥ 0`, `size` 1–100 (istek anotasyonları).
**Handler'daki kurallar ve transaction:**
- Salt okunur transaction; sıralama soyad, ad, id. Olmayan çalışan 404 `EMPLOYEE_NOT_FOUND`; pasif çalışan da görüntülenebilir.
**Neden böyle yaptım (tasarım kararları):**
- Filtreler Zehra'nın geçmiş aramasıyla aynı yöntemle (Criteria API) yazıldı: `:p is null or ...` biçimli JPQL PostgreSQL'de boş parametre tipi hatası verebiliyor.
- Listede her satırda departman ve yönetici adı göründüğü için ikisi ana sorguya katıldı; aksi halde 20 satırlık sayfada 40'a yakın ek sorgu atılırdı (N+1).
- Yetki sınıf seviyesinde: çalışan yönetimi uç noktalarının hepsi İK'ya ait, yeni eklenecek metotlar da kendiliğinden korunuyor.
**Testler:**
- `EmployeeQueriesIntegrationTest` — sıralama, departman ve aktiflik filtreleri, ikisi birlikte, ikinci sayfa, yönetici adı, geçersiz filtre/sayfa, detay ve 404
- `EmployeeControllerIntegrationTest` — uçtan uca: İK 200, yönetici 403, token yok 401, geçersiz sayfa 400, olmayan çalışan 404, şifre hash'i cevapta yok
**Karşılaştığım sorun ve çözümü:**
- Bu PR #117'nin (departman) üzerine kuruldu: ikisi de REQUIREMENTS.md özetini ve raporu değiştiriyor, ayrı ayrı açılsalar çakışırdı (CONTRIBUTING 5, bağlı PR'lar).
**Sunumda anlatacağım tek cümle:**
İK çalışanları departmana ve aktifliğe göre süzebiliyor; liste tek sorguyla geliyor ve şifre bilgisi hiçbir cevapta yer almıyor.
**Açık kalanlar:**
- Swagger açıklamaları.

