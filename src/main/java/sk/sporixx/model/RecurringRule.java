package sk.sporixx.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Reprezentuje pravidlo opakovanej platby (napr. mesačné predplatné, nájom).
 * Mapuje sa na DB tabuľku 'recurring_rules'.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecurringRule {

    public static final int STATUS_ACTIVE = 1;
    public static final int STATUS_PAUSED_BALANCE = 2;

    private int id;
    private int accountId;
    private int categoryId;
    private int statusId;
    private int transactionTypeId;
    private int spendingClassificationId;
    private double amount;
    private String description;
    private String frequencyType;
    private int frequencyInterval;
    private LocalDateTime startDate;
    private LocalDateTime nextDueDate;
    private LocalDateTime endDate; // nullable
    private Integer maxOccurrences; // nullable
    private int generatedCount;
    private boolean isActive;
    private LocalDateTime createdAt;
}