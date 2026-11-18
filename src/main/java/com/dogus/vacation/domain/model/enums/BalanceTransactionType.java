package com.dogus.vacation.domain.model.enums;

/**
 * Bakiye hareketinin türü ({@code balance_transactions.type}).
 */
public enum BalanceTransactionType {
    /** Hak ediş: yıllık hak ya da ilk bakiye. */
    ACCRUAL,
    /** Düşüm: onaylanan izin. */
    DEDUCTION,
    /** İade: iptal edilen izin. */
    REFUND
}
