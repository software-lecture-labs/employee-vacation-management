package com.dogus.vacation.domain.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.dogus.vacation.domain.exception.BusinessException;
import com.dogus.vacation.domain.exception.ErrorCode;
import com.dogus.vacation.domain.exception.InsufficientBalanceException;
import com.dogus.vacation.domain.exception.NotFoundException;
import com.dogus.vacation.domain.model.BalanceTransaction;
import com.dogus.vacation.domain.model.Employee;
import com.dogus.vacation.domain.model.LeaveBalance;
import com.dogus.vacation.domain.model.LeaveRequest;
import com.dogus.vacation.domain.model.LeaveType;
import com.dogus.vacation.domain.model.enums.BalanceTransactionType;
import com.dogus.vacation.domain.policy.EntitlementPolicy;
import com.dogus.vacation.infrastructure.persistence.BalanceTransactionRepository;
import com.dogus.vacation.infrastructure.persistence.LeaveBalanceRepository;
import com.dogus.vacation.infrastructure.persistence.LeaveTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * İzin bakiyesindeki her değişikliği yapan ve hareket kaydını yazan alan servisi
 * (FR-32, FR-35; BR-03, BR-05).
 *
 * <p>Bakiye yalnızca bu servis üzerinden değişir; her değişiklik aynı anda
 * {@code balance_transactions} tablosuna kim, ne zaman, kaç gün ve neden bilgisiyle
 * yazılır. Onay (T1), iptal (T2), çalışan ekleme (T4) ve yıllık hak ediş (T3) aynı
 * kuralları kullansın diye ortak iş burada toplanmıştır.
 *
 * <p><b>Transaction:</b> Servis kendi transaction'ını açmaz (alan katmanı kuralı);
 * {@link Propagation#MANDATORY} ile çağıran Handler'ın transaction'ına katılır. Bir
 * Handler dışından transaction'sız çağrılırsa Spring hemen
 * {@code IllegalTransactionStateException} fırlatır; böylece bakiye ile talep durumu
 * yanlışlıkla ayrı ayrı kaydedilemez. Servisin fırlattığı hatalar Handler'ın
 * transaction'ını geri aldırır.
 *
 * <p><b>Bakiyeden düşmeyen türler (BR-03):</b> Hastalık ve ücretsiz izin gibi
 * {@link LeaveType#isDeductsFromBalance()} {@code false} olan türlerde
 * {@link #checkSufficient}, {@link #deduct} ve {@link #refund} hiçbir şey yapmaz;
 * çağıranın tür kontrolü yapmasına gerek yoktur. Bu türler için yıllık bakiye de açılmaz.
 *
 * <p><b>Devir (BR-12):</b> Kullanılmayan yıllık izin {@link #carryOver} ile sonraki yıla geçer.
 *
 * <p><b>Yıllık bakiye kuralı tek yerde:</b> Bir çalışana bir yıl için hangi türlerde kaç
 * günlük bakiye açılacağı {@link #openYearBalances} içindedir. Yeni yıl (T3) ve yeni çalışan
 * (T4) aynı metodu çağırır; kural iki ayrı yerde yazılıp zamanla birbirinden ayrılmaz.
 */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class LeaveBalanceService {

    private final LeaveBalanceRepository balanceRepository;
    private final BalanceTransactionRepository transactionRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final EntitlementPolicy entitlementPolicy;

    public LeaveBalanceService(LeaveBalanceRepository balanceRepository,
                               BalanceTransactionRepository transactionRepository,
                               LeaveTypeRepository leaveTypeRepository,
                               EntitlementPolicy entitlementPolicy) {
        this.balanceRepository = balanceRepository;
        this.transactionRepository = transactionRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.entitlementPolicy = entitlementPolicy;
    }

    /**
     * Talep açılırken bakiyenin yetip yetmediğini kontrol eder; hiçbir şey yazmaz
     * (FR-16, BR-05). Onayda {@link #deduct} aynı kontrolü bir kez daha yapar, çünkü
     * arada başka bir talep onaylanmış olabilir.
     *
     * @throws InsufficientBalanceException kalan gün yetmiyorsa (422)
     * @throws NotFoundException            o yıl için bakiye yoksa (404)
     */
    public void checkSufficient(Employee employee, LeaveType leaveType, int year, BigDecimal days) {
        Objects.requireNonNull(employee, "employee");
        Objects.requireNonNull(leaveType, "leaveType");
        if (!leaveType.isDeductsFromBalance()) {
            return;
        }
        findBalance(employee, leaveType, year).requireAvailable(days);
    }

    /**
     * Onaylanan talebin iş günlerini bakiyeden düşer ve DEDUCTION hareketi yazar (T1).
     * Bakiye yılı talebin başlangıç yılıdır.
     *
     * @param request onaylanan talep
     * @param actor   onaylayan kişi; hareket kaydında "kim" olarak görünür
     * @throws InsufficientBalanceException kalan gün yetmiyorsa; bakiye ve hareket değişmez
     * @throws NotFoundException            o yıl için bakiye yoksa
     */
    public void deduct(LeaveRequest request, Employee actor) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(actor, "actor");
        if (!request.getLeaveType().isDeductsFromBalance()) {
            return;
        }
        LeaveBalance balance = balanceOf(request);
        balance.use(request.getWorkingDays());
        record(balance, request, BalanceTransactionType.DEDUCTION, request.getWorkingDays(),
                "İzin talebi #" + request.getId() + " onaylandı", actor);
    }

    /**
     * İptal edilen talebin iş günlerini bakiyeye iade eder ve REFUND hareketi yazar (T2).
     *
     * @param request iptal edilen talep
     * @param actor   iptali yapan ya da iptal talebini kabul eden kişi
     * @throws NotFoundException o yıl için bakiye yoksa
     */
    public void refund(LeaveRequest request, Employee actor) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(actor, "actor");
        if (!request.getLeaveType().isDeductsFromBalance()) {
            return;
        }
        LeaveBalance balance = balanceOf(request);
        balance.restore(request.getWorkingDays());
        record(balance, request, BalanceTransactionType.REFUND, request.getWorkingDays(),
                "İzin talebi #" + request.getId() + " iptal edildi", actor);
    }

    /**
     * Bir çalışana bir yıl için bakiye açar (yeni çalışan T4, yeni yıl T3). Hak edilen
     * gün sıfırdan büyükse ACCRUAL hareketi de yazılır; 0 gün (örneğin kıdemi 0 olan yeni
     * çalışanın yıllık izni) için satır açılır ama hareket yazılmaz, çünkü hareket
     * tablosu yalnızca pozitif gün kabul eder.
     *
     * @param entitledDays hak edilen gün, sıfır veya pozitif
     * @param actor        işlemi yapan İK çalışanı
     * @return kaydedilen bakiye
     * @throws BusinessException        {@link ErrorCode#DUPLICATE_VALUE}: o yıl için bakiye zaten varsa (409)
     * @throws IllegalArgumentException gün negatifse
     */
    public LeaveBalance openBalance(Employee employee, LeaveType leaveType, int year,
                                    BigDecimal entitledDays, Employee actor) {
        Objects.requireNonNull(employee, "employee");
        Objects.requireNonNull(leaveType, "leaveType");
        Objects.requireNonNull(entitledDays, "entitledDays");
        Objects.requireNonNull(actor, "actor");
        if (entitledDays.signum() < 0) {
            throw new IllegalArgumentException("Hak edilen gün negatif olamaz.");
        }
        if (balanceRepository.existsByEmployeeAndLeaveTypeAndYear(employee, leaveType, year)) {
            throw new BusinessException(ErrorCode.DUPLICATE_VALUE,
                    "Bu çalışanın " + leaveType.getName() + " bakiyesi " + year + " yılı için zaten açılmış.");
        }
        return saveNewBalance(employee, leaveType, year, entitledDays, actor);
    }

    /**
     * Bir çalışana bir yıl için bütün yıllık bakiyelerini açar (T3 yeni yıl, T4 yeni çalışan).
     * İzin türlerini kendisi okur; çok sayıda çalışan için türleri bir kez okuyup
     * {@link #openYearBalances(Employee, int, List, Employee)} kullanın.
     *
     * @return açılan bakiyeler; hiçbiri açılmadıysa boş liste
     */
    public List<LeaveBalance> openYearBalances(Employee employee, int year, Employee actor) {
        return openYearBalances(employee, year,
                leaveTypeRepository.findAllByActiveTrueAndDeductsFromBalanceTrueOrderByIdAsc(), actor);
    }

    /**
     * Bir çalışana bir yıl için verilen türlerde bakiye açar. Kurallar:
     * <ul>
     *   <li>Yalnızca aktif ve bakiyeden düşen türlere bakiye açılır (BR-03).</li>
     *   <li>Yıllık izin ({@code ANNUAL}) günü {@link EntitlementPolicy} ile kıdem ve yaşa göre
     *       hesaplanır (FR-33); diğer türler yıllık sınırları kadar açılır (örneğin mazeret 5 gün).
     *       Yıllık sınırı olmayan başka bir türün dayanacağı bir gün sayısı olmadığı için ona
     *       bakiye açılmaz.</li>
     *   <li>O yıl için o türde bakiyesi zaten olan çalışanın o türü atlanır, hata verilmez.
     *       Yıl içinde eklenen ve T4 ile bakiyesi açılan çalışan böylece T3'ü bozmaz (BR-11).</li>
     *   <li>İşe giriş yılı istenen yıldan sonraysa çalışan henüz işe başlamamıştır;
     *       hiçbir bakiye açılmaz.</li>
     * </ul>
     *
     * @param leaveTypes aday türler; uygun olmayanlar burada elenir
     * @param actor      işlemi yapan İK çalışanı; ACCRUAL hareketinde "kim" olarak görünür
     * @return açılan bakiyeler, tür sırasıyla; hiçbiri açılmadıysa boş liste
     */
    public List<LeaveBalance> openYearBalances(Employee employee, int year, List<LeaveType> leaveTypes,
                                               Employee actor) {
        Objects.requireNonNull(employee, "employee");
        Objects.requireNonNull(leaveTypes, "leaveTypes");
        Objects.requireNonNull(actor, "actor");
        List<LeaveBalance> opened = new ArrayList<>();
        if (employee.getHireDate().getYear() > year) {
            return opened;
        }
        for (LeaveType leaveType : leaveTypes) {
            BigDecimal entitledDays = yearlyEntitlement(employee, leaveType, year);
            if (entitledDays == null
                    || balanceRepository.existsByEmployeeAndLeaveTypeAndYear(employee, leaveType, year)) {
                continue;
            }
            opened.add(saveNewBalance(employee, leaveType, year, entitledDays, actor));
        }
        return opened;
    }

    /**
     * Bir yılın kalan gününü sonraki yılın aynı türdeki bakiyesine devreder (BR-12).
     *
     * <p>Eski bakiyede hak edilen gün kalan kadar azalır (DEDUCTION, "2027 yılına devredildi"),
     * yeni bakiyede aynı kadar artar (ACCRUAL, "2026 yılından devreden"). Eski yılın kalanı sıfır
     * olduğu için aynı gün iki yılda kullanılamaz; devir ikinci kez çağrılırsa devredecek gün
     * kalmamıştır ve hiçbir şey yapılmaz.
     *
     * @param from  önceki yılın bakiyesi
     * @param to    aynı çalışanın aynı türde sonraki yıl bakiyesi
     * @param actor işlemi yapan İK; iki hareket kaydında da "kim" olarak görünür
     * @return devredilen gün; kalan yoksa sıfır
     * @throws IllegalArgumentException bakiyeler aynı çalışanın, aynı türün ve ardışık yılların değilse
     */
    public BigDecimal carryOver(LeaveBalance from, LeaveBalance to, Employee actor) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(actor, "actor");
        if (!from.getEmployee().getId().equals(to.getEmployee().getId())
                || !from.getLeaveType().getId().equals(to.getLeaveType().getId())
                || to.getYear() != from.getYear() + 1) {
            throw new IllegalArgumentException(
                    "Devir aynı çalışanın aynı türdeki ardışık iki yılı arasında yapılır.");
        }
        BigDecimal days = from.remainingDays();
        if (days.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        from.transferOut(days);
        to.transferIn(days);
        record(from, null, BalanceTransactionType.DEDUCTION, days, to.getYear() + " yılına devredildi", actor);
        record(to, null, BalanceTransactionType.ACCRUAL, days, from.getYear() + " yılından devreden", actor);
        return days;
    }

    /** Türün o yıl için hak edilen günü; bakiye açılmayacaksa {@code null}. */
    private BigDecimal yearlyEntitlement(Employee employee, LeaveType leaveType, int year) {
        if (!leaveType.isActive() || !leaveType.isDeductsFromBalance()) {
            return null;
        }
        if (LeaveType.ANNUAL_CODE.equals(leaveType.getCode())) {
            return BigDecimal.valueOf(
                    entitlementPolicy.annualLeaveDays(employee.getHireDate(), employee.getBirthDate(), year));
        }
        return leaveType.getAnnualLimitDays();
    }

    private LeaveBalance saveNewBalance(Employee employee, LeaveType leaveType, int year,
                                        BigDecimal entitledDays, Employee actor) {
        LeaveBalance balance = balanceRepository.save(new LeaveBalance(employee, leaveType, year, entitledDays));
        if (entitledDays.signum() > 0) {
            record(balance, null, BalanceTransactionType.ACCRUAL, entitledDays,
                    year + " yılı hak edişi", actor);
        }
        return balance;
    }

    private LeaveBalance balanceOf(LeaveRequest request) {
        return findBalance(request.getEmployee(), request.getLeaveType(), request.getStartDate().getYear());
    }

    private LeaveBalance findBalance(Employee employee, LeaveType leaveType, int year) {
        return balanceRepository.findByEmployeeAndLeaveTypeAndYear(employee, leaveType, year)
                .orElseThrow(() -> new NotFoundException(ErrorCode.LEAVE_BALANCE_NOT_FOUND));
    }

    private void record(LeaveBalance balance, LeaveRequest request, BalanceTransactionType type,
                        BigDecimal days, String note, Employee actor) {
        transactionRepository.save(new BalanceTransaction(balance, request, type, days, note, actor));
    }
}
