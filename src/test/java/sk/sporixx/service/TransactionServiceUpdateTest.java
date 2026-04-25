package sk.sporixx.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sk.sporixx.model.Transaction;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy pre {@link TransactionService#updateTransaction}.
 *
 * !!! DÔLEŽITÉ: vždy používame {@code copyOf(tx)} predtým než mutujeme amount.
 * Dôvod: InMemoryTransactionRepository drží PRIAMU REFERENCIU na Transaction,
 * takže ak by sme mutovali pôvodný objekt cez tx.setAmount(150), zmutoval by sa
 * AJ "original" ktorý service načítava cez findById() — delta by bola 0.
 * V reálnej DB by sa tento problém neprejavil (findById vracia novú inštanciu),
 * ale pre čistotu testov musíme robiť kópiu.
 */
class TransactionServiceUpdateTest extends TransactionServiceTestSupport {

    protected Transaction copyOf(Transaction tx) {
        return Transaction.builder()
                .id(tx.getId())
                .accountId(tx.getAccountId())
                .targetAccountId(tx.getTargetAccountId())
                .categoryId(tx.getCategoryId())
                .transactionTypeId(tx.getTransactionTypeId())
                .transactionStatusId(tx.getTransactionStatusId())
                .spendingClassificationId(tx.getSpendingClassificationId()) // ✅ fix
                .amount(tx.getAmount())
                .currencyCode(tx.getCurrencyCode())
                .description(tx.getDescription())
                .completeDate(tx.getCompleteDate())
                .createdAt(tx.getCreatedAt())
                .build();
    }

    @Test
    @DisplayName("update INCOME: amount 100 → 150, balance += 50 (delta)")
    void update_increaseIncome_increasesBalance() {
        Transaction tx = transactionService.addTransaction(
                mainAccount.getId(), Transaction.TYPE_INCOME, null,
                categorySalaryId, null, "Plat", 100.0, "EUR", LocalDate.now());
        assertEquals(1100.0, mainAccount.getCurrentBalance(), 0.001);

        Transaction updated = copyOf(tx);
        updated.setAmount(150.0);
        transactionService.updateTransaction(updated);

        assertEquals(1150.0, mainAccount.getCurrentBalance(), 0.001);
    }

    @Test
    @DisplayName("update INCOME: amount 100 → 60, balance -= 40")
    void update_decreaseIncome_decreasesBalance() {
        Transaction tx = transactionService.addTransaction(
                mainAccount.getId(), Transaction.TYPE_INCOME, null,
                categorySalaryId, null, "Plat", 100.0, "EUR", LocalDate.now());

        Transaction updated = copyOf(tx);
        updated.setAmount(60.0);
        transactionService.updateTransaction(updated);

        assertEquals(1060.0, mainAccount.getCurrentBalance(), 0.001);
    }

    @Test
    @DisplayName("update EXPENSE: amount 50 → 30, balance += 20 (menší výdavok)")
    void update_decreaseExpense_increasesBalance() {
        Transaction tx = transactionService.addTransaction(
                mainAccount.getId(), Transaction.TYPE_EXPENSE, null,
                categoryFoodId, Transaction.CLASSIFICATION_NEED,
                "Obed", 50.0, "EUR", LocalDate.now());
        assertEquals(950.0, mainAccount.getCurrentBalance(), 0.001);

        Transaction updated = copyOf(tx);
        updated.setAmount(30.0);
        transactionService.updateTransaction(updated);

        assertEquals(970.0, mainAccount.getCurrentBalance(), 0.001);
    }

    @Test
    @DisplayName("update EXPENSE: amount 50 → 80, balance -= 30 (väčší výdavok)")
    void update_increaseExpense_decreasesBalance() {
        Transaction tx = transactionService.addTransaction(
                mainAccount.getId(), Transaction.TYPE_EXPENSE, null,
                categoryFoodId, Transaction.CLASSIFICATION_NEED,
                "Obed", 50.0, "EUR", LocalDate.now());

        Transaction updated = copyOf(tx);
        updated.setAmount(80.0);
        transactionService.updateTransaction(updated);

        assertEquals(920.0, mainAccount.getCurrentBalance(), 0.001);
    }

    @Test
    @DisplayName("update: rovnaká suma — balance sa nezmení (delta = 0)")
    void update_sameAmount_noBalanceChange() {
        Transaction tx = transactionService.addTransaction(
                mainAccount.getId(), Transaction.TYPE_INCOME, null,
                categorySalaryId, null, "Plat", 100.0, "EUR", LocalDate.now());

        double before = mainAccount.getCurrentBalance();
        Transaction updated = copyOf(tx);
        updated.setDescription("Iný popis"); // amount ostáva
        transactionService.updateTransaction(updated);

        assertEquals(before, mainAccount.getCurrentBalance(), 0.001);
    }

    // ============ VALIDÁCIE ============

    @Test
    @DisplayName("update: neexistujúce ID hodí not_found")
    void update_notFound_throws() {
        Transaction ghost = Transaction.builder()
                .id(99999)
                .accountId(mainAccount.getId())
                .transactionTypeId(Transaction.TYPE_INCOME)
                .amount(100.0)
                .description("Ghost")
                .completeDate(LocalDateTime.now())
                .build();

        TransactionException ex = assertThrows(TransactionException.class,
                () -> transactionService.updateTransaction(ghost));
        assertEquals("transaction.error.not_found", ex.getMessageKey());
    }

    @Test
    @DisplayName("update: záporný amount hodí invalid_amount")
    void update_negativeAmount_throws() {
        Transaction tx = transactionService.addTransaction(
                mainAccount.getId(), Transaction.TYPE_INCOME, null,
                categorySalaryId, null, "Plat", 100.0, "EUR", LocalDate.now());

        Transaction updated = copyOf(tx);
        updated.setAmount(-5.0);
        TransactionException ex = assertThrows(TransactionException.class,
                () -> transactionService.updateTransaction(updated));
        assertEquals("transaction.error.invalid_amount", ex.getMessageKey());
    }

    @Test
    @DisplayName("update: prázdny description hodí description_required")
    void update_blankDescription_throws() {
        Transaction tx = transactionService.addTransaction(
                mainAccount.getId(), Transaction.TYPE_INCOME, null,
                categorySalaryId, null, "Plat", 100.0, "EUR", LocalDate.now());

        Transaction updated = copyOf(tx);
        updated.setDescription("");
        assertThrows(TransactionException.class,
                () -> transactionService.updateTransaction(updated));
    }

    @Test
    @DisplayName("update: budúci completeDate hodí future_date")
    void update_futureDate_throws() {
        Transaction tx = transactionService.addTransaction(
                mainAccount.getId(), Transaction.TYPE_INCOME, null,
                categorySalaryId, null, "Plat", 100.0, "EUR", LocalDate.now());

        Transaction updated = copyOf(tx);
        updated.setCompleteDate(LocalDateTime.now().plusDays(1));
        TransactionException ex = assertThrows(TransactionException.class,
                () -> transactionService.updateTransaction(updated));
        assertEquals("transaction.error.future_date", ex.getMessageKey());
    }

    @Test
    @DisplayName("update: zmena typu (INCOME → EXPENSE) hodí type_cannot_change")
    void update_typeChange_throws() {
        Transaction tx = transactionService.addTransaction(
                mainAccount.getId(), Transaction.TYPE_INCOME, null,
                categorySalaryId, null, "Plat", 100.0, "EUR", LocalDate.now());

        Transaction updated = copyOf(tx);
        updated.setTransactionTypeId(Transaction.TYPE_EXPENSE);
        TransactionException ex = assertThrows(TransactionException.class,
                () -> transactionService.updateTransaction(updated));
        assertEquals("transaction.error.type_cannot_change", ex.getMessageKey());
    }

    @Test
    @DisplayName("update: nastavenie CATEGORY_SAVING ručne hodí invalid_category")
    void update_setSystemCategoryManually_throws() {
        Transaction tx = transactionService.addTransaction(
                mainAccount.getId(), Transaction.TYPE_EXPENSE, null,
                categoryFoodId, Transaction.CLASSIFICATION_NEED,
                "Obed", 50.0, "EUR", LocalDate.now());

        Transaction updated = copyOf(tx);
        updated.setCategoryId(Transaction.CATEGORY_SAVING);
        TransactionException ex = assertThrows(TransactionException.class,
                () -> transactionService.updateTransaction(updated));
        assertEquals("transaction.error.invalid_category", ex.getMessageKey());
    }

    @Test
    @DisplayName("update: zachovanie pôvodnej CATEGORY_SAVING (nemení sa) → OK")
    void update_keepSystemCategorySame_ok() {
        addSavingGoal(savingAccount.getId(), 10000.0, 500.0);
        transactionService.addTransaction(
                mainAccount.getId(), Transaction.TYPE_EXPENSE,
                savingAccount.getId(), 0, null,
                "Sporenie", 200.0, "EUR", LocalDate.now());

        Transaction expenseLeg = transactionRepo.findAll().stream()
                .filter(t -> t.getTransactionTypeId() == Transaction.TYPE_EXPENSE)
                .findFirst().orElseThrow();
        assertEquals(Integer.valueOf(Transaction.CATEGORY_SAVING),
                expenseLeg.getCategoryId());

        Transaction updated = copyOf(expenseLeg);
        updated.setDescription("Iný popis");
        assertDoesNotThrow(() -> transactionService.updateTransaction(updated));
    }

    // ============ BUG / NÁLEZY ============

    @Test
    @DisplayName("BUG nález: update transferu nepretransformuje zostatok DRUHEHO účtu")
    void update_transfer_doesNotUpdateOtherLeg() {
        addSavingGoal(savingAccount.getId(), 10000.0, 500.0);
        transactionService.addTransaction(
                mainAccount.getId(), Transaction.TYPE_EXPENSE,
                savingAccount.getId(), 0, null,
                "Transfer", 200.0, "EUR", LocalDate.now());

        assertEquals(800.0, mainAccount.getCurrentBalance(), 0.001);
        assertEquals(700.0, savingAccount.getCurrentBalance(), 0.001);

        Transaction expenseLeg = transactionRepo.findAll().stream()
                .filter(t -> t.getTransactionTypeId() == Transaction.TYPE_EXPENSE)
                .findFirst().orElseThrow();

        Transaction updated = copyOf(expenseLeg);
        updated.setAmount(300.0);
        transactionService.updateTransaction(updated);

        // expense leg balance: 800 - 100 (delta) = 700
        assertEquals(700.0, mainAccount.getCurrentBalance(), 0.001);

        // ALE income leg na saving accounte zostal +200 → saving zostal 700
        assertEquals(700.0, savingAccount.getCurrentBalance(), 0.001,
                "BUG: druhý leg transferu sa nepretransformoval");

        // Total cash: 700+700 = 1400 namiesto pôvodného 1500 → 100 € zmizlo
        double total = mainAccount.getCurrentBalance() + savingAccount.getCurrentBalance();
        assertEquals(1400.0, total, 0.001,
                "BUG: 100 € zmizlo zo systému kvôli editácii transferu");
    }
}