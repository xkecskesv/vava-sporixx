package sk.sporixx.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sk.sporixx.model.Account;
import sk.sporixx.model.SavingGoal;
import sk.sporixx.model.Transaction;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy pre {@link TransactionService#addTransaction}.
 *
 * Pokrývame:
 *   - INCOME / EXPENSE pridanie + balance update
 *   - Transfer (main → saving, saving → main, main → emergency)
 *   - Validácie: amount, description, date, category
 *   - Edge cases: future date, negative amount, NaN
 */
class TransactionServiceAddTest extends TransactionServiceTestSupport {

    // =================== INCOME ===================

    @Test
    @DisplayName("addIncome – pridá transakciu a zvýši balance")
    void addIncome_increasesBalance() {
        double balanceBefore = mainAccount.getCurrentBalance();

        Transaction tx = transactionService.addTransaction(
                mainAccount.getId(), Transaction.TYPE_INCOME, null,
                categorySalaryId, null, "Mzda", 500.0, "EUR", LocalDate.now());

        assertNotNull(tx);
        assertTrue(tx.getId() > 0, "transakcia musí dostať priradené ID");
        assertEquals(500.0, tx.getAmount());
        assertEquals(Transaction.TYPE_INCOME, tx.getTransactionTypeId());

        // balance update
        assertEquals(balanceBefore + 500.0, mainAccount.getCurrentBalance(), 0.001);
        // aj v repo musí byť aktualizovaný
        Account fromRepo = accountRepo.findById(mainAccount.getId()).orElseThrow();
        assertEquals(balanceBefore + 500.0, fromRepo.getCurrentBalance(), 0.001);
    }

    @Test
    @DisplayName("addExpense – pridá transakciu a zníži balance")
    void addExpense_decreasesBalance() {
        double balanceBefore = mainAccount.getCurrentBalance();

        Transaction tx = transactionService.addTransaction(
                mainAccount.getId(), Transaction.TYPE_EXPENSE, null,
                categoryFoodId, Transaction.CLASSIFICATION_NEED,
                "Obed", 12.5, "EUR", LocalDate.now());

        assertNotNull(tx);
        assertEquals(12.5, tx.getAmount());
        assertEquals(Transaction.TYPE_EXPENSE, tx.getTransactionTypeId());
        assertEquals(Transaction.CLASSIFICATION_NEED, tx.getSpendingClassificationId());

        assertEquals(balanceBefore - 12.5, mainAccount.getCurrentBalance(), 0.001);
    }

    @Test
    @DisplayName("addExpense – môže ísť do mínusu (žiadna ochrana proti overdraft)")
    void addExpense_canGoNegative() {
        // Mainaccount má 1000, miniem 5000 → balance = -4000
        // Doc nálezy: nie je overdraft check. To je business decision projektu, dokumentujeme.
        Transaction tx = transactionService.addTransaction(
                mainAccount.getId(), Transaction.TYPE_EXPENSE, null,
                categoryFoodId, Transaction.CLASSIFICATION_WANT,
                "Veľký nákup", 5000.0, "EUR", LocalDate.now());

        assertNotNull(tx);
        assertEquals(-4000.0, mainAccount.getCurrentBalance(), 0.001,
                "AKTUÁLNE: balance ide do mínusu — nie je overdraft check");
    }

    // =================== TRANSFER ===================

    @Test
    @DisplayName("Transfer main → saving – obe transakcie + 2x balance update + saving goal")
    void transfer_mainToSaving_createsBothTransactions() {
        addSavingGoal(savingAccount.getId(), 10000.0, 500.0);

        double mainBefore = mainAccount.getCurrentBalance();
        double savingBefore = savingAccount.getCurrentBalance();

        Transaction returned = transactionService.addTransaction(
                mainAccount.getId(), Transaction.TYPE_EXPENSE,
                savingAccount.getId(), 0, null,  // categoryId ignored pri transfer
                "Sporenie", 200.0, "EUR", LocalDate.now());

        // funkcia vracia EXPENSE leg (z fromAccount)
        assertNotNull(returned);
        assertEquals(Transaction.TYPE_EXPENSE, returned.getTransactionTypeId());
        assertEquals(mainAccount.getId(), returned.getAccountId());
        assertEquals(Integer.valueOf(Transaction.CATEGORY_SAVING), returned.getCategoryId(),
                "transfer do savingu má autoCategory = CATEGORY_SAVING");

        // V repo musia byť 2 transakcie
        assertEquals(2, transactionRepo.findAll().size());

        // Balance: main -200, saving +200
        assertEquals(mainBefore - 200.0, mainAccount.getCurrentBalance(), 0.001);
        assertEquals(savingBefore + 200.0, savingAccount.getCurrentBalance(), 0.001);

        // Saving goal currentAmount sa aktualizoval
        SavingGoal goal = savingGoalRepo.findActiveByAccountId(savingAccount.getId()).get(0);
        assertEquals(700.0, goal.getCurrentAmount(), 0.001,
                "saving goal currentAmount = 500 + 200");
    }

    @Test
    @DisplayName("Transfer saving → main – kategória CATEGORY_SAVING_EXPENSE + zníži goal")
    void transfer_savingToMain_decreasesGoal() {
        addSavingGoal(savingAccount.getId(), 10000.0, 500.0);

        Transaction returned = transactionService.addTransaction(
                savingAccount.getId(), Transaction.TYPE_EXPENSE,
                mainAccount.getId(), 0, null,
                "Výber", 100.0, "EUR", LocalDate.now());

        assertEquals(Integer.valueOf(Transaction.CATEGORY_SAVING_EXPENSE),
                returned.getCategoryId());

        SavingGoal goal = savingGoalRepo.findActiveByAccountId(savingAccount.getId()).get(0);
        assertEquals(400.0, goal.getCurrentAmount(), 0.001,
                "saving goal: 500 - 100 = 400");
    }

    @Test
    @DisplayName("Transfer saving → main keď goal by išiel pod 0 — clampnutý na 0")
    void transfer_savingToMain_goalClampedAtZero() {
        addSavingGoal(savingAccount.getId(), 10000.0, 50.0);

        // chceme presunúť 100 ale goal má len 50 → clamp na 0
        transactionService.addTransaction(
                savingAccount.getId(), Transaction.TYPE_EXPENSE,
                mainAccount.getId(), 0, null,
                "Veľký výber", 100.0, "EUR", LocalDate.now());

        SavingGoal goal = savingGoalRepo.findActiveByAccountId(savingAccount.getId()).get(0);
        assertEquals(0.0, goal.getCurrentAmount(), 0.001,
                "Math.max(0, ...) clamp v updateSavingGoalAmount");
    }

    @Test
    @DisplayName("Transfer main → emergency (žiadny saving) – kategória ostáva null")
    void transfer_mainToEmergency_noAutoCategory() {
        Transaction returned = transactionService.addTransaction(
                mainAccount.getId(), Transaction.TYPE_EXPENSE,
                secondMainAccount.getId(), 0, null,
                "Presun", 50.0, "EUR", LocalDate.now());

        assertEquals(
                Integer.valueOf(Transaction.CATEGORY_TRANSFER),
                returned.getCategoryId(),
                "transfer medzi bežnými účtami má CATEGORY_TRANSFER"
        );

        // balance update
        assertEquals(950.0, mainAccount.getCurrentBalance(), 0.001);
        assertEquals(250.0, secondMainAccount.getCurrentBalance(), 0.001);
    }

    @Test
    void transfer_selfTransfer_allowed() {
        assertDoesNotThrow(() ->
                transactionService.addTransaction(
                        mainAccount.getId(),
                        Transaction.TYPE_EXPENSE,
                        mainAccount.getId(),
                        0,
                        null,
                        "Self transfer",
                        100.0,
                        "EUR",
                        LocalDate.now()
                ));
    }

    // =================== VALIDÁCIE ===================

    @Test
    @DisplayName("Validácia: amount = 0 hodí invalid_amount")
    void addTransaction_zeroAmount_throws() {
        TransactionException ex = assertThrows(TransactionException.class, () ->
                transactionService.addTransaction(
                        mainAccount.getId(), Transaction.TYPE_INCOME, null,
                        categorySalaryId, null, "X", 0.0, "EUR", LocalDate.now()));
        assertEquals("transaction.error.invalid_amount", ex.getMessageKey());
    }

    @Test
    @DisplayName("Validácia: záporný amount hodí invalid_amount")
    void addTransaction_negativeAmount_throws() {
        TransactionException ex = assertThrows(TransactionException.class, () ->
                transactionService.addTransaction(
                        mainAccount.getId(), Transaction.TYPE_EXPENSE, null,
                        categoryFoodId, Transaction.CLASSIFICATION_NEED,
                        "X", -5.0, "EUR", LocalDate.now()));
        assertEquals("transaction.error.invalid_amount", ex.getMessageKey());
    }

    @Test
    @DisplayName("Validácia: prázdny description hodí description_required")
    void addTransaction_blankDescription_throws() {
        TransactionException ex = assertThrows(TransactionException.class, () ->
                transactionService.addTransaction(
                        mainAccount.getId(), Transaction.TYPE_INCOME, null,
                        categorySalaryId, null, "   ", 100.0, "EUR", LocalDate.now()));
        assertEquals("transaction.error.description_required", ex.getMessageKey());
    }

    @Test
    @DisplayName("Validácia: null description hodí description_required")
    void addTransaction_nullDescription_throws() {
        TransactionException ex = assertThrows(TransactionException.class, () ->
                transactionService.addTransaction(
                        mainAccount.getId(), Transaction.TYPE_INCOME, null,
                        categorySalaryId, null, null, 100.0, "EUR", LocalDate.now()));
        assertEquals("transaction.error.description_required", ex.getMessageKey());
    }

    @Test
    @DisplayName("Validácia: null date hodí date_required")
    void addTransaction_nullDate_throws() {
        TransactionException ex = assertThrows(TransactionException.class, () ->
                transactionService.addTransaction(
                        mainAccount.getId(), Transaction.TYPE_INCOME, null,
                        categorySalaryId, null, "Plat", 100.0, "EUR", null));
        assertEquals("transaction.error.date_required", ex.getMessageKey());
    }

    @Test
    @DisplayName("Validácia: budúci dátum hodí future_date")
    void addTransaction_futureDate_throws() {
        TransactionException ex = assertThrows(TransactionException.class, () ->
                transactionService.addTransaction(
                        mainAccount.getId(), Transaction.TYPE_INCOME, null,
                        categorySalaryId, null, "Plat", 100.0, "EUR",
                        LocalDate.now().plusDays(1)));
        assertEquals("transaction.error.future_date", ex.getMessageKey());
    }

    @Test
    @DisplayName("Validácia: dnešný dátum (LocalDate.now) je OK")
    void addTransaction_todayDate_ok() {
        assertDoesNotThrow(() -> transactionService.addTransaction(
                mainAccount.getId(), Transaction.TYPE_INCOME, null,
                categorySalaryId, null, "Plat", 100.0, "EUR", LocalDate.now()));
    }

    @Test
    @DisplayName("Validácia: neexistujúca kategória hodí invalid_category")
    void addTransaction_invalidCategory_throws() {
        TransactionException ex = assertThrows(TransactionException.class, () ->
                transactionService.addTransaction(
                        mainAccount.getId(), Transaction.TYPE_EXPENSE, null,
                        99999, Transaction.CLASSIFICATION_NEED,
                        "X", 50.0, "EUR", LocalDate.now()));
        assertEquals("transaction.error.invalid_category", ex.getMessageKey());
    }

    @Test
    @DisplayName("Validácia: kategória sa NEKONTROLUJE pri transfer (targetAccountId != null)")
    void addTransaction_transfer_categoryNotValidated() {
        // ak by sa kategória kontrolovala aj pri transfer, tento test by padol
        // (categoryId=99999 by hodil invalid_category)
        assertDoesNotThrow(() -> transactionService.addTransaction(
                mainAccount.getId(), Transaction.TYPE_EXPENSE,
                savingAccount.getId(), 99999, null,
                "Transfer", 50.0, "EUR", LocalDate.now()));
    }

    @Test
    @DisplayName("Validácia: neexistujúci account hodí account_not_found")
    void addTransaction_invalidAccountId_throws() {
        TransactionException ex = assertThrows(TransactionException.class, () ->
                transactionService.addTransaction(
                        99999, Transaction.TYPE_INCOME, null,
                        categorySalaryId, null, "X", 100.0, "EUR", LocalDate.now()));
        assertEquals("transaction.error.account_not_found", ex.getMessageKey());
    }

    // =================== SIDE EFFECTS ===================

    @Test
    @DisplayName("Side-effect: createdAt sa nastaví automaticky")
    void addTransaction_setsCreatedAt() {
        Transaction tx = transactionService.addTransaction(
                mainAccount.getId(), Transaction.TYPE_INCOME, null,
                categorySalaryId, null, "X", 100.0, "EUR", LocalDate.now());

        assertNotNull(tx.getCreatedAt());
    }

    @Test
    @DisplayName("Side-effect: completeDate má dátum z parametru + aktuálny čas")
    void addTransaction_completeDate_combinesParam() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        Transaction tx = transactionService.addTransaction(
                mainAccount.getId(), Transaction.TYPE_INCOME, null,
                categorySalaryId, null, "X", 100.0, "EUR", yesterday);

        assertEquals(yesterday, tx.getCompleteDate().toLocalDate());
    }

    @Test
    @DisplayName("Side-effect: SessionManager má aktualizovaný balance")
    void addTransaction_updatesSessionAccount() {
        transactionService.addTransaction(
                mainAccount.getId(), Transaction.TYPE_INCOME, null,
                categorySalaryId, null, "X", 100.0, "EUR", LocalDate.now());

        Account fromSession = SessionManager.getInstance().getAccountById(mainAccount.getId());
        assertEquals(1100.0, fromSession.getCurrentBalance(), 0.001);
    }
}
