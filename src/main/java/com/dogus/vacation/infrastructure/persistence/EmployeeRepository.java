package com.dogus.vacation.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import com.dogus.vacation.domain.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * {@code employees} tablosu için Spring Data repository'si.
 *
 * <p>Temel kaydetme, bulma ve listeleme metotları {@link JpaRepository}'den gelir;
 * özel sorgular onları kullanan use case ile birlikte eklenir.
 */
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    /**
     * Girişte kullanılır. E-posta büyük/küçük harfe duyarsız tekildir
     * ({@code uq_employees_email_lower}), bu yüzden en fazla bir kayıt döner.
     *
     * <p>Sorgu elle yazılmıştır: Spring Data metot adındaki {@code IgnoreCase}'ten
     * {@code upper(email) = upper(?)} üretir. Tekil indeks ise {@code lower(email)}
     * üzerindedir; PostgreSQL bir ifade indeksini yalnızca sorgu aynı ifadeyi kullanırsa
     * kullanabilir. {@code upper} ile her girişte bütün çalışan tablosu taranıyordu;
     * {@code lower} ile indeks kullanılır ve giriş, tekillik kısıtıyla aynı kuralla karşılaştırır.
     * Metodun adı ve davranışı (harf duyarsız arama) değişmedi.
     */
    @Query("select e from Employee e where lower(e.email) = lower(:email)")
    Optional<Employee> findByEmailIgnoreCase(@Param("email") String email);

    /**
     * Yıllık hak edişte (T3) bakiye açılacak çalışanlar: yalnızca aktifler (BR-10), id sırasıyla.
     * Sıra sabit olsun diye verilir; hata mesajları ve testler her çalıştırmada aynı çalışanı gösterir.
     */
    List<Employee> findAllByActiveTrueOrderByIdAsc();
}
