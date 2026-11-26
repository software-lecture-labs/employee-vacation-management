package com.dogus.vacation.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * {@link BaseEntity}'nin eşitlik kurallarını veritabanı olmadan doğrular.
 *
 * <p>Entity'ler {@code Set} veya {@code Map} içinde kullanıldığında yanlış bir eşitlik zor bulunan
 * hatalara yol açar: aynı kayıt iki kez sayılabilir ya da kaydedilmemiş iki nesne aynı sanılabilir.
 * Id'yi veritabanı verdiği için testlerde {@link ReflectionTestUtils} ile atanır.
 */
class BaseEntityTest {

    private static Department departmentWithId(Long id) {
        Department department = new Department("Yazılım Geliştirme");
        ReflectionTestUtils.setField(department, "id", id);
        return department;
    }

    @Test
    void anEntityIsEqualToItself() {
        Department unsaved = new Department("Yazılım Geliştirme");

        assertThat(unsaved).isEqualTo(unsaved);
    }

    @Test
    void twoObjectsOfTheSameRecordAreEqualAndHaveTheSameHashCode() {
        Department first = departmentWithId(7L);
        Department second = departmentWithId(7L);

        assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
    }

    @Test
    void differentRecordsAreNotEqual() {
        assertThat(departmentWithId(7L)).isNotEqualTo(departmentWithId(8L));
    }

    /** Kaydedilmemiş iki nesnenin ikisinin de id'si boştur; yine de farklı kayıtlardır. */
    @Test
    void twoUnsavedEntitiesAreNotEqual() {
        assertThat(new Department("A")).isNotEqualTo(new Department("A"));
    }

    @Test
    void entitiesOfDifferentTypesAreNotEqualEvenWithTheSameId() {
        PublicHoliday holiday = new PublicHoliday(LocalDate.of(2026, 10, 29), "Cumhuriyet Bayramı");
        ReflectionTestUtils.setField(holiday, "id", 7L);

        assertThat(departmentWithId(7L)).isNotEqualTo(holiday);
    }

    @Test
    void anEntityIsNotEqualToNull() {
        assertThat(departmentWithId(7L).equals(null)).isFalse();
    }

    /** Hash kodu id'ye bağlı değildir: nesne kaydedilmeden önce bir Set'e konduysa orada bulunmaya devam eder. */
    @Test
    void hashCodeDoesNotChangeWhenTheRecordIsSaved() {
        Department department = new Department("Yazılım Geliştirme");
        int beforeSave = department.hashCode();

        ReflectionTestUtils.setField(department, "id", 7L);

        assertThat(department.hashCode()).isEqualTo(beforeSave);
    }
}
