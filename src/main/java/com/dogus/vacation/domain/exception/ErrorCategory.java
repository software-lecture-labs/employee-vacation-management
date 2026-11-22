package com.dogus.vacation.domain.exception;

/**
 * Bir alan hatasının türü.
 *
 * <p>Alan katmanı HTTP bilmez; hatanın hangi HTTP koduna (REST'te) ya da hangi gRPC
 * durumuna çevrileceğine giriş katmanı bu türe bakarak karar verir.
 */
public enum ErrorCategory {

    /** Kimlik doğrulanamadı: giriş bilgileri hatalı ya da token yok/geçersiz (REST: 401). */
    UNAUTHENTICATED,

    /** Aranan kayıt yok (REST: 404). */
    NOT_FOUND,

    /** İşlemi yapan kişinin bu kayıt üzerinde yetkisi yok (REST: 403). */
    FORBIDDEN,

    /** Kaydın mevcut durumu işlemle çatışıyor (REST: 409). */
    CONFLICT,

    /** İstek geçerli ama bir iş kuralı izin vermiyor (REST: 422). */
    BUSINESS_RULE
}
