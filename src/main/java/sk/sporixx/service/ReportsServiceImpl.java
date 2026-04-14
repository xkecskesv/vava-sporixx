package sk.sporixx.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sk.sporixx.dto.*;
import sk.sporixx.model.Account;
import sk.sporixx.model.RecurringRule;
import sk.sporixx.model.SavingGoal;
import sk.sporixx.model.Transaction;
import sk.sporixx.repository.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementácia ReportsService.
 * Všetky dáta sú načítavané naprieč všetkými účtami používateľa.
 * Prevody medzi účtami (TYPE_SAVING, TYPE_SAVING_EXPENSE) sú vynechané
 * z income/expense/category/want-need reportov.
 */
public class ReportsServiceImpl implements ReportsService {

    private static final Logger logger = LoggerFactory.getLogger(ReportsServiceImpl.class);

    /**
     * Hranice pre adaptívne zobrazenie grafu Expectation vs Reality.
     * Grouping sa určuje podľa celkovej dĺžky goalu (createdAt → targetDate).
     * Rovnaký grouping sa použije pre obe krivky (expected aj actual)
     * aby boli na rovnakej osi X a dali sa porovnať.
     * DAY   — goal kratší ako 3 mesiace (< 90 dní)
     * MONTH — goal 3 mesiace až 5 rokov (90 — 1825 dní)
     * YEAR  — goal dlhší ako 5 rokov (> 1825 dní)
     */
    private static final int DAY_THRESHOLD_DAYS = 90;
    private static final int MONTH_THRESHOLD_DAYS = 1825;

    private static final DateTimeFormatter DAY_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter YEAR_FORMAT =
            DateTimeFormatter.ofPattern("yyyy");

    private final TransactionRepository transactionRepository;
    private final RecurringRuleRepository recurringRuleRepository;
    private final SavingGoalRepository savingGoalRepository;

    public ReportsServiceImpl(TransactionRepository transactionRepository,
                              RecurringRuleRepository recurringRuleRepository,
                              SavingGoalRepository savingGoalRepository) {
        this.transactionRepository = transactionRepository;
        this.recurringRuleRepository = recurringRuleRepository;
        this.savingGoalRepository = savingGoalRepository;
    }

    // Helper - získa všetky accountIds prihláseného používateľa
    private List<Integer> getAccountIds() {
        return SessionManager.getInstance().getAccountIds();
    }

    // Helper - vypočíta from datetime podľa ChartPeriod
    private LocalDateTime calculateFrom(ChartPeriod period) {
        return period.calculateStartDate().atStartOfDay();
    }


    //  INCOME VS EXPENSES
    @Override
    public IncomeExpenseData loadIncomeExpenseData(ChartPeriod period) {
        logger.info("Loading income/expense data for period: {}", period);

        try {
            LocalDateTime from = calculateFrom(period);
            logger.info("Calculating from date: {}", from);
            List<Integer> accountIds = getAccountIds();

            Map<String, Double> periodIncome = new TreeMap<>();
            Map<String, Double> periodExpense = new TreeMap<>();

            for (int accountId : accountIds) {
                if (period.isGroupByDay()) {
                    transactionRepository.sumByTypeAndDay(
                                    accountId, Transaction.TYPE_INCOME, from)
                            .forEach((day, sum) ->
                                    periodIncome.merge(day, sum, Double::sum));
                    transactionRepository.sumByTypeAndDay(
                                    accountId, Transaction.TYPE_EXPENSE, from)
                            .forEach((day, sum) ->
                                    periodExpense.merge(day, sum, Double::sum));
                } else {
                    transactionRepository.sumByTypeAndMonth(
                                    accountId, Transaction.TYPE_INCOME, from)
                            .forEach((month, sum) ->
                                    periodIncome.merge(month, sum, Double::sum));
                    transactionRepository.sumByTypeAndMonth(
                                    accountId, Transaction.TYPE_EXPENSE, from)
                            .forEach((month, sum) ->
                                    periodExpense.merge(month, sum, Double::sum));
                }
            }

            double totalIncome = periodIncome.values().stream()
                    .mapToDouble(Double::doubleValue).sum();
            double totalExpense = periodExpense.values().stream()
                    .mapToDouble(Double::doubleValue).sum();

            return IncomeExpenseData.builder()
                    .monthlyIncome(periodIncome)
                    .monthlyExpense(periodExpense)
                    .totalIncome(totalIncome)
                    .totalExpense(totalExpense)
                    .build();

        } catch (Exception e) {
            logger.error("Error loading income/expense data", e);
            throw new ServiceException("error.db_error", e);
        }
    }

    //  EXPENSES BY CATEGORY
    @Override
    public CategoryExpenseData loadCategoryExpenseData(ChartPeriod period) {
        logger.info("Loading category expense data for period: {}", period);

        try {
            LocalDateTime from = calculateFrom(period);
            List<Integer> accountIds = getAccountIds();

            Map<String, Double> expenseByCategory = new TreeMap<>();

            for (int accountId : accountIds) {
                transactionRepository.sumByCategoryAndDateRange(
                                accountId, from, LocalDateTime.now())
                        .forEach((category, sum) ->
                                expenseByCategory.merge(category, sum, Double::sum));
            }

            double totalExpense = expenseByCategory.values().stream()
                    .mapToDouble(Double::doubleValue).sum();

            return CategoryExpenseData.builder()
                    .expenseByCategory(expenseByCategory)
                    .totalExpense(totalExpense)
                    .build();

        } catch (Exception e) {
            logger.error("Error loading category expense data", e);
            throw new ServiceException("error.db_error", e);
        }
    }

    //  RECURRING EXPENSES
    @Override
    public RecurringExpenseData loadRecurringExpenseData() {
        logger.info("Loading recurring expense data");

        try {
            List<Integer> accountIds = getAccountIds();

            List<RecurringRule> allRules = new ArrayList<>();
            for (int accountId : accountIds) {
                allRules.addAll(
                        recurringRuleRepository.findActiveByAccountId(accountId));
            }

            double totalWant = allRules.stream()
                    .filter(r -> r.getSpendingClassificationId()
                            == Transaction.CLASSIFICATION_WANT)
                    .mapToDouble(RecurringRule::getAmount)
                    .sum();

            double totalNeed = allRules.stream()
                    .filter(r -> r.getSpendingClassificationId()
                            == Transaction.CLASSIFICATION_NEED)
                    .mapToDouble(RecurringRule::getAmount)
                    .sum();

            return RecurringExpenseData.builder()
                    .items(allRules)
                    .totalWant(totalWant)
                    .totalNeed(totalNeed)
                    .total(totalWant + totalNeed)
                    .build();

        } catch (Exception e) {
            logger.error("Error loading recurring expense data", e);
            throw new ServiceException("error.db_error", e);
        }
    }

    //  WANT VS NEED
    @Override
    public WantNeedData loadWantNeedData(ChartPeriod period) {
        logger.info("Loading want/need data for period: {}", period);

        try {
            LocalDateTime from = calculateFrom(period);
            List<Integer> accountIds = getAccountIds();

            double totalWant = 0;
            double totalNeed = 0;

            for (int accountId : accountIds) {
                Map<String, Double> result = transactionRepository
                        .sumByClassificationAndDateRange(
                                accountId, from, LocalDateTime.now());

                totalWant += result.getOrDefault("WANT", 0.0);
                totalNeed += result.getOrDefault("NEED", 0.0);
            }

            double total = totalWant + totalNeed;
            double wantPercentage = total > 0 ? (totalWant / total) * 100 : 0;
            double needPercentage = total > 0 ? (totalNeed / total) * 100 : 0;

            return WantNeedData.builder()
                    .totalWant(totalWant)
                    .totalNeed(totalNeed)
                    .wantPercentage(wantPercentage)
                    .needPercentage(needPercentage)
                    .build();

        } catch (Exception e) {
            logger.error("Error loading want/need data", e);
            throw new ServiceException("error.db_error", e);
        }
    }

    //  SAVING ACCOUNTS
    @Override
    public List<SavingAccountReportData> loadSavingAccountsData() {
        logger.info("Loading saving accounts report data");

        try {
            List<Account> savingAccounts = SessionManager.getInstance().getAccounts()
                    .stream()
                    .filter(Account::isSavingAccount)
                    .toList();

            if (savingAccounts.isEmpty()) {
                return Collections.emptyList();
            }

            List<Integer> savingAccountIds = savingAccounts.stream()
                    .map(Account::getId)
                    .collect(Collectors.toList());

            Map<Integer, SavingGoal> goalByAccountId = savingGoalRepository
                    .findActiveByAccountIds(savingAccountIds).stream()
                    .collect(Collectors.toMap(SavingGoal::getAccountId, g -> g));

            List<SavingAccountReportData> result = new ArrayList<>();

            for (Account account : savingAccounts) {
                SavingGoal goal = goalByAccountId.get(account.getId());
                if (goal == null) {
                    logger.warn("Saving account {} has no active goal, skipping",
                            account.getId());
                    continue;
                }

                double savedUp = goal.getCurrentAmount();
                double targetAmount = goal.getTargetAmount();
                double needToSave = Math.max(0, targetAmount - savedUp);

                long totalDays = ChronoUnit.DAYS.between(
                        goal.getCreatedAt().toLocalDate(),
                        goal.getTargetDate().toLocalDate());
                String grouping = resolveGrouping(totalDays);

                // Načítaj TYPE_INCOME transakcie pre saving účet od createdAt
                List<Transaction> transactions = transactionRepository
                        .findByAccountIdAndDateRange(
                                account.getId(),
                                goal.getCreatedAt(),
                                LocalDateTime.now())
                        .stream()
                        .filter(t -> t.getTransactionTypeId() == Transaction.TYPE_INCOME)
                        .toList();

                result.add(SavingAccountReportData.builder()
                        .accountId(account.getId())
                        .accountName(account.getDescription())
                        .savedUp(savedUp)
                        .needToSave(needToSave)
                        .targetAmount(targetAmount)
                        .targetDate(goal.getTargetDate())
                        .initialBalance(account.getInitialBalance())
                        .createdAt(goal.getCreatedAt())
                        .expectedProgress(calculateExpectedProgress(goal, totalDays, grouping))
                        .actualProgress(calculateActualProgress(account.getId(), goal, grouping))
                        .progressGrouping(grouping)
                        .transactions(transactions)
                        .build());
            }

            return result;

        } catch (Exception e) {
            logger.error("Error loading saving accounts data", e);
            throw new ServiceException("error.db_error", e);
        }
    }

    //  HELPERS — GROUPING
    /**
     * Určí grouping podľa celkovej dĺžky goalu.
     * Rovnaký grouping sa použije pre expected aj actual krivku
     * aby boli porovnateľné na rovnakej osi X.
     */
    private String resolveGrouping(long totalDays) {
        if (totalDays <= DAY_THRESHOLD_DAYS) return "DAY";
        if (totalDays <= MONTH_THRESHOLD_DAYS) return "MONTH";
        return "YEAR";
    }

    //  HELPERS — EXPECTED PROGRESS
    /**
     * Vypočíta lineárny expected progress od createdAt po targetDate.
     * Krivka je statická — zobrazuje celý plán sporenia.
     */
    private Map<String, Double> calculateExpectedProgress(SavingGoal goal,
                                                          long totalDays,
                                                          String grouping) {
        if ("DAY".equals(grouping)) return calculateExpectedByDay(goal, totalDays);
        if ("MONTH".equals(grouping)) return calculateExpectedByMonth(goal);
        return calculateExpectedByYear(goal);
    }

    private Map<String, Double> calculateExpectedByDay(SavingGoal goal, long totalDays) {
        Map<String, Double> expected = new TreeMap<>();
        if (totalDays <= 0) return expected;

        double initialBalance = getInitialBalance(goal.getAccountId());
        double remainingToSave = goal.getTargetAmount() - initialBalance;

        // Goal už splnený
        if (remainingToSave <= 0) {
            expected.put(goal.getCreatedAt().format(DAY_FORMAT), goal.getTargetAmount());
            return expected;
        }

        double step = remainingToSave / totalDays;
        LocalDateTime current = goal.getCreatedAt();
        double cumulative = initialBalance;

        while (!current.isAfter(goal.getTargetDate())) {
            cumulative += step;
            expected.put(current.format(DAY_FORMAT),
                    Math.min(cumulative, goal.getTargetAmount()));
            current = current.plusDays(1);
        }
        return expected;
    }

    private Map<String, Double> calculateExpectedByMonth(SavingGoal goal) {
        Map<String, Double> expected = new TreeMap<>();

        long totalMonths = ChronoUnit.MONTHS.between(
                goal.getCreatedAt().toLocalDate().withDayOfMonth(1),
                goal.getTargetDate().toLocalDate().withDayOfMonth(1));
        if (totalMonths <= 0) return expected;

        double initialBalance = getInitialBalance(goal.getAccountId());
        double remainingToSave = goal.getTargetAmount() - initialBalance;

        // Goal už splnený
        if (remainingToSave <= 0) {
            expected.put(goal.getCreatedAt().format(MONTH_FORMAT), goal.getTargetAmount());
            return expected;
        }

        double step = remainingToSave / totalMonths;
        LocalDateTime current = goal.getCreatedAt().withDayOfMonth(1);
        double cumulative = initialBalance;

        while (!current.isAfter(goal.getTargetDate())) {
            cumulative += step;
            expected.put(current.format(MONTH_FORMAT),
                    Math.min(cumulative, goal.getTargetAmount()));
            current = current.plusMonths(1);
        }
        return expected;
    }

    private Map<String, Double> calculateExpectedByYear(SavingGoal goal) {
        Map<String, Double> expected = new TreeMap<>();

        long totalYears = ChronoUnit.YEARS.between(
                goal.getCreatedAt().toLocalDate(),
                goal.getTargetDate().toLocalDate());
        if (totalYears <= 0) return expected;

        double initialBalance = getInitialBalance(goal.getAccountId());
        double remainingToSave = goal.getTargetAmount() - initialBalance;

        // Goal už splnený
        if (remainingToSave <= 0) {
            expected.put(goal.getCreatedAt().format(YEAR_FORMAT), goal.getTargetAmount());
            return expected;
        }

        double step = remainingToSave / totalYears;
        LocalDateTime current = goal.getCreatedAt();
        double cumulative = initialBalance;

        while (!current.isAfter(goal.getTargetDate())) {
            cumulative += step;
            expected.put(current.format(YEAR_FORMAT),
                    Math.min(cumulative, goal.getTargetAmount()));
            current = current.plusYears(1);
        }
        return expected;
    }

    //  HELPERS — ACTUAL PROGRESS
    /**
     * Vypočíta skutočný kumulatívny progress podľa TYPE_INCOME transakcií.
     * Krivka rastie postupne s pribúdajúcimi transakciami.
     * Rovnaký grouping ako expected — obe krivky na rovnakej osi X.
     */
    private Map<String, Double> calculateActualProgress(int accountId,
                                                        SavingGoal goal,
                                                        String grouping) {
        try {
            if ("DAY".equals(grouping)) return calculateActualByDay(accountId, goal);
            if ("MONTH".equals(grouping)) return calculateActualByMonth(accountId, goal);
            return calculateActualByYear(accountId, goal);
        } catch (Exception e) {
            logger.warn("Could not load actual progress for account {}", accountId);
            return Collections.emptyMap();
        }
    }

    private Map<String, Double> calculateActualByDay(int accountId, SavingGoal goal) {
        Map<String, Double> raw = new TreeMap<>(
                transactionRepository.sumByTypeAndDay(
                        accountId, Transaction.TYPE_INCOME, goal.getCreatedAt()));

        double initialBalance = getInitialBalance(accountId);

        if (raw.isEmpty() && initialBalance > 0) {
            raw.put(goal.getCreatedAt().format(DAY_FORMAT), 0.0);
        }

        return toCumulative(raw, initialBalance);
    }

    private Map<String, Double> calculateActualByMonth(int accountId, SavingGoal goal) {
        Map<String, Double> raw = new TreeMap<>(
                transactionRepository.sumByTypeAndMonth(
                        accountId, Transaction.TYPE_INCOME, goal.getCreatedAt()));

        double initialBalance = getInitialBalance(accountId);

        if (raw.isEmpty() && initialBalance > 0) {
            raw.put(goal.getCreatedAt().format(MONTH_FORMAT), 0.0);
        }

        return toCumulative(raw, initialBalance);
    }

    private Map<String, Double> calculateActualByYear(int accountId, SavingGoal goal) {
        Map<String, Double> monthly = transactionRepository.sumByTypeAndMonth(
                accountId, Transaction.TYPE_INCOME, goal.getCreatedAt());

        Map<String, Double> yearly = new TreeMap<>();
        monthly.forEach((month, sum) -> {
            String year = month.substring(0, 4);
            yearly.merge(year, sum, Double::sum);
        });

        double initialBalance = getInitialBalance(accountId);

        if (yearly.isEmpty() && initialBalance > 0) {
            yearly.put(goal.getCreatedAt().format(YEAR_FORMAT), 0.0);
        }

        return toCumulative(yearly, initialBalance);
    }

    /**
     * Načíta initialBalance saving účtu zo SessionManager.
     * Ak účet nie je nájdený, vráti 0.
     */
    private double getInitialBalance(int accountId) {
        Account account = SessionManager.getInstance().getAccountById(accountId);
        return account != null ? account.getInitialBalance() : 0.0;
    }

    /**
     * Prepočíta mapu denných/mesačných súm na kumulatívne hodnoty.
     * Štartuje od initialBalance saving účtu.
     * Vstup:  { "2026-02": 10000, "2026-03": 5000 }
     * Výstup: { "2026-02": 60000, "2026-03": 65000 } (pri initialBalance=50000)
     */
    private Map<String, Double> toCumulative(Map<String, Double> raw, double initialBalance) {
        Map<String, Double> cumulative = new TreeMap<>();
        double running = initialBalance;
        for (Map.Entry<String, Double> entry : raw.entrySet()) {
            running += entry.getValue();
            cumulative.put(entry.getKey(), running);
        }
        return cumulative;
    }

    /**
     * Prepočíta na kumulatívne hodnoty — štartuje od 0.
     */
    private Map<String, Double> toCumulative(Map<String, Double> raw) {
        return toCumulative(raw, 0.0);
    }
}