package sk.sporixx.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import sk.sporixx.model.Transaction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance testy pre TransactionService.
 */
@DisplayName("TransactionService – Performance testy")
class TransactionServicePerformanceTest extends TransactionServiceTestSupport {

    @Nested
    @DisplayName("Pridanie transakcií")
    class AddPerformance {

        @Test
        @Timeout(value = 3, unit = TimeUnit.SECONDS)
        @DisplayName("Pridanie 500 príjmov prebehne do 3 s")
        void add500Incomes_withinTimeLimit() {
            long start = System.nanoTime();
            for (int i = 1; i <= 500; i++) {
                transactionService.addTransaction(
                        mainAccount.getId(), Transaction.TYPE_INCOME, null,
                        categorySalaryId, null, "Salary-" + i,
                        1000.0 + i, "EUR", LocalDate.now());
            }
            long ms = (System.nanoTime() - start) / 1_000_000;
            System.out.printf("[PERF] addTransaction (500 incomes): %d ms%n", ms);

            List<Transaction> all = transactionService.getTransactions(mainAccount.getId());
            assertEquals(500, all.size());
        }

        @Test
        @Timeout(value = 3, unit = TimeUnit.SECONDS)
        @DisplayName("Pridanie 500 výdavkov prebehne do 3 s")
        void add500Expenses_withinTimeLimit() {
            // navýšime zostatok aby nevyšiel do mínusu
            mainAccount.setCurrentBalance(1_000_000.0);

            long start = System.nanoTime();
            for (int i = 1; i <= 500; i++) {
                transactionService.addTransaction(
                        mainAccount.getId(), Transaction.TYPE_EXPENSE, null,
                        categoryFoodId, Transaction.CLASSIFICATION_NEED, "Expense-" + i,
                        10.0, "EUR", LocalDate.now());
            }
            long ms = (System.nanoTime() - start) / 1_000_000;
            System.out.printf("[PERF] addTransaction (500 expenses): %d ms%n", ms);
        }
    }

    @Nested
    @DisplayName("Načítanie transakcií")
    class LoadPerformance {

        @Test
        @Timeout(value = 2, unit = TimeUnit.SECONDS)
        @DisplayName("Načítanie 1 000 transakcií prebehne do 2 s")
        void load1000Transactions_withinTimeLimit() {
            LocalDateTime now = LocalDateTime.now();
            for (int i = 1; i <= 1000; i++) {
                insertRawTransaction(mainAccount.getId(), Transaction.TYPE_INCOME,
                        categorySalaryId, 100.0, "T-" + i, now.minusDays(i % 365));
            }

            long start = System.nanoTime();
            List<Transaction> result = transactionService.getTransactions(mainAccount.getId());
            long ms = (System.nanoTime() - start) / 1_000_000;

            assertEquals(1000, result.size());
            System.out.printf("[PERF] getTransactions (1 000): %d ms%n", ms);
        }

        @Test
        @Timeout(value = 2, unit = TimeUnit.SECONDS)
        @DisplayName("getAllTransactions naprieč 3 účtami (1 000 transakcií) prebehne do 2 s")
        void getAllTransactions_withinTimeLimit() {
            LocalDateTime now = LocalDateTime.now();
            for (int i = 1; i <= 400; i++) {
                insertRawTransaction(mainAccount.getId(), Transaction.TYPE_INCOME,
                        categorySalaryId, 50.0, "M-" + i, now.minusDays(i));
                insertRawTransaction(savingAccount.getId(), Transaction.TYPE_INCOME,
                        categorySalaryId, 50.0, "S-" + i, now.minusDays(i));
            }
            for (int i = 1; i <= 200; i++) {
                insertRawTransaction(secondMainAccount.getId(), Transaction.TYPE_EXPENSE,
                        categoryFoodId, 20.0, "E-" + i, now.minusDays(i));
            }

            long start = System.nanoTime();
            List<Transaction> result = transactionService.getAllTransactions();
            long ms = (System.nanoTime() - start) / 1_000_000;

            assertEquals(1000, result.size());
            System.out.printf("[PERF] getAllTransactions (1 000): %d ms%n", ms);
        }
    }

    @Nested
    @DisplayName("Mazanie transakcií")
    class DeletePerformance {

        @Test
        @Timeout(value = 3, unit = TimeUnit.SECONDS)
        @DisplayName("Vymazanie 300 transakcií prebehne do 3 s")
        void delete300Transactions_withinTimeLimit() {
            List<Integer> ids = new java.util.ArrayList<>();
            for (int i = 1; i <= 300; i++) {
                Transaction t = transactionService.addTransaction(
                        mainAccount.getId(), Transaction.TYPE_INCOME, null,
                        categorySalaryId, null, "Del-" + i,
                        100.0, "EUR", LocalDate.now());
                ids.add(t.getId());
            }

            long start = System.nanoTime();
            transactionService.deleteTransactions(ids);
            long ms = (System.nanoTime() - start) / 1_000_000;

            assertTrue(transactionService.getTransactions(mainAccount.getId()).isEmpty());
            System.out.printf("[PERF] deleteTransactions x300: %d ms%n", ms);
        }
    }

    @Nested
    @DisplayName("Vyhľadávanie transakcií")
    class SearchPerformance {

        @Test
        @Timeout(value = 2, unit = TimeUnit.SECONDS)
        @DisplayName("Vyhľadávanie naprieč 1 000 transakciami prebehne do 2 s")
        void search1000Transactions_withinTimeLimit() {
            LocalDateTime now = LocalDateTime.now();
            for (int i = 1; i <= 1000; i++) {
                insertRawTransaction(mainAccount.getId(), Transaction.TYPE_EXPENSE,
                        categoryFoodId, 10.0, "Nákup " + i, now.minusDays(i % 30));
            }

            long start = System.nanoTime();
            List<Transaction> result = transactionService.searchTransactions("Nákup", mainAccount.getId());
            long ms = (System.nanoTime() - start) / 1_000_000;

            assertFalse(result.isEmpty());
            System.out.printf("[PERF] searchTransactions (1 000): %d ms%n", ms);
        }
    }
}

