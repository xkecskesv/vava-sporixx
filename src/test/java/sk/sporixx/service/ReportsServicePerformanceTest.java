package sk.sporixx.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import sk.sporixx.dto.*;
import sk.sporixx.dto.SavingAccountReportData;
import sk.sporixx.model.Transaction;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance testy pre ReportsService.
 */
@DisplayName("ReportsService – Performance testy")
class ReportsServicePerformanceTest extends ReportsServiceTestSupport {

    @Nested
    @DisplayName("loadIncomeExpenseData() – výkon")
    class IncomeExpensePerformance {

        @Test
        @Timeout(value = 1, unit = TimeUnit.SECONDS)
        @DisplayName("loadIncomeExpenseData() s 3 000 transakciami prebehne do 1 s")
        void with3000Transactions_withinTimeLimit() {
            LocalDateTime now = LocalDateTime.now();
            for (int i = 1; i <= 1500; i++) {
                tx(mainAccount.getId(), Transaction.TYPE_INCOME, 1, null, 100.0, now.minusDays(i % 360));
                tx(mainAccount.getId(), Transaction.TYPE_EXPENSE, 2, Transaction.CLASSIFICATION_NEED, 50.0, now.minusDays(i % 360));
            }

            long start = System.nanoTime();
            IncomeExpenseData data = reportsService.loadIncomeExpenseData(ChartPeriod.TWELVE_MONTHS);
            long ms = (System.nanoTime() - start) / 1_000_000;

            assertNotNull(data);
            System.out.printf("[PERF] loadIncomeExpenseData TWELVE_MONTHS (3 000 tx): %d ms%n", ms);
        }

        @Test
        @Timeout(value = 2, unit = TimeUnit.SECONDS)
        @DisplayName("loadIncomeExpenseData() sa volá pre všetky ChartPeriod hodnoty do 2 s")
        void allPeriods_withinTimeLimit() {
            LocalDateTime now = LocalDateTime.now();
            for (int i = 1; i <= 500; i++) {
                tx(mainAccount.getId(), Transaction.TYPE_INCOME, 1, null, 80.0, now.minusDays(i % 180));
                tx(mainAccount.getId(), Transaction.TYPE_EXPENSE, 2, Transaction.CLASSIFICATION_WANT, 40.0, now.minusDays(i % 180));
            }

            long start = System.nanoTime();
            for (ChartPeriod period : ChartPeriod.values()) {
                assertNotNull(reportsService.loadIncomeExpenseData(period));
            }
            long ms = (System.nanoTime() - start) / 1_000_000;
            System.out.printf("[PERF] loadIncomeExpenseData všetky ChartPeriod (500 tx): %d ms%n", ms);
        }

        @Test
        @Timeout(value = 2, unit = TimeUnit.SECONDS)
        @DisplayName("loadIncomeExpenseData() sa volá 500-krát do 2 s")
        void called500x_withinTimeLimit() {
            LocalDateTime now = LocalDateTime.now();
            for (int i = 1; i <= 100; i++) {
                tx(mainAccount.getId(), Transaction.TYPE_INCOME, 1, null, 100.0, now.minusDays(i));
            }

            long start = System.nanoTime();
            for (int i = 0; i < 500; i++) {
                reportsService.loadIncomeExpenseData(ChartPeriod.ONE_MONTH);
            }
            long ms = (System.nanoTime() - start) / 1_000_000;
            System.out.printf("[PERF] loadIncomeExpenseData x500 volaní: %d ms%n", ms);
        }
    }

    @Nested
    @DisplayName("loadCategoryExpenseData() – výkon")
    class CategoryExpensePerformance {

        @Test
        @Timeout(value = 1, unit = TimeUnit.SECONDS)
        @DisplayName("loadCategoryExpenseData() s 2 000 výdavkami prebehne do 1 s")
        void with2000Expenses_withinTimeLimit() {
            LocalDateTime now = LocalDateTime.now();
            for (int i = 1; i <= 2000; i++) {
                tx(mainAccount.getId(), Transaction.TYPE_EXPENSE,
                        (i % 5) + 1, Transaction.CLASSIFICATION_NEED, 15.0, now.minusDays(i % 30));
            }

            long start = System.nanoTime();
            CategoryExpenseData data = reportsService.loadCategoryExpenseData(ChartPeriod.ONE_MONTH);
            long ms = (System.nanoTime() - start) / 1_000_000;

            assertNotNull(data);
            System.out.printf("[PERF] loadCategoryExpenseData ONE_MONTH (2 000 tx): %d ms%n", ms);
        }
    }

    @Nested
    @DisplayName("loadWantNeedData() – výkon")
    class WantNeedPerformance {

        @Test
        @Timeout(value = 1, unit = TimeUnit.SECONDS)
        @DisplayName("loadWantNeedData() s 2 000 výdavkami prebehne do 1 s")
        void with2000Expenses_withinTimeLimit() {
            LocalDateTime now = LocalDateTime.now();
            for (int i = 1; i <= 1000; i++) {
                tx(mainAccount.getId(), Transaction.TYPE_EXPENSE, 1,
                        Transaction.CLASSIFICATION_NEED, 20.0, now.minusDays(i % 30));
                tx(mainAccount.getId(), Transaction.TYPE_EXPENSE, 2,
                        Transaction.CLASSIFICATION_WANT, 15.0, now.minusDays(i % 30));
            }

            long start = System.nanoTime();
            WantNeedData data = reportsService.loadWantNeedData(ChartPeriod.ONE_MONTH);
            long ms = (System.nanoTime() - start) / 1_000_000;

            assertNotNull(data);
            System.out.printf("[PERF] loadWantNeedData ONE_MONTH (2 000 tx): %d ms%n", ms);
        }

        @Test
        @Timeout(value = 2, unit = TimeUnit.SECONDS)
        @DisplayName("loadWantNeedData() sa volá 500-krát do 2 s")
        void called500x_withinTimeLimit() {
            LocalDateTime now = LocalDateTime.now();
            for (int i = 1; i <= 100; i++) {
                tx(mainAccount.getId(), Transaction.TYPE_EXPENSE, 1,
                        Transaction.CLASSIFICATION_NEED, 30.0, now.minusDays(i));
            }

            long start = System.nanoTime();
            for (int i = 0; i < 500; i++) {
                reportsService.loadWantNeedData(ChartPeriod.ONE_MONTH);
            }
            long ms = (System.nanoTime() - start) / 1_000_000;
            System.out.printf("[PERF] loadWantNeedData x500 volaní: %d ms%n", ms);
        }
    }

    @Nested
    @DisplayName("loadSavingAccountsData() – výkon")
    class SavingAccountsPerformance {

        @Test
        @Timeout(value = 1, unit = TimeUnit.SECONDS)
        @DisplayName("loadSavingAccountsData() s 500 saving goals prebehne do 1 s")
        void with500Goals_withinTimeLimit() {
            for (int i = 10; i < 510; i++) {
                savingGoalRepo.save(sk.sporixx.model.SavingGoal.builder()
                        .accountId(i)
                        .name("Goal-" + i)
                        .goalTypeId(1)
                        .targetAmount(5000.0)
                        .currentAmount(1000.0 * (i % 5))
                        .targetDate(LocalDateTime.now().plusYears(1))
                        .isActive(true)
                        .createdAt(LocalDateTime.now())
                        .build());
            }

            long start = System.nanoTime();
            List<SavingAccountReportData> data = reportsService.loadSavingAccountsData();
            long ms = (System.nanoTime() - start) / 1_000_000;

            assertNotNull(data);
            System.out.printf("[PERF] loadSavingAccountsData (500 goals): %d ms%n", ms);
        }
    }
}



