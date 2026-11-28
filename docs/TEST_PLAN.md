# Test Planı

Bu doküman projenin nasıl test edildiğini anlatır: hangi test türlerini kullandığımızı, testleri
nasıl çalıştıracağınızı ve analiz dokümanındaki her iş kuralının, rol senaryosunun ve demo adımının
hangi testle doğrulandığını.

- Gereksinimlerin listesi ve durumu: [`REQUIREMENTS.md`](REQUIREMENTS.md)
- Transaction'ların tasarımı: README, "Transaction Senaryoları"
- Kapsam ve kod kalitesi: README, "SonarQube Analysis"

**Durum işaretleri:** ✅ testi var ve `dev`'de geçiyor · 🟡 bir kısmı test edildi, eksik kısım
"Bekleyen" sütununda · ⬜ testi henüz yok

---

## 1. Amaç ve ilkeler

1. **Her kural bir testle kanıtlanır.** Analiz dokümanındaki her iş kuralının, rol senaryosunun
   ve demo adımının en az bir otomatik testi vardır (bölüm 5). Sunumdaki demo, bu testlerin
   Swagger'da elle tekrarıdır.
2. **Testler gerçek veritabanında koşar.** Veritabanına giden testler Supabase'e değil, aynı
   `db/scripts/*.sql` scriptleriyle kurulan geçici bir PostgreSQL 17 konteynerine gider
   (Testcontainers). CHECK kısıtları, tekil indeksler ve foreign key'ler testte de geçerlidir.
3. **Rollback ölçülür, varsayılmaz.** Her transactional işlemin (T1–T5) bir adımı testte bilerek
   hata verdirilir ve veritabanında iz kalmadığı satır sayılarak doğrulanır (bölüm 6).
4. **"Bugün" sabittir.** Tarihe bağlı kurallar (geçmiş tarih, iznin başlaması, kıdem, yaş)
   `Clock` üzerinden okunur; testler sabit bir tarih verir. Testin sonucu çalıştırıldığı güne
   bağlı değildir.
5. **Testler birbirinden bağımsızdır.** Her entegrasyon testi boş bir veritabanıyla başlar; test
   sırası sonucu değiştirmez (`DatabaseResetTest` bunu denetler).

---

## 2. Test türleri

| Tür | Ne test eder | Araçlar | Örnek sınıf | Hız |
|---|---|---|---|---|
| **Birim (domain)** | Entity metotları, durum geçişleri, politikalar; Spring ve veritabanı yok | JUnit 5, AssertJ | `LeaveRequestTest`, `LeaveBalanceTest`, `ApproverResolverTest`, `SeniorityAndAgeEntitlementPolicyTest` | Milisaniye |
| **Birim (use case)** | Validator kuralları ve Handler'ın dallanması; bağımlılıklar sahte | JUnit 5, Mockito | `SubmitLeaveValidatorTest`, `LoginHandlerTest`, `GetEmployeeBalancesHandlerTest` | Milisaniye |
| **Sözleşme** | Bir arayüzün her uygulamasının aynı kurallara uyması (LSP) | Soyut JUnit sınıfı | `WorkingDayCalculatorContractTest` → `WeekdayWorkingDayCalculatorTest` | Milisaniye |
| **Entegrasyon (use case)** | Mediator → Validator → Handler → Repository → PostgreSQL zinciri; transaction ve rollback | `@SpringBootTest`, Testcontainers, `@MockitoSpyBean` | `CreateEmployeeIntegrationTest`, `LeaveCancellationIntegrationTest`, `RunAnnualAccrualIntegrationTest` | Saniye |
| **Uç nokta (web dilimi)** | Controller: adres, parametre, HTTP kodu, hata biçimi; use case sahte | `@WebMvcTest`, MockMvc | `DepartmentControllerTest`, `LeaveTypeControllerTest`, `LeaveHistoryControllerTest` | Saniye |
| **Uçtan uca API** | Gerçek giriş (JWT), rol kontrolü (401/403), JSON cevabı, veritabanı | `@SpringBootTest`, MockMvc + `springSecurity()` | `EmployeeControllerIntegrationTest`, `LeaveCancellationControllerIntegrationTest`, `AuthenticationAndAuthorizationIntegrationTest` | Saniye |
| **Kalıcılık** | Entity eşlemeleri, sorgu sayısı (N+1), indeks kullanımı | Testcontainers, Hibernate istatistikleri | `PersistenceMappingTest`, `EmployeeRepositoryIntegrationTest`, `LeaveBalanceControllerIntegrationTest.mineRunsASingleQueryWhateverTheNumberOfBalances` | Saniye |
| **Mimari** | Her use case'in Request / Response / Handler / Validator dörtlüsü (NFR-01) | Sınıf tarama | `UseCaseStructureTest` | Milisaniye |
| **Doküman** | Swagger örneklerinin Request / Response record'larıyla birebir aynı olması | `/v3/api-docs` + JsonPath | `OpenApiDocumentationTest` | Saniye |

**Neden bu karışım:** Kuralların çoğu domain'de (entity ve politikalar) olduğu için en çok test
orada ve en hızlı olanlar onlar. Transaction ve rollback ancak gerçek veritabanında ölçülebildiği
için T1–T5 entegrasyon testindedir. Rol kontrolü (`@PreAuthorize`) ancak güvenlik zinciri
çalışırken test edilebildiği için 401/403 uçtan uca API testindedir.

---

## 3. Testleri çalıştırma

**Ön koşul:** Java 21 ve çalışan Docker Desktop (Testcontainers konteyneri Docker'da açılır).
`.env` gerekmez: test profili kendi sahte JWT anahtarını ve konteyner bağlantısını kullanır.

| Ne | Komut |
|---|---|
| Bütün testler, kapsam raporu | `./mvnw verify` |
| Tek sınıf | `./mvnw test -Dtest=LeaveCancellationIntegrationTest` |
| Tek metot | `./mvnw test -Dtest="LeaveCancellationIntegrationTest#aFailureInTheRefundRollsBackTheCancellation"` |
| Bir paketteki testler | `./mvnw test -Dtest="com/dogus/vacation/domain/**/*Test"` |
| Kapsam raporu (HTML) | `./mvnw verify` sonrası `target/site/jacoco/index.html` |
| SonarQube analizi | README, "SonarQube Analysis" |

> Windows PowerShell'de `-D...` ile başlayan parametreleri tırnak içinde yazın
> (`"-Dtest=LeaveRequestTest"`); tırnaksız yazılınca PowerShell noktalı parametreyi bölebilir.

**Sürekli entegrasyon:** Her PR'da ve `dev` / `main`'e her push'ta GitHub Actions
`./mvnw verify` çalıştırır (`.github/workflows/build.yml`). Testlerden biri kırılırsa PR'da
kırmızı çarpı görünür; inceleyen kişi kırmızı PR'ı birleştirmez.

**Test profili:** `src/test/resources/application-test.yml`. Bağlantı bilgisi konteynerden gelir
(`@ServiceConnection`); log seviyesi WARN. Demo verisi (`DataSeeder`) test profilinde yüklenmez.

---

## 4. Test yazma kuralları

1. **İsim bir cümledir.** Metot adı neyin doğrulandığını İngilizce bir cümle olarak söyler:
   `aStartedLeaveCannotBeCancelled`, `theLastActiveHrCannotBeDeactivated`. Kısaltma ve
   `test1` yok.
2. **Düzen: hazırla – çalıştır – doğrula** (Arrange – Act – Assert), aralarında boş satır.
3. **Veritabanı testleri `AbstractIntegrationTest`'ten türer.** Aynı Spring bağlamı ve aynı
   konteyner paylaşılır; her test öncesi tablolar boşaltılır ve 4 hazır izin türü yeniden yüklenir.
4. **Test sınıfına `@Transactional` konmaz.** Konursa test kendi transaction'ı içinde koşar ve
   "rollback sonrası veritabanında iz kaldı mı?" sorusu ölçülemez. Bunun yerine
   `countRows("leave_request_history")` gibi doğrudan sayım yapılır.
5. **Test verisi `TestData` ile kurulur.** `TestData.department()`, `TestData.employee(dept, role)`,
   `TestData.balance(...)`, `TestData.leaveRequest(...)`. Aynı veriyi her testte elle kurmak yok.
6. **Hata hem kodla hem durumla doğrulanır.** Yalnızca "hata verdi" yetmez: `ErrorCode`
   (`MINIMUM_ACTIVE_HR_REQUIRED`), HTTP kodu (422) ve veritabanının değişmediği birlikte
   kontrol edilir.
7. **Rollback testinde hata gerçek bir bileşenden gelir.** `@MockitoSpyBean` ile bir repository
   veya servisin tek metodu hata fırlatacak şekilde ayarlanır; Handler'ın kendisi değiştirilmez.
8. **Türkçe harf tuzağı.** PostgreSQL `upper()` ile Java `toUpperCase()` "İ/i" harflerinde farklı
   davranır. Büyük/küçük harf karşılaştıran testlerde ASCII adlar kullanılır; e-posta
   karşılaştırması `lower()` ile yapılır (`EmployeeRepositoryIntegrationTest`).

---

## 5. Senaryo kapsama matrisi

Sütunlar: **Senaryo** (analiz dokümanındaki adıyla) · **Karşılayan yer** (sınıf veya uç nokta) ·
**Test** (sınıf ve metot) · **Sahip** · **Durum** · **Bekleyen** (🟡 ve ⬜ satırlarda eksik olan).

### 5.1 İş kuralları

| Senaryo | Karşılayan yer | Test | Sahip | Durum | Bekleyen |
|---|---|---|---|---|---|
| BR-01 İş günü: başlangıç ve bitiş dahil, Pzt–Cum | `WeekdayWorkingDayCalculator` | `WorkingDayCalculatorContractTest.countsWorkingDaysIncludingStartAndEnd` (6 durum: tek gün, Pzt–Cum, Cuma–Pzt, hafta sonu, iki hafta), `rejectsEndDateBeforeStartDate` | Zehra | ✅ | |
| BR-02 Resmi tatiller düşülür (bonus) | `HolidayAwareWorkingDayCalculator` | Aynı sözleşme testinden türeyecek (LSP) | Sude | ⬜ | FR-38 ile birlikte |
| BR-03 Hastalık ve ücretsiz izin bakiyeden düşmez | `LeaveBalanceService` | `LeaveBalanceServiceIntegrationTest.deductDoesNothingForLeaveTypesThatDoNotUseTheBalance`, `refundIgnoresLeaveTypesThatDoNotUseTheBalance` | Zehra | 🟡 | Onaydan sonra bakiyenin değişmediği (T1, Bilge) |
| BR-04 Mazeret izni yılda en fazla 5 gün | `SubmitLeaveHandler` | — | Bilge | ⬜ | 6. gün talebi 422 |
| BR-05 Bakiye negatife düşmez, iki kez kontrol | `LeaveBalance.use`, `LeaveBalanceService.deduct`, `SubmitLeaveHandler` | `LeaveBalanceTest.useRejectsMoreDaysThanRemainingAndKeepsTheBalance`, `LeaveBalanceServiceIntegrationTest.deductRollsBackWhenTheBalanceIsInsufficient` | Bilge, Zehra | 🟡 | Talep açarken kontrol; rollback senaryoları A ve D |
| BR-06 Geçmiş tarih yok, bitiş ≥ başlangıç, en fazla 30 iş günü | `SubmitLeaveValidator` | `SubmitLeaveValidatorTest.pastStartDateIsRejected`, `endBeforeStartIsRejected`, `rangeCrossingYearIsRejected`, `requestStartingTodayIsAllowed` | Bilge | 🟡 | 30 iş günü sınırı; uç noktada 400 |
| BR-07 Onaylayıcı sırası (İK → başka İK, yönetici, yöneticisizse İK) | `ApproverResolver` | `ApproverResolverTest.employeeWithActiveManagerIsDecidedByThatManagerOnly`, `employeeWithoutManagerIsDecidedByAnyHr`, `inactiveManagerCountsAsNoManager`, `hrRequestIsDecidedByAnotherHrEvenIfHrHasManager`, `ownRequestIsForbiddenWithSelfApprovalCode` | Bilge | 🟡 | Onay uç noktasında aynı dallar |
| BR-08 Onaylı izin sadece başlamadan iptal edilir | `LeaveRequest.cancel`, `requestCancellation`, `approveCancellationRequest` | `LeaveRequestTest.startedLeaveCannotBeCancelled`, `cancellationRequestNotAllowedOnStartDayOrForPendingRequest`, `cancellationRequestCannotBeApprovedOnceLeaveStarted`; `LeaveCancellationIntegrationTest.aStartedLeaveCannotBeCancelled`, `noCancellationRequestForAStartedLeave` | Sude | ✅ | |
| BR-09 Red ve iptalde gerekçe zorunlu | `CancelLeaveValidator`, `RequestLeaveCancellationValidator`, `LeaveRequest.reject` | `LeaveCancellationIntegrationTest.theReasonIsRequired`, `LeaveCancellationControllerIntegrationTest.cancellingWithoutAReasonIs400`, `LeaveRequestTest.rejectRequiresReason` | Sude, Bilge | 🟡 | Red uç noktasında 400 (Bilge) |
| BR-10 Pasif çalışan giriş yapamaz, talep açamaz | `LoginHandler`, `SubmitLeaveHandler` | `LoginHandlerTest.inactiveEmployeeCannotSignInEvenWithCorrectPassword`, `AuthenticationAndAuthorizationIntegrationTest.inactiveEmployeeCannotSignIn`, `EmployeeControllerIntegrationTest.aDeactivatedEmployeeCanNoLongerLogIn` | Onur, Bilge | 🟡 | Talep açamama (Bilge) |
| BR-11 Hak ediş aynı yıl iki kez çalışmaz | `RunAnnualAccrualHandler`, `annual_accrual_runs` tekil kısıtı | `RunAnnualAccrualIntegrationTest.aSecondRunForTheSameYearIsRejectedAndChangesNothing`, `RunAnnualAccrualHandlerTest.aConcurrentRunForTheSameYearIsReportedAsAlreadyRun`, `LeaveBalanceControllerIntegrationTest.runningTheSameYearTwiceIs409` | Zehra | ✅ | |
| BR-12 Kullanılmayan yıllık izin devreder (bonus) | `RunCarryOverHandler`, `LeaveBalanceService.carryOver` | `RunCarryOverIntegrationTest.carriesTheRemainingAnnualDaysToThisYear`, `runningItAgainCarriesNothingMore`, `requiresThisYearsAccrualToHaveRun` | Zehra | ✅ | |
| BR-13 En az 2 aktif İK | `HrMinimumRule` | `EmployeeChangesIntegrationTest.oneOfTheLastTwoActiveHrCannotBeDeactivated`, `theRoleOfOneOfTheLastTwoActiveHrCannotChange`, `anHrCanBeDeactivatedWhenThreeActiveHrExist`; `EmployeeControllerIntegrationTest.theLastActiveHrCannotBeDeactivated` | Sude | ✅ | |
| BR-14 Sadece talep sahibi, sadece bekleyen talebini geri çeker | `LeaveRequest.withdraw`, `WithdrawLeaveHandler` | `LeaveRequestTest.onlyPendingRequestCanBeWithdrawn` | Bilge | 🟡 | Uç nokta: başkasının talebi 403, onaylı talep 409 |
| BR-15 İptal yetkisi (yönetici; yöneticisizse İK; İK'nın iznini başka İK) | `ApproverResolver` (`CancelLeaveHandler`) | `LeaveCancellationIntegrationTest.theManagerCancelsAnApprovedLeaveAndTheDaysAreRefunded`, `anHrCannotCancelTheLeaveOfSomeoneWhoHasAManager`, `anHrCancelsTheLeaveOfSomeoneWithoutAManager`, `anotherHrCancelsAnHrLeaveButNotTheirOwn`; `LeaveCancellationControllerIntegrationTest.anEmployeeCannotUseTheCompanyCancellation` | Sude | ✅ | |
| BR-16 Her durum değişikliği aynı transaction'da geçmişe yazılır | `LeaveHistoryService` (`propagation = MANDATORY`) | `LeaveHistoryServiceIntegrationTest.historyIsRolledBackWithTheSurroundingTransaction`, `refusesToRunOutsideATransaction`, `insertsImmediatelyInsideTheTransaction` | Zehra | 🟡 | Rollback senaryosu B (onayda geçmiş yazılamazsa) |
| BR-17 İptal talebine onaylayıcı karar verir; günler sadece kabulde iade | `ApproveLeaveCancellationHandler`, `RejectLeaveCancellationHandler`, `WithdrawLeaveCancellationHandler` | `LeaveCancellationIntegrationTest.theOwnerRequestsACancellationAndNoDaysAreRefundedYet`, `theManagerApprovesTheCancellationRequestAndTheDaysAreRefunded`, `theManagerRejectsTheCancellationRequestAndTheLeaveStaysApproved`, `theOwnerWithdrawsTheCancellationRequest`, `theOwnerCannotApproveTheirOwnCancellationRequest` | Sude | ✅ | |
| BR-18 3 gün kala acil; başlangıçta sonuçsuz | `LeaveRequest.expireCancellationRequest`, `CancellationUrgencyPolicy`, gece görevi | `LeaveRequestTest.cancellationRequestExpiresOnStartDayOnly` | Onur | 🟡 | Acil bayrağı ve gece görevi (sabit `Clock` ile) |

### 5.2 Rol senaryoları ve yetki

| Senaryo | Karşılayan yer | Test | Sahip | Durum | Bekleyen |
|---|---|---|---|---|---|
| Token yok veya bozuk → 401 | `SecurityConfig` | `AuthenticationAndAuthorizationIntegrationTest.requestWithoutTokenIs401`, `tamperedTokenIs401`; her controller testinde `...WithoutATokenIs401` | Onur | ✅ | |
| Yanlış rol → 403, ortak hata biçimi | `@PreAuthorize` | `AuthenticationAndAuthorizationIntegrationTest.employeeGets403OnHrOnlyEndpoints`; `EmployeeControllerIntegrationTest.aManagerCannotCreateAnEmployee`, `DepartmentControllerIntegrationTest.anEmployeeCannotCreateADepartment` | Onur, herkes | ✅ | |
| Giriş: yanlış şifre ile bilinmeyen e-posta aynı cevabı alır | `LoginHandler` | `AuthenticationAndAuthorizationIntegrationTest.unknownEmailGetsSameAnswerAsWrongPassword`, `LoginHandlerTest.unknownEmailIs401AndStillComparesAPassword` | Onur | ✅ | |
| Yönetici yalnızca kendi ekibinin bakiyesini görür | `GetEmployeeBalancesHandler` | `LeaveBalanceControllerIntegrationTest.managerSeesTheirOwnTeamMember`, `managerCannotSeeSomeoneOutsideTheirTeam`; `GetEmployeeBalancesHandlerTest.aManagerCannotSeeAnotherManagersTeamMember` | Zehra | ✅ | |
| Çalışan talep detayında onaylayanı, red/iptal gerekçesini görür | `LeaveRequestMapper.toDetailView` | `LeaveRequestMapperTest.detailViewOfPendingRequestHasNoDecisionYet`; iptal cevabında `cancelReason` (`LeaveCancellationControllerIntegrationTest`) | Bilge | 🟡 | `GET /api/leave-requests/{id}` uç noktası |
| Yönetici onaydan önce mevcut bakiyeyi görür | `GetPendingApprovalsResponse.currentBalance` | — | Bilge | ⬜ | Bekleyenler listesi testi |
| İK-1'in talebini İK-2 onaylar | `ApproverResolver` | `ApproverResolverTest.hrRequestIsDecidedByAnotherHrEvenIfHrHasManager` | Bilge | 🟡 | Onay uç noktasında |
| İK şirket izin geçmişini filtreler | `SearchLeaveHistoryHandler` | `SearchLeaveHistoryIntegrationTest.filtersByDepartmentEmployeeAndStatus`, `combinesFilters`, `dateFilterIncludesTheWholeOfBothDaysInIstanbulTime`, `loadsAPageWithoutAQueryPerRow`; `LeaveHistoryControllerTest` | Zehra | ✅ | |
| Talep sahibi dışındakiler iptal talebi açamaz / geri alamaz | `RequestLeaveCancellationHandler`, `WithdrawLeaveCancellationHandler` | `LeaveCancellationIntegrationTest.onlyTheOwnerCanRequestACancellation`, `onlyTheOwnerCanWithdrawTheCancellationRequest` | Sude | ✅ | |

### 5.3 Demo senaryoları

Sunumda Swagger'da elle yapılacak 8 adım ve her birinin otomatik karşılığı. Demo verisi:
README, "Demo Kullanıcıları".

| Demo adımı | Karşılayan yer | Test | Sahip | Durum | Bekleyen |
|---|---|---|---|---|---|
| 1. İK çalışan ekler; hak kıdem ve yaşa göre hesaplanır (T4) | `POST /api/employees`, `EntitlementPolicy` | `EmployeeControllerIntegrationTest.hrCreatesAnEmployeeWithInitialBalances`, `theNewEmployeeCanLogIn`; `CreateEmployeeIntegrationTest.opensThisYearsAnnualAndExcuseBalancesInTheSameTransaction`; `SeniorityAndAgeEntitlementPolicyTest.calculatesAnnualLeaveDaysBySeniorityAndAge` | Sude, Zehra | ✅ | |
| 2. Cuma–Salı talebi 3 iş günü | `POST /api/leave-requests` | `WorkingDayCalculatorContractTest` (Cuma–Pazartesi = 2) | Bilge | 🟡 | Talep uç noktasında `workingDays = 3` |
| 3. Aynı tarihe ikinci talep çakışır (409) | `SubmitLeaveHandler` (FR-17) | — | Bilge | ⬜ | |
| 4. Yönetici onaylar; bakiye düşer, hareket oluşur (T1) | `POST /api/leave-requests/{id}/approve` | `LeaveBalanceServiceIntegrationTest.deductIncreasesUsedDaysAndRecordsADeduction`; `LeaveRequestTest.approveRecordsDecision` | Bilge | 🟡 | Onay entegrasyon testi |
| 5. Rollback gösterisi (T1, senaryo A) | `ApproveLeaveHandler` | — | Bilge (DBeaver: Zehra) | ⬜ | Bölüm 6, senaryo A |
| 6. Çalışan onay adresini çağırır → 403 | `@PreAuthorize` | `AuthenticationAndAuthorizationIntegrationTest.employeeGets403OnHrOnlyEndpoints` (aynı mekanizma) | Onur | 🟡 | Onay adresine özel 403 testi |
| 7. İK-1 kendi talebini onaylayamaz; İK-2 onaylar (FR-24, BR-07) | `ApproverResolver` | `ApproverResolverTest.ownRequestIsForbiddenWithSelfApprovalCode`, `hrRequestIsDecidedByAnotherHrEvenIfHrHasManager` | Bilge | 🟡 | Onay uç noktasında 403 |
| 8. İptal talebi kabul edilir; günler iade edilir, İK geçmişinde görünür (T2) | `/cancel-request`, `/cancel-request/approve`, `GET /api/leave-requests/history` | `LeaveCancellationControllerIntegrationTest.theEmployeeRequestsAndTheManagerApprovesTheCancellation`; `LeaveCancellationIntegrationTest.theManagerApprovesTheCancellationRequestAndTheDaysAreRefunded` (geçmiş satırları dahil) | Sude, Zehra | ✅ | Canlı demoda iznin önce onay akışıyla onaylanması (Bilge) |

### 5.4 Kalite gereksinimleri

| Gereksinim | Test veya kanıt | Durum |
|---|---|---|
| NFR-01 Katmanlı mimari, her use case dörtlü | `UseCaseStructureTest`; `application` ve `domain`'de HTTP/Swagger importu yok | ✅ |
| NFR-02 Sonar notları A, Quality Gate yeşil | Yerel SonarQube; README tarama tablosu | 🟡 Son tarama 29–30 Kasım |
| NFR-03 Yeni kodda kapsam ≥ %80 | JaCoCo (`target/site/jacoco`), SonarQube; ilk taramada %89.7 | 🟡 Son tarama 29–30 Kasım |
| NFR-04 Gizli bilgi kodda yok | `JwtPropertiesTest.toStringHidesTheSecret`, `rejectsMissingOrShortSecretSoTheAppDoesNotStart`; `LoginHandlerTest.passwordIsNotPrintedInLogs` | ✅ |
| NFR-05 Tek biçimde JSON hata, stack trace yok | `GlobalExceptionHandlerTest.unexpectedErrorHidesDetails`, `malformedJsonBecomes400InSameFormat`, `wrongHttpMethodBecomes405InSameFormat` | ✅ |
| NFR-06 Gelen veri doğrulanır | `SpringMediatorTest.annotationErrorsStopRequestBeforeHandler`, `customValidatorErrorsStopRequestBeforeHandler`; her `XxxValidatorTest` | ✅ |
| NFR-07 Listeler sayfalı (varsayılan 20) | `ListDepartmentsRequestTest.usesTheFirstPageOfTwentyWhenPagingIsNotGiven`, `EmployeeQueriesIntegrationTest.returnsTheRequestedPage`, `SearchLeaveHistoryValidatorTest.usesTheFirstPageOfTwentyWhenPagingIsNotGiven` | ✅ |
| NFR-08 Aynı talep aynı anda iki kez onaylanamaz | `GlobalExceptionHandlerTest.optimisticLockConflictBecomes409` | 🟡 İki thread'li eşzamanlı onay testi (senaryo C, Bilge) |
| NFR-09 README ile 15 dakikada kurulum | Temiz bilgisayarda kurulum provası | ⬜ 29–30 Kasım (Sude) |
| NFR-10 SLF4J, `System.out` yok | SonarQube kuralı | 🟡 Son tarama |

---

## 6. Transaction ve rollback testleri

Her transactional işlemde bir adım testte bilerek hata verdirilir; ardından veritabanındaki
satırlar sayılarak işlemin tamamen geri alındığı doğrulanır.

| Kod | İşlem | Hata nasıl oluşturulur | Doğrulanan | Test | Sahip | Durum |
|---|---|---|---|---|---|---|
| T1 | İzin onayı: durum, geçmiş, bakiye düşümü | Senaryo A (aşağıda) | Talep PENDING; geçmiş ve hareket yok | — | Bilge | ⬜ |
| T2 | Onaylı iznin iptali (şirket adına) | `@MockitoSpyBean BalanceTransactionRepository.save` hata fırlatır | İzin APPROVED; geçmiş satırı ve iade hareketi yok; bakiye aynı | `LeaveCancellationIntegrationTest.aFailureInTheRefundRollsBackTheCancellation` | Sude | ✅ |
| T2 | İptal talebinin kabulü | Aynı | İzin CANCEL_REQUESTED kalır; iade yok | `LeaveCancellationIntegrationTest.aFailureInTheRefundKeepsTheCancellationRequestOpen` | Sude | ✅ |
| T3 | Yıllık hak ediş | `@MockitoSpyBean EntitlementPolicy` 3. çalışanda hata fırlatır | Hiçbir çalışana hak yazılmaz, çalıştırma kaydı yok; yıl yeniden çalıştırılabilir | `RunAnnualAccrualIntegrationTest.aFailureHalfwayRollsBackEverythingAndTheYearCanBeRunAgain` | Zehra | ✅ |
| T4 | Çalışan ekleme ve ilk bakiyeler | Bakiye açma adımı hata verir | Çalışan da yok; e-posta yeniden kullanılabilir | `CreateEmployeeIntegrationTest.aFailureInTheBalanceStepRollsBackTheEmployee` | Sude | ✅ |
| T5 | Süresi dolan iptal talepleri (talep başına ayrı transaction) | Taleplerden biri hata verir | Diğer talepler sonuçsuz olur, hatalı olan değişmez | — | Onur | ⬜ |
| BR-16 | Geçmiş satırı ana transaction'la birlikte geri alınır | Dış transaction geri alınır | Geçmiş satırı yok | `LeaveHistoryServiceIntegrationTest.historyIsRolledBackWithTheSurroundingTransaction` | Zehra | ✅ |

**Planlanan gerçek rollback senaryoları** (plan bölüm 8.2):

| Senaryo | Nasıl oluşur | Beklenen | Test | Durum |
|---|---|---|---|---|
| A. Onayda bakiye yetmiyor | Bakiye 5 gün; 3'er günlük iki talep açılır (ikisi de açılışta geçer). Biri onaylanınca kalan 2 olur; ikincisi onaylanınca bakiye adımı hata verir | 422; ikinci talep PENDING, eklenen geçmiş satırı geri alınmış, bakiye 2 | Onay rollback testi (Bilge); canlı demo 5 | ⬜ |
| B. Geçmiş yazılamıyor | `@MockitoSpyBean LeaveHistoryService` hata fırlatır | Durum, bakiye ve hareket tablosu baştaki gibi | Onay rollback testi (Bilge) | ⬜ |
| C. Eşzamanlı onay | İki İK aynı talebi aynı anda onaylar (iki thread) | İlki başarılı, ikincisi 409; bakiye bir kez düşer | Eşzamanlı onay testi (Bilge, NFR-08) | ⬜ |
| D. Veritabanı son savunma hattı | Handler kontrolü atlatılmış gibi `used_days > entitled_days` yazılmaya çalışılır | CHECK ihlali, transaction tamamen geri alınır | Kalıcılık testi (Zehra) | ⬜ |

> **`@MockitoSpyBean` ve `MANDATORY` servisler:** `LeaveBalanceService` ve `LeaveHistoryService`
> `propagation = MANDATORY` ile işaretlidir. Bu servislerin kendisi spy yapılınca Mockito'nun
> ayarlama çağrıları transaction dışında servise ulaşıyor ve `IllegalTransactionStateException`
> veriyor; yarım kalan ayarlama diğer testleri de bozuyor.
> T2 testinde bu yüzden bir alt katman (`BalanceTransactionRepository.save`) spy yapıldı;
> senaryo B'de de aynı yol önerilir (`LeaveRequestHistoryRepository.save`).

---

## 7. Demo öncesi elle kontrol listesi

Sunumdan önce (29–30 Kasım) Swagger'da, demo verisiyle bir kez baştan sona yapılır. Adımlar
bölüm 5.3'teki sırayla aynıdır. Her adımda bakılacaklar:

- [ ] Uygulama `dev` profiliyle açılıyor; demo verisi yüklü (README, "Demo Kullanıcıları")
- [ ] Swagger'da **Authorize** ile İK, yönetici ve çalışan token'ları alınabiliyor
- [ ] Her adımda beklenen HTTP kodu ve Swagger'daki örnekle aynı biçimde cevap geliyor
- [ ] Hata cevaplarında `code` ve Türkçe `message` var, stack trace yok
- [ ] Demo 5 (rollback): istekten önce ve sonra DBeaver'da `leave_requests`, `leave_balances`,
      `leave_request_history` sayıları aynı; SQL logunda INSERT'ten sonra rollback satırı var
- [ ] Demo 8 sonrası İK geçmişinde (`GET /api/leave-requests/history`) iptal satırları görünüyor
- [ ] Demo bittiğinde veritabanı sıfırlanıp demo verisi yeniden yükleniyor

---

## 8. Durum özeti (28 Kasım 2026)

- **Toplam:** 507 test (`./mvnw verify`, `dev`); hepsi geçiyor.
- **İlk Sonar taraması (20 Kasım):** kapsam %89.7, Quality Gate geçti.
- **Bekleyenler:** Talep ve onay akışının testleri (Bilge: BR-04, BR-05, BR-06 sınırı, T1 ve
  rollback senaryoları A–C, demo 2–7), iptal uyarısı ve gece görevi (Onur: BR-18, T5), veritabanı
  son savunma hattı (Zehra: senaryo D), resmi tatiller (Sude, bonus: BR-02).
- Bu doküman, bekleyen satırlar birleştikçe aynı PR'ın son commit'inde güncellenir: 🟡/⬜ yerine
  ✅ ve test adı yazılır.
