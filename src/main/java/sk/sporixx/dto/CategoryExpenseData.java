package sk.sporixx.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * DTO pre Expenses by Category koláčový graf.
 */
@Data
@Builder
public class CategoryExpenseData {
    /** Kľúč: názov kategórie, hodnota: suma */
    private Map<String, Double> expenseByCategory;

    private double totalExpense;
}