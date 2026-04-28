package sk.sporixx.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sk.sporixx.dto.ChartPeriod;
import sk.sporixx.dto.WantNeedData;
import sk.sporixx.model.Transaction;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * INTEGRATION testy {@link ReportsService#loadWantNeedData(ChartPeriod)} proti reálnej JDBC DB.
 * <p>
 * Trieda overuje že JOIN s {@code spending_classification} funguje, že NULL
 * classification sa nesprávne nezaráta, a že percentuálne výpočty bezpečne riešia
 * delenie nulou.
 */
class ReportsServiceWantNeedJdbcTest extends ReportsServiceJdbcTestSupport {

    private static final int CAT_FOOD = 1;
    private static final int CAT_SALARY = 9;

    // ====================== ZÁKLADNÉ ======================

    @Test
    @DisplayName("Prázdne dáta — všetky polia 0, žiadne NaN")
    void empty_returnsZerosNoDivisionByZero() {
        WantNeedData data = reportsService.loadWantNeedData(ChartPeriod.TWELVE_MONTHS);

        assertNotNull(data);
        assertEquals(0.0, data.getTotalWant(), 0.001);
        assertEquals(0.0, data.getTotalNeed(), 0.001);
        assertEquals(0.0, data.getWantPercentage(), 0.001);
        assertEquals(0.0, data.getNeedPercentage(), 0.001);
        assertFalse(Double.isNaN(data.getWantPercentage()),
                "delenie nulou MUSÍ byť ošetrené (no NaN)");
        assertFalse(Double.isNaN(data.getNeedPercentage()));
    }

    @Test
    @DisplayName("Iba NEED výdavky — wantPercentage=0, needPercentage=100")
    void allNeed_yields100PercentNeed() {
        insertTransaction(mainAccount.getId(), Transaction.TYPE_EXPENSE, CAT_FOOD,
                Transaction.CLASSIFICATION_NEED, 100.0, LocalDateTime.now().minusDays(3));

        WantNeedData data = reportsService.loadWantNeedData(ChartPeriod.TWELVE_MONTHS);

        assertEquals(100.0, data.getTotalNeed(), 0.001);
        assertEquals(0.0, data.getTotalWant(), 0.001);
        assertEquals(100.0, data.getNeedPercentage(), 0.001);
        assertEquals(0.0, data.getWantPercentage(), 0.001);
    }

    @Test
    @DisplayName("60/40 NEED/WANT split — percentá zodpovedajú")
    void mixedRatio_calculatesPercentagesCorrectly() {
        insertTransaction(mainAccount.getId(), Transaction.TYPE_EXPENSE, CAT_FOOD,
                Transaction.CLASSIFICATION_NEED, 60.0, LocalDateTime.now().minusDays(3));
        insertTransaction(mainAccount.getId(), Transaction.TYPE_EXPENSE, CAT_FOOD,
                Transaction.CLASSIFICATION_WANT, 40.0, LocalDateTime.now().minusDays(2));

        WantNeedData data = reportsService.loadWantNeedData(ChartPeriod.TWELVE_MONTHS);

        assertEquals(60.0, data.getTotalNeed(), 0.001);
        assertEquals(40.0, data.getTotalWant(), 0.001);
        assertEquals(60.0, data.getNeedPercentage(), 0.001);
        assertEquals(40.0, data.getWantPercentage(), 0.001);
    }

    @Test
    @DisplayName("Percentá sumarizujú na 100% (alebo 0 keď nie sú dáta)")
    void percentagesSumTo100() {
        insertTransaction(mainAccount.getId(), Transaction.TYPE_EXPENSE, CAT_FOOD,
                Transaction.CLASSIFICATION_NEED, 33.33, LocalDateTime.now().minusDays(3));
        insertTransaction(mainAccount.getId(), Transaction.TYPE_EXPENSE, CAT_FOOD,
                Transaction.CLASSIFICATION_WANT, 66.67, LocalDateTime.now().minusDays(2));

        WantNeedData data = reportsService.loadWantNeedData(ChartPeriod.TWELVE_MONTHS);

        double sum = data.getNeedPercentage() + data.getWantPercentage();
        assertEquals(100.0, sum, 0.001, "want% + need% = 100");
    }

    // ====================== EXCLUSIONS ======================

    @Test
    @DisplayName("Transakcia s NULL classification NESMIE byť započítaná")
    void nullClassification_excluded() {
        // Bez classification (klasický transfer)
        insertTransaction(mainAccount.getId(), Transaction.TYPE_EXPENSE, CAT_FOOD,
                null, 200.0, LocalDateTime.now().minusDays(3));
        // S NEED
        insertTransaction(mainAccount.getId(), Transaction.TYPE_EXPENSE, CAT_FOOD,
                Transaction.CLASSIFICATION_NEED, 50.0, LocalDateTime.now().minusDays(2));

        WantNeedData data = reportsService.loadWantNeedData(ChartPeriod.TWELVE_MONTHS);

        assertEquals(50.0, data.getTotalNeed(), 0.001);
        assertEquals(0.0, data.getTotalWant(), 0.001,
                "transakcia s NULL classification sa nesmie počítať");
    }

    @Test
    @DisplayName("INCOME transakcie sa NEZAPOČÍTAVAJÚ (len EXPENSE má want/need)")
    void incomeNotCounted() {
        // Income s classification — divné, ale niekto by to mohol omylom uložiť
        insertTransaction(mainAccount.getId(), Transaction.TYPE_INCOME, CAT_SALARY,
                Transaction.CLASSIFICATION_NEED, 1000.0, LocalDateTime.now().minusDays(3));
        // Expense s NEED
        insertTransaction(mainAccount.getId(), Transaction.TYPE_EXPENSE, CAT_FOOD,
                Transaction.CLASSIFICATION_NEED, 50.0, LocalDateTime.now().minusDays(2));

        WantNeedData data = reportsService.loadWantNeedData(ChartPeriod.TWELVE_MONTHS);

        assertEquals(50.0, data.getTotalNeed(), 0.001,
                "iba TYPE_EXPENSE s NEED sa zaratá, INCOME sa ignoruje");
    }

    // ====================== ACROSS ACCOUNTS ======================

    @Test
    @DisplayName("Naprieč viacerými účtami sa want/need správne agreguje")
    void aggregatesAcrossAccounts() {
        // NEED na main 100
        insertTransaction(mainAccount.getId(), Transaction.TYPE_EXPENSE, CAT_FOOD,
                Transaction.CLASSIFICATION_NEED, 100.0, LocalDateTime.now().minusDays(3));
        // WANT na saving 50
        insertTransaction(savingAccount.getId(), Transaction.TYPE_EXPENSE, CAT_FOOD,
                Transaction.CLASSIFICATION_WANT, 50.0, LocalDateTime.now().minusDays(3));

        WantNeedData data = reportsService.loadWantNeedData(ChartPeriod.TWELVE_MONTHS);

        assertEquals(100.0, data.getTotalNeed(), 0.001);
        assertEquals(50.0, data.getTotalWant(), 0.001);
    }

    // ====================== DATE BOUNDARY ======================

    @Test
    @DisplayName("ONE_WEEK — staršie transakcie sa nezarátajú")
    void oneWeek_excludesOlder() {
        // Mladšia
        insertTransaction(mainAccount.getId(), Transaction.TYPE_EXPENSE, CAT_FOOD,
                Transaction.CLASSIFICATION_NEED, 50.0, LocalDateTime.now().minusDays(2));
        // Staršia
        insertTransaction(mainAccount.getId(), Transaction.TYPE_EXPENSE, CAT_FOOD,
                Transaction.CLASSIFICATION_NEED, 5000.0, LocalDateTime.now().minusMonths(2));

        WantNeedData data = reportsService.loadWantNeedData(ChartPeriod.ONE_WEEK);

        assertEquals(50.0, data.getTotalNeed(), 0.001);
    }
}
