package sk.sporixx.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sk.sporixx.dto.CategoryExpenseData;
import sk.sporixx.dto.ChartPeriod;
import sk.sporixx.model.Transaction;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * INTEGRATION testy {@link ReportsService#loadCategoryExpenseData(ChartPeriod)}
 * proti reálnej JDBC SQLite databáze.
 * <p>
 * Trieda overuje že SQL JOIN s {@code categories} tabuľkou funguje správne,
 * a že kategorizácia, exclusion a TOP-N agregát sú spojazdnené.
 */
class ReportsServiceCategoryExpenseJdbcTest extends ReportsServiceJdbcTestSupport {

    private static final int CAT_FOOD = 1;
    private static final int CAT_CLOTHING = 2;
    private static final int CAT_ENTERTAINMENT = 3;
    private static final int CAT_RENT = 4;
    private static final int CAT_TRANSPORT = 5;
    private static final int CAT_INVESTMENT = 8;
    private static final int CAT_SALARY = 9;

    // ====================== ZÁKLADNÉ AGREGÁCIE ======================

    @Test
    @DisplayName("Prázdne dáta — totalExpense=0, prázdna mapa")
    void empty_returnsZeroAndEmptyMap() {
        CategoryExpenseData data = reportsService.loadCategoryExpenseData(ChartPeriod.TWELVE_MONTHS);

        assertNotNull(data);
        assertEquals(0.0, data.getTotalExpense(), 0.001);
        assertTrue(data.getExpenseByCategory().isEmpty());
    }

    @Test
    @DisplayName("3 expense (2× Food, 1× Entertainment) — agreguje na 2 kategórie")
    void groupsByCategoryName() {
        // Food: 50 + 30 = 80
        insertTransaction(mainAccount.getId(), Transaction.TYPE_EXPENSE,
                CAT_FOOD, Transaction.CLASSIFICATION_NEED, 50.0,
                LocalDateTime.now().minusDays(3));
        insertTransaction(mainAccount.getId(), Transaction.TYPE_EXPENSE,
                CAT_FOOD, Transaction.CLASSIFICATION_NEED, 30.0,
                LocalDateTime.now().minusDays(2));
        // Entertainment: 20
        insertTransaction(mainAccount.getId(), Transaction.TYPE_EXPENSE,
                CAT_ENTERTAINMENT, Transaction.CLASSIFICATION_WANT, 20.0,
                LocalDateTime.now().minusDays(1));

        CategoryExpenseData data = reportsService.loadCategoryExpenseData(ChartPeriod.TWELVE_MONTHS);

        assertEquals(100.0, data.getTotalExpense(), 0.001);
        assertEquals(2, data.getExpenseByCategory().size(), "2 unikátne kategórie");
        assertEquals(80.0, data.getExpenseByCategory().get("Food"), 0.001,
                "agregát Food = 50 + 30 = 80");
        assertEquals(20.0, data.getExpenseByCategory().get("Entertainment"), 0.001);
    }

    @Test
    @DisplayName("Naprieč viacerými účtami sa rovnaká kategória SPÁJA pod 1 kľúč")
    void mergesAcrossAccounts() {
        // Food na main 50
        insertTransaction(mainAccount.getId(), Transaction.TYPE_EXPENSE,
                CAT_FOOD, Transaction.CLASSIFICATION_NEED, 50.0,
                LocalDateTime.now().minusDays(3));
        // Food na saving 30
        insertTransaction(savingAccount.getId(), Transaction.TYPE_EXPENSE,
                CAT_FOOD, Transaction.CLASSIFICATION_NEED, 30.0,
                LocalDateTime.now().minusDays(2));

        CategoryExpenseData data = reportsService.loadCategoryExpenseData(ChartPeriod.TWELVE_MONTHS);

        assertEquals(80.0, data.getTotalExpense(), 0.001);
        assertEquals(1, data.getExpenseByCategory().size(),
                "Food zo 2 účtov sa má spojiť pod jeden kľúč");
        assertEquals(80.0, data.getExpenseByCategory().get("Food"), 0.001);
    }

    // ====================== EXCLUSIONS ======================

    @Test
    @DisplayName("Transfer expense (CATEGORY_SAVING_EXPENSE=7) NIE JE v reporte")
    void excludesTransferCategory() {
        insertTransaction(mainAccount.getId(), Transaction.TYPE_EXPENSE,
                CAT_FOOD, Transaction.CLASSIFICATION_NEED, 100.0,
                LocalDateTime.now().minusDays(3));
        // Transfer expense — kategória 7 (Saving Expense)
        insertTransaction(mainAccount.getId(), Transaction.TYPE_EXPENSE,
                Transaction.CATEGORY_SAVING_EXPENSE, null, 500.0,
                LocalDateTime.now().minusDays(3));

        CategoryExpenseData data = reportsService.loadCategoryExpenseData(ChartPeriod.TWELVE_MONTHS);

        assertEquals(100.0, data.getTotalExpense(), 0.001);
        assertFalse(data.getExpenseByCategory().containsKey("Saving Expense"),
                "kategória 'Saving Expense' nesmie byť v report mape");
    }

    @Test
    @DisplayName("INCOME transakcie sú EXKLUDOVANÉ (sumByCategoryAndDateRange filtruje TYPE_EXPENSE)")
    void onlyExpenseType() {
        // INCOME — nesmie byť v reporte
        insertTransaction(mainAccount.getId(), Transaction.TYPE_INCOME,
                CAT_SALARY, null, 1000.0, LocalDateTime.now().minusDays(3));
        // EXPENSE — má byť v reporte
        insertTransaction(mainAccount.getId(), Transaction.TYPE_EXPENSE,
                CAT_FOOD, Transaction.CLASSIFICATION_NEED, 50.0,
                LocalDateTime.now().minusDays(3));

        CategoryExpenseData data = reportsService.loadCategoryExpenseData(ChartPeriod.TWELVE_MONTHS);

        assertEquals(50.0, data.getTotalExpense(), 0.001);
        assertEquals(1, data.getExpenseByCategory().size());
        assertFalse(data.getExpenseByCategory().containsKey("Salary"));
    }

    // ====================== TOP-5 + OTHER ======================

    @Test
    @DisplayName("Presne 5 kategórií — žiadne 'Other' (limit je 5)")
    void exactly5Categories_noOther() {
        // 5 rôznych kategórií
        insertTransaction(mainAccount.getId(), Transaction.TYPE_EXPENSE, CAT_FOOD,
                Transaction.CLASSIFICATION_NEED, 100.0, LocalDateTime.now().minusDays(3));
        insertTransaction(mainAccount.getId(), Transaction.TYPE_EXPENSE, CAT_CLOTHING,
                Transaction.CLASSIFICATION_WANT, 90.0, LocalDateTime.now().minusDays(3));
        insertTransaction(mainAccount.getId(), Transaction.TYPE_EXPENSE, CAT_ENTERTAINMENT,
                Transaction.CLASSIFICATION_WANT, 80.0, LocalDateTime.now().minusDays(3));
        insertTransaction(mainAccount.getId(), Transaction.TYPE_EXPENSE, CAT_RENT,
                Transaction.CLASSIFICATION_NEED, 70.0, LocalDateTime.now().minusDays(3));
        insertTransaction(mainAccount.getId(), Transaction.TYPE_EXPENSE, CAT_TRANSPORT,
                Transaction.CLASSIFICATION_NEED, 60.0, LocalDateTime.now().minusDays(3));

        CategoryExpenseData data = reportsService.loadCategoryExpenseData(ChartPeriod.TWELVE_MONTHS);

        assertEquals(5, data.getExpenseByCategory().size(),
                "presne 5 kategórií = 5 kľúčov, žiadne Other");
        assertFalse(data.getExpenseByCategory().containsKey("Other"));
        assertEquals(400.0, data.getTotalExpense(), 0.001);
    }

    @Test
    @DisplayName("6 kategórií — TOP 4 + 'Other' (zvyšok agreguje)")
    void moreThan5_collapsedToOther() {
        // 6 kategórií so sumami: 100, 90, 80, 70, 60, 50
        // Očakávaný výsledok podľa limitToTopCategories: TOP 4 (100, 90, 80, 70) + Other (60+50=110)
        insertTransaction(mainAccount.getId(), Transaction.TYPE_EXPENSE, CAT_FOOD,
                Transaction.CLASSIFICATION_NEED, 100.0, LocalDateTime.now().minusDays(3));
        insertTransaction(mainAccount.getId(), Transaction.TYPE_EXPENSE, CAT_CLOTHING,
                Transaction.CLASSIFICATION_WANT, 90.0, LocalDateTime.now().minusDays(3));
        insertTransaction(mainAccount.getId(), Transaction.TYPE_EXPENSE, CAT_ENTERTAINMENT,
                Transaction.CLASSIFICATION_WANT, 80.0, LocalDateTime.now().minusDays(3));
        insertTransaction(mainAccount.getId(), Transaction.TYPE_EXPENSE, CAT_RENT,
                Transaction.CLASSIFICATION_NEED, 70.0, LocalDateTime.now().minusDays(3));
        insertTransaction(mainAccount.getId(), Transaction.TYPE_EXPENSE, CAT_TRANSPORT,
                Transaction.CLASSIFICATION_NEED, 60.0, LocalDateTime.now().minusDays(3));
        insertTransaction(mainAccount.getId(), Transaction.TYPE_EXPENSE, CAT_INVESTMENT,
                Transaction.CLASSIFICATION_WANT, 50.0, LocalDateTime.now().minusDays(3));

        CategoryExpenseData data = reportsService.loadCategoryExpenseData(ChartPeriod.TWELVE_MONTHS);

        Map<String, Double> map = data.getExpenseByCategory();
        assertEquals(5, map.size(), "TOP 4 + Other = 5 kľúčov");
        assertTrue(map.containsKey("Other"), "musí existovať kľúč 'Other'");
        assertEquals(110.0, map.get("Other"), 0.001,
                "Other = 60 + 50 = 110");

        // Total musí ostať pôvodný (Other zlúči nie zmaže)
        assertEquals(450.0, data.getTotalExpense(), 0.001);
    }

    // ====================== DATE BOUNDARY ======================

    @Test
    @DisplayName("ONE_WEEK perióda — staršie expense ako 7 dní sú vynechané")
    void oneWeek_excludesOlder() {
        // Mladšie ako 7 dní — započítané
        insertTransaction(mainAccount.getId(), Transaction.TYPE_EXPENSE, CAT_FOOD,
                Transaction.CLASSIFICATION_NEED, 50.0, LocalDateTime.now().minusDays(2));
        // Staršie — vynechané
        insertTransaction(mainAccount.getId(), Transaction.TYPE_EXPENSE, CAT_FOOD,
                Transaction.CLASSIFICATION_NEED, 9999.0, LocalDateTime.now().minusDays(20));

        CategoryExpenseData data = reportsService.loadCategoryExpenseData(ChartPeriod.ONE_WEEK);

        assertEquals(50.0, data.getTotalExpense(), 0.001,
                "iba transakcia v perióde sa zaratá");
    }

    // ====================== CATEGORY NAME MAPPING ======================

    @Test
    @DisplayName("JOIN s categories: kľúč v mape je 'name' z DB, nie ID")
    void joinReturnsActualCategoryName() {
        insertTransaction(mainAccount.getId(), Transaction.TYPE_EXPENSE, CAT_FOOD,
                Transaction.CLASSIFICATION_NEED, 50.0, LocalDateTime.now().minusDays(3));

        CategoryExpenseData data = reportsService.loadCategoryExpenseData(ChartPeriod.TWELVE_MONTHS);

        assertTrue(data.getExpenseByCategory().containsKey("Food"),
                "kľúčom má byť názov kategórie 'Food', nie '1'");
    }
}
