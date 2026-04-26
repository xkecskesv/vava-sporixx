package sk.sporixx.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sk.sporixx.dto.RecurringExpenseData;
import sk.sporixx.model.Transaction;

import static org.junit.jupiter.api.Assertions.*;

/**
 * INTEGRATION testy {@link ReportsService#loadRecurringExpenseData()} proti reálnej JDBC DB.
 * <p>
 * Trieda overuje že {@code RecurringRuleRepositoryImpl.findActiveByAccountId} skutočne
 * filtruje is_active=1 v SQL, mapuje všetky stĺpce a poskytuje dáta v správnej forme.
 */
class ReportsServiceRecurringExpenseJdbcTest extends ReportsServiceJdbcTestSupport {

    private static final int CAT_FOOD = 1;
    private static final int CAT_RENT = 4;
    private static final int CAT_ENTERTAINMENT = 3;

    @Test
    @DisplayName("Žiadne rules — všetky polia 0, prázdny zoznam")
    void empty() {
        RecurringExpenseData data = reportsService.loadRecurringExpenseData();

        assertNotNull(data);
        assertTrue(data.getItems().isEmpty());
        assertEquals(0.0, data.getTotalWant(), 0.001);
        assertEquals(0.0, data.getTotalNeed(), 0.001);
        assertEquals(0.0, data.getTotal(), 0.001);
    }

    @Test
    @DisplayName("3 active rules — agreguje want/need správne")
    void aggregatesWantAndNeed() {
        insertRecurringRule(mainAccount.getId(), CAT_RENT, Transaction.CLASSIFICATION_NEED,
                500.0, "Nájom", true);
        insertRecurringRule(mainAccount.getId(), CAT_ENTERTAINMENT, Transaction.CLASSIFICATION_WANT,
                15.0, "Netflix", true);
        insertRecurringRule(mainAccount.getId(), CAT_ENTERTAINMENT, Transaction.CLASSIFICATION_WANT,
                10.0, "Spotify", true);

        RecurringExpenseData data = reportsService.loadRecurringExpenseData();

        assertEquals(3, data.getItems().size());
        assertEquals(500.0, data.getTotalNeed(), 0.001);
        assertEquals(25.0, data.getTotalWant(), 0.001);
        assertEquals(525.0, data.getTotal(), 0.001);
    }

    @Test
    @DisplayName("INACTIVE rule (is_active=0) NESMIE byť v reporte")
    void inactiveExcluded_atSqlLevel() {
        // Active
        insertRecurringRule(mainAccount.getId(), CAT_RENT, Transaction.CLASSIFICATION_NEED,
                500.0, "Aktívny nájom", true);
        // Inactive
        insertRecurringRule(mainAccount.getId(), CAT_RENT, Transaction.CLASSIFICATION_NEED,
                999.0, "Zrušený nájom", false);

        RecurringExpenseData data = reportsService.loadRecurringExpenseData();

        assertEquals(1, data.getItems().size(),
                "iba aktívna rule sa má objaviť (SQL musí filtrovať is_active=1)");
        assertEquals(500.0, data.getTotalNeed(), 0.001);

        // Sanity check — overiť že v DB sú obe (aby sme vedeli že iba SQL filter to vyriešil)
        try (java.sql.Connection conn = openConnection();
             java.sql.Statement stmt = conn.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM recurring_rules")) {
            assertTrue(rs.next());
            assertEquals(2, rs.getInt(1), "v DB majú byť 2 rules, len 1 aktívna");
        } catch (java.sql.SQLException e) {
            fail(e);
        }
    }

    @Test
    @DisplayName("Naprieč viacerými účtami sa rules spájajú")
    void mergesAcrossAccounts() {
        insertRecurringRule(mainAccount.getId(), CAT_RENT, Transaction.CLASSIFICATION_NEED,
                500.0, "Hlavný nájom", true);
        insertRecurringRule(savingAccount.getId(), CAT_FOOD, Transaction.CLASSIFICATION_NEED,
                100.0, "Sporenie potraviny", true);

        RecurringExpenseData data = reportsService.loadRecurringExpenseData();

        assertEquals(2, data.getItems().size());
        assertEquals(600.0, data.getTotalNeed(), 0.001);
    }

    @Test
    @DisplayName("Mapping: amount, description a classification sú v Java objekte správne")
    void fieldMapping_intactAfterRoundtrip() {
        insertRecurringRule(mainAccount.getId(), CAT_RENT, Transaction.CLASSIFICATION_NEED,
                123.45, "Test mapovania", true);

        RecurringExpenseData data = reportsService.loadRecurringExpenseData();

        assertEquals(1, data.getItems().size());
        var rule = data.getItems().get(0);
        assertEquals(123.45, rule.getAmount(), 0.001, "amount mapping");
        assertEquals("Test mapovania", rule.getDescription(), "description mapping");
        assertEquals(Transaction.CLASSIFICATION_NEED, rule.getSpendingClassificationId(),
                "classification mapping");
    }
}
