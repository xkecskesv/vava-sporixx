package sk.sporixx.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sk.sporixx.dto.ChartPeriod;
import sk.sporixx.dto.WantNeedData;
import sk.sporixx.model.Transaction;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy pre {@link ReportsService#loadWantNeedData(ChartPeriod)}.
 *
 * Pokrývame:
 *   - prázdne dáta — žiadne percentuálne delenie nulou
 *   - počítanie WANT vs NEED
 *   - percentuálne výpočty
 *   - exclusion null classification
 */
class ReportsServiceWantNeedTest extends ReportsServiceTestSupport {

    @Test
    @DisplayName("Prázdne dáta — všetky polia 0 (no division by zero)")
    void empty_returnsZerosNoDivisionByZero() {
        WantNeedData data = reportsService.loadWantNeedData(ChartPeriod.TWELVE_MONTHS);

        assertNotNull(data);
        assertEquals(0.0, data.getTotalWant(), 0.001);
        assertEquals(0.0, data.getTotalNeed(), 0.001);
        assertEquals(0.0, data.getWantPercentage(), 0.001,
                "delenie nulou = 0% (nie NaN)");
        assertEquals(0.0, data.getNeedPercentage(), 0.001);
    }

    @Test
    @DisplayName("100% NEED, 0% WANT")
    void allNeed() {
        tx(mainAccount.getId(), Transaction.TYPE_EXPENSE, 1, Transaction.CLASSIFICATION_NEED, 100.0,
                LocalDateTime.now().minusDays(3));

        WantNeedData data = reportsService.loadWantNeedData(ChartPeriod.TWELVE_MONTHS);

        assertEquals(100.0, data.getTotalNeed(), 0.001);
        assertEquals(0.0, data.getTotalWant(), 0.001);
        assertEquals(100.0, data.getNeedPercentage(), 0.001);
        assertEquals(0.0, data.getWantPercentage(), 0.001);
    }

    @Test
    @DisplayName("60/40 split want vs need")
    void mixedRatio() {
        tx(mainAccount.getId(), Transaction.TYPE_EXPENSE, 1, Transaction.CLASSIFICATION_WANT, 60.0,
                LocalDateTime.now().minusDays(3));
        tx(mainAccount.getId(), Transaction.TYPE_EXPENSE, 1, Transaction.CLASSIFICATION_NEED, 40.0,
                LocalDateTime.now().minusDays(2));

        WantNeedData data = reportsService.loadWantNeedData(ChartPeriod.TWELVE_MONTHS);

        assertEquals(60.0, data.getTotalWant(), 0.001);
        assertEquals(40.0, data.getTotalNeed(), 0.001);
        assertEquals(60.0, data.getWantPercentage(), 0.001);
        assertEquals(40.0, data.getNeedPercentage(), 0.001);
    }

    @Test
    @DisplayName("Percentá sumarizujú na 100 (alebo na 0 keď nie sú dáta)")
    void percentagesSum100() {
        tx(mainAccount.getId(), Transaction.TYPE_EXPENSE, 1, Transaction.CLASSIFICATION_WANT, 33.0,
                LocalDateTime.now().minusDays(3));
        tx(mainAccount.getId(), Transaction.TYPE_EXPENSE, 1, Transaction.CLASSIFICATION_NEED, 67.0,
                LocalDateTime.now().minusDays(2));

        WantNeedData data = reportsService.loadWantNeedData(ChartPeriod.TWELVE_MONTHS);

        double totalPercent = data.getWantPercentage() + data.getNeedPercentage();
        assertEquals(100.0, totalPercent, 0.001);
    }

    @Test
    @DisplayName("Transakcie s null classification sa nezapočítajú")
    void nullClassification_excluded() {
        // expense bez WANT/NEED (napr. starý záznam alebo transfer leg)
        tx(mainAccount.getId(), Transaction.TYPE_EXPENSE, 1, null, 200.0,
                LocalDateTime.now().minusDays(3));
        tx(mainAccount.getId(), Transaction.TYPE_EXPENSE, 1, Transaction.CLASSIFICATION_NEED, 50.0,
                LocalDateTime.now().minusDays(2));

        WantNeedData data = reportsService.loadWantNeedData(ChartPeriod.TWELVE_MONTHS);

        assertEquals(50.0, data.getTotalNeed(), 0.001);
        assertEquals(0.0, data.getTotalWant(), 0.001);
    }

    @Test
    @DisplayName("INCOME transakcie sa nezapočítajú (len EXPENSE)")
    void onlyExpense() {
        tx(mainAccount.getId(), Transaction.TYPE_INCOME, 2, null, 1000.0,
                LocalDateTime.now().minusDays(3));
        tx(mainAccount.getId(), Transaction.TYPE_EXPENSE, 1, Transaction.CLASSIFICATION_NEED, 50.0,
                LocalDateTime.now().minusDays(2));

        WantNeedData data = reportsService.loadWantNeedData(ChartPeriod.TWELVE_MONTHS);

        assertEquals(50.0, data.getTotalNeed(), 0.001);
    }

    @Test
    @DisplayName("Naprieč viacerými účtami")
    void acrossAccounts() {
        tx(mainAccount.getId(), Transaction.TYPE_EXPENSE, 1, Transaction.CLASSIFICATION_NEED, 100.0,
                LocalDateTime.now().minusDays(3));
        tx(savingAccount.getId(), Transaction.TYPE_EXPENSE, 1, Transaction.CLASSIFICATION_WANT, 50.0,
                LocalDateTime.now().minusDays(3));

        WantNeedData data = reportsService.loadWantNeedData(ChartPeriod.TWELVE_MONTHS);

        assertEquals(100.0, data.getTotalNeed(), 0.001);
        assertEquals(50.0, data.getTotalWant(), 0.001);
    }

    @Test
    @DisplayName("ONE_WEEK perióda — staršie sa nezapočítavajú")
    void oneWeek_excludesOlder() {
        tx(mainAccount.getId(), Transaction.TYPE_EXPENSE, 1, Transaction.CLASSIFICATION_NEED, 50.0,
                LocalDateTime.now().minusDays(2));
        tx(mainAccount.getId(), Transaction.TYPE_EXPENSE, 1, Transaction.CLASSIFICATION_NEED, 5000.0,
                LocalDateTime.now().minusMonths(2));

        WantNeedData data = reportsService.loadWantNeedData(ChartPeriod.ONE_WEEK);

        assertEquals(50.0, data.getTotalNeed(), 0.001);
    }
}
