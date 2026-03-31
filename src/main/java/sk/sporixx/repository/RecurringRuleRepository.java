package sk.sporixx.repository;

import sk.sporixx.model.RecurringRule;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository rozhranie pre prístup k opakovaným platbám.
 * Optimalizované pre dopytovanie podľa jedného konkrétneho účtu.
 */
public interface RecurringRuleRepository {

    /**
     * Nájde všetky aktívne opakované platby pre konkrétny účet.
     * V SQL filtruje podľa 'account_id' a 'is_active' = 1.
     */
    List<RecurringRule> findActiveByAccountId(int accountId);

    /**
     * Nájde blížiace sa platby pre daný účet (pre Activities panel).
     * Zoradené podľa next_due_date vzostupne.
     */
    List<RecurringRule> findUpcomingByAccountId(int accountId, LocalDateTime now, int limit);

    /**
     * Uloží nové pravidlo pre opakovanú platbu alebo aktualizuje existujúce.
     */
    RecurringRule save(RecurringRule rule);

    /**
     * Aktualizuje dátum nasledujúcej splatnosti a inkrementuje počítadlo
     * vygenerovaných transakcií. Volá sa po úspešnom automatickom
     * vytvorení transakcie z tohto pravidla.
     */
    void updateNextDueDate(int ruleId, LocalDateTime nextDueDate, int generatedCount);
}
