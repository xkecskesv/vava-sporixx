package sk.sporixx.dto;

import lombok.Builder;
import lombok.Data;
import sk.sporixx.model.RecurringRule;

import java.util.List;

/**
 * DTO pre Recurring Expenses stĺpcový graf.
 * Každý RecurringRule má spendingClassificationId (want/need).
 */
@Data
@Builder
public class RecurringExpenseData {
    /** Priamo RecurringRule objekty - majú description, amount, spendingClassificationId */
    private List<RecurringRule> items;
    private double totalWant;
    private double totalNeed;
    private double total;
}
