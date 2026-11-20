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
