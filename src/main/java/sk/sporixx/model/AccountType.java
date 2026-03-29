package sk.sporixx.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Reprezentuje typ účtu (napr. Main Account, Emergency Fund, Saving Account).
 * Mapuje sa na tabuľku "account_types" v databáze.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountType {

    private int id;
    private String name;
    private String description;
    private LocalDateTime createdAt;

    // Konštanty pre typy účtov
    public static final int MAIN_ACCOUNT = 1;
    public static final int EMERGENCY_FUND = 2;
    public static final int SAVING_ACCOUNT = 3;
}
