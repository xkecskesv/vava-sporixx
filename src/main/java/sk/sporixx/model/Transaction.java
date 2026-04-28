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
    private Integer categoryId;
    private int transactionTypeId;
    private int transactionStatusId;
    private Integer spendingClassificationId;   // nullable - len pre TYPE_EXPENSE
    private double amount;
    private String currencyCode;
    private String description;
    private LocalDateTime completeDate;
    private LocalDateTime createdAt;

    // Transaction Type (DB tabuľka transaction_type)
    public static final int TYPE_INCOME = 1;
    public static final int TYPE_EXPENSE = 2;

    public boolean isIncome()        { return this.transactionTypeId == TYPE_INCOME; }
    public boolean isExpense()       { return this.transactionTypeId == TYPE_EXPENSE; }

    // Spending Classification (DB tabuľka spending_classification)
    public static final int CLASSIFICATION_NEED = 1;
    public static final int CLASSIFICATION_WANT = 2;

    public boolean isNeed() { return spendingClassificationId != null && spendingClassificationId == CLASSIFICATION_NEED; }
    public boolean isWant() { return spendingClassificationId != null && spendingClassificationId == CLASSIFICATION_WANT; }

    // Transaction Status (DB tabuľka transaction_status)
    public static final int STATUS_COMPLETED = 1;
    public static final int STATUS_PENDING = 2;

    // System Category IDs - neprebíjateľné používateľom (user_id = null v DB)
    public static final int CATEGORY_SAVING         = 6;  // main to saving transfer
    public static final int CATEGORY_SAVING_EXPENSE = 7;  // saving to main transfer
    public static final int CATEGORY_INVESTMENT = 8;
    public static final int CATEGORY_TRANSFER = 9;
}