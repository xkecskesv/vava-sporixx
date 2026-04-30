package sk.sporixx.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sk.sporixx.dto.SavingAccountReportData;
import sk.sporixx.model.Account;
import sk.sporixx.model.Role;
import sk.sporixx.model.Transaction;
import sk.sporixx.model.User;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy pre {@link ReportsService#loadSavingAccountsData()}.
 *
 * Pokrývame:
 *   - prázdny stav (žiadne saving účty)
 *   - DAY/MONTH/YEAR grouping podľa dĺžky goalu
 *   - Expected progress (lineárny od init balance po target)
 *   - Actual progress (kumulatívny zo skutočných incomeov)
 *   - Skip saving account ktorý nemá active goal
 *   - savedUp = currentAmount, needToSave = target - savedUp
 *   - needToSave clampnutá na 0 keď savedUp > target
 */
class ReportsServiceSavingAccountsTest extends ReportsServiceTestSupport {

    @Test
    @DisplayName("Žiadne saving účty — prázdny zoznam")
    void noSavingAccounts_returnsEmpty() {
        // Prepíšeme session — iba main account, žiadny saving
        SessionManager.getInstance().clearSession();
        User u = User.builder().id(1).email("x@x.sk").firstName("X")
                .lastName("Y").role(Role.USER).isActive(true).build();
        Account onlyMain = Account.builder()
                .id(99).ownerUserId(1).accountTypeId(Account.MAIN_ACCOUNT)
                .currentBalance(100).initialBalance(100).isActive(true)
                .createdAt(LocalDateTime.now().minusYears(1)).build();
        SessionManager.getInstance().setSession(u, List.of(onlyMain));

        List<SavingAccountReportData> result = reportsService.loadSavingAccountsData();
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Saving účet bez aktívneho goalu — skipnutý (nezahrnutý do report listu)")
    void savingAccountWithoutGoal_skipped() {
        // Žiadny goal nie je vytvorený pre savingAccount
        List<SavingAccountReportData> result = reportsService.loadSavingAccountsData();

        assertTrue(result.isEmpty(),
                "saving účet bez goalu sa skipne s warn logom");
    }

    @Test
    @DisplayName("Základné polia: savedUp, needToSave, target")
    void basicFields() {
        goal(savingAccount.getId(), 10000.0, 3000.0,
                LocalDateTime.now().minusMonths(2),
                LocalDateTime.now().plusMonths(10));

        List<SavingAccountReportData> result = reportsService.loadSavingAccountsData();

        assertEquals(1, result.size());
        SavingAccountReportData d = result.get(0);
        assertEquals(savingAccount.getId(), d.getAccountId());
        assertEquals(3000.0, d.getSavedUp(), 0.001);
        assertEquals(7000.0, d.getNeedToSave(), 0.001, "10000 - 3000 = 7000");
        assertEquals(10000.0, d.getTargetAmount(), 0.001);
    }

    @Test
    @DisplayName("needToSave clampnutý na 0 keď je savedUp > target (over-saved)")
    void overSaved_needToSaveClampedToZero() {
        // savedUp=12000 > target=10000 → needToSave by bolo -2000, ale Math.max(0, ..)
        goal(savingAccount.getId(), 10000.0, 12000.0,
                LocalDateTime.now().minusMonths(2),
                LocalDateTime.now().plusMonths(10));

        List<SavingAccountReportData> result = reportsService.loadSavingAccountsData();

        assertEquals(0.0, result.get(0).getNeedToSave(), 0.001);
    }

    // ============ GROUPING ============

    @Test
    @DisplayName("Goal kratší ako 90 dní → grouping = DAY")
    void shortGoal_groupedByDay() {
        // Total dni = 60 < 90 → DAY
        goal(savingAccount.getId(), 10000.0, 0.0,
                LocalDateTime.now().minusDays(30),
                LocalDateTime.now().plusDays(30));

        List<SavingAccountReportData> result = reportsService.loadSavingAccountsData();

        assertEquals("DAY", result.get(0).getProgressGrouping());
    }

    @Test
    @DisplayName("Goal medzi 90 a 1825 dni → grouping = MONTH")
    void mediumGoal_groupedByMonth() {
        // Total = 365 dni → MONTH
        goal(savingAccount.getId(), 10000.0, 0.0,
                LocalDateTime.now().minusMonths(2),
                LocalDateTime.now().plusMonths(10));

        List<SavingAccountReportData> result = reportsService.loadSavingAccountsData();

        assertEquals("MONTH", result.get(0).getProgressGrouping());
    }

    @Test
    @DisplayName("Goal dlhší ako 1825 dni (~5 rokov) → grouping = YEAR")
    void longGoal_groupedByYear() {
        // Total = 10 rokov ≈ 3650 dni → YEAR
        goal(savingAccount.getId(), 100000.0, 0.0,
                LocalDateTime.now().minusYears(2),
                LocalDateTime.now().plusYears(8));

        List<SavingAccountReportData> result = reportsService.loadSavingAccountsData();

        assertEquals("YEAR", result.get(0).getProgressGrouping());
    }

    @Test
    @DisplayName("Hranica 90 dni — totalDays == 90 → DAY (≤ threshold)")
    void exactly90Days_isDay() {
        // resolveGrouping: totalDays <= 90 → DAY
        goal(savingAccount.getId(), 10000.0, 0.0,
                LocalDateTime.now().minusDays(45),
                LocalDateTime.now().plusDays(45));

        List<SavingAccountReportData> result = reportsService.loadSavingAccountsData();

        assertEquals("DAY", result.get(0).getProgressGrouping());
    }

    // ============ EXPECTED PROGRESS ============

    @Test
    @DisplayName("Expected progress je lineárny — start = initialBalance, end = target")
    void expectedProgress_isLinear() {
        goal(savingAccount.getId(), 10000.0, 0.0,
                LocalDateTime.now().minusMonths(2),
                LocalDateTime.now().plusMonths(10));

        List<SavingAccountReportData> result = reportsService.loadSavingAccountsData();
        var expected = result.get(0).getExpectedProgress();

        assertFalse(expected.isEmpty());

        // Prvý bod = initialBalance
        double firstValue = expected.values().iterator().next();
        assertEquals(savingAccount.getInitialBalance(), firstValue, 0.001,
                "expected[0] = initialBalance");

        // Posledný bod = targetAmount
        double lastValue = expected.values().stream()
                .reduce((a, b) -> b).orElseThrow();
        assertEquals(10000.0, lastValue, 0.001,
                "expected[last] = targetAmount");
    }

    @Test
    @DisplayName("Expected progress — keď je goal už splnený (initial >= target), iba 1 bod")
    void expectedProgress_alreadyMet_singlePoint() {
        // Saving má initial 500. Goal target = 100. → remainingToSave = -400 → already met
        goal(savingAccount.getId(), 100.0, 50.0,
                LocalDateTime.now().minusDays(30),
                LocalDateTime.now().plusDays(30));

        List<SavingAccountReportData> result = reportsService.loadSavingAccountsData();
        var expected = result.get(0).getExpectedProgress();

        assertEquals(1, expected.size(),
                "ak je remainingToSave <= 0, vráti sa 1 bod = targetAmount");
        assertEquals(100.0, expected.values().iterator().next(), 0.001);
    }

    // ============ ACTUAL PROGRESS ============

    @Test
    @DisplayName("Actual progress — kumulatívne sčítanie INCOME transakcií + initialBalance")
    void actualProgress_cumulative() {
        // goal: 10000, initial=500, started 2 mesiace dozadu, target o 10 mesiacov
        goal(savingAccount.getId(), 10000.0, 0.0,
                LocalDateTime.now().minusMonths(2),
                LocalDateTime.now().plusMonths(10));

        // 2 income transakcie na saving účte
        tx(savingAccount.getId(), Transaction.TYPE_INCOME, null, null, 1000.0,
                LocalDateTime.now().minusMonths(2).plusDays(5));
        tx(savingAccount.getId(), Transaction.TYPE_INCOME, null, null, 500.0,
                LocalDateTime.now().minusMonths(1).plusDays(5));

        List<SavingAccountReportData> result = reportsService.loadSavingAccountsData();
        var actual = result.get(0).getActualProgress();

        assertFalse(actual.isEmpty());

        // Posledná hodnota = initialBalance + sum(income) = 500 + 1000 + 500 = 2000
        double lastValue = actual.values().stream()
                .reduce((a, b) -> b).orElseThrow();
        assertEquals(2000.0, lastValue, 0.001,
                "kumulatívna suma od initialBalance");
    }

    @Test
    @DisplayName("Actual progress — bez incomeov vracia plateau na initialBalance")
    void actualProgress_noIncome_flatAtInitial() {
        goal(savingAccount.getId(), 10000.0, 0.0,
                LocalDateTime.now().minusMonths(2),
                LocalDateTime.now().plusMonths(10));

        List<SavingAccountReportData> result = reportsService.loadSavingAccountsData();
        var actual = result.get(0).getActualProgress();

        // Bez incomeov: všetky body sa rovnajú initialBalance (500)
        assertTrue(actual.values().stream()
                .allMatch(v -> Math.abs(v - 500.0) < 0.001),
                "bez incomeov je actual flat na initialBalance");
    }

    @Test
    @DisplayName("Transactions list obsahuje len TYPE_INCOME pre tento saving účet")
    void transactionsList_filteredByIncomeAndAccount() {
        goal(savingAccount.getId(), 10000.0, 0.0,
                LocalDateTime.now().minusMonths(2),
                LocalDateTime.now().plusMonths(10));

        // 1 income na saving — má sa objaviť
        tx(savingAccount.getId(), Transaction.TYPE_INCOME, null, null, 100.0,
                LocalDateTime.now().minusDays(5));
        // 1 expense na saving — NEmá sa objaviť
        tx(savingAccount.getId(), Transaction.TYPE_EXPENSE, null, null, 50.0,
                LocalDateTime.now().minusDays(4));
        // 1 income na main — NEmá sa objaviť (iný účet)
        tx(mainAccount.getId(), Transaction.TYPE_INCOME, null, null, 200.0,
                LocalDateTime.now().minusDays(3));

        List<SavingAccountReportData> result = reportsService.loadSavingAccountsData();
        var transactions = result.get(0).getTransactions();

        assertEquals(1, transactions.size());
        assertEquals(100.0, transactions.get(0).getAmount(), 0.001);
        assertTrue(transactions.get(0).isIncome());
    }
}
