## Özet
<!-- 1-2 cümle: bu PR ne yapıyor, neden -->

## İlgili iş
<!-- Örnek: Closes #23 · FR-21, FR-24 · BR-05, BR-07
     Not: dev'e birleşen PR issue'yu kapatmaz; birleşince issue'yu elle kapatın. -->
Closes #

## Değişiklikler
<!-- Dosya veya sınıf bazında kısa maddeler -->
- 

## Nasıl test edildi
- [ ] `./mvnw verify` yeşil
- [ ] Swagger'da başarılı ve hatalı istek denendi (örnek istek/cevap aşağıda)

<!-- Örnek istek ve cevap:
```http
POST /api/...
```
```json
{ }
```
-->

## Kontrol listesi
- [ ] Base branch `dev` (zincirli PR ise birleştirmeden önce `dev`'e çevrildi)
- [ ] Controller'da iş kuralı yok, Entity dışarı dönmüyor
- [ ] Yeni ve değişen kod için test var
- [ ] SonarQube for IDE'de yeni uyarı yok
- [ ] Commit başlık ve açıklamaları [CONTRIBUTING.md](../CONTRIBUTING.md) 4. bölüme uygun
- [ ] Entity veya SQL script değiştiyse Zehra inceleyici olarak eklendi
- [ ] README veya ilgili doküman güncellendi
- [ ] `docs/REQUIREMENTS.md`'de karşılanan satırlar ✅ yapıldı, PR numarası yazıldı
- [ ] `docs/reports/<ad>.md`'ye bu PR'ın bölümü eklendi
