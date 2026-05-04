package sk.sporixx.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Dátový model, ktorý reprezentuje jeden saving goal na saving účte.
 * Mapuje sa na DB tabuľku 'saving_goals'.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavingGoal {

    private int id;
    private int accountId;
    private String name;
    private int goalTypeId;
    private double targetAmount;
    private double currentAmount; // Aktualizuje sa pri transakciách
    private LocalDateTime targetDate;
    private boolean isActive; // V DB je to integer, ale v Jave to mapujeme na boolean
    private LocalDateTime createdAt;
}
