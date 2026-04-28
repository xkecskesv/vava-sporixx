package sk.sporixx.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import sk.sporixx.dto.AccountsSummaryData;
import sk.sporixx.dto.ActivitiesData;
import sk.sporixx.dto.AnalyticsData;
import sk.sporixx.dto.ChartPeriod;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance testy pre Overview modul.
 *
 * Overujeme, že kľúčové operácie zvládnu veľké objemy dát v prijateľnom čase.
 * Každý test má definovaný časový limit pomocou @Timeout.
 *
 * Testované scenáre:
 *   - loadAccountsSummary() pri veľkom počte účtov a saving goals
 *   - loadActivities() pri veľkom počte transakcií a recurring rules
 *   - loadAnalytics() pri veľkom počte transakcií naprieč dlhým obdobím
 */
@DisplayName("Overview – Performance testy")
class OverviewPerformanceTest extends OverviewServiceTestSupport {

    // ======================== ACCOUNTS SUMMARY ========================

    @Nested
    @DisplayName("loadAccountsSummary() – výkon")
    class AccountsSummaryPerformance {

        @Test
        @Timeout(value = 1, unit = TimeUnit.SECONDS)
        @DisplayName("loadAccountsSummary() s 500 saving goals prebehne do 1 s")
        void summaryWith500Goals_withinTimeLimit() {
            // Pridáme 500 saving goals pre rôzne (fiktívne) accounty
            for (int i = 10; i < 510; i++) {
                addSavingGoal(i, 1000.0 * i, 100.0 * i);
            }

            long start = System.nanoTime();
            AccountsSummaryData data = overviewService.loadAccountsSummary();
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;

            assertNotNull(data);
            System.out.printf("[PERF] loadAccountsSummary (500 goals): %d ms%n", elapsedMs);
        }

        @Test
        @Timeout(value = 1, unit = TimeUnit.SECONDS)
        @DisplayName("loadAccountsSummary() sa volá 1 000-krát za sebou do 1 s")
        void summaryCalledRepeatedly_withinTimeLimit() {
            addSavingGoal(savingAccount.getId(), 5000.0, 2000.0);

            long start = System.nanoTime();
            for (int i = 0; i < 1000; i++) {
                overviewService.loadAccountsSummary();
            }
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;

            System.out.printf("[PERF] loadAccountsSummary x1000 volaní: %d ms%n", elapsedMs);
        }
    }

    // ======================== ACTIVITIES ========================

    @Nested
    @DisplayName("loadActivities() – výkon")
    class ActivitiesPerformance {

        @Test
        @Timeout(value = 1, unit = TimeUnit.SECONDS)
        @DisplayName("loadActivities() s 2 000 transakciami prebehne do 1 s")
        void activitiesWith2000Transactions_withinTimeLimit() {
            LocalDateTime now = LocalDateTime.now();
            for (int i = 1; i <= 1000; i++) {
                addIncome(mainAccount.getId(), 50.0 * i, now.minusDays(i));
            }
            for (int i = 1; i <= 1000; i++) {
                addExpense(mainAccount.getId(), 30.0 * i, now.minusDays(i));
            }

            long start = System.nanoTime();
            ActivitiesData data = overviewService.loadActivities(mainAccount.getId());
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;

            assertNotNull(data);
            assertFalse(data.getRecentTransactions().isEmpty());
            System.out.printf("[PERF] loadActivities (2 000 transakcií): %d ms%n", elapsedMs);
        }

        @Test
        @Timeout(value = 1, unit = TimeUnit.SECONDS)
        @DisplayName("loadActivities() s 500 recurring rules prebehne do 1 s")
        void activitiesWith500RecurringRules_withinTimeLimit() {
            LocalDateTime future = LocalDateTime.now().plusDays(10);
            for (int i = 1; i <= 500; i++) {
                addRecurringRule(mainAccount.getId(), 10.0 * i, future.plusDays(i));
            }

            long start = System.nanoTime();
            ActivitiesData data = overviewService.loadActivities(mainAccount.getId());
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;

            assertNotNull(data);
            System.out.printf("[PERF] loadActivities (500 recurring rules): %d ms%n", elapsedMs);
        }

        @Test
        @Timeout(value = 2, unit = TimeUnit.SECONDS)
        @DisplayName("loadActivities() sa volá 500-krát za sebou do 2 s")
        void activitiesCalledRepeatedly_withinTimeLimit() {
            LocalDateTime now = LocalDateTime.now();
            for (int i = 1; i <= 100; i++) {
                addExpense(mainAccount.getId(), 20.0, now.minusDays(i));
            }
            for (int i = 1; i <= 10; i++) {
                addRecurringRule(mainAccount.getId(), 50.0, now.plusDays(i));
            }

            long start = System.nanoTime();
            for (int i = 0; i < 500; i++) {
                overviewService.loadActivities(mainAccount.getId());
            }
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;

            System.out.printf("[PERF] loadActivities x500 volaní: %d ms%n", elapsedMs);
        }
    }

    // ======================== ANALYTICS ========================

    @Nested
    @DisplayName("loadAnalytics() – výkon")
    class AnalyticsPerformance {

        @Test
        @Timeout(value = 1, unit = TimeUnit.SECONDS)
        @DisplayName("loadAnalytics() s 5 000 transakciami (ONE_MONTH) prebehne do 1 s")
        void analyticsWith5000Transactions_oneMonth_withinTimeLimit() {
            LocalDateTime now = LocalDateTime.now();
            for (int i = 1; i <= 2500; i++) {
                addIncome(mainAccount.getId(), 10.0, now.minusDays(i % 28));
            }
            for (int i = 1; i <= 2500; i++) {
                addExpense(mainAccount.getId(), 5.0, now.minusDays(i % 28));
            }

            long start = System.nanoTime();
            AnalyticsData data = overviewService.loadAnalytics(
                    ChartPeriod.ONE_MONTH, mainAccount.getId());
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;

            assertNotNull(data);
            System.out.printf("[PERF] loadAnalytics ONE_MONTH (5 000 transakcií): %d ms%n", elapsedMs);
        }

        @Test
        @Timeout(value = 1, unit = TimeUnit.SECONDS)
        @DisplayName("loadAnalytics() s 3 000 transakciami (TWELVE_MONTHS) prebehne do 1 s")
        void analyticsWith3000Transactions_twelveMonths_withinTimeLimit() {
            LocalDateTime now = LocalDateTime.now();
            for (int i = 1; i <= 1500; i++) {
                addIncome(mainAccount.getId(), 100.0, now.minusDays(i % 360));
            }
            for (int i = 1; i <= 1500; i++) {
                addExpense(mainAccount.getId(), 80.0, now.minusDays(i % 360));
            }

            long start = System.nanoTime();
            AnalyticsData data = overviewService.loadAnalytics(
                    ChartPeriod.TWELVE_MONTHS, mainAccount.getId());
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;

            assertNotNull(data);
            System.out.printf("[PERF] loadAnalytics TWELVE_MONTHS (3 000 transakcií): %d ms%n", elapsedMs);
        }

        @Test
        @Timeout(value = 2, unit = TimeUnit.SECONDS)
        @DisplayName("loadAnalytics() pre všetky ChartPeriod hodnoty prebehne do 2 s")
        void analyticsAllPeriods_withinTimeLimit() {
            LocalDateTime now = LocalDateTime.now();
            for (int i = 1; i <= 500; i++) {
                addIncome(mainAccount.getId(), 50.0, now.minusDays(i));
                addExpense(mainAccount.getId(), 30.0, now.minusDays(i));
            }

            long start = System.nanoTime();
            for (ChartPeriod period : ChartPeriod.values()) {
                AnalyticsData data = overviewService.loadAnalytics(period, mainAccount.getId());
                assertNotNull(data);
            }
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;

            System.out.printf("[PERF] loadAnalytics všetky ChartPeriod (500 transakcií každé): %d ms%n", elapsedMs);
        }

        @Test
        @Timeout(value = 2, unit = TimeUnit.SECONDS)
        @DisplayName("loadAnalytics() sa volá 200-krát za sebou do 2 s")
        void analyticsCalledRepeatedly_withinTimeLimit() {
            LocalDateTime now = LocalDateTime.now();
            for (int i = 1; i <= 200; i++) {
                addExpense(mainAccount.getId(), 25.0, now.minusDays(i % 30));
            }

            long start = System.nanoTime();
            for (int i = 0; i < 200; i++) {
                overviewService.loadAnalytics(
                        ChartPeriod.ONE_MONTH, mainAccount.getId());
            }
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;

            System.out.printf("[PERF] loadAnalytics x200 volaní: %d ms%n", elapsedMs);
        }
    }
}






