package sk.sporixx.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Datový model pre používateľa.
 * Mapuje sa na DB tabuľku 'users'.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private int id;
    private String email;
    private String passwordHash;
    private String firstName;
    private String lastName;
    private String photoPath; // nullable
    private String gender;
    private LocalDateTime createdAt;

    @Builder.Default
    private boolean isActive = true;

    @Builder.Default
    private Role role = Role.USER;

    @Builder.Default
    private String languageCode = "en";
    @Builder.Default
    private String currencyCode = "EUR";

    // XP polia
    @Builder.Default
    private double savingXp = 0.0;
    @Builder.Default
    private double budgetXp = 0.0;
    @Builder.Default
    private double investorXp = 0.0;
    @Builder.Default
    private double spenderXp = 0.0;

    // Level polia
    @Builder.Default
    private int savingLevel = 0;
    @Builder.Default
    private int budgetLevel = 0;
    @Builder.Default
    private int investorLevel = 0;
    @Builder.Default
    private int spenderLevel = 0;

    public boolean hasRole(Role expectedRole) {
        return this.role == expectedRole;
    }

    public double getTotalXp() {
        return savingXp + budgetXp + investorXp + spenderXp;
    }

    public double getXpProgress() {
        return Math.min(getTotalXp() / 200.0, 1.0);
    }

    public String getFinancialLevelKey() {
        double total = getTotalXp();
        if (total >= 200) return "milestone.financial_level.pro";
        if (total >= 150) return "milestone.financial_level.expert";
        if (total >= 100) return "milestone.financial_level.skilled";
        if (total >= 50)  return "milestone.financial_level.aware";
        return "milestone.financial_level.started";
    }
}