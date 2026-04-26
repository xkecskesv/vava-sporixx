package sk.sporixx.service;

import sk.sporixx.model.Account;
import sk.sporixx.model.SavingGoal;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

public interface AccountService {

    /**
     * Vytvorí Private účet (len description + amount).
     */
    void createPrivateAccount(String description, double initialAmount);

    /**
     * Vytvorí Saving účet + k nemu SavingGoal.
     *
     * @param description   popis účtu
     * @param initialAmount počiatočný zostatok
     * @param targetAmount  cieľová suma
     * @param targetDate    dátum do kedy chce nasporiť
     */
    void createSavingAccount(String description, double initialAmount, double targetAmount, LocalDate targetDate);

    /**
     * Vytvorí saving účet s konkrétnym createdAt - používa sa pri importe XML.
     * @param createdAt pôvodný dátum vytvorenia účtu
     */
    Account createSavingAccountFromImport(String description, double initialAmount, double targetAmount, LocalDate targetDate, LocalDateTime createdAt);

    /**
     * Deaktivuje účet (soft delete).
     * Main Account a Emergency Fund sa nedajú deaktivovať.
     */
    void deleteAccount(int accountId);

    /**
     * Aktualizuje popis účtu.
     */
    void updateAccountDescription(int accountId, String description);

    /**
     * Aktualizuje saving účet a jeho goal.
     */
    void updateSavingAccount(int accountId, String description, double targetAmount, LocalDate targetDate);

    /**
     * Načíta aktívny SavingGoal pre daný účet.
     * UI použije pre predvyplnenie formulára pri editácii.
     */
    Optional<SavingGoal> getSavingGoal(int accountId);
}
