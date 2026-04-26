package sk.sporixx.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * DTO pre Income vs Expenses líniový graf.
 */
@Data
@Builder
public class IncomeExpenseData {
    /** Kľúč: "2026-01" — mesiac. Hodnota: [income, expense] */
    private Map<String, Double> monthlyIncome;
    private Map<String, Double> monthlyExpense;
    private double totalIncome;
    private double totalExpense;
}
