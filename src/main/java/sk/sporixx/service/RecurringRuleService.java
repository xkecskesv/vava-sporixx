package sk.sporixx.service;

import sk.sporixx.model.RecurringRule;

import java.time.LocalDate;
import java.util.List;

public interface RecurringRuleService {

    /**
     * Načíta všetky aktívne recurring rules prihláseného používateľa.
     */
    List<RecurringRule> getRecurringRules();

    /**
     * Pridá nové pravidlo opakovanej platby.
     * Nastaví nextDueDate = startDate.
     */
    RecurringRule addRecurringRule(int accountId,
                                   int categoryId,
                                   int transactionTypeId,
                                   Integer spendingClassificationId,
                                   String description,
                                   double amount,
                                   String frequencyType,
                                   int frequencyInterval,
                                   LocalDate startDate,
                                   Integer maxOccurrences);

    /**
     * Aktualizuje existujúce pravidlo.
     */
    void updateRecurringRule(int ruleId,
                             int categoryId,
                             Integer spendingClassificationId,
                             String description,
                             double amount,
                             String frequencyType,
                             int frequencyInterval,
                             Integer maxOccurrences);

    /**
     * Deaktivuje pravidlo (soft delete).
     */
    void deleteRecurringRule(int ruleId);

    /**
     * Skontroluje všetky aktívne pravidlá a vytvorí transakcie
     * pre tie kde nextDueDate <= dnes.
     * Volá sa pri štarte aplikácie.
     */
    void processRecurringRules();
}
