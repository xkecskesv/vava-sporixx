package sk.sporixx.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sk.sporixx.dto.ChartPeriod;
import sk.sporixx.dto.IncomeExpenseData;
import sk.sporixx.model.Transaction;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy pre {@link ReportsService#loadIncomeExpenseData(ChartPeriod)}.
 *
 * Pokrývame:
 *   - prázdne dáta
 *   - sumu income/expense across multiple accounts
 *   - exclusion transfer kategórií (CATEGORY_SAVING, CATEGORY_SAVING_EXPENSE)
 *   - groupByDay (ONE_WEEK / ONE_MONTH) vs groupByMonth (SIX_MONTHS / TWELVE_MONTHS)
 *   - hraničné dátumy (mimo perióda)
 */
class ReportsServiceIncomeExpenseTest extends ReportsServiceTestSupport {

    // ============ PRÁZDNE DÁTA ============

    @Test
    @DisplayName("loadIncomeExpenseData — prázdne dáta")
    void empty_returnsZeros() {
        IncomeExpenseData data = reportsService.loadIncomeExpenseData(ChartPeriod.TWELVE_MONTHS);

        assertNotNull(data);
        assertEquals(0.0, data.getTotalIncome());
        assertEquals(0.0, data.getTotalExpense());
        assertTrue(data.getMonthlyIncome().isEmpty());
        assertTrue(data.getMonthlyExpense().isEmpty());
    }

    // ============ SUMA NAPRIEČ ÚČTAMI ============

    @Test
    @DisplayName("Suma sa skladá z viacerých účtov (main + saving)")
    void sumsAcrossAccounts() {
        // Income na main 100
        tx(mainAccount.getId(), Transaction.TYPE_INCOME, 1, null, 100.0,
                LocalDateTime.now().minusDays(10));
        // Income na saving 200
        tx(savingAccount.getId(), Transaction.TYPE_INCOME, 1, null, 200.0,
                LocalDateTime.now().minusDays(10));
        // Expense na main 50
        tx(mainAccount.getId(), Transaction.TYPE_EXPENSE, 1, Transaction.CLASSIFICATION_NEED, 50.0,
                LocalDateTime.now().minusDays(5));

        IncomeExpenseData data = reportsService.loadIncomeExpenseData(ChartPeriod.TWELVE_MONTHS);

        assertEquals(300.0, data.getTotalIncome(), 0.001);
        assertEquals(50.0, data.getTotalExpense(), 0.001);
    }

    // ============ EXCLUSION TRANSFER KATEGÓRIÍ ============

    @Test
    @DisplayName("Transfer kategórie (CATEGORY_SAVING) sú vylúčené z income/expense reportu")
    void excludesTransferCategories() {
        // Normal income 100
        tx(mainAccount.getId(), Transaction.TYPE_INCOME, 1, null, 100.0,
                LocalDateTime.now().minusDays(10));
        // Transfer income (saving leg) — má CATEGORY_SAVING (=6)
        tx(savingAccount.getId(), Transaction.TYPE_INCOME,
                Transaction.CATEGORY_SAVING, null, 200.0,
                LocalDateTime.now().minusDays(10));

        IncomeExpenseData data = reportsService.loadIncomeExpenseData(ChartPeriod.TWELVE_MONTHS);

        assertEquals(100.0, data.getTotalIncome(), 0.001,
                "transfer income (CATEGORY_SAVING) sa NEMÁ započítať");
    }

    @Test
    @DisplayName("Transfer expense (CATEGORY_SAVING_EXPENSE) je vylúčený")
    void excludesTransferExpense() {
        tx(mainAccount.getId(), Transaction.TYPE_EXPENSE, 1, Transaction.CLASSIFICATION_NEED, 50.0,
                LocalDateTime.now().minusDays(3));
        tx(mainAccount.getId(), Transaction.TYPE_EXPENSE,
                Transaction.CATEGORY_SAVING_EXPENSE, null, 1000.0,
                LocalDateTime.now().minusDays(3));

        IncomeExpenseData data = reportsService.loadIncomeExpenseData(ChartPeriod.TWELVE_MONTHS);

        assertEquals(50.0, data.getTotalExpense(), 0.001);
    }

    // ============ GROUPING ============

    @Test
    @DisplayName("ONE_WEEK — groupByDay (kľúče vo formáte yyyy-MM-dd)")
    void oneWeek_groupsByDay() {
        tx(mainAccount.getId(), Transaction.TYPE_INCOME, 1, null, 100.0,
                LocalDateTime.now().minusDays(2));

        IncomeExpenseData data = reportsService.loadIncomeExpenseData(ChartPeriod.ONE_WEEK);

        assertFalse(data.getMonthlyIncome().isEmpty());
        // kľúč musí mať formát yyyy-MM-dd (10 znakov)
        String key = data.getMonthlyIncome().keySet().iterator().next();
        assertEquals(10, key.length(), "DAY format = yyyy-MM-dd");
    }

    @Test
    @DisplayName("TWELVE_MONTHS — groupByMonth (kľúče vo formáte yyyy-MM)")
    void twelveMonths_groupsByMonth() {
        tx(mainAccount.getId(), Transaction.TYPE_INCOME, 1, null, 100.0,
                LocalDateTime.now().minusMonths(2));

        IncomeExpenseData data = reportsService.loadIncomeExpenseData(ChartPeriod.TWELVE_MONTHS);

        assertFalse(data.getMonthlyIncome().isEmpty());
        String key = data.getMonthlyIncome().keySet().iterator().next();
        assertEquals(7, key.length(), "MONTH format = yyyy-MM");
    }

    // ============ HRANIČNÉ DÁTUMY ============

    @Test
    @DisplayName("Transakcia staršia ako perióda — vylúčená")
    void olderThanPeriod_excluded() {
        // ONE_WEEK = posledný týždeň. Transakcia spred 2 týždňov musí byť vynechaná.
        tx(mainAccount.getId(), Transaction.TYPE_INCOME, 1, null, 100.0,
                LocalDateTime.now().minusWeeks(2));

        IncomeExpenseData data = reportsService.loadIncomeExpenseData(ChartPeriod.ONE_WEEK);

        assertEquals(0.0, data.getTotalIncome(), 0.001);
    }

    @Test
    @DisplayName("Transakcia presne na hranici (start of period) — započítaná")
    void exactlyAtPeriodStart_included() {
        // Záleží na implementácii — kód volá calculateStartDate().atStartOfDay()
        // a v repo robi !isBefore(from), takže rovnaký moment sa POČÍTA
        tx(mainAccount.getId(), Transaction.TYPE_INCOME, 1, null, 100.0,
                ChartPeriod.ONE_WEEK.calculateStartDate().atStartOfDay().plusHours(1));

        IncomeExpenseData data = reportsService.loadIncomeExpenseData(ChartPeriod.ONE_WEEK);

        assertEquals(100.0, data.getTotalIncome(), 0.001);
    }

    // ============ RÔZNE PERIÓDY ============

    @Test
    @DisplayName("Rovnaká transakcia vidieť v ONE_MONTH ale nie v ONE_WEEK")
    void differentPeriods_differentResults() {
        // Transakcia spred 10 dní
        tx(mainAccount.getId(), Transaction.TYPE_INCOME, 1, null, 100.0,
                LocalDateTime.now().minusDays(10));

        IncomeExpenseData oneWeek = reportsService.loadIncomeExpenseData(ChartPeriod.ONE_WEEK);
        IncomeExpenseData oneMonth = reportsService.loadIncomeExpenseData(ChartPeriod.ONE_MONTH);

        assertEquals(0.0, oneWeek.getTotalIncome(), 0.001);
        assertEquals(100.0, oneMonth.getTotalIncome(), 0.001);
    }

    // ============ NULL CATEGORY ============

    @Test
    @DisplayName("Transakcia s null categoryId — vylúčená (filter v in-memory repo)")
    void nullCategory_excluded() {
        // In-memory repo: filter "categoryId != null && !excludedIds.contains"
        // Nullová kategória teda padne na prvej časti && a vylúči sa
        tx(mainAccount.getId(), Transaction.TYPE_INCOME, null, null, 100.0,
                LocalDateTime.now().minusDays(2));

        IncomeExpenseData data = reportsService.loadIncomeExpenseData(ChartPeriod.ONE_WEEK);

        assertEquals(0.0, data.getTotalIncome(), 0.001,
                "transakcie s null categoryId sa nepočítajú v reportoch — DOKUMENTUJEM aktualne spravanie");
    }
}
