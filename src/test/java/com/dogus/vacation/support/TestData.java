package com.dogus.vacation.support;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import com.dogus.vacation.domain.model.Department;
import com.dogus.vacation.domain.model.Employee;
import com.dogus.vacation.domain.model.LeaveBalance;
import com.dogus.vacation.domain.model.LeaveRequest;
import com.dogus.vacation.domain.model.LeaveType;
import com.dogus.vacation.domain.model.enums.Role;

/**
 * Testler için makul varsayılan değerli nesneler üretir.
 *
 * <p>Nesneler kaydedilmemiş döner: veritabanlı testlerde repository ile kaydedilir,
 * Mockito'lu birim testlerinde doğrudan kullanılır. Tekil olması gereken alanlar
 * (departman adı, e-posta) her çağrıda farklı üretilir, testler çakışmaz.
 * Varsayılandan farklı bir değer gerekiyorsa dönen nesnenin setter'ı kullanılır.
 */
public final class TestData {

    /** 2026 hak edişinde 6 yıl kıdem (20 gün). */
    public static final LocalDate DEFAULT_HIRE_DATE = LocalDate.of(2020, 1, 15);

    /** 2026'da 36 yaş: yaş kuralına takılmaz. */
    public static final LocalDate DEFAULT_BIRTH_DATE = LocalDate.of(1990, 6, 1);

    /** Gerçek bir BCrypt hash'i değildir; giriş testleri kendi hash'ini verir. */
    public static final String DEFAULT_PASSWORD_HASH = "test-password-hash";

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    private TestData() {
    }

    public static Department department() {
        return new Department("Test Departmanı " + next());
    }

    public static Employee employee(Department department) {
        return employee(department, Role.EMPLOYEE);
    }

    public static Employee employee(Department department, Role role) {
        int n = next();
        Employee employee = new Employee("Ad" + n, "Soyad" + n, "user" + n + "@test.local",
                DEFAULT_HIRE_DATE, DEFAULT_BIRTH_DATE, role, department);
        employee.setPasswordHash(DEFAULT_PASSWORD_HASH);
        return employee;
    }

    /** Gün sayısı metin olarak verilir ("14", "2.5"): BigDecimal tam değeri korur. */
    public static LeaveBalance balance(Employee employee, LeaveType leaveType, int year, String entitledDays) {
        return new LeaveBalance(employee, leaveType, year, new BigDecimal(entitledDays));
    }

    public static LeaveRequest leaveRequest(Employee employee, LeaveType leaveType,
                                            LocalDate startDate, LocalDate endDate, String workingDays) {
        return new LeaveRequest(employee, leaveType, startDate, endDate,
                new BigDecimal(workingDays), "Test talebi");
    }

    private static int next() {
        return SEQUENCE.incrementAndGet();
    }
}
