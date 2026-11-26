# Teknik Kararlar

Bu doküman projede alınan teknik kararları ve gerekçelerini anlatır: REST yerine SOAP ya da gRPC
kullanılsaydı ne değişeceği, katmanların birbirine nasıl bağlandığı ve tek tek kararların
alternatifleriyle karşılaştırması. Yapının kendisi (paketler, katman diyagramı) `ARCHITECTURE.md`'de,
veritabanı kararları [`DATABASE_DESIGN.md`](DATABASE_DESIGN.md)'dedir; burada tekrar edilmez.

## İçindekiler

1. [SOAP ve gRPC'ye uyarlanabilirlik](#1-soap-ve-grpcye-uyarlanabilirlik)
2. [Katmanlar arası bağımlılık yönü](#2-katmanlar-arası-bağımlılık-yönü)
3. [Karar kayıtları](#3-karar-kayıtları)

---

## 1. SOAP ve gRPC'ye uyarlanabilirlik

**Kısa cevap:** REST yerine (ya da yanına) SOAP veya gRPC eklemek yalnızca giriş katmanını yeniden
yazmak demektir. Use case'ler, Validator'lar, Handler'lar, alan servisleri, politikalar, repository'ler,
entity'ler, veritabanı ve transaction'lar olduğu gibi kalır. Protokoller şimdi geliştirilmedi; bu bölüm
mevcut kodun buna nasıl hazır olduğunu ve işin ne kadar süreceğini anlatır.

### 1.1 Neden mümkün?

REST Controller'ı iş yapmaz; isteği bir Request record'una çevirir ve `mediator.send()` çağırır:

```java
@PostMapping("/api/auth/login")
public LoginResponse login(@RequestBody LoginRequest request) {
    return mediator.send(request);
}
```

Bir SOAP endpoint'i ya da gRPC servisi de aynısını yapar: gelen mesajı aynı Request record'una çevirir
ve aynı `mediator.send()`'i çağırır. Mediator, record üzerindeki doğrulama anotasyonlarını ve isteğin
Validator'ını kendisi çalıştırdığı için (`SpringMediator`) yeni giriş katmanında ayrıca doğrulama
yazılmaz.

Bunu mümkün kılan, use case katmanının protokolü hiç bilmemesidir. Bugünkü kodda `application` ve
`domain` paketlerinde HTTP, servlet, Spring MVC, Spring Security'nin web tarafı ya da Swagger'a ait tek
bir import yoktur. Bu katmanların kullandığı Spring parçaları bean tanımı (`@Component`, `@Service`),
transaction (`@Transactional`), Spring Data ve şifre karşılaştırma (`PasswordEncoder`) ile sınırlıdır;
bunların hiçbiri protokole bağlı değildir.

Protokole bağlı olabilecek üç şey arayüzlerin arkasına alınmıştır:

| İhtiyaç | Use case'in bağlı olduğu arayüz | REST'teki uygulaması | SOAP / gRPC'de |
|---|---|---|---|
| İşlemi yapan kim? | `CurrentUserProvider` | `JwtCurrentUserProvider` (token'ı `SecurityContextHolder`'dan okur) | Kendi uygulaması (SOAP: aynı HTTP başlığı; gRPC: metadata) |
| Giriş yapana token ver | `AccessTokenIssuer` | `JwtTokenService` | Aynı sınıf kullanılır |
| Hata türü | `ErrorCode` → `ErrorCategory` | `GlobalExceptionHandler` HTTP koduna çevirir | Fault ya da gRPC durumuna çeviren yeni bir sınıf |

Swagger açıklamaları da use case katmanında değil, `api/docs` altındaki arayüzlerdedir
(`LeaveBalanceApi`, `LeaveTypeApi`, `LeaveHistoryApi`); Controller'lar bunları uygular. Plan, Request
record'larına `@Schema` konacağını öngörüyordu. Bu yapılmadı: record'lar REST'e ait hiçbir anotasyon
taşımaz.

### 1.2 Hangi bileşen değişir?

| Bileşen | REST (bugün) | SOAP / XML Web Service | gRPC |
|---|---|---|---|
| Giriş sınıfları | `@RestController` | Yeni: Spring Web Services `@Endpoint` sınıfları | Yeni: `.proto`'dan üretilen `*ImplBase`'den türeyen servisler (Spring gRPC) |
| Sözleşme | OpenAPI (koddan üretilir) | Yeni: XSD; WSDL ondan üretilir | Yeni: `.proto` dosyaları |
| Mesaj sınıfları | Request ve Response record'ları doğrudan JSON olur | XSD'den üretilen JAXB sınıfları + record'a çeviren mapper | Protobuf sınıfları + mapper |
| Hata çevirisi | `ErrorCategory` → HTTP (401, 403, 404, 409, 422) | Yeni: `ErrorCode` → SOAP Fault | Yeni: `ErrorCategory` → `UNAUTHENTICATED`, `PERMISSION_DENIED`, `NOT_FOUND`, `ABORTED`, `FAILED_PRECONDITION` |
| Kimlik doğrulama | `Authorization: Bearer` başlığı | Değişmez: SOAP da HTTP üzerinden gider, aynı başlık ve aynı JWT doğrulaması | Yeni: metadata'dan token okuyan interceptor; JWT doğrulayıcı aynı |
| İşlemi yapanın bulunması | `JwtCurrentUserProvider` | Büyük olasılıkla aynı sınıf | Yeni `CurrentUserProvider` uygulaması |
| Doğrulama | Mediator | Değişmez | Değişmez |
| Test aracı | Swagger UI | SoapUI / Postman | grpcurl / Postman |
| Mediator, Handler, Validator, alan servisleri, politikalar, repository, entity, veritabanı, transaction | — | Değişmez | Değişmez |
| İş kuralları (BR-01 … BR-18) | Handler ve alan servislerinde | Değişmez | Değişmez |

REST'in kaldırılması da gerekmez: iki giriş katmanı aynı mediator'ı paylaşarak yan yana çalışabilir.

### 1.3 Ne kadar sürer?

Proje planında (15 Kasım) yapılan tahmin:

| Dönüşüm | Kurulum | İşlem başına | 8 temel izin işlemi | 29 işlemin tamamı |
|---|---|---|---|---|
| SOAP (Spring-WS, contract-first) | 1–1,5 kişi-gün | 0,25–0,5 kişi-gün | 3–4 kişi-gün | 8–12 kişi-gün |
| gRPC (Spring gRPC) | 1,5–2 kişi-gün | 0,25–0,5 kişi-gün | 4–5 kişi-gün | 9–13 kişi-gün |

- **Kurulum** bağımlılıkları, kod üretimi eklentisini, hata çeviricisini ve kimlik adımını kapsar.
- **İşlem başına** iş: bir mesaj tanımı, bir mapper ve bir giriş metodu. Handler'a dokunulmaz.
- **8 temel işlem:** talep açma, kendi taleplerim, talep detayı, geri çekme, bekleyenler, onay, red, bakiye.
- gRPC'nin biraz daha uzun sürmesinin nedeni, tarih ve ondalık sayıların (`LocalDate`, `BigDecimal`)
  protobuf'ta doğrudan karşılığının olmaması ve ayrı bir kimlik interceptor'ı gerekmesidir.
- Tahminler bu ekibin deneyimine göre kaba bir ölçektir. Spring Boot 4.1, Spring gRPC için otomatik
  yapılandırma getirdiğinden gRPC için ek bir framework gerekmez.

### 1.4 Taşınabilirliği koruyan kurallar

Aşağıdakiler Handler, Validator ve alan katmanında bulunmaz; PR incelemesinde kontrol edilir:

- `HttpServletRequest`, `ResponseEntity`, `HttpStatus`, `ResponseStatusException`
- `SecurityContextHolder` okuması (kimlik `CurrentUserProvider`'dan gelir)
- Spring'in `Page` nesnesini dışarı vermek (`PageResponse<T>` kullanılır)
- Swagger/OpenAPI anotasyonları
- HTTP durum kodu taşıyan hatalar (hatalar `ErrorCode` taşır; HTTP kodunu yalnızca
  `GlobalExceptionHandler` seçer)

Her use case'in aynı pakette dört dosyadan (Request, Response, Handler, Validator) oluştuğunu
`UseCaseStructureTest` denetler. Swagger örneklerinin Response record'larıyla uyumunu
`OpenApiDocumentationTest` denetler.

---

## 2. Katmanlar arası bağımlılık yönü

**Karar:** Bağımlılıklar tek yöne akar: `api → application → domain → infrastructure.persistence`.
Alan servisleri (`LeaveBalanceService`, `LeaveHistoryService`) ve Handler'lar Spring Data
repository'lerini doğrudan kullanır. Entity'ler `domain.model`'dedir ve JPA anotasyonları taşır.

**Alternatif: port–adapter (hexagonal) mimari.** Alan katmanı kendi repository arayüzlerini tanımlar
(`LeaveBalances` gibi), altyapı katmanı bunları Spring Data ile uygular. Entity'ler de JPA'dan
arındırılıp ayrı persistence sınıflarına eşlenir.

| | Seçilen: katmanlı | Alternatif: port–adapter |
|---|---|---|
| Sınıf sayısı | 10 repository arayüzü | 10 port + 10 adapter; entity'ler de ayrılırsa 10 persistence sınıfı ve eşleyicileri |
| Alan katmanı framework'ten bağımsız mı? | Hayır: JPA ve Spring Data'ya bağlı | Evet |
| Veritabanını değiştirmek | Repository ve entity'ler etkilenir | Yalnızca adapter'lar |
| Protokolü değiştirmek (SOAP/gRPC) | Etkilenmez | Etkilenmez |
| 16 günlük takvim ve 4 kişilik ekip için | Uygun | Kod miktarı ve eşleme hatası riski yüksek |

**Neden:** Projenin teknik sorusu protokol değişimiydi (bölüm 1). Protokol bağımsızlığını giriş
tarafındaki mediator ve arayüzler zaten sağlıyor. Port–adapter yapısının asıl kazancı veritabanını
değiştirebilmektir; bu projede veritabanı sabit (PostgreSQL) ve şema önce tasarlanıyor. Bu kazanç
için sınıf sayısını iki-üç katına çıkarmak takvime sığmazdı.

**Bedeli:** Alan katmanı JPA ve Spring Data'ya bağımlı. İş kuralları birim testlerinde Mockito ile ya
da Testcontainers'la gerçek veritabanında sınanıyor. İleride veritabanı değişirse etkilenen sınıflar
`domain.model`, `domain.service` ve `infrastructure.persistence`'tır; Controller'lar etkilenmez.

**Sınır:** Handler'lar birbirini çağırmaz. İki use case'in ortak işi bir alan servisine taşınır (örneğin
bakiye düşme ve iadesi `LeaveBalanceService`'te, durum geçmişi `LeaveHistoryService`'te).

---

## 3. Karar kayıtları

Her karar aynı biçimde yazıldı: neye karar verildi, neden, düşünülen alternatif ve kararın bedeli.

| # | Karar | Konu |
|---|---|---|
| K1 | Kendi yazdığımız küçük mediator | Use case altyapısı |
| K2 | Transaction sınırı Handler; alan servisleri `MANDATORY` | Transaction |
| K3 | Hatalar HTTP kodu değil `ErrorCode` taşır | Hata yönetimi |
| K4 | `open-in-view: false` | Kalıcılık |
| K5 | "Bugün" her yerde `Clock` bean'inden alınır | Test edilebilirlik |
| K6 | Oturumsuz JWT (HS256); CSRF kapalı | Güvenlik |
| K7 | Şema SQL scriptleriyle; `ddl-auto: validate` | Şema yönetimi |
| K8 | Testler Testcontainers'la gerçek PostgreSQL'de | Test |
| K9 | Gün sayıları `BigDecimal` | Veri türü |
| K10 | Çalıştırılabilir WAR + gömülü Tomcat | Paketleme |
| K11 | Swagger açıklamaları ayrı arayüzlerde | API dokümantasyonu |
| K12 | Yıllık izin devri ayrı bir işlem | İş kuralı tasarımı |
| K13 | PR'lar "Rebase and merge" ile birleşir | Sürüm kontrolü |

### K1. Kendi yazdığımız küçük mediator

- **Karar:** Controller her isteği `Mediator.send()`'e verir. `SpringMediator` açılışta bütün Handler
  ve Validator bean'lerini istek tipine göre eşler; çağrıda önce doğrulama anotasyonlarını, sonra
  isteğin Validator'ını, en son Handler'ı çalıştırır.
- **Neden:** Bölüm 1'deki protokol bağımsızlığını sağlayan bu yapı. Yeni bir use case eklemek mediator'ı
  değiştirmez (Open/Closed); Controller'ın tek bağımlılığı mediator'dır.
- **Alternatif:** Controller'dan doğrudan servis çağırmak (daha az dosya, ama her giriş katmanı doğrulamayı
  ve servis seçimini tekrar eder) ya da hazır bir kütüphane (dış bağımlılık; sunumda içi açıklanamaz).
- **Bedeli:** Her use case dört dosya. Bunu IntelliJ dosya şablonu ve `UseCaseStructureTest` hafifletiyor.

### K2. Transaction sınırı Handler; alan servisleri `MANDATORY`

- **Karar:** `@Transactional` yalnızca Handler'ın `handle()` metodunda. `LeaveBalanceService` ve
  `LeaveHistoryService` `@Transactional(propagation = MANDATORY)`: kendileri transaction açmaz, açık bir
  transaction yoksa hata verir.
- **Neden:** Bir iş adımı (örneğin onay: bakiye düş, durumu değiştir, geçmişe yaz) ya bütünüyle olur ya
  hiç olmaz. Sınır tek yerde olunca rollback davranışı da tek yerden okunur. `MANDATORY`, bir servisin
  yanlışlıkla transaction dışından çağrılıp yarım kayıt bırakmasını engeller.
- **Alternatif:** Varsayılan `REQUIRED` (servis gerekirse kendi transaction'ını açar). Daha esnek, ama
  servis tek başına çağrıldığında bakiye düşüp geçmiş yazılmayabilir.
- **Bedeli:** Alan servisleri testte de bir transaction içinde çağrılmalı.
- **Kanıt:** T3 rollback testi (`RunAnnualAccrualIntegrationTest`) üçüncü çalışanda hata oluşturur;
  hiçbir bakiye, hareket ve çalıştırma kaydı kalmaz.

### K3. Hatalar HTTP kodu değil `ErrorCode` taşır

- **Karar:** Alan hataları (`BusinessException`, `NotFoundException`) bir `ErrorCode` taşır; her kodun
  bir `ErrorCategory`'si vardır (UNAUTHENTICATED, FORBIDDEN, NOT_FOUND, CONFLICT, BUSINESS_RULE).
  Kategoriyi HTTP koduna yalnızca `GlobalExceptionHandler` çevirir. Bütün hatalar aynı JSON biçimiyle
  döner; Spring Security'nin 401 ve 403'leri de (`SecurityErrorWriter`).
- **Neden:** Alan katmanı HTTP bilmez (bölüm 1). gRPC'de aynı kategori gRPC durumuna çevrilir.
- **Alternatif:** `ResponseStatusException` ya da `@ResponseStatus` (kısa, ama alan katmanını HTTP'ye bağlar).
- **Bedeli:** Yeni bir hata için `ErrorCode`'a bir satır eklenir.

### K4. `open-in-view: false`

- **Karar:** Hibernate oturumu Handler'ın transaction'ı bitince kapanır. Entity'den cevaba (view) çeviri
  Handler içinde biter; Controller'a entity çıkmaz.
- **Neden:** Açık bırakılırsa JSON'a çevrilirken gizli lazy-load'lar oluşur (N+1) ve transaction dışında
  veritabanına gidilir. Kapalıyken bu tür bir hata testte hemen görünür.
- **Alternatif:** Spring Boot'un varsayılanı (açık). Geliştirme sırasında rahat, ama sorgu sayısını gizler.
- **Bedeli:** İlişkili veriler Handler'da `join fetch` ya da `@EntityGraph` ile bilinçli yüklenmeli.
  Ölçümler `DATABASE_DESIGN.md` bölüm 5'te.

### K5. "Bugün" her yerde `Clock` bean'inden alınır

- **Karar:** `LocalDate.now()` doğrudan çağrılmaz; `ClockConfig` Europe/Istanbul saat dilimli bir `Clock`
  verir. Uygulamanın, JVM'in, Jackson'ın ve JDBC'nin saat dilimi de Europe/Istanbul.
- **Neden:** Tarihe bağlı kurallar (geçmiş tarih, iptal süresi, hak ediş yılı, devir yalnızca biten
  yıldan) testte sabit bir saatle tekrarlanabilir şekilde sınanır. Örneğin yılbaşı gecesi İstanbul
  saatiyle yıl henüz bitmediği için devrin reddedildiği test.
- **Alternatif:** `LocalDate.now()` (testte tarih sabitlenemez; test yılbaşında ya da gece yarısı kırılır).
- **Bedeli:** Tarih kullanan her sınıf `Clock`'u constructor'dan alır.

### K6. Oturumsuz JWT (HS256); CSRF kapalı

- **Karar:** Girişte HS256 ile imzalı bir JWT verilir (`sub` = çalışan id'si, `role`, `email`, süre
  varsayılan 60 dakika). Doğrulamayı Spring Security'nin OAuth2 Resource Server filtresi yapar; elle filtre
  yazılmaz. Oturum tutulmaz (`STATELESS`). Rol kontrolü Controller'da `@PreAuthorize`, kayıt bazlı
  yetki (örneğin "yalnızca kendi ekibi") Handler'da. Şifreler BCrypt.
- **Neden:** Arayüz yok, istemci Swagger UI ya da başka bir servis. Token her istekte geldiği için
  sunucu durum tutmaz ve aynı token SOAP'ta da kullanılabilir.
- **CSRF neden kapalı:** CSRF saldırısı tarayıcının çerezi kendiliğinden göndermesine dayanır. Bu API
  çerez kullanmaz; token'ı istemci her istekte `Authorization` başlığına kendisi koyar. SonarQube bu
  satırı (java:S4502) güvenlik uyarısı olarak gösteriyor; gerekçe `SecurityConfig`'te yazılı.
- **Alternatif:** Oturumlu giriş (çerez + CSRF koruması; tarayıcı arayüzü için doğru seçim) ya da
  RS256 (açık/gizli anahtar; token'ı başka servisler doğrulayacaksa gerekir, tek uygulamada gereksiz).
- **Bedeli:** Token süresi dolmadan geri alınamaz (yenileme token'ı ya da kara liste yok). Pasif
  çalışan yeni token alamaz; mevcut token'ın süresi kısa tutulur.

### K7. Şema SQL scriptleriyle; `ddl-auto: validate`

- **Karar:** Şema `db/scripts` altındaki sıralı SQL scriptleriyle kurulur. Entity'ler tablolardan
  üretildi; Hibernate açılışta yalnızca uyumu denetler (`validate`), tablo oluşturmaz ya da değiştirmez.
- **Neden:** Veritabanı önce tasarlandı; kısıtlar, CHECK'ler ve ifade indeksleri (`lower(email)`)
  SQL'de açıkça yazılı. Entity ile tablo uyuşmazsa uygulama hiç açılmaz; hata canlıda değil açılışta
  görülür.
- **Alternatif:** Flyway (sürüm takibi otomatik; ama dört kişi aynı Supabase veritabanını paylaştığı için
  şema değişikliği yine tek kişiden geçmek zorunda) ya da `ddl-auto: update` (kısıtları ve indeksleri
  eksik kurar, geri alınamaz).
- **Bedeli:** Script'in Supabase'de çalıştırılması elle yapılır ve `db/README.md`'ye kaydedilir.
  Ayrıntı: `DATABASE_DESIGN.md` bölüm 9.

### K8. Testler Testcontainers'la gerçek PostgreSQL'de

- **Karar:** Entegrasyon testleri `postgres:17-alpine` imajıyla geçici bir veritabanı açar ve aynı SQL
  scriptleriyle kurar. Testler Supabase'e bağlanmaz. CI (GitHub Actions) her PR'da `./mvnw verify` çalıştırır
  ve hiçbir gizli değere ihtiyaç duymaz.
- **Neden:** CHECK kısıtları, ifade indeksleri, iyimser kilit ve rollback gibi davranışlar gerçek
  PostgreSQL'de sınanmalı. H2 gibi bellek içi veritabanları bunların bir kısmını desteklemez ya da farklı
  davranır.
- **Alternatif:** H2 (hızlı, Docker gerektirmez, ama farklı SQL lehçesi) ya da ortak test veritabanı
  (testler birbirinin verisini bozar).
- **Bedeli:** Testler için Docker açık olmalı; ilk çalıştırma imajı indirir.

### K9. Gün sayıları `BigDecimal`

- **Karar:** Hak edilen, kullanılan ve talep edilen gün alanları Java'da `BigDecimal`, veritabanında
  `numeric(4,1)`. `double` kullanılmaz.
- **Neden:** Yarım gün (0,5) değerleri toplanıp çıkarılırken kayan nokta hatası olmaz.
- **Alternatif:** `double` (0,1 + 0,2 gibi işlemlerde yuvarlama hatası) ya da gün yerine saat sayan `int`.
- **Bedeli:** Karşılaştırmada `equals` yerine `compareTo` kullanılmalı.

### K10. Çalıştırılabilir WAR + gömülü Tomcat

- **Karar:** Paketleme `war`. Uygulama sınıfı `SpringBootServletInitializer`'dan türer; aynı dosya
  `java -jar` ile gömülü Tomcat'te ya da bir uygulama sunucusuna deploy edilerek çalışır.
- **Neden:** Ders "Application Server üzerinden yayın" istiyor; hoca gömülü Tomcat'i yeterli buldu.
  WAR, bonus olarak aynı dosyanın Payara Micro'da da çalıştırılabilmesini sağlar.
- **Alternatif:** JAR (Spring Boot'un varsayılanı; ama harici sunucuya deploy edilemez).
- **Bedeli:** Yok denecek kadar az; `java -jar` davranışı aynı.

### K11. Swagger açıklamaları ayrı arayüzlerde

- **Karar:** Uç noktaların açıklamaları ve örnekleri `api/docs/XxxApi` arayüzlerinde; Controller'lar
  bunları uygular. Örnek JSON'lar `ApiExamples`'ta.
- **Neden:** Açıklamalar Request/Response record'larına (`@Schema`) yazılsaydı use case katmanı
  Swagger kütüphanesine bağımlı olurdu (bölüm 1). Controller'a yazılsaydı Controller'lar yüzlerce
  satır olurdu.
- **Alternatif:** springdoc'un koddan otomatik çıkardığı belgeyle yetinmek (örnek ve hata cevabı olmaz).
- **Bedeli:** Açıklama ile kod iki ayrı dosyada; örneklerin kodla uyumunu `OpenApiDocumentationTest`
  denetliyor (bir Response'a alan eklenip örnek güncellenmezse test kırılır).

### K12. Yıllık izin devri ayrı bir işlem

- **Karar:** Kullanılmayan yıllık izin, yıllık hak edişten (T3) ayrı bir işlemle ve yalnızca biten
  yıldan devredilir (`RunCarryOver`). Eski yılın kalanı sıfırlanır; ikinci çalıştırma hiçbir şey
  değiştirmez.
- **Neden:** T3 gelecek yıl için Aralık'ta çalıştırılabiliyor. Devir de o anda yapılsaydı çalışan
  Aralık'ın kalanında aynı günleri kullanabilir, günler iki yılda birden kullanılmış olurdu.
- **Alternatif:** Devri T3'ün içine koymak (gereksinim listesindeki ilk düşünce; yukarıdaki hata yüzünden
  bırakıldı).
- **Bedeli:** İK'nın yılbaşından sonra ikinci bir işlem çalıştırması gerekir. Bu yılın hak edişi
  çalışmadan devir yapılamaz (`ACCRUAL_NOT_RUN`, 409).

### K13. PR'lar "Rebase and merge" ile birleşir

- **Karar:** Her görev bir issue, bir branch ve bir PR. PR'lar `dev`'e "Rebase and merge" ile birleşir;
  "Squash and merge" kullanılmaz. Her commit'in başlığı (Conventional Commits) ve açıklaması
  (Neden / Ne değişti / Test / Refs) vardır.
- **Neden:** Küçük ve anlamlı commit'ler `dev` geçmişinde tek tek kalır; her üyenin katkısı görünür.
- **Alternatif:** Squash (temiz ama katkıyı tek commit'e ezer) ya da merge commit (geçmiş dallanır,
  okunması zorlaşır).
- **Bedeli:** Rebase sonrası commit kimlikleri değişir. Bir önceki PR'ın üzerine açılan branch, o PR
  birleşince `git rebase --onto origin/dev <eski son commit>` ile taşınmalı. Kurallar `CONTRIBUTING.md`'de.
