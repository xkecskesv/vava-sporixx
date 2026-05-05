package sk.sporixx.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sk.sporixx.model.Transaction;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy pre {@link TransactionService#deleteTransactions(List)}.
 *
 * Pokrývame:
 *   - happy path (1, viac, všetky)
 *   - revert balance (income reverse, expense reverse)
 *   - prázdny zoznam
 *   - neexistujúce ID (skip vs error)
 *   - mixed batch
 */
class TransactionServiceDeleteTest extends TransactionServiceTestSupport {

    @Test
    @DisplayName("delete INCOME — balance -= amount (reverz)")
    void delete_income_revertsBalance() {
        Transaction income = transactionService.addTransaction(
                mainAccount.getId(), Transaction.TYPE_INCOME, null,
                categorySalaryId, null, "Plat", 200.0, "EUR", LocalDate.now());
        assertEquals(1200.0, mainAccount.getCurrentBalance(), 0.001);

        transactionService.deleteTransactions(List.of(income.getId()));

        assertEquals(1000.0, mainAccount.getCurrentBalance(), 0.001,
                "po delete income sa balance vráti na pôvodnú hodnotu");
        assertTrue(transactionRepo.findById(income.getId()).isEmpty());
    }

    @Test
    @DisplayName("delete EXPENSE — balance += amount (reverz)")
    void delete_expense_revertsBalance() {
        Transaction expense = transactionService.addTransaction(
                mainAccount.getId(), Transaction.TYPE_EXPENSE, null,
                categoryFoodId, Transaction.CLASSIFICATION_NEED,
                "Obed", 50.0, "EUR", LocalDate.now());
        assertEquals(950.0, mainAccount.getCurrentBalance(), 0.001);

        transactionService.deleteTransactions(List.of(expense.getId()));

        assertEquals(1000.0, mainAccount.getCurrentBalance(), 0.001);
    }

    @Test
    @DisplayName("delete batch — viac transakcií naraz")
    void delete_batch_revertsAll() {
        Transaction income = transactionService.addTransaction(
                mainAccount.getId(), Transaction.TYPE_INCOME, null,
                categorySalaryId, null, "Plat", 500.0, "EUR", LocalDate.now());
        Transaction expense = transactionService.addTransaction(
                mainAccount.getId(), Transaction.TYPE_EXPENSE, null,
                categoryFoodId, Transaction.CLASSIFICATION_NEED,
                "Obed", 100.0, "EUR", LocalDate.now());
        // balance: 1000 + 500 - 100 = 1400
        assertEquals(1400.0, mainAccount.getCurrentBalance(), 0.001);

        transactionService.deleteTransactions(List.of(income.getId(), expense.getId()));

        // späť na 1000
        assertEquals(1000.0, mainAccount.getCurrentBalance(), 0.001);
        assertEquals(0, transactionRepo.findAll().size());
    }

    @Test
    @DisplayName("delete prázdny list hodí no_selection")
    void delete_emptyList_throws() {
        TransactionException ex = assertThrows(TransactionException.class,
                () -> transactionService.deleteTransactions(List.of()));
        assertEquals("transaction.error.no_selection", ex.getMessageKey());
    }

    @Test
    @DisplayName("delete neexistujúce ID — kód SKIPNE (loguje warn, nehodí exception)")
    void delete_nonExistent_silentlySkipped() {
        // Aktuálny kód: ak findById() vráti empty, len sa zaloguje warn a continue
        // → exception sa nehodi, vrátime sa "úspešne"
        assertDoesNotThrow(() ->
                transactionService.deleteTransactions(List.of(99999)));
    }

    @Test
    @DisplayName("delete mix existujúce + neexistujúce — existujúce sa zmaže, neexistujúce sa preskočí")
    void delete_mixed_partialSuccess() {
        Transaction expense = transactionService.addTransaction(
                mainAccount.getId(), Transaction.TYPE_EXPENSE, null,
                categoryFoodId, Transaction.CLASSIFICATION_NEED,
                "Obed", 50.0, "EUR", LocalDate.now());

        transactionService.deleteTransactions(List.of(expense.getId(), 99999));

        // expense sa zmazal, balance vrátený
        assertTrue(transactionRepo.findById(expense.getId()).isEmpty());
        assertEquals(1000.0, mainAccount.getCurrentBalance(), 0.001);
    }

    @Test
    @DisplayName("BUG nález: delete transferu zmaže IBA jeden leg")
    void delete_transferLeg_leavesOtherIntact() {
        // Transfer 200: main → saving
        addSavingGoal(savingAccount.getId(), 10000.0, 500.0);
        transactionService.addTransaction(
                mainAccount.getId(), Transaction.TYPE_EXPENSE,
                savingAccount.getId(), 0, null,
                "Transfer", 200.0, "EUR", LocalDate.now());

        // Máme 2 transakcie: expense(main, 200) + income(saving, 200)
        assertEquals(2, transactionRepo.findAll().size());

        // Zmaže iba expense leg
        Transaction expenseLeg = transactionRepo.findAll().stream()
                .filter(t -> t.getTransactionTypeId() == Transaction.TYPE_EXPENSE)
                .findFirst().orElseThrow();
        transactionService.deleteTransactions(List.of(expenseLeg.getId()));

        // Balance: main += 200 (revert expense) → 1000
        // Saving: NEZMENENE → 700 (transfer income leg ostal)
        assertEquals(1000.0, mainAccount.getCurrentBalance(), 0.001);
        assertEquals(500.0, savingAccount.getCurrentBalance(), 0.001,
                "BUG: druhy leg transferu zostal a saving má 'umelých' 200 €");

        // Total: 1700, ale pôvodne bolo 1500 → 200 € NAPLODILO V SYSTÉME
        double total = mainAccount.getCurrentBalance() + savingAccount.getCurrentBalance();
        assertEquals(1500.0, total, 0.001,
                "BUG: zmazanie len jedného legu transferu vytvorilo phantom money");
    }

    @Test
    @DisplayName("delete celý batch transferu (oba legi) — balance konzistentný")
    void delete_bothTransferLegs_consistentBalance() {
        addSavingGoal(savingAccount.getId(), 10000.0, 500.0);
        transactionService.addTransaction(
                mainAccount.getId(), Transaction.TYPE_EXPENSE,
                savingAccount.getId(), 0, null,
                "Transfer", 200.0, "EUR", LocalDate.now());

        List<Integer> allIds = transactionRepo.findAll().stream()
                .map(Transaction::getId).toList();
        assertEquals(2, allIds.size());

        transactionService.deleteTransactions(allIds);

        // späť do pôvodného stavu
        assertEquals(1000.0, mainAccount.getCurrentBalance(), 0.001);
        assertEquals(500.0, savingAccount.getCurrentBalance(), 0.001);
    }

    @Test
    @DisplayName("delete viackrát to isté ID — druhé volanie skipne")
    void delete_duplicateInList_secondSkipped() {
        Transaction expense = transactionService.addTransaction(
                mainAccount.getId(), Transaction.TYPE_EXPENSE, null,
                categoryFoodId, Transaction.CLASSIFICATION_NEED,
                "Obed", 50.0, "EUR", LocalDate.now());

        // duplikát v batchi
        assertDoesNotThrow(() -> transactionService.deleteTransactions(
                List.of(expense.getId(), expense.getId())));

        assertEquals(1000.0, mainAccount.getCurrentBalance(), 0.001,
                "po prvom delete tx už neexistuje, druhé sa skipne — žiadny double-revert");
    }
}
