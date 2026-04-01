package sk.sporixx.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Reprezentuje finančnú transakciu (príjem, výdavok, investment).
 * Ak je to transakcia medzi účtami (saving, savaing_expense)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    private int id;
    private int accountId;
    private Integer targetAccountId;    // nullable - len pre prevody medzi účtami
    private int categoryId;
    private int transactionTypeId;
    private int transactionStatusId;
    private Integer spendingClassificationId;   // nullable - len pre TYPE_EXPENSE
    private double amount;
    private String currencyCode;
    private String description;
    private LocalDateTime completeDate;
    private LocalDateTime createdAt;

    // ── Transaction Type (DB tabuľka transaction_type) ──
    public static final int TYPE_INCOME = 1;
    public static final int TYPE_EXPENSE = 2;
    public static final int TYPE_SAVING = 3;
    public static final int TYPE_INVESTMENT = 4;
    public static final int TYPE_SAVING_EXPENSE = 5;

    public boolean isIncome()        { return this.transactionTypeId == TYPE_INCOME; }
    public boolean isExpense()       { return this.transactionTypeId == TYPE_EXPENSE; }
    public boolean isSaving()        { return this.transactionTypeId == TYPE_SAVING; }
    public boolean isSavingExpense() { return this.transactionTypeId == TYPE_SAVING_EXPENSE; }
    public boolean isInvestment()    { return this.transactionTypeId == TYPE_INVESTMENT; }

    /** Transakcia medzi účtami (Saving, Saving Expense) */
    public boolean isTransferBetweenAccounts() {
        return isSaving() || isSavingExpense();
    }

    // ── Spending Classification (DB tabuľka spending_classification) ──
    public static final int CLASSIFICATION_NEED = 1;
    public static final int CLASSIFICATION_WANT = 2;

    public boolean isNeed() { return spendingClassificationId != null && spendingClassificationId == CLASSIFICATION_NEED; }
    public boolean isWant() { return spendingClassificationId != null && spendingClassificationId == CLASSIFICATION_WANT; }

    // ── Transaction Status (DB tabuľka transaction_status) ──
    public static final int STATUS_COMPLETED = 1;
    public static final int STATUS_PENDING = 2;
}