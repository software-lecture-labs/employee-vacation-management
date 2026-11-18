package com.dogus.vacation.domain.model.enums;

/**
 * Kullanıcının sistemdeki rolü; her çalışanın tek bir rolü vardır.
 *
 * <p>Veritabanında metin olarak saklanır ({@code employees.role}) ve
 * {@code ck_employees_role} kısıtı yalnızca bu üç değere izin verir.
 */
public enum Role {
    EMPLOYEE,
    MANAGER,
    HR
}
