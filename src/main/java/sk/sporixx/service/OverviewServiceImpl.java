package sk.sporixx.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sk.sporixx.dto.AccountsSummaryData;
import sk.sporixx.dto.ActivitiesData;
import sk.sporixx.dto.AnalyticsData;
import sk.sporixx.dto.ChartPeriod;
import sk.sporixx.model.Account;
import sk.sporixx.model.RecurringRule;
import sk.sporixx.model.SavingGoal;
import sk.sporixx.model.Transaction;
import sk.sporixx.repository.RecurringRuleRepository;
import sk.sporixx.repository.SavingGoalRepository;
import sk.sporixx.repository.TransactionRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementácia OverviewService.
 * Dáta o účtoch čerpá zo SessionManager (účty sú v pamäti od prihlásenia).
 * Transakcie, recurring rules a saving goals načítava z DB cez repozitáre.
 * UI vrstva volá metódy bez parametrov — service si všetko získa sama.
 * Tým sa dodržuje čistý tok: UI -> Service -> Repository -> DB
 */
public class OverviewServiceImpl implements OverviewService {

    private static final Logger logger = LoggerFactory.getLogger(OverviewServiceImpl.class);

    private final TransactionRepository transactionRepository;
    private final RecurringRuleRepository recurringRuleRepository;
    private final SavingGoalRepository savingGoalRepository;

    public OverviewServiceImpl(TransactionRepository transactionRepository,
                               RecurringRuleRepository recurringRuleRepository,
                               SavingGoalRepository savingGoalRepository) {
        this.transactionRepository = transactionRepository;
        this.recurringRuleRepository = recurringRuleRepository;
        this.savingGoalRepository = savingGoalRepository;
    }

    @Override
    public AccountsSummaryData loadAccountsSummary() {
        List<Account> accounts = SessionManager.getInstance().getAccounts();
        logger.info("Loading accounts summary (count={})", accounts.size());

        try {
            if (accounts.isEmpty()) {
                return AccountsSummaryData.builder()
                        .totalBalance(0.0)
                        .accounts(Collections.emptyList())
                        .savingGoalByAccountId(Collections.emptyMap())
                        .build();
            }

            // Total balance z pamäte (účty sú už v session)
            double totalBalance = accounts.stream()
                    .mapToDouble(Account::getCurrentBalance)
                    .sum();

            // Saving goals len pre saving účty (z DB)
            List<Integer> savingAccountIds = accounts.stream()
                    .filter(Account::isSavingAccount)
                    .map(Account::getId)
                    .collect(Collectors.toList());

            Map<Integer, SavingGoal> savingGoals = savingAccountIds.isEmpty()
                    ? Collections.emptyMap()
                    : savingGoalRepository.findActiveByAccountIds(savingAccountIds).stream()
                      .collect(Collectors.toMap(SavingGoal::getAccountId, goal -> goal));

            return AccountsSummaryData.builder()
                    .totalBalance(totalBalance)
                    .accounts(accounts)
                    .savingGoalByAccountId(savingGoals)
                    .build();

        } catch (Exception e) {
            logger.error("Error loading accounts summary", e);
            throw new ServiceException("error.db_error", e);
        }
    }

    @Override
    public AnalyticsData loadAnalytics(ChartPeriod chartPeriod, int accountId) {
        logger.info("Loading analytics for period={}, accountId={}", chartPeriod, accountId);

        try {
            LocalDateTime from = chartPeriod.calculateStartDate().atStartOfDay();

            // Repozitár voláme iba pre JEDEN účet
            Map<String, Double> chartData = chartPeriod.isGroupByDay()
                    ? transactionRepository.sumByTypeAndDay(accountId, Transaction.TYPE_INCOME, from)
                    : transactionRepository.sumByTypeAndMonth(accountId, Transaction.TYPE_INCOME, from);

            // Sčítame všetky hodnoty (sumy príjmov) z mapy chartData
            double totalIncome = chartData.values().stream()
                    .mapToDouble(Double::doubleValue)
                    .sum();

            return AnalyticsData.builder()
                    .chartPeriod(chartPeriod)
                    .chartData(chartData)
                    .totalIncome(totalIncome)
                    .build();

        } catch (Exception e) {
            logger.error("Error loading analytics", e);
            throw new ServiceException("error.db_error", e);
        }
    }

    @Override
    public ActivitiesData loadActivities(int accountId) {
        logger.info("Loading activities for accountId={}", accountId);

        try {
            // Upcoming payments (najbližšie 3 pre tento jeden účet)
            LocalDateTime now = LocalDateTime.now();
            List<RecurringRule> upcoming = recurringRuleRepository.findUpcomingByAccountId(accountId, now, 3);

            // Nedávne transakcie (posledné 2 týždne pre tento jeden účet)
            LocalDateTime twoWeeksAgo = now.minusWeeks(2).toLocalDate().atStartOfDay();
            List<Transaction> recent = transactionRepository.findByAccountIdAndDateRange(
                    accountId, twoWeeksAgo, now);

            return ActivitiesData.builder()
                    .upcomingPayments(upcoming)
                    .recentTransactions(recent)
                    .build();

        } catch (Exception e) {
            logger.error("Error loading activities", e);
            throw new ServiceException("error.db_error", e);
        }
    }
}