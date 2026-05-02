package sk.sporixx.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import sk.sporixx.dto.ChartPeriod;
import sk.sporixx.dto.IncomeExpenseData;
import sk.sporixx.dto.SavingAccountReportData;
import sk.sporixx.model.Transaction;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Performance testy pre ExportService.
 *
 * Meriame rýchlosť generovania XML pri veľkých objemoch dát:
 *   - IncomeExpense: 500 periód v PeriodIncome + PeriodExpense
 *   - SavingAccounts: 50 účtov × 200 transakcií = 10 000 Transaction elementov
 *   - Opakované volania exportu
 */
@DisplayName("ExportService – Performance testy")
class ExportServicePerformanceTest {

    private FakeReportsService fakeReports;
    private ExportService exportService;
    private Path tempFile;

    @BeforeEach
    void setUp() throws Exception {
        fakeReports = new FakeReportsService();
        exportService = new ExportServiceImpl(fakeReports);
        tempFile = Files.createTempFile("export-perf-test-", ".xml");
    }

    @AfterEach
    void tearDown() throws Exception {
        Files.deleteIfExists(tempFile);
    }

    // ======================== INCOME EXPENSE ========================

    @Nested
    @DisplayName("exportIncomeExpenseToXml – výkon")
    class IncomeExpensePerformance {

        @Test
        @Timeout(value = 1, unit = TimeUnit.SECONDS)
        @DisplayName("Export s 500 periódami v PeriodIncome a PeriodExpense prebehne do 1 s")
        void export500Periods_withinTimeLimit() throws Exception {
            Map<String, Double> income = new LinkedHashMap<>();
            Map<String, Double> expense = new LinkedHashMap<>();
            for (int i = 1; i <= 500; i++) {
                income.put("2026-period-" + i, (double) i * 10);
                expense.put("2026-period-" + i, (double) i * 7);
            }
            fakeReports.setIncomeExpenseData(IncomeExpenseData.builder()
                    .monthlyIncome(income)
                    .monthlyExpense(expense)
                    .totalIncome(1_250_000).totalExpense(875_000)
                    .build());

            long start = System.nanoTime();
            exportService.exportIncomeExpenseToXml(ChartPeriod.TWELVE_MONTHS, tempFile.toString());
            long ms = (System.nanoTime() - start) / 1_000_000;

            System.out.printf("[PERF] exportIncomeExpense (500 periód): %d ms%n", ms);
            assertTrue(Files.size(tempFile) > 0);
        }

        @Test
        @Timeout(value = 2, unit = TimeUnit.SECONDS)
        @DisplayName("100 opakovaných exportov IncomeExpense prebehne do 2 s")
        void export100Times_withinTimeLimit() throws Exception {
            Map<String, Double> income = new LinkedHashMap<>();
            Map<String, Double> expense = new LinkedHashMap<>();
            for (int i = 1; i <= 12; i++) {
                income.put("2026-" + String.format("%02d", i), i * 500.0);
                expense.put("2026-" + String.format("%02d", i), i * 300.0);
            }
            fakeReports.setIncomeExpenseData(IncomeExpenseData.builder()
                    .monthlyIncome(income).monthlyExpense(expense)
                    .totalIncome(39_000).totalExpense(23_400).build());

            long start = System.nanoTime();
            for (int i = 0; i < 100; i++) {
                Path tmpPath = Files.createTempFile("export-perf-100-", ".xml");
                try {
                    exportService.exportIncomeExpenseToXml(ChartPeriod.TWELVE_MONTHS, tmpPath.toString());
                } finally {
                    Files.deleteIfExists(tmpPath);
                }
            }
            long ms = (System.nanoTime() - start) / 1_000_000;
            System.out.printf("[PERF] exportIncomeExpense x100: %d ms%n", ms);
        }
    }

    // ======================== SAVING ACCOUNTS ========================

    @Nested
    @DisplayName("exportSavingAccountsToXml – výkon")
    class SavingAccountsPerformance {

        @Test
        @Timeout(value = 2, unit = TimeUnit.SECONDS)
        @DisplayName("Export 50 saving účtov s 200 transakciami každý (10 000 tx) prebehne do 2 s")
        void export50AccountsWith200Transactions_withinTimeLimit() throws Exception {
            List<SavingAccountReportData> accounts = new ArrayList<>();
            LocalDateTime date = LocalDateTime.of(2026, 1, 1, 0, 0);

            for (int a = 1; a <= 50; a++) {
                List<Transaction> txs = new ArrayList<>();
                for (int t = 1; t <= 200; t++) {
                    txs.add(Transaction.builder()
                            .accountId(a).amount(50.0)
                            .completeDate(date.plusDays(t))
                            .description("vklad " + t)
                            .categoryId(null).currencyCode("EUR").build());
                }

                Map<String, Double> progress = new LinkedHashMap<>();
                for (int m = 1; m <= 12; m++) {
                    progress.put("2026-" + String.format("%02d", m), m * 100.0);
                }

                accounts.add(SavingAccountReportData.builder()
                        .accountId(a).accountName("Účet " + a)
                        .savedUp(5000).needToSave(5000).targetAmount(10_000)
                        .targetDate(LocalDateTime.of(2027, 1, 1, 0, 0))
                        .initialBalance(0).createdAt(date)
                        .progressGrouping("MONTH")
                        .expectedProgress(progress).actualProgress(progress)
                        .transactions(txs).build());
            }

            fakeReports.setSavingAccountsData(accounts);

            long start = System.nanoTime();
            exportService.exportSavingAccountsToXml(tempFile.toString());
            long ms = (System.nanoTime() - start) / 1_000_000;

            System.out.printf("[PERF] exportSavingAccounts (50 účtov × 200 tx = 10 000 tx): %d ms%n", ms);
            assertTrue(Files.size(tempFile) > 0);
        }

        @Test
        @Timeout(value = 1, unit = TimeUnit.SECONDS)
        @DisplayName("Export 100 saving účtov bez transakcií prebehne do 1 s")
        void export100AccountsNoTransactions_withinTimeLimit() throws Exception {
            List<SavingAccountReportData> accounts = new ArrayList<>();
            for (int a = 1; a <= 100; a++) {
                accounts.add(SavingAccountReportData.builder()
                        .accountId(a).accountName("Účet " + a)
                        .savedUp(1000).needToSave(4000).targetAmount(5000)
                        .targetDate(LocalDateTime.of(2027, 6, 1, 0, 0))
                        .initialBalance(0).createdAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                        .progressGrouping("MONTH")
                        .expectedProgress(Map.of()).actualProgress(Map.of())
                        .transactions(List.of()).build());
            }
            fakeReports.setSavingAccountsData(accounts);

            long start = System.nanoTime();
            exportService.exportSavingAccountsToXml(tempFile.toString());
            long ms = (System.nanoTime() - start) / 1_000_000;

            System.out.printf("[PERF] exportSavingAccounts (100 účtov, bez tx): %d ms%n", ms);
            assertTrue(Files.size(tempFile) > 0);
        }
    }

    // ====================== FAKE ======================

    static class FakeReportsService implements ReportsService {
        private IncomeExpenseData incomeExpenseData;
        private List<SavingAccountReportData> savingAccountsData;

        void setIncomeExpenseData(IncomeExpenseData data) { this.incomeExpenseData = data; }
        void setSavingAccountsData(List<SavingAccountReportData> data) { this.savingAccountsData = data; }

        @Override public sk.sporixx.dto.WantNeedData loadWantNeedData(ChartPeriod p) { throw new UnsupportedOperationException(); }
        @Override public IncomeExpenseData loadIncomeExpenseData(ChartPeriod p) { return incomeExpenseData; }
        @Override public sk.sporixx.dto.CategoryExpenseData loadCategoryExpenseData(ChartPeriod p) { throw new UnsupportedOperationException(); }
        @Override public sk.sporixx.dto.RecurringExpenseData loadRecurringExpenseData() { throw new UnsupportedOperationException(); }
        @Override public List<SavingAccountReportData> loadSavingAccountsData() { return savingAccountsData; }
    }
}

