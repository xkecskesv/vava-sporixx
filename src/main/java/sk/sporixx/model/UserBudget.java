package sk.sporixx.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Datový model pre používateľský rozpočet (aplikovaná šablóna).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBudget {

    private int id;
    private int accountId;
    private int budgetTemplateId;
    private String periodType;   // "monthly", "weekly"
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean active;
    private LocalDateTime createdAt;

    // Transient
    private String templateName;
}