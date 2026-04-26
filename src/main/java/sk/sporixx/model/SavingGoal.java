package sk.sporixx.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavingGoal {

    private int id;
    private int accountId;
    private String name;
    private int goalTypeId;
    private double targetAmount;   // V UI: 192,000.00
    private double currentAmount;  // Aktualizuje sa pri transakciách
    private LocalDateTime targetDate;
    private boolean isActive;          // V DB je to integer, ale v Jave to mapujeme na boolean
    private LocalDateTime createdAt;
}
