package com.dogus.vacation.domain.service;

import java.math.BigDecimal;
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
import com.dogus.vacation.infrastructure.persistence.BalanceTransactionRepository;
import com.dogus.vacation.infrastructure.persistence.LeaveBalanceRepository;
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
 * çağıranın tür kontrolü yapmasına gerek yoktur.
 */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class LeaveBalanceService {

    private final LeaveBalanceRepository balanceRepository;
    private final BalanceTransactionRepository transactionRepository;

    public LeaveBalanceService(LeaveBalanceRepository balanceRepository,
                               BalanceTransactionRepository transactionRepository) {
        this.balanceRepository = balanceRepository;
        this.transactionRepository = transactionRepository;
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
