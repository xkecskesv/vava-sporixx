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
    private int accountTypeId;
    private String defaultCurrencyCode;
    private double initialBalance;
    private double currentBalance;
    private String description;
    private boolean isActive;
    private LocalDateTime createdAt;

    public boolean isMainAccount()     { return this.accountTypeId == AccountType.MAIN_ACCOUNT; }
    public boolean isEmergencyFund()   { return this.accountTypeId == AccountType.EMERGENCY_FUND; }
    public boolean isSavingAccount()   { return this.accountTypeId == AccountType.SAVING_ACCOUNT; }
}
