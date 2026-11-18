package com.dogus.vacation.domain.model;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.Hibernate;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Bütün entity'lerin ortak alanları: {@code id}, {@code created_at}, {@code updated_at}.
 *
 * <p>{@code @MappedSuperclass} kendi tablosu olmayan bir üst sınıftır; alanları her
 * alt sınıfın tablosuna eklenir. Zaman damgalarını Spring Data JPA auditing doldurur
 * ({@code JpaAuditingConfig}); saat, uygulamanın {@code Clock} bean'inden alınır.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    /** {@code BIGSERIAL}: değeri veritabanı üretir. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * İki entity aynı türdeyse ve aynı veritabanı kaydını gösteriyorsa eşittir.
     * Henüz kaydedilmemiş (id'si olmayan) entity yalnızca kendisine eşittir.
     * {@link Hibernate#getClass} lazy proxy'leri gerçek sınıflarıyla karşılaştırır.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) {
            return false;
        }
        Long thisId = getId();
        return thisId != null && thisId.equals(((BaseEntity) other).getId());
    }

    /** Kayıttan önce ve sonra aynı kalması için id'ye değil sınıfa bağlıdır. */
    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
