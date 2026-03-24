package sk.sporixx.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Dátový model pre účet (Main Account, Emergency Fund, Saving Account).
 * Mapuje sa na DB tabuľku 'accounts'.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    private int id;
    private int ownerUserId;
    private int regionId;
    private String accountName;
    private String defaultCurrencyCode;
    private double initialBalance;
    private double currentBalance;
    private boolean isActive;
    private LocalDateTime createdAt;

    // Konštanty pre typy účtov
    public static final String MAIN_ACCOUNT = "Main Account";
    public static final String EMERGENCY_FUND = "Emergency Fund";
    public static final String SAVING_ACCOUNT = "Saving Account";
}
