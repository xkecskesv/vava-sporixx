package sk.sporixx.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import sk.sporixx.dto.AnalyticsData;
import sk.sporixx.dto.ChartPeriod;
import sk.sporixx.model.Transaction;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy pre {@link OverviewService#loadAnalytics(ChartPeriod, int)}.
 *
 * Pokrývame:
 *   - prázdne dáta → totalIncome = 0, chartData prázdna mapa
 *   - správny výpočet totalIncome
 *   - filtrovanie podľa accountId (cudzie transakcie sa neobjavujú)
 *   - groupovanie po dňoch (ONE_WEEK, ONE_MONTH)
 *   - groupovanie po mesiacoch (SIX_MONTHS, TWELVE_MONTHS)
 *   - výdavky sa do analytics NEpočítajú (len income)
 *   - transakcia mimo obdobia sa ignoruje
 */
@DisplayName("OverviewService – loadAnalytics()")
class OverviewServiceAnalyticsTest extends OverviewServiceTestSupport {

    // ======================== PRÁZDNE DÁTA ========================

    @Nested
    @DisplayName("Prázdne dáta")
    class EmptyData {

        @Test
        @DisplayName("žiadne transakcie → totalIncome = 0 a chartData je prázdna")
        void noTransactions_zeroIncome_emptyChart() {
            AnalyticsData data = overviewService.loadAnalytics(ChartPeriod.ONE_MONTH, mainAccount.getId());

            assertEquals(0.0, data.getTotalIncome(), 0.001);
            assertTrue(data.getChartData().isEmpty());
        }

        @Test
        @DisplayName("len výdavky → totalIncome = 0 (výdavky sa do analytics nepočítajú)")
        void onlyExpenses_zeroIncome() {
            LocalDateTime now = LocalDateTime.now();
            addExpense(mainAccount.getId(), 200.0, now);
            addExpense(mainAccount.getId(), 50.0, now.minusDays(3));

            AnalyticsData data = overviewService.loadAnalytics(ChartPeriod.ONE_MONTH, mainAccount.getId());

            assertEquals(0.0, data.getTotalIncome(), 0.001,
                    "Výdavky sa do analytics NEpočítajú");
            assertTrue(data.getChartData().isEmpty());
        }
    }

    // ======================== TOTALINCOME ========================

    @Nested
    @DisplayName("Total income výpočet")
    class TotalIncomeCalculation {

        @Test
        @DisplayName("jedna income transakcia → totalIncome = jej amount")
        void singleIncome_correctTotal() {
            addIncome(mainAccount.getId(), 500.0, LocalDateTime.now().minusDays(5));

            AnalyticsData data = overviewService.loadAnalytics(ChartPeriod.ONE_MONTH, mainAccount.getId());

            assertEquals(500.0, data.getTotalIncome(), 0.001);
        }

        @Test
        @DisplayName("viac income transakcií → totalIncome = ich súčet")
        void multipleIncomes_summedCorrectly() {
            LocalDateTime now = LocalDateTime.now();
            addIncome(mainAccount.getId(), 1000.0, now.minusDays(2));
            addIncome(mainAccount.getId(), 500.0, now.minusDays(10));
            addIncome(mainAccount.getId(), 250.0, now.minusDays(20));

            AnalyticsData data = overviewService.loadAnalytics(ChartPeriod.ONE_MONTH, mainAccount.getId());

            assertEquals(1750.0, data.getTotalIncome(), 0.001);
        }

        @Test
        @DisplayName("income + expense → totalIncome = len income (expense ignorovaný)")
        void mixedTransactions_onlyIncomeInTotal() {
            LocalDateTime now = LocalDateTime.now();
            addIncome(mainAccount.getId(), 1000.0, now.minusDays(5));
            addExpense(mainAccount.getId(), 300.0, now.minusDays(3));

            AnalyticsData data = overviewService.loadAnalytics(ChartPeriod.ONE_MONTH, mainAccount.getId());

            assertEquals(1000.0, data.getTotalIncome(), 0.001,
                    "Expense sa do totalIncome NEZAPOČÍTAVA");
        }
    }

    // ======================== FILTROVANIE PODĽA ÚČTU ========================

    @Nested
    @DisplayName("Filtrovanie podľa accountId")
    class AccountFiltering {

        @Test
        @DisplayName("income pre iný účet sa nezobrazí")
        void otherAccountIncome_notIncluded() {
            LocalDateTime now = LocalDateTime.now();
            addIncome(mainAccount.getId(), 1000.0, now.minusDays(5));
            addIncome(emergencyAccount.getId(), 9999.0, now.minusDays(3)); // iný účet

            AnalyticsData data = overviewService.loadAnalytics(ChartPeriod.ONE_MONTH, mainAccount.getId());

            assertEquals(1000.0, data.getTotalIncome(), 0.001,
                    "Iba transakcie mainAccount musia byť v analytics");
        }

        @Test
        @DisplayName("analytics pre emergencyAccount vrátia len jeho transakcie")
        void emergencyAccount_isolatedData() {
            LocalDateTime now = LocalDateTime.now();
            addIncome(mainAccount.getId(), 500.0, now.minusDays(5));
            addIncome(emergencyAccount.getId(), 200.0, now.minusDays(3));

            AnalyticsData data = overviewService.loadAnalytics(ChartPeriod.ONE_MONTH, emergencyAccount.getId());

            assertEquals(200.0, data.getTotalIncome(), 0.001);
        }
    }

    // ======================== GROUPOVANIE PO DŇOCH ========================

    @Nested
    @DisplayName("Groupovanie po dňoch (ONE_WEEK, ONE_MONTH)")
    class GroupByDay {

        @Test
        @DisplayName("ONE_WEEK: 2 income v rôznych dňoch → 2 záznamy v chartData")
        void oneWeek_twoDifferentDays_twoEntries() {
            LocalDateTime now = LocalDateTime.now();
            addIncome(mainAccount.getId(), 100.0, now.minusDays(1));
            addIncome(mainAccount.getId(), 200.0, now.minusDays(3));

            AnalyticsData data = overviewService.loadAnalytics(ChartPeriod.ONE_WEEK, mainAccount.getId());

            assertEquals(2, data.getChartData().size(),
                    "2 rôzne dni → 2 záznamy v chartData");
        }

        @Test
        @DisplayName("ONE_MONTH: 2 income v rovnaký deň → 1 záznam, sú sčítané")
        void oneMonth_sameDayIncomes_aggregated() {
            LocalDateTime today = LocalDateTime.now().withHour(10);
            addIncome(mainAccount.getId(), 100.0, today);
            addIncome(mainAccount.getId(), 300.0, today.withHour(15));

            AnalyticsData data = overviewService.loadAnalytics(ChartPeriod.ONE_MONTH, mainAccount.getId());

            assertEquals(1, data.getChartData().size(),
                    "Rovnaký deň → 1 aggregovaný záznam");
            double dayTotal = data.getChartData().values().iterator().next();
            assertEquals(400.0, dayTotal, 0.001);
        }

        @Test
        @DisplayName("ONE_WEEK: transakcia spred 2 týždňov sa nezobrazí")
        void oneWeek_oldTransaction_excluded() {
            LocalDateTime now = LocalDateTime.now();
            addIncome(mainAccount.getId(), 500.0, now.minusWeeks(2)); // mimo ONE_WEEK

            AnalyticsData data = overviewService.loadAnalytics(ChartPeriod.ONE_WEEK, mainAccount.getId());

            assertEquals(0.0, data.getTotalIncome(), 0.001,
                    "Transakcia staršia ako 1 týždeň sa nesmie objaviť v ONE_WEEK");
        }
    }

    // ======================== GROUPOVANIE PO MESIACOCH ========================

    @Nested
    @DisplayName("Groupovanie po mesiacoch (SIX_MONTHS, TWELVE_MONTHS)")
    class GroupByMonth {

        @Test
        @DisplayName("SIX_MONTHS: 2 income v rôznych mesiacoch → 2 záznamy")
        void sixMonths_differentMonths_twoEntries() {
            LocalDateTime now = LocalDateTime.now();
            addIncome(mainAccount.getId(), 1000.0, now.minusMonths(1));
            addIncome(mainAccount.getId(), 800.0, now.minusMonths(3));

            AnalyticsData data = overviewService.loadAnalytics(ChartPeriod.SIX_MONTHS, mainAccount.getId());

            assertEquals(2, data.getChartData().size());
        }

        @Test
        @DisplayName("TWELVE_MONTHS: 2 income v rovnakom mesiaci → 1 záznam, sú sčítané")
        void twelveMonths_sameMonth_aggregated() {
            LocalDateTime base = LocalDateTime.now().minusMonths(2).withDayOfMonth(5);
            addIncome(mainAccount.getId(), 2000.0, base);
            addIncome(mainAccount.getId(), 1000.0, base.withDayOfMonth(20));

            AnalyticsData data = overviewService.loadAnalytics(ChartPeriod.TWELVE_MONTHS, mainAccount.getId());

            assertEquals(1, data.getChartData().size(),
                    "Rovnaký mesiac → 1 aggregovaný záznam");
            double monthTotal = data.getChartData().values().iterator().next();
            assertEquals(3000.0, monthTotal, 0.001);
        }

        @Test
        @DisplayName("SIX_MONTHS: transakcia spred 7 mesiacov sa nezobrazí")
        void sixMonths_tooOldTransaction_excluded() {
            LocalDateTime now = LocalDateTime.now();
            addIncome(mainAccount.getId(), 9999.0, now.minusMonths(7)); // mimo SIX_MONTHS

            AnalyticsData data = overviewService.loadAnalytics(ChartPeriod.SIX_MONTHS, mainAccount.getId());

            assertEquals(0.0, data.getTotalIncome(), 0.001,
                    "Transakcia staršia ako 6 mesiacov sa nesmie objaviť v SIX_MONTHS");
        }

        @Test
        @DisplayName("TWELVE_MONTHS: transakcia spred 13 mesiacov sa nezobrazí")
        void twelveMonths_tooOldTransaction_excluded() {
            LocalDateTime now = LocalDateTime.now();
            addIncome(mainAccount.getId(), 9999.0, now.minusMonths(13));

            AnalyticsData data = overviewService.loadAnalytics(ChartPeriod.TWELVE_MONTHS, mainAccount.getId());

            assertEquals(0.0, data.getTotalIncome(), 0.001);
        }
    }

    // ======================== ChartPeriod metadáta ========================

    @Nested
    @DisplayName("ChartPeriod v response")
    class ChartPeriodMeta {

        @Test
        @DisplayName("loadAnalytics vracia správny ChartPeriod")
        void chartPeriod_matchesRequest() {
            AnalyticsData data = overviewService.loadAnalytics(ChartPeriod.SIX_MONTHS, mainAccount.getId());
            assertEquals(ChartPeriod.SIX_MONTHS, data.getChartPeriod());
        }

        @Test
        @DisplayName("ONE_WEEK groupByDay = true")
        void oneWeek_isGroupByDay() {
            assertTrue(ChartPeriod.ONE_WEEK.isGroupByDay());
        }

        @Test
        @DisplayName("TWELVE_MONTHS groupByDay = false")
        void twelveMonths_isNotGroupByDay() {
            assertFalse(ChartPeriod.TWELVE_MONTHS.isGroupByDay());
        }
    }
}

