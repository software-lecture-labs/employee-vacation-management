package com.dogus.vacation.infrastructure.persistence;

import java.util.ArrayList;
import java.util.List;

import com.dogus.vacation.domain.model.Employee;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

/**
 * Çalışan listesi için dinamik filtre (FR-09).
 *
 * <p>İki filtre de boş olabilir; yalnızca dolu olanlar {@code AND} ile birleştirilir.
 * Geçmiş aramasındaki ({@link LeaveRequestHistorySpecifications}) gibi Criteria API kullanılır:
 * PostgreSQL'in boş parametre tipi sorunları yaşanmaz.
 */
public final class EmployeeSpecifications {

    private EmployeeSpecifications() {
    }

    /**
     * @param departmentId çalışanın departmanı; boşsa bütün departmanlar
     * @param active       aktiflik; boşsa aktif ve pasif çalışanlar birlikte
     */
    public static Specification<Employee> matching(Long departmentId, Boolean active) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (departmentId != null) {
                predicates.add(cb.equal(root.get("department").get("id"), departmentId));
            }
            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
