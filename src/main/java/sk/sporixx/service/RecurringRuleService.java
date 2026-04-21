package sk.sporixx.service;

import sk.sporixx.model.RecurringRule;

import java.time.LocalDate;
import java.util.List;

public interface RecurringRuleService {

    List<RecurringRule> getRecurringRules();

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

    void updateRecurringRule(int ruleId,
                             int categoryId,
                             Integer spendingClassificationId,
                             String description,
                             double amount,
                             String frequencyType,
                             int frequencyInterval,
                             Integer maxOccurrences);

    void deleteRecurringRule(int ruleId);

    void processRecurringRules();
}
