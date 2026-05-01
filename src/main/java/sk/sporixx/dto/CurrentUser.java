package sk.sporixx.dto;

import lombok.Builder;
import lombok.Data;
import sk.sporixx.model.Role;

/**
 * DTO pre prihláseného používateľa - bez passwordHash.
 * Toto vidí UI vrstva. Service vrstva pracuje s plným User modelom.
 */
@Data
@Builder
public class CurrentUser {
    private int id;
    private String name;
    private String surname;
    private String email;
    private String gender;
    private String photoPath;  // môže byť null - nie každý má fotku
    private Role role;

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

    /** Helper pre zobrazenie v UI (sidebar, profile header) */
    public String getFullName() {
        return name + " " + surname;
    }

    // UI použije pre checkbox "I am parent"
    public boolean checkisParent() {
        return role == Role.FAMILY_MANAGER;
    }

    public boolean checkhasPhoto() {
        return photoPath != null && !photoPath.isBlank();
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
