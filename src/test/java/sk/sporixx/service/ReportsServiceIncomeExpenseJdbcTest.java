package sk.sporixx.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sk.sporixx.dto.ChartPeriod;
import sk.sporixx.dto.IncomeExpenseData;
import sk.sporixx.model.Transaction;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * INTEGRATION testy {@link ReportsService#loadIncomeExpenseData(ChartPeriod)}
 * proti reálnej JDBC SQLite databáze.
 * <p>
 * Na rozdiel od unit testov ({@code ReportsServiceIncomeExpenseTest}) tu používame
 * reálne {@code TransactionRepositoryImpl}, takže testy odhalia:
 *  <ul>
 *      <li>chyby v SQL dotazoch (zle JOINy, GROUP BY, ORDER BY)</li>
 *      <li>chyby v mapovaní stĺpcov DB → Java polia</li>
 *      <li>NULL handling v WHERE klauzulách</li>
 *      <li>boundary problémy s dátumovým formátovaním</li>
 *      <li>FK violations</li>
 *  </ul>
 */
class ReportsServiceIncomeExpenseJdbcTest extends ReportsServiceJdbcTestSupport {

    private static final int CAT_FOOD = 1;
    private static final int CAT_SALARY = 9;

    // ====================== ZÁKLADNÉ AGREGÁCIE ======================

    @Test
    @DisplayName("Prázdne dáta — totalIncome=0, totalExpense=0, žiadne kľúče v mape")
    void empty_returnsZeros() {
        IncomeExpenseData data = reportsService.loadIncomeExpenseData(ChartPeriod.TWELVE_MONTHS);

        assertNotNull(data);
        assertEquals(0.0, data.getTotalIncome(), 0.001);
        assertEquals(0.0, data.getTotalExpense(), 0.001);
        assertTrue(data.getMonthlyIncome().isEmpty());
        assertTrue(data.getMonthlyExpense().isEmpty());
    }

    @Test
    @DisplayName("Jeden income na main accounte — totalIncome=100")
    void singleIncome_sumsCorrectly() {
        insertTransaction(mainAccount.getId(), Transaction.TYPE_INCOME,
                CAT_SALARY, null, 100.0, LocalDateTime.now().minusDays(5));

        IncomeExpenseData data = reportsService.loadIncomeExpenseData(ChartPeriod.TWELVE_MONTHS);

        assertEquals(100.0, data.getTotalIncome(), 0.001);
        assertEquals(0.0, data.getTotalExpense(), 0.001);
    }

    @Test
    @DisplayName("Jeden expense na main accounte — totalExpense=50")
    void singleExpense_sumsCorrectly() {
        insertTransaction(mainAccount.getId(), Transaction.TYPE_EXPENSE,
                CAT_FOOD, Transaction.CLASSIFICATION_NEED, 50.0,
                LocalDateTime.now().minusDays(3));

        IncomeExpenseData data = reportsService.loadIncomeExpenseData(ChartPeriod.TWELVE_MONTHS);

        assertEquals(0.0, data.getTotalIncome(), 0.001);
        assertEquals(50.0, data.getTotalExpense(), 0.001);
    }

    @Test
    @DisplayName("Suma sa skladá naprieč viacerými účtami (main + saving)")
    void sumsAcrossMultipleAccounts() {
        // Income na main 100
        insertTransaction(mainAccount.getId(), Transaction.TYPE_INCOME,
                CAT_SALARY, null, 100.0, LocalDateTime.now().minusDays(10));
        // Income na saving 200
        insertTransaction(savingAccount.getId(), Transaction.TYPE_INCOME,
                CAT_SALARY, null, 200.0, LocalDateTime.now().minusDays(10));
        // Expense na main 50
        insertTransaction(mainAccount.getId(), Transaction.TYPE_EXPENSE,
                CAT_FOOD, Transaction.CLASSIFICATION_NEED, 50.0,
                LocalDateTime.now().minusDays(5));

        IncomeExpenseData data = reportsService.loadIncomeExpenseData(ChartPeriod.TWELVE_MONTHS);

        assertEquals(300.0, data.getTotalIncome(), 0.001,
                "main + saving income = 100 + 200 = 300");
        assertEquals(50.0, data.getTotalExpense(), 0.001);
    }

    // ====================== EXCLUSION TRANSFER KATEGÓRIÍ ======================

    @Test
    @DisplayName("Transfer income (CATEGORY_SAVING=6) NESMIE byť zarátaný do total income")
    void excludesTransferIncome() {
        // Bežný income 100 (kategória Salary)
        insertTransaction(mainAccount.getId(), Transaction.TYPE_INCOME,
                CAT_SALARY, null, 100.0, LocalDateTime.now().minusDays(10));
        // Transfer income (saving leg) — kategória CATEGORY_SAVING (=6)
        insertTransaction(savingAccount.getId(), Transaction.TYPE_INCOME,
                Transaction.CATEGORY_SAVING, null, 200.0,
                LocalDateTime.now().minusDays(10));

        IncomeExpenseData data = reportsService.loadIncomeExpenseData(ChartPeriod.TWELVE_MONTHS);

        // SPRÁVNE: iba bežný income sa zaratá → 100
        assertEquals(100.0, data.getTotalIncome(), 0.001,
                "transfer income (CATEGORY_SAVING) sa NESMIE počítať do bežného príjmu");
    }

    @Test
    @DisplayName("Transfer expense (CATEGORY_SAVING_EXPENSE=7) NESMIE byť zarátaný do total expense")
    void excludesTransferExpense() {
        // Bežný expense 50 (Food)
        insertTransaction(mainAccount.getId(), Transaction.TYPE_EXPENSE,
                CAT_FOOD, Transaction.CLASSIFICATION_NEED, 50.0,
                LocalDateTime.now().minusDays(3));
        // Transfer expense (main → saving) — kategória CATEGORY_SAVING_EXPENSE (=7)
        insertTransaction(mainAccount.getId(), Transaction.TYPE_EXPENSE,
                Transaction.CATEGORY_SAVING_EXPENSE, null, 1000.0,
                LocalDateTime.now().minusDays(3));

        IncomeExpenseData data = reportsService.loadIncomeExpenseData(ChartPeriod.TWELVE_MONTHS);

        assertEquals(50.0, data.getTotalExpense(), 0.001,
                "transfer expense (CATEGORY_SAVING_EXPENSE) sa NESMIE počítať");
    }

    @Test
    @DisplayName("Mixed: bežné transakcie + transfery — len bežné sa zarátajú")
    void mixedRegularAndTransfer() {
        // Bežné
        insertTransaction(mainAccount.getId(), Transaction.TYPE_INCOME, CAT_SALARY, null,
                500.0, LocalDateTime.now().minusDays(10));
        insertTransaction(mainAccount.getId(), Transaction.TYPE_EXPENSE, CAT_FOOD,
                Transaction.CLASSIFICATION_NEED, 100.0, LocalDateTime.now().minusDays(5));
        // Transfery (kategórie 6 a 7)
        insertTransaction(savingAccount.getId(), Transaction.TYPE_INCOME,
                Transaction.CATEGORY_SAVING, null, 200.0, LocalDateTime.now().minusDays(8));
        insertTransaction(mainAccount.getId(), Transaction.TYPE_EXPENSE,
                Transaction.CATEGORY_SAVING_EXPENSE, null, 200.0,
                LocalDateTime.now().minusDays(8));

        IncomeExpenseData data = reportsService.loadIncomeExpenseData(ChartPeriod.TWELVE_MONTHS);

        assertEquals(500.0, data.getTotalIncome(), 0.001);
        assertEquals(100.0, data.getTotalExpense(), 0.001);
    }

    // ====================== GROUPING (DAY vs MONTH) ======================

    @Test
    @DisplayName("ONE_WEEK perióda — groupByDay (kľúče vo formáte yyyy-MM-dd, 10 znakov)")
    void oneWeek_groupsByDay() {
        insertTransaction(mainAccount.getId(), Transaction.TYPE_INCOME,
                CAT_SALARY, null, 100.0, LocalDateTime.now().minusDays(2));

        IncomeExpenseData data = reportsService.loadIncomeExpenseData(ChartPeriod.ONE_WEEK);

        assertFalse(data.getMonthlyIncome().isEmpty());
        String key = data.getMonthlyIncome().keySet().iterator().next();
        assertEquals(10, key.length(),
                "DAY groupingu zodpovedá kľúč 'yyyy-MM-dd' (10 znakov), dostali sme: " + key);
    }

    @Test
    @DisplayName("TWELVE_MONTHS perióda — groupByMonth (kľúče vo formáte yyyy-MM, 7 znakov)")
    void twelveMonths_groupsByMonth() {
        insertTransaction(mainAccount.getId(), Transaction.TYPE_INCOME,
                CAT_SALARY, null, 100.0, LocalDateTime.now().minusMonths(2));

        IncomeExpenseData data = reportsService.loadIncomeExpenseData(ChartPeriod.TWELVE_MONTHS);

        assertFalse(data.getMonthlyIncome().isEmpty());
        String key = data.getMonthlyIncome().keySet().iterator().next();
        assertEquals(7, key.length(),
                "MONTH groupingu zodpovedá kľúč 'yyyy-MM' (7 znakov), dostali sme: " + key);
    }

    @Test
    @DisplayName("Viaceré transakcie v rovnakom dni sa AGREGUJÚ pod jeden kľúč")
    void multipleTransactionsSameDay_aggregateUnderSingleKey() {
        LocalDateTime sameDay = LocalDateTime.now().minusDays(2).withHour(10);
        insertTransaction(mainAccount.getId(), Transaction.TYPE_INCOME,
                CAT_SALARY, null, 100.0, sameDay);
        insertTransaction(mainAccount.getId(), Transaction.TYPE_INCOME,
                CAT_SALARY, null, 50.0, sameDay.withHour(15));

        IncomeExpenseData data = reportsService.loadIncomeExpenseData(ChartPeriod.ONE_WEEK);

        assertEquals(1, data.getMonthlyIncome().size(),
                "transakcie v ten istý deň sa majú agregovať na 1 kľúč");
        assertEquals(150.0, data.getMonthlyIncome().values().iterator().next(), 0.001,
                "agregovaná suma 100+50=150");
    }

    @Test
    @DisplayName("Viaceré dni v ONE_WEEK perióde — každý deň má vlastný kľúč")
    void differentDaysInOneWeek_separateKeys() {
        insertTransaction(mainAccount.getId(), Transaction.TYPE_INCOME,
                CAT_SALARY, null, 100.0, LocalDateTime.now().minusDays(1));
        insertTransaction(mainAccount.getId(), Transaction.TYPE_INCOME,
                CAT_SALARY, null, 200.0, LocalDateTime.now().minusDays(3));
        insertTransaction(mainAccount.getId(), Transaction.TYPE_INCOME,
                CAT_SALARY, null, 300.0, LocalDateTime.now().minusDays(5));

        IncomeExpenseData data = reportsService.loadIncomeExpenseData(ChartPeriod.ONE_WEEK);

        assertEquals(3, data.getMonthlyIncome().size(), "3 rôzne dni = 3 kľúče");
        assertEquals(600.0, data.getTotalIncome(), 0.001);
    }

    // ====================== DATE BOUNDARY ======================

    @Test
    @DisplayName("Transakcia staršia ako perióda — VYNECHANÁ z reportu")
    void olderThanPeriod_excluded() {
        // ONE_WEEK = posledných 7 dní. Transakcia spred 2 týždňov musí vypadnúť.
        insertTransaction(mainAccount.getId(), Transaction.TYPE_INCOME,
                CAT_SALARY, null, 9999.0, LocalDateTime.now().minusWeeks(2));

        IncomeExpenseData data = reportsService.loadIncomeExpenseData(ChartPeriod.ONE_WEEK);

        assertEquals(0.0, data.getTotalIncome(), 0.001,
                "transakcia mimo perióda nesmie byť v reporte");
    }

    @Test
    @DisplayName("Transakcia presne na dnešok — započítaná do ONE_WEEK")
    void today_included() {
        insertTransaction(mainAccount.getId(), Transaction.TYPE_INCOME,
                CAT_SALARY, null, 100.0, LocalDateTime.now().minusHours(2));

        IncomeExpenseData data = reportsService.loadIncomeExpenseData(ChartPeriod.ONE_WEEK);

        assertEquals(100.0, data.getTotalIncome(), 0.001);
    }

    @Test
    @DisplayName("Cross-period: transakcia v ONE_MONTH musí byť, v ONE_WEEK nesmie")
    void differentPeriods_yieldDifferentResults() {
        // Spred 10 dní — v rámci ONE_MONTH, mimo ONE_WEEK
        insertTransaction(mainAccount.getId(), Transaction.TYPE_INCOME,
                CAT_SALARY, null, 100.0, LocalDateTime.now().minusDays(10));

        IncomeExpenseData oneWeek = reportsService.loadIncomeExpenseData(ChartPeriod.ONE_WEEK);
        IncomeExpenseData oneMonth = reportsService.loadIncomeExpenseData(ChartPeriod.ONE_MONTH);

        assertEquals(0.0, oneWeek.getTotalIncome(), 0.001,
                "transakcia spred 10 dní nesmie byť v ONE_WEEK");
        assertEquals(100.0, oneMonth.getTotalIncome(), 0.001,
                "tá istá transakcia musí byť v ONE_MONTH");
    }

    // ====================== TYPE FILTER ======================

    @Test
    @DisplayName("Income filter neprieputní expense (a naopak)")
    void typeFilter_separatesIncomeAndExpense() {
        insertTransaction(mainAccount.getId(), Transaction.TYPE_INCOME,
                CAT_SALARY, null, 100.0, LocalDateTime.now().minusDays(2));
        insertTransaction(mainAccount.getId(), Transaction.TYPE_EXPENSE,
                CAT_FOOD, Transaction.CLASSIFICATION_NEED, 50.0,
                LocalDateTime.now().minusDays(2));

        IncomeExpenseData data = reportsService.loadIncomeExpenseData(ChartPeriod.ONE_WEEK);

        assertEquals(100.0, data.getTotalIncome(), 0.001);
        assertEquals(50.0, data.getTotalExpense(), 0.001);
    }
}
