package sk.sporixx.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/** DTO pre zobrazenie nastaveného budgetu používateľa.
 * UI ho používa pri obrazovke budgeting.
 */
@Data
@Builder
public class BudgetData {
    // Budget Setup
    private double monthlyIncome;
    private double food;
    private double rent;
    private double transport;
    private double utilities;
    private double other;
    private double essentialExpensesTotal; // food+rent+transport+utilities+other

    // Recommended Allocation: sumy aj percentá
    private double essentialExpenses;
    private double essentialExpensesPercent;
    private double emergencyFund;
    private double emergencyFundPercent;
    private double savings;
    private double savingsPercent;
    private double toInvest;
    private double toInvestPercent;
    private double funMoney;
    private double funMoneyPercent;

    // Emergency Fund
    private double emergencyFundCurrent;
    private double minimalEmergencyFund;
    private double optimalEmergencyFund;
    private Map<String, Double> emergencyFundHistory; // pre graf
}
