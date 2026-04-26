package sk.sporixx.repository;

import sk.sporixx.model.SavingGoal;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository rozhranie pre prístup k cieľom sporenia.
 * Mapuje sa na DB tabuľku 'saving_goals'.
 * Implementuje DB kolega.
 */
public interface SavingGoalRepository {

    /**
     * Nájde aktívne sporiace ciele pre jeden konkrétny účet.
     */
    List<SavingGoal> findActiveByAccountId(int accountId);

    /**
     * Nájde všetky aktívne sporiace ciele pre zoznam účtov.
     */
    List<SavingGoal> findActiveByAccountIds(List<Integer> accountIds);

    /**
     * Nájde sporiaci cieľ podľa ID.
     * SQL: SELECT * FROM saving_goals WHERE id = ?
     */
    Optional<SavingGoal> findById(int id);

    /**
     * Uloží nový sporiaci cieľ do databázy.
     */
    SavingGoal save(SavingGoal savingGoal);

    /**
     * Aktualizuje len aktuálnu nasporenú sumu (current_amount) pre daný cieľ.
     */
    void updateCurrentAmount(int goalId, double currentAmount);

    /**
     * Aktualizuje cieľovú sumu (target_amount) pre daný cieľ.
     */
    void updateTargetAmount(int goalId, double targetAmount);

    /**
     * Aktualizuje cieľový dátum (target_date) pre daný cieľ.
     */
    void updateTargetDate(int goalId, LocalDateTime targetDate);
}