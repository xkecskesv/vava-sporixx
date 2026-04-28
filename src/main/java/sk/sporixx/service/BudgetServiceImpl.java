package sk.sporixx.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sk.sporixx.dto.BudgetData;
import sk.sporixx.dto.BudgetWarning;
import sk.sporixx.model.Account;
import sk.sporixx.model.Budget;
import sk.sporixx.model.Transaction;
import sk.sporixx.repository.BudgetRepository;
import sk.sporixx.repository.TransactionRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Implementácia BudgetService.
 * Spravuje Budget Setup, Custom Allocation a Emergency Fund.
 * Štandardné percentá (z celého monthlyIncome):
 *   emergency_fund = 10%, savings = 30%, to_invest = 40%, fun_money = 20%
 * Fallback percentá (zo zvyšku po essential (pri vysokých výdavkoch)):
 *   emergency_fund = 15%, savings = 40%, to_invest = 30%, fun_money = 15%
 */
public class BudgetServiceImpl implements BudgetService {

    private static final Logger logger =
            LoggerFactory.getLogger(BudgetServiceImpl.class);

    // štandardné percentá (z celého monthlyIncome)
    private static final double EMERGENCY_FUND_PERCENT = 0.10;
    private static final double SAVINGS_PERCENT = 0.30;
    private static final double TO_INVEST_PERCENT = 0.40;

    // fallback percentá (zo zvyšku po essential)
    // väčší dôraz na sporenie, menej agresívne investovanie
    private static final double FALLBACK_EMERGENCY_PERCENT = 0.15;
    private static final double FALLBACK_SAVINGS_PERCENT = 0.40;
    private static final double FALLBACK_TO_INVEST_PERCENT = 0.30;

    // Emergency Fund limity
    // minimálny = 3x mesačné výdavky, optimálny = 6x mesačné výdavky
    private static final double MINIMAL_EMERGENCY_MULTIPLIER = 3.0;
    private static final double OPTIMAL_EMERGENCY_MULTIPLIER = 6.0;

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;

    public BudgetServiceImpl(BudgetRepository budgetRepository,
                             TransactionRepository transactionRepository) {
        this.budgetRepository = budgetRepository;
        this.transactionRepository = transactionRepository;
    }

    // NAČÍTANIE
    @Override
    public BudgetData loadBudgetData() {
        int userId = SessionManager.getInstance().getCurrentUserId();
        logger.info("Loading budget data for userId: {}", userId);

        try {
            // história emergency fondu sa načíta vždy
            double emergencyFundCurrent = getEmergencyFundBalance();
            Map<String, Double> emergencyFundHistory = loadEmergencyFundHistory();

            Optional<Budget> budgetOpt = budgetRepository.findByUserId(userId);

            if (budgetOpt.isEmpty()) {
                logger.info("No budget found for userId: {}, returning defaults", userId);
                return buildDefaultBudgetData(emergencyFundCurrent, emergencyFundHistory);
            }

            Budget budget = budgetOpt.get();

            return BudgetData.builder()
                    .monthlyIncome(budget.getMonthlyIncome())
                    .food(budget.getFood())
                    .rent(budget.getRent())
                    .transport(budget.getTransport())
                    .utilities(budget.getUtilities())
                    .other(budget.getOther())
                    .essentialExpensesTotal(calculateEssentialTotal(budget))
                    .essentialExpenses(budget.getEssentialExpenses())
                    .emergencyFund(budget.getEmergencyFund())
                    .savings(budget.getSavings())
                    .toInvest(budget.getToInvest())
                    .funMoney(budget.getFunMoney())
                    .essentialExpensesPercent(toPercent(budget.getEssentialExpenses(), budget.getMonthlyIncome()))
                    .emergencyFundPercent(toPercent(budget.getEmergencyFund(), budget.getMonthlyIncome()))
                    .savingsPercent(toPercent(budget.getSavings(), budget.getMonthlyIncome()))
                    .toInvestPercent(toPercent(budget.getToInvest(), budget.getMonthlyIncome()))
                    .funMoneyPercent(toPercent(budget.getFunMoney(), budget.getMonthlyIncome()))
                    .emergencyFundCurrent(emergencyFundCurrent)
                    .minimalEmergencyFund(budget.getMinimalEmergencyFund())
                    .optimalEmergencyFund(budget.getOptimalEmergencyFund())
                    .emergencyFundHistory(emergencyFundHistory)
                    .build();

        } catch (Exception e) {
            logger.error("Failed to load budget data", e);
            throw new BudgetException("error.db_error", e);
        }
    }

    // BUDGET SETUP
    @Override
    public BudgetWarning saveBudgetSetup(double monthlyIncome, double food,
                                         double rent, double transport,
                                         double utilities, double other) {
        logger.info("Saving budget setup: monthlyIncome={}", monthlyIncome);

        // validácia
        if (monthlyIncome <= 0) {
            throw new BudgetException("budget.error.invalid_income");
        }
        if (food < 0 || rent < 0 || transport < 0
                || utilities < 0 || other < 0) {
            throw new BudgetException("budget.error.negative_expense");
        }

        double essentialTotal = round2(food + rent + transport + utilities + other);
        if (essentialTotal > monthlyIncome) {
            throw new BudgetException("budget.error.expenses_exceed_income");
        }

        try {
            int userId = SessionManager.getInstance().getCurrentUserId();

            // vypočítaj alokáciu
            AllocationResult allocation = calculateAllocation(
                    monthlyIncome, essentialTotal);

            // Emergency Fund limity
            double minimalEmergencyFund =
                    essentialTotal * MINIMAL_EMERGENCY_MULTIPLIER;
            double optimalEmergencyFund =
                    essentialTotal * OPTIMAL_EMERGENCY_MULTIPLIER;

            Optional<Budget> existing = budgetRepository.findByUserId(userId);

            if (existing.isEmpty()) {
                Budget budget = Budget.builder()
                        .userId(userId)
                        .monthlyIncome(monthlyIncome)
                        .food(food).rent(rent)
                        .transport(transport)
                        .utilities(utilities).other(other)
                        .essentialExpenses(essentialTotal)
                        .emergencyFund(allocation.emergencyFund)
                        .savings(allocation.savings)
                        .toInvest(allocation.toInvest)
                        .funMoney(allocation.funMoney)
                        .minimalEmergencyFund(minimalEmergencyFund)
                        .optimalEmergencyFund(optimalEmergencyFund)
                        .isActive(true)
                        .createdAt(LocalDateTime.now())
                        .build();
                budgetRepository.save(budget);
            } else {
                Budget budget = existing.get();
                budget.setMonthlyIncome(monthlyIncome);
                budget.setFood(food);
                budget.setRent(rent);
                budget.setTransport(transport);
                budget.setUtilities(utilities);
                budget.setOther(other);
                budget.setEssentialExpenses(essentialTotal);
                budget.setEmergencyFund(allocation.emergencyFund);
                budget.setSavings(allocation.savings);
                budget.setToInvest(allocation.toInvest);
                budget.setFunMoney(allocation.funMoney);
                budget.setMinimalEmergencyFund(minimalEmergencyFund);
                budget.setOptimalEmergencyFund(optimalEmergencyFund);
                budgetRepository.update(budget);
            }

            logger.info("Budget setup saved — warning: {}",
                    allocation.warning);
            return allocation.warning;

        } catch (BudgetException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to save budget setup", e);
            throw new BudgetException("error.db_error", e);
        }
    }

    // CUSTOM ALLOCATION
    @Override
    public BudgetWarning saveCustomAllocation(double essentialExpenses,
                                              double emergencyFund,
                                              double savings,
                                              double toInvest) {
        logger.info("Saving custom allocation: essential={}, emergency={}, " +
                        "savings={}, toInvest={}",
                essentialExpenses, emergencyFund, savings, toInvest);

        int userId = SessionManager.getInstance().getCurrentUserId();
        Optional<Budget> existing = budgetRepository.findByUserId(userId);

        if (existing.isEmpty()) {
            throw new BudgetException("budget.error.setup_required");
        }

        Budget budget = existing.get();
        double monthlyIncome = budget.getMonthlyIncome();

        // validácia
        if (essentialExpenses < 0 || emergencyFund < 0
                || savings < 0 || toInvest < 0) {
            throw new BudgetException("budget.error.negative_expense");
        }

        double total = essentialExpenses + emergencyFund + savings + toInvest;
        if (total > monthlyIncome) {
            throw new BudgetException("budget.error.expenses_exceed_income");
        }

        // funMoney = zvyšok
        double funMoney = monthlyIncome - total;

        // upozornenie ak essential < skutočné výdavky z Budget Setup
        double actualEssential = calculateEssentialTotal(budget);
        BudgetWarning warning = BudgetWarning.NONE;
        if (essentialExpenses < actualEssential) {
            logger.warn("Custom essential ({}) < actual essential ({})",
                    essentialExpenses, actualEssential);
            warning = BudgetWarning.ESSENTIAL_BELOW_ACTUAL;
        }

        try {
            budget.setEssentialExpenses(essentialExpenses);
            budget.setEmergencyFund(emergencyFund);
            budget.setSavings(savings);
            budget.setToInvest(toInvest);
            budget.setFunMoney(funMoney);
            budgetRepository.update(budget);

            logger.info("Custom allocation saved — warning: {}", warning);
            return warning;

        } catch (Exception e) {
            logger.error("Failed to save custom allocation", e);
            throw new BudgetException("error.db_error", e);
        }
    }

    // HELPERS
    /**
     * Vypočíta alokáciu podľa monthlyIncome a essentialTotal.
     * Štandardný prípad: percentá z celého monthlyIncome.
     * Fallback prípad: percentá zo zvyšku (väčší dôraz na sporenie).
     */
    private AllocationResult calculateAllocation(double monthlyIncome, double essentialTotal) {

        double emergencyFromTotal = monthlyIncome * EMERGENCY_FUND_PERCENT;
        double savingsFromTotal = monthlyIncome * SAVINGS_PERCENT;
        double toInvestFromTotal = monthlyIncome * TO_INVEST_PERCENT;
        double totalAllocated = essentialTotal + emergencyFromTotal + savingsFromTotal + toInvestFromTotal;

        if (totalAllocated <= monthlyIncome) {
            // štandardný prípad
            double funMoney = monthlyIncome - essentialTotal
                    - emergencyFromTotal - savingsFromTotal - toInvestFromTotal;
            logger.info("Standard allocation applied");
            return new AllocationResult(
                    round2(emergencyFromTotal),
                    round2(savingsFromTotal),
                    round2(toInvestFromTotal),
                    round2(funMoney),
                    BudgetWarning.NONE);
        }

        // fallback: essential príliš vysoké
        double remaining = monthlyIncome - essentialTotal;
        logger.info("Essential expenses too high, applying fallback " +
                "allocation from remaining: {}", remaining);

        if (remaining <= 0) {
            logger.warn("Essential expenses exceed monthly income!");
            return new AllocationResult(0, 0, 0, 0, BudgetWarning.FALLBACK_ALLOCATION_APPLIED);
        }

        double emergencyFund = round2(remaining * FALLBACK_EMERGENCY_PERCENT);
        double savings = round2(remaining * FALLBACK_SAVINGS_PERCENT);
        double toInvest = round2(remaining * FALLBACK_TO_INVEST_PERCENT);
        double funMoney = round2(remaining - emergencyFund - savings - toInvest);

        logger.info("Fallback allocation: emergency={}, savings={}, " +
                        "toInvest={}, funMoney={}",
                emergencyFund, savings, toInvest, funMoney);

        return new AllocationResult(emergencyFund, savings, toInvest,
                funMoney, BudgetWarning.FALLBACK_ALLOCATION_APPLIED);
    }

    /**
     * Interný helper objekt pre výsledok výpočtu alokácie.
     */
    private record AllocationResult(double emergencyFund, double savings,
                                    double toInvest, double funMoney,
                                    BudgetWarning warning) {
    }

    private double calculateEssentialTotal(Budget budget) {
        return budget.getFood() + budget.getRent()
                + budget.getTransport() + budget.getUtilities()
                + budget.getOther();
    }

    private double toPercent(double amount, double monthlyIncome) {
        if (monthlyIncome <= 0) return 0;
        return Math.round((amount / monthlyIncome) * 100.0 * 10.0) / 10.0;
    }

    private double getEmergencyFundBalance() {
        return SessionManager.getInstance().getAccounts().stream()
                .filter(Account::isEmergencyFund)
                .findFirst()
                .map(Account::getCurrentBalance)
                .orElse(0.0);
    }

    private Map<String, Double> loadEmergencyFundHistory() {
        try {
            Account emergencyAccount = SessionManager.getInstance()
                    .getAccounts().stream()
                    .filter(Account::isEmergencyFund)
                    .findFirst()
                    .orElse(null);

            if (emergencyAccount == null) return new TreeMap<>();

            int accountId = emergencyAccount.getId();

            LocalDateTime from = LocalDateTime.now()
                    .minusMonths(11).withDayOfMonth(1)
                    .withHour(0).withMinute(0).withSecond(0);

            // vypočíta presný balance k dátumu from
            List<Transaction> allBeforeFrom = transactionRepository
                    .findByAccountIdAndDateRange(accountId,
                            emergencyAccount.getCreatedAt(), from);

            double balanceAtFrom = emergencyAccount.getInitialBalance();
            for (Transaction tx : allBeforeFrom) {
                if (tx.isIncome()) balanceAtFrom += tx.getAmount();
                else balanceAtFrom -= tx.getAmount();
            }

            // načíta income a expense za posledných 12 mesiacov
            Map<String, Double> incomeByMonth = transactionRepository
                    .sumByTypeAndMonth(accountId, Transaction.TYPE_INCOME, from);
            Map<String, Double> expenseByMonth = transactionRepository
                    .sumByTypeAndMonth(accountId, Transaction.TYPE_EXPENSE, from);

            // zlúči do net (income - expense)
            Map<String, Double> netByMonth = new TreeMap<>();
            incomeByMonth.forEach((month, sum) ->
                    netByMonth.merge(month, sum, Double::sum));
            expenseByMonth.forEach((month, sum) ->
                    netByMonth.merge(month, -sum, Double::sum));

            // doplní chýbajúce mesiace
            LocalDateTime current = from;
            LocalDateTime until = LocalDateTime.now().withDayOfMonth(1);
            while (!current.isAfter(until)) {
                netByMonth.putIfAbsent(
                        current.format(DateTimeFormatter.ofPattern("yyyy-MM")), 0.0);
                current = current.plusMonths(1);
            }

            logger.info("Emergency fund history size: {}", netByMonth.size());
            logger.info("balanceAtFrom: {}", balanceAtFrom);

            // kumulatívne od presného balance k dátumu from
            return toCumulative(netByMonth, balanceAtFrom);

        } catch (Exception e) {
            logger.warn("Could not load emergency fund history", e);
            return new TreeMap<>();
        }
    }

    private BudgetData buildDefaultBudgetData(double emergencyFundCurrent,
                                              Map<String, Double> emergencyFundHistory) {
        return BudgetData.builder()
                .monthlyIncome(0).food(0).rent(0)
                .transport(0).utilities(0).other(0)
                .essentialExpensesTotal(0)
                .essentialExpenses(0).essentialExpensesPercent(0)
                .emergencyFund(0).emergencyFundPercent(0)
                .savings(0).savingsPercent(0)
                .toInvest(0).toInvestPercent(0)
                .funMoney(0).funMoneyPercent(0)
                .emergencyFundCurrent(emergencyFundCurrent)
                .minimalEmergencyFund(0)
                .optimalEmergencyFund(0)
                .emergencyFundHistory(emergencyFundHistory)
                .build();
    }

    private Map<String, Double> toCumulative(Map<String, Double> raw, double initialBalance) {
        Map<String, Double> cumulative = new TreeMap<>();
        double running = initialBalance;
        for (Map.Entry<String, Double> entry : raw.entrySet()) {
            running += entry.getValue();
            cumulative.put(entry.getKey(), running);
        }
        return cumulative;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}