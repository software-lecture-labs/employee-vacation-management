package com.dogus.vacation.application.common;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/**
 * Sayfalı liste cevabı. Spring'in {@link Page} nesnesi dışarı verilmez: JSON biçimi Spring
 * sürümüne bağlı kalmaz ve SOAP/gRPC giriş katmanları da aynı yapıyı kullanabilir (plan 6.2).
 *
 * @param content       bu sayfadaki kayıtlar
 * @param page          sayfa numarası, 0'dan başlar
 * @param size          sayfa büyüklüğü
 * @param totalElements filtreye uyan toplam kayıt
 * @param totalPages    toplam sayfa
 * @param <T>           kayıt tipi (bir view record'u)
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public PageResponse {
        content = List.copyOf(content);
    }

    /**
     * Repository'den gelen sayfayı, her kaydı view'a çevirerek cevaba dönüştürür.
     *
     * <pre>{@code
     * PageResponse<EmployeeView> response = PageResponse.of(employeePage, EmployeeMapper::toView);
     * }</pre>
     */
    public static <E, T> PageResponse<T> of(Page<E> page, Function<? super E, ? extends T> mapper) {
        List<T> content = page.getContent().stream().<T>map(mapper).toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
