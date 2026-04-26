package sk.sporixx.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Datový model pre finančné skóre používateľa.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialScore {

    private int id;
    private int userId;
    private double scoreValue;
    private LocalDateTime calculatedAt;
    private LocalDate periodStart;
    private LocalDate periodEnd;
}
