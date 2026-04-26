package sk.sporixx.service;

import sk.sporixx.model.RecurringRule;
import sk.sporixx.model.Transaction;
import sk.sporixx.repository.RecurringRuleRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RecurringRuleServiceImpl implements RecurringRuleService {

    private static final Logger logger =
            LoggerFactory.getLogger(RecurringRuleServiceImpl.class);

    private final RecurringRuleRepository recurringRuleRepository;
    private final TransactionService transactionService;

    public RecurringRuleServiceImpl(RecurringRuleRepository recurringRuleRepository,
                                    TransactionService transactionService) {
        this.recurringRuleRepository = recurringRuleRepository;
        this.transactionService = transactionService;
    }

    @Override
    public List<RecurringRule> getRecurringRules() {
        logger.info("Loading recurring rules for current user");
        try {
            List<Integer> accountIds = SessionManager.getInstance().getAccountIds();
            List<RecurringRule> all = new ArrayList<>();
            for (int accountId : accountIds) {
                all.addAll(recurringRuleRepository.findActiveByAccountId(accountId));
            }
            all.sort(Comparator.comparing(RecurringRule::getNextDueDate));
            return all;
        } catch (Exception e) {
            logger.error("Failed to load recurring rules", e);
            throw new RecurringRuleException("error.db_error", e);
        }
    }

    @Override
    public RecurringRule addRecurringRule(int accountId,
                                          int categoryId,
                                          Integer spendingClassificationId,
                                          String description,
                                          double amount,
                                          String frequencyType,
                                          int frequencyInterval,
                                          LocalDate startDate,
                                          LocalDate endDate) {
        logger.info("Adding recurring rule: accountId={}, description={}, amount={}",
                accountId, description, amount);

        if (amount <= 0) {
            throw new RecurringRuleException("recurring.error.invalid_amount");
        }
        if (description == null || description.isBlank()) {
            throw new RecurringRuleException("recurring.error.description_required");
        }
        if (startDate == null) {
            throw new RecurringRuleException("recurring.error.start_date_required");
        }
        if (frequencyInterval <= 0) {
            throw new RecurringRuleException("recurring.error.invalid_interval");
        }
        if (SessionManager.getInstance().getAccountById(accountId) == null) {
            throw new RecurringRuleException("recurring.error.account_not_found");
        }

        try {
            LocalDateTime startDateTime = startDate.atStartOfDay();

            RecurringRule rule = RecurringRule.builder()
                    .accountId(accountId)
                    .categoryId(categoryId)
                    .transactionTypeId(Transaction.TYPE_EXPENSE)
                    .spendingClassificationId(spendingClassificationId != null
                            ? spendingClassificationId : 0)
                    .description(description)
                    .amount(amount)
                    .frequencyType(frequencyType)
                    .frequencyInterval(frequencyInterval)
                    .startDate(startDateTime)
                    .nextDueDate(startDateTime)
                    .endDate(endDate != null ? endDate.atStartOfDay() : null)
                    .maxOccurrences(null)
                    .generatedCount(0)
                    .statusId(1)
                    .isActive(true)
                    .createdAt(LocalDateTime.now())
                    .build();

            RecurringRule saved = recurringRuleRepository.save(rule);
            logger.info("Recurring rule created: id={}", saved.getId());
            return saved;

        } catch (RecurringRuleException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to add recurring rule", e);
            throw new RecurringRuleException("error.db_error", e);
        }
    }

    @Override
    public void updateRecurringRule(int ruleId,
                             int categoryId,
                             Integer spendingClassificationId,
                             String description,
                             double amount,
                             String frequencyType,
                             int frequencyInterval,
                             LocalDate endDate) {
        logger.info("Updating recurring rule id={}", ruleId);

        if (amount <= 0) {
            throw new RecurringRuleException("recurring.error.invalid_amount");
        }
        if (description == null || description.isBlank()) {
            throw new RecurringRuleException("recurring.error.description_required");
        }
        if (frequencyInterval <= 0) {
            throw new RecurringRuleException("recurring.error.invalid_interval");
        }

        RecurringRule rule = recurringRuleRepository.findById(ruleId)
                .orElseThrow(() -> new RecurringRuleException("recurring.error.not_found"));

        if (SessionManager.getInstance().getAccountById(rule.getAccountId()) == null) {
            throw new RecurringRuleException("recurring.error.not_found");
        }

        try {
            rule.setCategoryId(categoryId);
            rule.setSpendingClassificationId(spendingClassificationId != null
                    ? spendingClassificationId : 0);
            rule.setDescription(description);
            rule.setAmount(amount);
            rule.setFrequencyType(frequencyType);
            rule.setFrequencyInterval(frequencyInterval);
            rule.setEndDate(endDate != null ? endDate.atStartOfDay() : null);
            rule.setMaxOccurrences(null);

            recurringRuleRepository.save(rule);
            logger.info("Recurring rule updated: id={}", ruleId);

        } catch (RecurringRuleException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to update recurring rule id={}", ruleId, e);
            throw new RecurringRuleException("error.db_error", e);
        }
    }

    @Override
    public void deleteRecurringRule(int ruleId) {
        logger.info("Deactivating recurring rule id={}", ruleId);

        RecurringRule rule = recurringRuleRepository.findById(ruleId)
                .orElseThrow(() -> new RecurringRuleException("recurring.error.not_found"));

        if (SessionManager.getInstance().getAccountById(rule.getAccountId()) == null) {
            throw new RecurringRuleException("recurring.error.not_found");
        }

        try {
            recurringRuleRepository.deactivateById(ruleId);
            logger.info("Recurring rule deactivated: id={}", ruleId);

        } catch (RecurringRuleException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to deactivate recurring rule id={}", ruleId, e);
            throw new RecurringRuleException("error.db_error", e);
        }
    }

    @Override
    public void processRecurringRules() {
        logger.info("Processing recurring rules");

        List<Integer> accountIds = SessionManager.getInstance().getAccountIds();
        LocalDateTime now = LocalDateTime.now();

        for (int accountId : accountIds) {
            List<RecurringRule> rules =
                    recurringRuleRepository.findActiveByAccountId(accountId);

            for (RecurringRule rule : rules) {
                if (rule.getNextDueDate() == null) continue;

                // všetky zmešklané platby v loop
                while (!rule.getNextDueDate().isAfter(now)) {

                    // kontrola endDate
                    if (rule.getEndDate() != null
                            && rule.getNextDueDate().isAfter(rule.getEndDate())) {
                        recurringRuleRepository.deactivateById(rule.getId());
                        logger.info("Recurring rule id={} reached end date, deactivated",
                                rule.getId());
                        break;
                    }

                    try {
                        transactionService.addTransaction(
                                rule.getAccountId(),
                                rule.getTransactionTypeId(),
                                null,
                                rule.getCategoryId(),
                                rule.getSpendingClassificationId() != 0
                                        ? rule.getSpendingClassificationId() : null,
                                rule.getDescription(),
                                rule.getAmount(),
                                SessionManager.getInstance()
                                        .getAccountById(rule.getAccountId())
                                        .getDefaultCurrencyCode(),
                                rule.getNextDueDate().toLocalDate());

                        LocalDateTime nextDueDate = calculateNextDueDate(rule);
                        int newCount = rule.getGeneratedCount() + 1;
                        recurringRuleRepository.updateNextDueDate(rule.getId(), nextDueDate, newCount);
                        rule.setNextDueDate(nextDueDate);
                        rule.setGeneratedCount(newCount);

                        logger.info("Recurring rule id={} processed, nextDueDate={}",
                                rule.getId(), nextDueDate);

                    } catch (Exception e) {
                        logger.error("Failed to process recurring rule id={}", rule.getId(), e);
                        break;
                    }
                }
            }
        }
    }

    private LocalDateTime calculateNextDueDate(RecurringRule rule) {
        LocalDateTime current = rule.getNextDueDate();
        return switch (rule.getFrequencyType().toUpperCase()) {
            case "DAILY"   -> current.plusDays(rule.getFrequencyInterval());
            case "WEEKLY"  -> current.plusWeeks(rule.getFrequencyInterval());
            case "MONTHLY" -> current.plusMonths(rule.getFrequencyInterval());
            case "YEARLY"  -> current.plusYears(rule.getFrequencyInterval());
            default -> {
                logger.warn("Unknown frequencyType: {}, defaulting to MONTHLY",
                        rule.getFrequencyType());
                yield current.plusMonths(rule.getFrequencyInterval());
            }
        };
    }
}
