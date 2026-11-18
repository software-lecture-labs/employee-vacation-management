package com.dogus.vacation.application.mediator;

/**
 * Use case'lere tek giriş kapısı.
 *
 * <p>Controller'lar ve gece görevi sadece bu arayüzü bilir: isteği kurar ve
 * {@code mediator.send(request)} der. Mediator isteği önce doğrular, hata yoksa
 * isteğin Handler'ını çağırır. Giriş katmanı (REST, ileride SOAP veya gRPC)
 * değişse de Handler'lar değişmez.
 */
public interface Mediator {

    /**
     * @param request gönderilecek istek
     * @param <R>     cevap tipi
     * @return Handler'ın cevabı
     * @throws ValidationException istek doğrulamadan geçemezse
     */
    <R> R send(Request<R> request);
}
