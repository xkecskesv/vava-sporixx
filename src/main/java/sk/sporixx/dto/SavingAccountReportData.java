package sk.sporixx.dto;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

/**
 * DTO pre jeden Saving Account v Reports.
 * Obsahuje dáta pre koláčový graf (saved up vs need to save)
 * a Expectation vs Reality líniový graf.
 */
@Data
@Builder
public class SavingAccountReportData {
    private int accountId;
    private String accountName;      // description účtu = goalName
    private double savedUp;          // currentAmount zo SavingGoal
    private double needToSave;       // targetAmount - currentAmount
    private double targetAmount;

    /**
     * Expectation vs Reality graf.
     * Kľúč: "2026"/"2026-01"/"2026-01-01" (rok/rok-mesiac/rok-mesiac-den)
     * Expected: lineárny progress od začiatku k targetDate
     * Actual: skutočný currentAmount v danom mesiaci
     */
    private Map<String, Double> expectedProgress;
    private Map<String, Double> actualProgress;

    /**
     * Určuje ako sú dáta zoskupené v grafoch expectedProgress a actualProgress.
     * "DAY"   — goal kratší ako 3 mesiace → os X zobrazuje dni  ("2026-03-25")
     * "MONTH" — goal 3 mesiace až 2 roky  → os X zobrazuje mesiace ("2026-03")
     * "YEAR"  — goal dlhší ako 2 roky     → os X zobrazuje roky ("2026")
     * UI controller použije túto hodnotu na správne naformátovanie osi X.
     */
    private String progressGrouping;  // DAY/MONTH/YEAR
}
