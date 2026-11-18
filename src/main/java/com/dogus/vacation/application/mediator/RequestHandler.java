package com.dogus.vacation.application.mediator;

/**
 * Tek bir use case'in iş mantığını çalıştırır.
 *
 * <p>Her {@code XxxHandler} bu arayüzü uygular ve bir Spring bean'idir
 * ({@code @Component}). Veritabanına bakan kurallar (bakiye, çakışma, yetki, durum)
 * burada kontrol edilir; transaction sınırı {@code handle} metodundadır.
 * {@link Mediator} Handler'ı ancak istek doğrulandıktan sonra çağırır.
 *
 * @param <Q> işlenen istek tipi
 * @param <R> dönen cevap tipi
 */
public interface RequestHandler<Q extends Request<R>, R> {

    /**
     * @param request doğrulanmış istek
     * @return use case'in cevabı
     */
    R handle(Q request);
}
