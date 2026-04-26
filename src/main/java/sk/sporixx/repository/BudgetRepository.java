package sk.sporixx.repository;

import sk.sporixx.model.Budget;
import java.util.Optional;

/**
 * Repository rozhranie pre prístup k budget nastaveniam používateľa.
 * Každý používateľ má max 1 aktívny budget záznam.
 */
public interface BudgetRepository {

    /**
     * Nájde aktívny budget pre daného používateľa.
     * Vracia Optional — používateľ nemusí mať budget nastavený.
     */
    Optional<Budget> findByUserId(int userId);

    /**
     * Uloží nový budget záznam.
     * Volá sa keď používateľ prvýkrát nastaví budget.
     */
    Budget save(Budget budget);

    /**
     * Aktualizuje existujúci budget záznam.
     * Volá sa pri každej zmene Budget Setup alebo Custom Allocation.
     */
    void update(Budget budget);
}