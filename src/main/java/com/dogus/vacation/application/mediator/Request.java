package com.dogus.vacation.application.mediator;

/**
 * Bir use case'e gönderilen istek.
 *
 * <p>Her use case'in {@code XxxRequest} record'u bu arayüzü uygular. Tip parametresi
 * isteğin cevabını belirtir; böylece {@link Mediator#send(Request)} doğru tipi döner
 * ve Controller'da tip dönüşümü gerekmez.
 *
 * @param <R> Handler'ın döneceği cevap tipi ({@code XxxResponse})
 */
public interface Request<R> {
}
