# Çalışma Raporu: Bilge

- **GitHub:** @bilgeerol
- **Modül:** İzin talebi ve onay akışı (`leave-request`, `leave-approval`)
- **Sorumlu olduğum gereksinimler:** FR-14, FR-16, FR-17, FR-18, FR-19, FR-20, FR-21, FR-22, FR-24 · bonus FR-36, FR-37 · BR-04, BR-05 (Zehra ile), BR-06, BR-07, BR-09 (red), BR-10 (talep açma), BR-14 · NFR-05, NFR-08 · T1
- **Altyapı işleri:** mediator ve doğrulama hattı, IntelliJ dört dosya şablonu, global hata yönetimi, `ApproverResolver`, eşzamanlı onay testi
- **Dokümanlar:** `API_DOCUMENTATION.md`, sunum iskeleti

---

## 18 Kasım 2026 — feat(mediator): use case mediator'ı ve doğrulama hattı (#91)
**İlgili gereksinimler:** NFR-01, NFR-06 (doğrudan bir FR karşılanmadı; bütün use case'ler bu altyapının üzerine yazılacak)
**Use case(ler):** Yok (altyapı). Sınıflar: `Request`, `RequestHandler`, `RequestValidator`, `Mediator`, `SpringMediator`, `ValidationError`, `ValidationException`
**Ne yaptım:**
- `Request<R>`, `RequestHandler<Q, R>`, `RequestValidator<Q>` ve `Mediator` arayüzlerini yazdım; Controller'lar sadece `mediator.send(request)` çağıracak.
- `SpringMediator`: uygulama açılırken bütün Handler ve Validator bean'lerini generic tiplerinden istek tipine göre eşliyor. Aynı isteğe iki Handler varsa uygulama açılmıyor.
- `send()` sırası: önce istek record'undaki Jakarta anotasyonları, sonra `XxxValidator`, hata yoksa Handler. Hata varsa `ValidationException` atılıyor ve Handler hiç çağrılmıyor.
- `ValidationError` (alan adı + Türkçe mesaj) ve `ValidationException` (değiştirilemez hata listesi) eklendi.
**Validator'a koyduğum kurallar:**
- Yok (altyapı). Mediator'un kuralı: anotasyonlar geçmezse `XxxValidator` çalıştırılmıyor.
**Handler'daki kurallar ve transaction:**
- Mediator transaction açmıyor; `@Transactional` Handler'ın `handle()` metodunda kalıyor. Mediator Handler'ı Spring bean'i üzerinden çağırdığı için transaction proxy'si devrede.
- `@Transactional` yüzünden proxy'lenen Handler'ın istek tipi, asıl sınıfa bakılarak (`ClassUtils.getUserClass`) bulunuyor.
**Neden böyle yaptım (tasarım kararları):**
- Doğrulama Controller'da (`@Valid`) değil mediator'da: gece görevi (T5) ve ileride SOAP/gRPC giriş katmanı da aynı doğrulamadan geçiyor.
- Anotasyon hatası varsa Validator çalışmıyor; Validator'lar boş alan kontrolü yapmak zorunda kalmıyor.
- Yeni use case eklemek `SpringMediator`'ı değiştirmiyor; yeni bean'ler açılışta kendiliğinden bulunuyor (Open/Closed).
- Hatalar alan adına göre sıralanıyor; aynı istek her seferinde aynı sırada hata döndürüyor.
**Testler:**
- `SpringMediatorTest.sendsValidRequestToItsHandler` — geçerli istek Handler'a gidiyor
- `SpringMediatorTest.annotationErrorsStopRequestBeforeHandler` — anotasyon hatasında Handler çağrılmıyor
- `SpringMediatorTest.customValidatorIsSkippedWhenAnnotationsFail` — anotasyon hatasında Validator çalışmıyor
- `SpringMediatorTest.customValidatorErrorsStopRequestBeforeHandler` — Validator hatasında Handler çağrılmıyor
- `SpringMediatorTest.annotationErrorsAreSortedByFieldName` — hatalar alan adına göre sıralı
- `SpringMediatorTest.worksWithoutCustomValidator` — Validator olmayan istek de çalışıyor
- `SpringMediatorTest.findsRequestTypeOfProxiedHandler` — proxy'li Handler'ın istek tipi bulunuyor
- `SpringMediatorTest.failsWhenRequestHasNoHandler`, `rejectsTwoHandlersForSameRequest`, `rejectsMissingRequest` — hatalı kurulum ve boş istek
- `ValidationExceptionTest` — 4 birim testi (mesaj, değiştirilemez liste, boş liste, boş alan)
- `EmployeeVacationManagementApplicationTests` — henüz hiç Handler yokken de uygulama açılıyor
**Karşılaştığım sorun ve çözümü:**
- Yok. Proxy'li Handler'ın generic tipinin okunamaması önceden düşünülüp `findsRequestTypeOfProxiedHandler` testiyle kapatıldı.
**Sunumda anlatacağım tek cümle:**
Controller sadece `mediator.send(request)` der; doğrulama ve doğru Handler'ı bulma tek bir sınıfta, yeni bir use case eklemek bu sınıfa hiç dokunmaz.
**Açık kalanlar:**
- `ValidationException`'ın 400 ve `fieldErrors` cevabına çevrilmesi global hata yönetimi PR'ında yapılacak.
- PR, IntelliJ şablonu ve bu rapor eklenmeden birleştirildi; ikisi #92'de tamamlandı.

## 20 Kasım 2026 — chore(mediator): use case için IntelliJ dosya şablonu (#92)
**İlgili gereksinimler:** NFR-01
**Use case(ler):** Yok (geliştirme aracı)
**Ne yaptım:**
- `docs/intellij/use-case/` altına IntelliJ "File and Code Templates" için bir ana şablon (`Request.java.ft`) ve üç child şablon (`Response`, `Validator`, `Handler`) ekledim.
- `README.md` ile şablonun IntelliJ'e nasıl ekleneceğini ve kullanımını yazdım: pakete sağ tık → New → Use Case → sadece use case adı (`SubmitLeave`).
**Validator'a koyduğum kurallar:**
- Yok
**Handler'daki kurallar ve transaction:**
- Yok. Şablondaki Handler `@Transactional` ile geliyor; sadece okuyan use case'lerde `readOnly = true` yapılması README'de yazıyor.
**Neden böyle yaptım (tasarım kararları):**
- `.idea/` repoya girmediği için şablonlar `docs` altında tutuluyor; herkes kendi IntelliJ'ine bir kez ekliyor.
- Ad olarak sadece use case adı yazılıyor, dört dosya aynı paket ve isim düzeniyle oluşuyor; elle oluştururken çıkan isim hataları önleniyor.
**Testler:**
- IntelliJ'de New → Use Case → `Deneme` ile dört dosyanın doğru ad ve paketle oluştuğu elle denendi; deneme dosyaları silindi.
**Karşılaştığım sorun ve çözümü:**
- Velocity, `#end`'den sonraki satır sonunu yuttuğu için `package` ile `import` satırları bitişik çıkıyordu; şablonlarda ilk satırdan sonra iki boş satır bırakılarak düzeltildi.
**Sunumda anlatacağım tek cümle:**
Her use case dört dosya; şablon sayesinde ekipteki herkes bunları aynı düzenle tek adımda oluşturuyor.
**Açık kalanlar:**
- Şablonu ekipteki diğer üç kişinin de IntelliJ'ine eklemesi gerekiyor.

## 20 Kasım 2026 — feat(error-handling): ortak hata yönetimi (#93)
**İlgili gereksinimler:** NFR-05 · FR-16, FR-17, FR-24, NFR-08 için hata kodları hazırlandı (kuralların kendisi kendi PR'larında)
**Use case(ler):** Yok (altyapı). Sınıflar: `ErrorCategory`, `ErrorCode`, `BusinessException`, `NotFoundException`, `ForbiddenActionException`, `InvalidStateTransitionException`, `InsufficientBalanceException`, `ErrorResponse`, `GlobalExceptionHandler`
**Ne yaptım:**
- `ErrorCode`: her alan hatasının sabit kodu, türü (`ErrorCategory`) ve Türkçe varsayılan mesajı; örneğin `INSUFFICIENT_BALANCE`, `LEAVE_REQUEST_NOT_FOUND`, `SELF_APPROVAL_NOT_ALLOWED`.
- `BusinessException` ve plandaki alt sınıfları: `NotFoundException` (404), `ForbiddenActionException` (403), `InvalidStateTransitionException` (409), `InsufficientBalanceException` (422). Alt sınıflar yanlış türde bir kodla oluşturulamıyor.
- `ErrorResponse`: plandaki tek JSON biçimi (`timestamp`, `status`, `code`, `message`, `path`, `fieldErrors`).
- `GlobalExceptionHandler`: `ValidationException` → 400 ve `fieldErrors`; `BusinessException` → türüne göre 404/403/409/422; iyimser kilit çatışması → 409 `CONCURRENT_UPDATE`; beklenmeyen hata → 500 ve genel mesaj. Bozuk JSON ve yanlış HTTP metodu gibi Spring MVC hataları da aynı biçimde dönüyor.
**Validator'a koyduğum kurallar:**
- Yok
**Handler'daki kurallar ve transaction:**
- Alan hataları `RuntimeException`'dan türüyor; Handler'dan çıkınca transaction geri alınıyor. Handler içinde yakalanıp yutulmamaları gerektiği sınıfın açıklamasında yazıyor.
**Neden böyle yaptım (tasarım kararları):**
- Alan katmanı HTTP kodu bilmiyor, sadece hatanın türünü (`ErrorCategory`) biliyor; tür → HTTP kodu eşlemesi tek yerde, `GlobalExceptionHandler`'da. İleride gRPC aynı türleri kendi durum kodlarına çevirebilir.
- Yeni bir iş kuralı için `ErrorCode`'a bir sabit eklemek yetiyor; `GlobalExceptionHandler` değişmiyor (Open/Closed).
- 500'de ayrıntı sadece loga yazılıyor (SLF4J), istemciye stack trace veya sınıf adı gitmiyor (NFR-05).
- Zaman damgası `Clock` bean'inden geliyor; testte sabit saatle doğrulanabiliyor.
- `@PreAuthorize`'dan gelen Spring Security hataları bilerek yakalanmıyor; 401/403'ü Security üretiyor, yoksa 500'e dönüşürlerdi.
**Testler:**
- `BusinessExceptionTest` — varsayılan ve ayrıntılı mesaj, boş kod, her kodun türü ve Türkçe mesajı, alt sınıfların yanlış kodu reddetmesi
- `GlobalExceptionHandlerTest.validationExceptionBecomes400WithFieldErrors` — 400 ve alan hataları
- `GlobalExceptionHandlerTest.businessExceptionStatusFollowsItsCategory` — 404, 403, 409, 422
- `GlobalExceptionHandlerTest.detailedBusinessMessageIsReturned` — "talep 5 gün, kalan 3 gün" mesajı aynen dönüyor
- `GlobalExceptionHandlerTest.optimisticLockConflictBecomes409` — eşzamanlı güncelleme 409
- `GlobalExceptionHandlerTest.unexpectedErrorHidesDetails` — 500'de hata ayrıntısı cevaba girmiyor
- `GlobalExceptionHandlerTest.malformedJsonBecomes400InSameFormat`, `wrongHttpMethodBecomes405InSameFormat` — Spring MVC hataları aynı biçimde
- `GlobalExceptionHandlerTest.timestampComesFromClock` — zaman damgası `Clock`'tan
**Karşılaştığım sorun ve çözümü:**
- Yok
**Sunumda anlatacağım tek cümle:**
Handler sadece "bakiye yetersiz" der, hangi HTTP koduyla ve hangi JSON'la döneceğine tek bir sınıf karar verir; bu yüzden bütün hatalar aynı biçimde döner.
**Açık kalanlar:**
- 401 ve rol kaynaklı 403 cevaplarının da aynı JSON biçiminde dönmesi Onur'un güvenlik ayarında (`AuthenticationEntryPoint`, `AccessDeniedHandler`) yapılmalı; `ErrorResponse` kullanılabilir.
- Swagger'daki hata örnekleri 24 Kasım'da eklenecek.

## 20 Kasım 2026 — feat(leave-request): talep durum geçişleri ve onaylayıcı seçimi (#94)
**İlgili gereksinimler:** BR-07, BR-08, BR-09, FR-24 · FR-18, FR-21, FR-22, FR-23, FR-25, FR-28, FR-29, FR-31 için entity geçişleri (endpoint'leri kendi PR'larında)
**Use case(ler):** Yok (alan katmanı). Sınıflar: `LeaveRequest`, `LeaveRequestStatus`, `ApproverResolver`, `ApprovalRoute`
**Ne yaptım:**
- `LeaveRequest`'e geçiş metotlarını ekledim: `approve`, `reject`, `withdraw`, `requestCancellation`, `withdrawCancellationRequest`, `rejectCancellationRequest`, `approveCancellationRequest`, `cancel`, `expireCancellationRequest`; ayrıca `isOwnedBy`.
- Geçersiz geçişte `InvalidStateTransitionException` (409) atılıyor; mesaj Türkçe durum adını içeriyor: "Talep 'Reddedildi' durumundayken onaylanamaz."
- `LeaveRequestStatus`'a Türkçe ad (`label()`) ve son durum bilgisi (`isFinal()`) eklendi.
- `ApproverResolver`: İK'nın talebini başka bir aktif İK; diğerlerini aktif yöneticisi; yöneticisi yoksa veya pasifse herhangi bir aktif İK onaylıyor (BR-07, plan K-3). `checkCanDecide` kendi talebinde `SELF_APPROVAL_NOT_ALLOWED`, yanlış kişide `ACTION_NOT_ALLOWED` ile 403 atıyor (FR-24).
**Validator'a koyduğum kurallar:**
- Yok. Gerekçe alanlarının dolu olması entity'de de korunuyor (boş gerekçe programlama hatası sayılıyor); kullanıcıya dönen 400 ilgili Validator'larda yazılacak (BR-09).
**Handler'daki kurallar ve transaction:**
- Entity transaction açmıyor; geçişler onay (T1) ve iptal (T2) Handler'larının transaction'ı içinde çağrılacak.
- Tarih kuralları `today` parametresiyle geliyor: başlamış izin iptal edilemiyor (BR-08), bitişi geçmiş talep onaylanamıyor ama reddedilebiliyor (plan K-11), iptal talebi başlangıç gününde sonuçsuz oluyor (BR-18).
**Neden böyle yaptım (tasarım kararları):**
- Durum değişikliği entity'nin kendi metotlarında: hangi Handler çağırırsa çağırsın reddedilmiş talep onaylanamıyor; kural tek yerde.
- Entity'de setter yok; durum ancak geçerli bir geçişle değişiyor.
- `ApproverResolver` veritabanına bakmıyor, sadece talep sahibine ve yöneticisine bakıyor; bu yüzden saf birim testiyle denenebiliyor. Kural her işlemde yeniden hesaplanıyor; talep beklerken yönetici değişirse yeni yönetici karar veriyor.
- Aynı `ApproverResolver` onay, red, iptal talebi kararı ve şirket adına iptal için kullanılacak (BR-15, BR-17); kural kopyalanmıyor.
- `#93`'teki hata sınıflarına bağlı olduğu için PR, #93 birleşene kadar draft kaldı.
**Testler:**
- `LeaveRequestTest` — 25 test: her geçişin başarılı yolu, geçersiz durumdan geçiş (409), bitişi geçmiş talebin onaylanamaması, başlamış iznin iptal edilememesi, iptal talebinin başlangıç gününde sonuçsuz olması, son durumlardan hiçbir geçiş olmaması, her durumun Türkçe adı
- `ApproverResolverTest.employeeWithActiveManagerIsDecidedByThatManagerOnly` — yöneticisi olan çalışanı sadece yöneticisi onaylar, İK onaylayamaz
- `ApproverResolverTest.inactiveManagerCountsAsNoManager` — pasif yönetici varsa talep İK'ya düşer
- `ApproverResolverTest.hrRequestIsDecidedByAnotherHrEvenIfHrHasManager` — İK'nın talebini başka İK onaylar
- `ApproverResolverTest.ownRequestIsForbiddenWithSelfApprovalCode` — kendi talebi 403 `SELF_APPROVAL_NOT_ALLOWED`
- `ApproverResolverTest` diğer 4 test — yöneticisiz çalışan, pasif İK, yanlış onaylayıcı, doğru onaylayıcı
**Karşılaştığım sorun ve çözümü:**
- Bu iş #93'teki hata sınıflarına bağlıydı ve #93 henüz birleşmemişti; branch #93'ün branch'inden açıldı, PR draft olarak bekletildi.
**Sunumda anlatacağım tek cümle:**
Reddedilmiş bir talebi onaylamaya çalışan her kod entity'nin kendisinden 409 alır; kimin onaylayacağı da tek bir sınıfta, `ApproverResolver`'da.
**Açık kalanlar:**
- #93 birleşince branch `dev`'e rebase edilip draft'tan çıkarılacak.
- Durum geçmişini (`leave_request_history`) yazmak Zehra'nın `LeaveHistoryService`'inde; Handler'lar geçişten sonra onu çağıracak.
