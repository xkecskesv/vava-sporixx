package sk.sporixx.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import sk.sporixx.dto.BudgetData;
import sk.sporixx.model.Transaction;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance testy pre BudgetService.
 */
@DisplayName("BudgetService – Performance testy")
class BudgetServicePerformanceTest extends BudgetServiceTestSupport {

    @Nested
    @DisplayName("loadBudgetData() – výkon")
    class LoadPerformance {

        @Test
        @Timeout(value = 1, unit = TimeUnit.SECONDS)
        @DisplayName("loadBudgetData() sa volá 1 000-krát za sebou do 1 s")
        void loadBudgetData1000x_withinTimeLimit() {
            budgetService.saveBudgetSetup(3000.0, 400.0, 800.0, 150.0, 100.0, 200.0);

            long start = System.nanoTime();
            for (int i = 0; i < 1000; i++) {
                budgetService.loadBudgetData();
            }
            long ms = (System.nanoTime() - start) / 1_000_000;
            System.out.printf("[PERF] loadBudgetData x1000: %d ms%n", ms);
        }

        @Test
        @Timeout(value = 2, unit = TimeUnit.SECONDS)
        @DisplayName("loadBudgetData() s 2 000 emergency transakciami prebehne do 2 s")
        void loadBudgetDataWith2000Transactions_withinTimeLimit() {
            budgetService.saveBudgetSetup(3000.0, 400.0, 800.0, 150.0, 100.0, 200.0);
            LocalDateTime now = LocalDateTime.now();
            for (int i = 1; i <= 1000; i++) {
                insertEmergencyTransaction(Transaction.TYPE_INCOME, 50.0, now.minusDays(i));
                insertEmergencyTransaction(Transaction.TYPE_EXPENSE, 30.0, now.minusDays(i));
            }

            long start = System.nanoTime();
            BudgetData data = budgetService.loadBudgetData();
            long ms = (System.nanoTime() - start) / 1_000_000;

            assertNotNull(data);
            System.out.printf("[PERF] loadBudgetData (2 000 emergency tx): %d ms%n", ms);
        }
    }

    @Nested
    @DisplayName("saveBudgetSetup() – výkon")
    class SavePerformance {

        @Test
        @Timeout(value = 2, unit = TimeUnit.SECONDS)
        @DisplayName("Uloženie budget setup 500-krát prebehne do 2 s")
        void saveBudgetSetup500x_withinTimeLimit() {
            long start = System.nanoTime();
            for (int i = 1; i <= 500; i++) {
                budgetService.saveBudgetSetup(
                        1000.0 * i, 100.0 * i, 200.0, 50.0, 30.0, 20.0);
            }
            long ms = (System.nanoTime() - start) / 1_000_000;
            System.out.printf("[PERF] saveBudgetSetup x500: %d ms%n", ms);
        }
    }

    @Nested
    @DisplayName("saveCustomAllocation() – výkon")
    class CustomAllocationPerformance {

        @Test
        @Timeout(value = 2, unit = TimeUnit.SECONDS)
        @DisplayName("Uloženie custom allocation 500-krát prebehne do 2 s")
        void saveCustomAllocation500x_withinTimeLimit() {
            budgetService.saveBudgetSetup(3000.0, 400.0, 800.0, 150.0, 100.0, 200.0);

            long start = System.nanoTime();
            for (int i = 1; i <= 500; i++) {
                budgetService.saveCustomAllocation(
                        1000.0 + i, 200.0, 300.0, 100.0);
            }
            long ms = (System.nanoTime() - start) / 1_000_000;
            System.out.printf("[PERF] saveCustomAllocation x500: %d ms%n", ms);
        }
    }
}

