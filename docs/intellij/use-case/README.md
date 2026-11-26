# IntelliJ use case şablonu

Her use case dört dosyadır: `XxxRequest`, `XxxResponse`, `XxxValidator`, `XxxHandler`.
Bu klasördeki şablon, use case adını bir kez yazınca dördünü birden üretir.
`.idea/` repoya girmediği için şablon burada durur ve herkes kendi IntelliJ'ine bir kez ekler.

## Kurulum (bir kez, ~5 dakika)

1. IntelliJ'de **Settings → Editor → File and Code Templates** aç, **Files** sekmesine geç.
2. Üstteki şema seçicisinde **Project** seç (şablon sadece bu projede görünsün).
3. **+** ile yeni şablon ekle:
   - **Name:** `Use Case`
   - **Extension:** `java`
   - **File name:** `${NAME}Request`
   - İçerik: [`Request.java.ft`](Request.java.ft) dosyasının tamamı
4. `Use Case` seçiliyken **Create Child Template File** düğmesine (+ yanındaki) üç kez bas.
   Her biri için **Extension** `java`, **File name** ve içerik:

   | File name | İçerik |
   |---|---|
   | `${NAME}Response` | [`Response.java.ft`](Response.java.ft) |
   | `${NAME}Validator` | [`Validator.java.ft`](Validator.java.ft) |
   | `${NAME}Handler` | [`Handler.java.ft`](Handler.java.ft) |

5. **Apply → OK**.

## Kullanım

1. Proje ağacında use case'in paketini oluştur, örneğin
   `application/usecase/leaverequest/submit`.
2. Pakete sağ tıkla → **New → Use Case**.
3. Ad olarak sadece use case adını yaz: `SubmitLeave`.
4. Dört dosya oluşur: `SubmitLeaveRequest`, `SubmitLeaveResponse`,
   `SubmitLeaveValidator`, `SubmitLeaveHandler`.

## Oluştuktan sonra

- Request ve Response record'larına alanları ekle.
- Sadece okuyan use case'lerde Handler'daki `@Transactional` → `@Transactional(readOnly = true)`.
- Validator'da alanlar arası kural yoksa `validate` boş liste döner; sınıf yine kalır
  (her use case dört dosyadır).
- Handler'daki `UnsupportedOperationException` satırı iş mantığıyla değiştirilir;
  bu satırla PR açılmaz.
