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
 * INTEGRATION testy {@link ReportsService#loadSavingAccountsData()} proti reálnej JDBC DB.
 * <p>
 * Najkomplexnejší report — overuje:
 *  <ul>
 *      <li>JOIN/výber saving accountov len pre prihláseného usera</li>
 *      <li>SavingGoalRepositoryImpl: findActiveByAccountId filtruje is_active=1</li>
 *      <li>TransactionRepositoryImpl: findByAccountIdAndDateRange filtruje INCOME</li>
 *      <li>Lineárne expected progress + kumulatívne actual progress</li>
 *      <li>Hraničné prípady (over-saved, no goal, no income)</li>
 *  </ul>
 */
class ReportsServiceSavingAccountsJdbcTest extends ReportsServiceJdbcTestSupport {

    @Test
    @DisplayName("Žiadne saving účty — prázdny zoznam")
    void noSavingAccounts_returnsEmpty() {
        // Prepíšeme session — iba main account, žiaden saving
        SessionManager.getInstance().clearSession();
        User u = User.builder().id(testUser.getId()).email(testUser.getEmail())
                .firstName("X").lastName("Y").role(Role.USER).isActive(true).build();
        SessionManager.getInstance().setSession(u, List.of(mainAccount));

        List<SavingAccountReportData> result = reportsService.loadSavingAccountsData();
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Saving účet bez aktívneho goalu — skipnutý")
    void savingAccountWithoutGoal_skipped() {
        // Žiadny goal v DB pre savingAccount
        List<SavingAccountReportData> result = reportsService.loadSavingAccountsData();
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Saving účet s neaktívnym goalom (is_active=0) — skipnutý")
    void savingAccountWithInactiveGoal_skipped() {
        // Vlož goal s is_active=0 priamo cez SQL (insertSavingGoal ho ukladá ako active=1)
        execSql("INSERT INTO saving_goals (account_id, name, goal_type_id, " +
                "target_amount, current_amount, target_date, is_active, created_at) " +
                "VALUES (" + savingAccount.getId() + ", 'Old goal', 1, 1000.0, 100.0, " +
                "'2027-01-01 00:00:00', 0, '2025-01-01 00:00:00')");

        List<SavingAccountReportData> result = reportsService.loadSavingAccountsData();
        assertTrue(result.isEmpty(),
                "neaktívny goal sa nesmie objaviť — repository musí filtrovať is_active=1");
    }

    // ====================== ZÁKLADNÉ POLIA ======================

    @Test
    @DisplayName("Saving account s aktívnym goalom — savedUp, needToSave, target")
    void basicFields_correctMapping() {
        insertSavingGoal(savingAccount.getId(), 10000.0, 3000.0,
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
    @DisplayName("Over-saved (current > target) — needToSave clampnutý na 0")
    void overSaved_needToSaveClampedToZero() {
        // current=12000, target=10000 → matematicky -2000, clamp na 0
        insertSavingGoal(savingAccount.getId(), 10000.0, 12000.0,
                LocalDateTime.now().minusMonths(2),
                LocalDateTime.now().plusMonths(10));

        List<SavingAccountReportData> result = reportsService.loadSavingAccountsData();

        assertEquals(0.0, result.get(0).getNeedToSave(), 0.001,
                "needToSave nesmie byť záporné");
    }

    // ====================== GROUPING ======================

    @Test
    @DisplayName("Krátky goal (≤ 90 dní) → grouping = DAY")
    void shortGoal_groupedByDay() {
        insertSavingGoal(savingAccount.getId(), 10000.0, 0.0,
                LocalDateTime.now().minusDays(30),
                LocalDateTime.now().plusDays(30));  // total 60 dní

        List<SavingAccountReportData> result = reportsService.loadSavingAccountsData();

        assertEquals("DAY", result.get(0).getProgressGrouping());
    }

    @Test
    @DisplayName("Stredný goal (90 < dni ≤ 1825) → grouping = MONTH")
    void mediumGoal_groupedByMonth() {
        insertSavingGoal(savingAccount.getId(), 10000.0, 0.0,
                LocalDateTime.now().minusMonths(2),
                LocalDateTime.now().plusMonths(10));  // ~1 rok

        List<SavingAccountReportData> result = reportsService.loadSavingAccountsData();

        assertEquals("MONTH", result.get(0).getProgressGrouping());
    }

    @Test
    @DisplayName("Dlhý goal (> 1825 dní) → grouping = YEAR")
    void longGoal_groupedByYear() {
        insertSavingGoal(savingAccount.getId(), 100000.0, 0.0,
                LocalDateTime.now().minusYears(2),
                LocalDateTime.now().plusYears(8));  // 10 rokov

        List<SavingAccountReportData> result = reportsService.loadSavingAccountsData();

        assertEquals("YEAR", result.get(0).getProgressGrouping());
    }

    // ====================== EXPECTED PROGRESS ======================

    @Test
    @DisplayName("Expected progress je lineárny — start = initialBalance, end = target")
    void expectedProgress_linearFromInitialToTarget() {
        insertSavingGoal(savingAccount.getId(), 10000.0, 0.0,
                LocalDateTime.now().minusMonths(2),
                LocalDateTime.now().plusMonths(10));

        List<SavingAccountReportData> result = reportsService.loadSavingAccountsData();
        var expected = result.get(0).getExpectedProgress();

        assertFalse(expected.isEmpty());

        double firstValue = expected.values().iterator().next();
        assertEquals(savingAccount.getInitialBalance(), firstValue, 0.001,
                "expected[0] = initialBalance (500)");

        double lastValue = expected.values().stream()
                .reduce((a, b) -> b).orElseThrow();
        assertEquals(10000.0, lastValue, 0.001, "expected[last] = targetAmount");
    }

    @Test
    @DisplayName("Already-met goal (initial >= target) — expected má 1 bod = target")
    void expectedProgress_alreadyMet_singlePoint() {
        // Saving má initial 500. Goal target 100. → already met.
        insertSavingGoal(savingAccount.getId(), 100.0, 50.0,
                LocalDateTime.now().minusDays(30),
                LocalDateTime.now().plusDays(30));

        List<SavingAccountReportData> result = reportsService.loadSavingAccountsData();
        var expected = result.get(0).getExpectedProgress();

        assertEquals(1, expected.size(), "1 bod ak už je goal splnený");
        assertEquals(100.0, expected.values().iterator().next(), 0.001);
    }

    // ====================== ACTUAL PROGRESS ======================

    @Test
    @DisplayName("Actual progress — kumulatívne sčítanie INCOME transakcií + initialBalance")
    void actualProgress_cumulative() {
        insertSavingGoal(savingAccount.getId(), 10000.0, 0.0,
                LocalDateTime.now().minusMonths(2),
                LocalDateTime.now().plusMonths(10));

        // 2 income transakcie na saving accounte
        insertTransaction(savingAccount.getId(), Transaction.TYPE_INCOME, 1, null,
                1000.0, LocalDateTime.now().minusMonths(2).plusDays(5));
        insertTransaction(savingAccount.getId(), Transaction.TYPE_INCOME, 1, null,
                500.0, LocalDateTime.now().minusMonths(1).plusDays(5));

        List<SavingAccountReportData> result = reportsService.loadSavingAccountsData();
        var actual = result.get(0).getActualProgress();

        assertFalse(actual.isEmpty());

        double lastValue = actual.values().stream()
                .reduce((a, b) -> b).orElseThrow();
        assertEquals(2000.0, lastValue, 0.001,
                "kumulatívna suma od initialBalance: 500 + 1000 + 500 = 2000");
    }

    @Test
    @DisplayName("Bez incomeov — actual progress je flat na initialBalance")
    void actualProgress_noIncome_flatAtInitial() {
        insertSavingGoal(savingAccount.getId(), 10000.0, 0.0,
                LocalDateTime.now().minusMonths(2),
                LocalDateTime.now().plusMonths(10));

        List<SavingAccountReportData> result = reportsService.loadSavingAccountsData();
        var actual = result.get(0).getActualProgress();

        assertTrue(actual.values().stream()
                        .allMatch(v -> Math.abs(v - 500.0) < 0.001),
                "bez incomeov je actual flat na initialBalance");
    }

    @Test
    @DisplayName("Transactions list obsahuje LEN INCOME pre tento saving účet")
    void transactionsList_filteredByIncomeAndAccount() {
        insertSavingGoal(savingAccount.getId(), 10000.0, 0.0,
                LocalDateTime.now().minusMonths(2),
                LocalDateTime.now().plusMonths(10));

        // INCOME na saving — má sa objaviť
        insertTransaction(savingAccount.getId(), Transaction.TYPE_INCOME, 1, null, 100.0,
                LocalDateTime.now().minusDays(5));
        // EXPENSE na saving — NEmá sa objaviť (filtruje len INCOME)
        insertTransaction(savingAccount.getId(), Transaction.TYPE_EXPENSE, 1,
                Transaction.CLASSIFICATION_NEED, 50.0, LocalDateTime.now().minusDays(4));
        // INCOME na main — NEmá sa objaviť (iný účet)
        insertTransaction(mainAccount.getId(), Transaction.TYPE_INCOME, 1, null, 200.0,
                LocalDateTime.now().minusDays(3));

        List<SavingAccountReportData> result = reportsService.loadSavingAccountsData();
        var transactions = result.get(0).getTransactions();

        assertEquals(1, transactions.size(), "iba 1 INCOME na saving účte");
        assertEquals(100.0, transactions.get(0).getAmount(), 0.001);
        assertTrue(transactions.get(0).isIncome());
    }

    @Test
    @DisplayName("Description z DB stĺpca 'name' sa správne mapuje na Java pole 'description'")
    void transactionDescription_mappedFromNameColumn() {
        insertSavingGoal(savingAccount.getId(), 10000.0, 0.0,
                LocalDateTime.now().minusMonths(2),
                LocalDateTime.now().plusMonths(10));

        // Špecifický description string aby sme overili že JDBC repo nezamieňa stĺpce
        execSql("INSERT INTO transactions (account_id, name, category_id, " +
                "transaction_type_id, amount, status_id, transaction_date) VALUES (" +
                savingAccount.getId() + ", 'My distinctive description', 1, 1, 100.0, 1, " +
                "'" + LocalDateTime.now().minusDays(5).format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "')");

        List<SavingAccountReportData> result = reportsService.loadSavingAccountsData();
        var tx = result.get(0).getTransactions().get(0);

        assertEquals("My distinctive description", tx.getDescription(),
                "JDBC repo musí mapovať 'name' z DB na Java pole 'description'");
    }
}
