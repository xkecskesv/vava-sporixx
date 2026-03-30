package sk.sporixx.repository;

import sk.sporixx.model.SavingGoal;

import java.util.List;
import java.util.Optional;

/**
 * Repository rozhranie pre prístup k cieľom sporenia.
 */
public interface SavingGoalRepository {

    /**
     * Nájde aktívny sporiacy cieľ pre jeden konkrétny účet.
     * V SQL filtruje podľa 'account_id' a 'is_active' = 1.
     */
    Optional<SavingGoal> findActiveByAccountId(int accountId);

    /**
     * Nájde všetky aktívne sporiace ciele pre zoznam účtov.
     * Ideálne pre použitie v Overview, kde potrebujeme načítať ciele pre všetky
     * účty používateľa jedným dopytom.
     */
    List<SavingGoal> findActiveByAccountIds(List<Integer> accountIds);

    Optional<SavingGoal> findById(int id);

    /**
     * Uloží nový sporiaci cieľ do databázy alebo aktualizuje existujúci
     * (ak už má pridelené ID).
     */
    SavingGoal save(SavingGoal savingGoal);

    /**
     * Aktualizuje len aktuálnu nasporenú sumu (current_amount) pre daný cieľ.
     * Volá sa typicky po pridaní novej transakcie na daný sporiaci účet.
     */
    void updateCurrentAmount(int goalId, double currentAmount);
}
