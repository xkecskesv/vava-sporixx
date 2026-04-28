package sk.sporixx.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sk.sporixx.dto.RecurringExpenseData;
import sk.sporixx.model.Transaction;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy pre {@link ReportsService#loadRecurringExpenseData()}.
 *
 * Pokrývame:
 *   - prázdny stav
 *   - aggregation podľa want/need
 *   - rules naprieč viacerými účtami
 *   - inactive rules sú vynechané (findActiveByAccountId v repo)
 */
class ReportsServiceRecurringExpenseTest extends ReportsServiceTestSupport {

    @Test
    @DisplayName("Prázdne dáta — všetky 0")
    void empty() {
        RecurringExpenseData data = reportsService.loadRecurringExpenseData();

        assertNotNull(data);
        assertTrue(data.getItems().isEmpty());
        assertEquals(0.0, data.getTotalWant(), 0.001);
        assertEquals(0.0, data.getTotalNeed(), 0.001);
        assertEquals(0.0, data.getTotal(), 0.001);
    }

    @Test
    @DisplayName("Sumarizácia want/need podľa classification")
    void aggregatesWantAndNeed() {
        rule(mainAccount.getId(), 50.0, Transaction.CLASSIFICATION_NEED, "Nájom");
        rule(mainAccount.getId(), 20.0, Transaction.CLASSIFICATION_WANT, "Netflix");
        rule(mainAccount.getId(), 15.0, Transaction.CLASSIFICATION_WANT, "Spotify");

        RecurringExpenseData data = reportsService.loadRecurringExpenseData();

        assertEquals(3, data.getItems().size());
        assertEquals(50.0, data.getTotalNeed(), 0.001);
        assertEquals(35.0, data.getTotalWant(), 0.001);
        assertEquals(85.0, data.getTotal(), 0.001);
    }

    @Test
    @DisplayName("Rules z viacerých účtov sú spojené")
    void mergesAcrossAccounts() {
        rule(mainAccount.getId(), 50.0, Transaction.CLASSIFICATION_NEED, "Nájom");
        rule(savingAccount.getId(), 30.0, Transaction.CLASSIFICATION_NEED, "Sporenie auto");

        RecurringExpenseData data = reportsService.loadRecurringExpenseData();

        assertEquals(2, data.getItems().size());
        assertEquals(80.0, data.getTotalNeed(), 0.001);
    }

    @Test
    @DisplayName("Inactive rules sa nezapočítajú")
    void inactiveExcluded() {
        // Aktivne
        rule(mainAccount.getId(), 50.0, Transaction.CLASSIFICATION_NEED, "Aktívne");

        // Neaktívne — pridané priamo cez repo s isActive=false
        var inactiveRule = sk.sporixx.model.RecurringRule.builder()
                .accountId(mainAccount.getId())
                .amount(999.0)
                .description("Neaktívne")
                .spendingClassificationId(Transaction.CLASSIFICATION_NEED)
                .transactionTypeId(Transaction.TYPE_EXPENSE)
                .frequencyType("MONTHLY")
                .frequencyInterval(1)
                .startDate(java.time.LocalDateTime.now().minusMonths(2))
                .nextDueDate(java.time.LocalDateTime.now().plusDays(5))
                .isActive(false)  // ← INACTIVE
                .createdAt(java.time.LocalDateTime.now().minusMonths(2))
                .build();
        recurringRepo.save(inactiveRule);

        RecurringExpenseData data = reportsService.loadRecurringExpenseData();

        assertEquals(1, data.getItems().size(), "iba aktívna rule sa zarátala");
        assertEquals(50.0, data.getTotalNeed(), 0.001);
    }
}
