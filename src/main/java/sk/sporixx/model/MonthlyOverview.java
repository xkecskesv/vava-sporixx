package sk.sporixx.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Pomocný datový objekt pre mesačný prehľad príjmov a výdavkov.
 * Príklad:
 *   month = "2026-03"
 *   totalIncome = 2500.00
 *   totalExpense = 1800.00
 *   netCashflow = 700.00
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyOverview {

    private String month;          // formát "YYYY-MM" (napr. "2026-03")
    private double totalIncome;
    private double totalExpense;

    /**
     * Vypočíta čistý cashflow (príjem - výdavky).
     */
    public double getNetCashflow() {
        return totalIncome - totalExpense;
    }

    /**
     * Kontroluje, či bol mesiac ziskový (príjmy > výdavky).
     */
    public boolean isProfitable() {
        return totalIncome > totalExpense;
    }
}