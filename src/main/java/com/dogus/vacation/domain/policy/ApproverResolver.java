package com.dogus.vacation.domain.policy;

import java.util.Objects;

import com.dogus.vacation.domain.exception.ErrorCode;
import com.dogus.vacation.domain.exception.ForbiddenActionException;
import com.dogus.vacation.domain.model.Employee;
import com.dogus.vacation.domain.model.enums.Role;
import org.springframework.stereotype.Component;

/**
 * Bir izin talebine kimin karar vereceğini belirler (BR-07, FR-24).
 *
 * <p>Sırayla:
 * <ol>
 *   <li>Talep sahibi İK ise: kendisi dışındaki herhangi bir aktif İK.</li>
 *   <li>Değilse ve aktif bir yöneticisi varsa: o yönetici.</li>
 *   <li>Yöneticisi yoksa veya pasifse: herhangi bir aktif İK (plan K-3).</li>
 * </ol>
 *
 * <p>Kural her işlem anında yeniden hesaplanır; talep beklerken yönetici değişirse yeni
 * yönetici karar verir. Aynı kural onay, red, iptal talebinin kabulü/reddi ve şirket
 * adına iptal için kullanılır (BR-15, BR-17). Rol kontrolü ({@code @PreAuthorize})
 * endpoint'tedir; bu sınıf "bu talebe bu kişi mi karar verir" sorusunu cevaplar.
 */
@Component
public class ApproverResolver {

    /**
     * @param requester talep sahibi
     * @return talebe kimin karar vereceği
     */
    public ApprovalRoute resolve(Employee requester) {
        Objects.requireNonNull(requester, "requester");
        if (requester.getRole() == Role.HR) {
            return ApprovalRoute.toAnyHr(requester);
        }
        Employee manager = requester.getManager();
        if (manager != null && manager.isActive()) {
            return ApprovalRoute.toManager(manager);
        }
        return ApprovalRoute.toAnyHr(null);
    }

    /**
     * @param actor     işlemi yapmak isteyen kişi
     * @param requester talep sahibi
     * @return {@code actor} bu talebe karar verebiliyorsa {@code true}; kendi talebine asla
     */
    public boolean canDecide(Employee actor, Employee requester) {
        return actor != null && !actor.equals(requester) && resolve(requester).allows(actor);
    }

    /**
     * Handler'larda kullanılır: karar veremeyen kişi için 403 fırlatır.
     *
     * @throws ForbiddenActionException kendi talebiyse {@code SELF_APPROVAL_NOT_ALLOWED},
     *                                  başka birinin onaylayıcısıysa {@code ACTION_NOT_ALLOWED}
     */
    public void checkCanDecide(Employee actor, Employee requester) {
        Objects.requireNonNull(actor, "actor");
        if (actor.equals(requester)) {
            throw new ForbiddenActionException(ErrorCode.SELF_APPROVAL_NOT_ALLOWED);
        }
        if (!resolve(requester).allows(actor)) {
            throw new ForbiddenActionException();
        }
    }
}
