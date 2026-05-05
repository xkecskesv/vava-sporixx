package sk.sporixx.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sk.sporixx.dto.CategoryExpenseData;
import sk.sporixx.dto.ChartPeriod;
import sk.sporixx.model.Transaction;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy pre {@link ReportsService#loadCategoryExpenseData(ChartPeriod)}.
 *
 * Pokrývame:
 *   - prázdne dáta
 *   - sumu po kategóriách
 *   - exclusion CATEGORY_SAVING / CATEGORY_SAVING_EXPENSE
 *   - exclusion INCOME (len expense)
 *   - "Other" kategória (limitToTopCategories) keď je >5 kategórií
 */
class ReportsServiceCategoryExpenseTest extends ReportsServiceTestSupport {

    @Test
    @DisplayName("Prázdne dáta — empty map a totalExpense = 0")
    void empty_returnsZero() {
        CategoryExpenseData data = reportsService.loadCategoryExpenseData(ChartPeriod.TWELVE_MONTHS);

        assertNotNull(data);
        assertEquals(0.0, data.getTotalExpense(), 0.001);
        assertTrue(data.getExpenseByCategory().isEmpty());
    }

    @Test
    @DisplayName("Sumarizácia po kategóriách")
    void groupsByCategory() {
        // 3 expense — Food: 50+30=80, Entertainment: 20
        tx(mainAccount.getId(), Transaction.TYPE_EXPENSE, 1, Transaction.CLASSIFICATION_NEED, 50.0,
                LocalDateTime.now().minusDays(3));
        tx(mainAccount.getId(), Transaction.TYPE_EXPENSE, 1, Transaction.CLASSIFICATION_NEED, 30.0,
                LocalDateTime.now().minusDays(2));
        tx(mainAccount.getId(), Transaction.TYPE_EXPENSE, 2, Transaction.CLASSIFICATION_WANT, 20.0,
                LocalDateTime.now().minusDays(1));

        CategoryExpenseData data = reportsService.loadCategoryExpenseData(ChartPeriod.TWELVE_MONTHS);

        assertEquals(100.0, data.getTotalExpense(), 0.001);
        assertEquals(2, data.getExpenseByCategory().size(),
                "máme 2 unikátne kategórie");
    }

    @Test
    @DisplayName("Exclusion: CATEGORY_SAVING_EXPENSE sa nezapočíta")
    void excludesTransferCategory() {
        tx(mainAccount.getId(), Transaction.TYPE_EXPENSE, 1, Transaction.CLASSIFICATION_NEED, 100.0,
                LocalDateTime.now().minusDays(3));
        tx(mainAccount.getId(), Transaction.TYPE_EXPENSE,
                Transaction.CATEGORY_SAVING_EXPENSE, null, 500.0,
                LocalDateTime.now().minusDays(3));

        CategoryExpenseData data = reportsService.loadCategoryExpenseData(ChartPeriod.TWELVE_MONTHS);

        assertEquals(100.0, data.getTotalExpense(), 0.001);
    }

    @Test
    @DisplayName("Iba TYPE_EXPENSE — INCOME sa nezapočítava")
    void onlyExpenseType() {
        tx(mainAccount.getId(), Transaction.TYPE_INCOME, 2, null, 1000.0,
                LocalDateTime.now().minusDays(3));
        tx(mainAccount.getId(), Transaction.TYPE_EXPENSE, 1, Transaction.CLASSIFICATION_NEED, 50.0,
                LocalDateTime.now().minusDays(3));

        CategoryExpenseData data = reportsService.loadCategoryExpenseData(ChartPeriod.TWELVE_MONTHS);

        assertEquals(50.0, data.getTotalExpense(), 0.001);
    }

    @Test
    @DisplayName("Top 4 + Other — keď je viac ako 5 kategórií")
    void moreThan5_collapsedIntoOther() {
        // 6 rôznych kategórií so sumami: 100, 90, 80, 70, 60, 50
        // Top 4: 100, 90, 80, 70 → samostatné
        // Zvyšok: 60+50=110 → Other
        for (int i = 1; i <= 6; i++) {
            double amount = 110.0 - i * 10;  // 100, 90, 80, 70, 60, 50
            tx(mainAccount.getId(), Transaction.TYPE_EXPENSE,
                    100 + i, Transaction.CLASSIFICATION_NEED, amount,
                    LocalDateTime.now().minusDays(3));
        }

        CategoryExpenseData data = reportsService.loadCategoryExpenseData(ChartPeriod.TWELVE_MONTHS);

        // 4 samostatné + Other = 5 položiek
        assertEquals(5, data.getExpenseByCategory().size());
        assertTrue(data.getExpenseByCategory().containsKey("Other"));
        assertEquals(110.0, data.getExpenseByCategory().get("Other"), 0.001);

        // total nezmenená — Other obsahuje sumu zvyšných
        assertEquals(450.0, data.getTotalExpense(), 0.001);
    }

    @Test
    @DisplayName("Presne 5 kategórií — žiadne Other (limit je 5)")
    void exactly5_noOther() {
        for (int i = 1; i <= 5; i++) {
            tx(mainAccount.getId(), Transaction.TYPE_EXPENSE,
                    100 + i, Transaction.CLASSIFICATION_NEED, 100.0 * i,
                    LocalDateTime.now().minusDays(3));
        }

        CategoryExpenseData data = reportsService.loadCategoryExpenseData(ChartPeriod.TWELVE_MONTHS);

        assertEquals(5, data.getExpenseByCategory().size());
        assertFalse(data.getExpenseByCategory().containsKey("Other"));
    }

    @Test
    @DisplayName("Naprieč viacerými účtami sa kategórie spájajú")
    void mergesAcrossAccounts() {
        // Food na main 50
        tx(mainAccount.getId(), Transaction.TYPE_EXPENSE, 1, Transaction.CLASSIFICATION_NEED, 50.0,
                LocalDateTime.now().minusDays(3));
        // Food na saving 30 (rovnaká kategória)
        tx(savingAccount.getId(), Transaction.TYPE_EXPENSE, 1, Transaction.CLASSIFICATION_NEED, 30.0,
                LocalDateTime.now().minusDays(3));

        CategoryExpenseData data = reportsService.loadCategoryExpenseData(ChartPeriod.TWELVE_MONTHS);

        assertEquals(80.0, data.getTotalExpense(), 0.001);
        assertEquals(1, data.getExpenseByCategory().size(),
                "rovnaká kategória zo 2 účtov sa spája");
    }

    @Test
    @DisplayName("ONE_WEEK perióda — staršie expense vynechané")
    void oneWeek_excludesOlder() {
        tx(mainAccount.getId(), Transaction.TYPE_EXPENSE, 1, Transaction.CLASSIFICATION_NEED, 50.0,
                LocalDateTime.now().minusDays(2));
        tx(mainAccount.getId(), Transaction.TYPE_EXPENSE, 1, Transaction.CLASSIFICATION_NEED, 1000.0,
                LocalDateTime.now().minusDays(20));

        CategoryExpenseData data = reportsService.loadCategoryExpenseData(ChartPeriod.ONE_WEEK);

        assertEquals(50.0, data.getTotalExpense(), 0.001);
    }
}
