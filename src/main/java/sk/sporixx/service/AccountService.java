package sk.sporixx.service;

import sk.sporixx.model.Account;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface AccountService {

    /**
     * Vytvorí Private účet (len description + amount).
     */
    Account createPrivateAccount(String description, double initialAmount);

    /**
     * Vytvorí Saving účet + k nemu SavingGoal.
     * @param description popis účtu
     * @param initialAmount počiatočný zostatok
     * @param targetAmount cieľová suma
     * @param targetDate dátum do kedy chce nasporiť
     */
    Account createSavingAccount(String description, double initialAmount, double targetAmount,
                                LocalDate targetDate);

    /**
     * Vytvorí saving účet s konkrétnym createdAt — používa sa pri importe XML.
     * @param createdAt pôvodný dátum vytvorenia účtu
     */
    Account createSavingAccountFromImport(String description, double initialAmount,
                                          double targetAmount, LocalDate targetDate,
                                          LocalDateTime createdAt);

    /**
     * Deaktivuje účet (soft delete).
     * Main Account a Emergency Fund sa nedajú deaktivovať.
     */
    void deleteAccount(int accountId);

    /**
     * Aktualizuje popis účtu.
     */
    void updateAccountDescription(int accountId, String description);
}
