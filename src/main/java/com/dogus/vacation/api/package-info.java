/**
 * Giriş katmanı: REST Controller'lar ve {@code advice/GlobalExceptionHandler}.
 *
 * <p>Controller'da iş kuralı yoktur; her metot isteği bir Request record'una çevirip
 * {@code mediator.send(request)} çağırır. Entity bu katmana çıkmaz.
 */
package com.dogus.vacation.api;
