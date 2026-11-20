package com.dogus.vacation.domain.policy;

import com.dogus.vacation.domain.model.Employee;
import com.dogus.vacation.domain.model.enums.Role;

/**
 * Bir çalışanın taleplerine kimin karar vereceği (BR-07).
 *
 * <p>Ya belirli bir yöneticidir ({@link Kind#MANAGER}) ya da herhangi bir aktif İK'dır
 * ({@link Kind#HR}); İK'nın kendi talebinde talep sahibi bu gruptan çıkarılır.
 * {@link ApproverResolver#resolve(Employee)} üretir.
 *
 * @param kind       karar veren grubun türü
 * @param manager    {@code MANAGER} ise karar verecek yönetici, değilse {@code null}
 * @param excludedHr {@code HR} ise karar veremeyecek İK (talep sahibi), değilse {@code null}
 */
public record ApprovalRoute(Kind kind, Employee manager, Employee excludedHr) {

    /** Karar veren grubun türü. */
    public enum Kind {
        /** Çalışanın kayıtlı ve aktif yöneticisi. */
        MANAGER,
        /** Herhangi bir aktif İK çalışanı. */
        HR
    }

    static ApprovalRoute toManager(Employee manager) {
        return new ApprovalRoute(Kind.MANAGER, manager, null);
    }

    static ApprovalRoute toAnyHr(Employee excludedHr) {
        return new ApprovalRoute(Kind.HR, null, excludedHr);
    }

    /**
     * @param actor işlemi yapmak isteyen kişi
     * @return bu kişi bu rotada karar verebiliyorsa {@code true}
     */
    public boolean allows(Employee actor) {
        if (actor == null || !actor.isActive()) {
            return false;
        }
        return switch (kind) {
            case MANAGER -> actor.equals(manager);
            case HR -> actor.getRole() == Role.HR && !actor.equals(excludedHr);
        };
    }
}
