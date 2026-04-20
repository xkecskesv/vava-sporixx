package sk.sporixx.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Model pre budget nastavenie používateľa.
 * Mapuje sa na tabuľku 'user_budgets'.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Budget {

    private int id;
    private int userId;

    // Budget Setup
    private double monthlyIncome;
    private double food;
    private double rent;
    private double transport;
    private double utilities;
    private double other;

    // Recommended / Custom Allocation
    private double essentialExpenses;
    private double emergencyFund;
    private double savings;
    private double toInvest;
    private double funMoney;

    // Emergency Fund limits
    private double minimalEmergencyFund;
    private double optimalEmergencyFund;

    private boolean isActive;
    private LocalDateTime createdAt;
}
