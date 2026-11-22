package com.dogus.vacation.application.common;

import com.dogus.vacation.domain.model.Employee;

/**
 * İşlemi yapan, giriş yapmış çalışan.
 *
 * <p>Request record'larında {@code actorId} alanı yoktur: Handler giriş yapanı bu
 * arayüzden alır. REST'te kimlik JWT'den gelir ({@code JwtCurrentUserProvider});
 * birim testlerinde Mockito ile sahtesi verilir:
 * <pre>{@code
 * CurrentUserProvider currentUser = mock(CurrentUserProvider.class);
 * when(currentUser.currentEmployee()).thenReturn(ayse);
 * }</pre>
 *
 * <p>Handler'da kullanım ({@code @Transactional} metot içinde, çünkü dönen entity'nin
 * ilişkileri tembel yüklenir):
 * <pre>{@code
 * Employee actor = currentUser.currentEmployee();
 * if (!actor.isActive()) {
 *     throw new BusinessException(ErrorCode.INACTIVE_EMPLOYEE);   // BR-10
 * }
 * }</pre>
 *
 * <p>Gece görevi gibi sistem işlemlerinde giriş yapan kullanıcı yoktur; o akışlar bu
 * arayüzü çağırmaz, geçmişe {@code actor = null} yazar.
 */
public interface CurrentUserProvider {

    /**
     * Veritabanına gitmeden, token'daki id.
     *
     * @return giriş yapan çalışanın id'si
     * @throws IllegalStateException giriş yapmış kullanıcı yoksa
     */
    Long currentEmployeeId();

    /**
     * @return giriş yapan çalışan (veritabanındaki güncel hali; pasife alınmış olabilir)
     * @throws IllegalStateException giriş yapmış kullanıcı yoksa
     * @throws com.dogus.vacation.domain.exception.NotFoundException çalışan kaydı bulunamazsa (404)
     */
    Employee currentEmployee();
}
