package sk.sporixx.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import sk.sporixx.model.*;
import sk.sporixx.repository.*;
import sk.sporixx.service.testovanie.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spoločný základ pre testy TransactionService.
 *
 * Setup pripraví:
 *  - 1 prihláseného usera
 *  - 1 main account, 1 saving account (oba aktívne, owned by user)
 *  - 3 systémové kategórie (Food=1, Salary=2, plus systémové saving=6, saving_expense=7)
 *
 * Tearovanie čistí session — singleton SessionManager je proces-wide,
 * ak by sme zabudli, ďalší test by mal "preliaty" prihlásený stav.
 */
abstract class TransactionServiceTestSupport {

    protected InMemoryTransactionRepository transactionRepo;
    protected InMemoryAccountRepository accountRepo;
    protected InMemoryCategoryRepository categoryRepo;
    protected InMemorySavingGoalRepository savingGoalRepo;
    protected TransactionService transactionService;

    protected User testUser;
    protected Account mainAccount;
    protected Account savingAccount;
    protected Account secondMainAccount;  // pre transfer testy bez savingu

    // Kategórie — IDs sa priradia auto pri save
    protected int categoryFoodId;
    protected int categorySalaryId;
    // CATEGORY_SAVING (=6), CATEGORY_SAVING_EXPENSE (=7) — systémové konštanty z Transaction modelu
    // tieto musíme do repo vložiť priamo s pevným ID

    @BeforeEach
    void baseSetUp() {
        transactionRepo = new InMemoryTransactionRepository();
        accountRepo = new InMemoryAccountRepository();
        categoryRepo = new InMemoryCategoryRepository();
        savingGoalRepo = new InMemorySavingGoalRepository();

        transactionService = new TransactionServiceImpl(
                transactionRepo, accountRepo, categoryRepo, savingGoalRepo);

        // user
        testUser = User.builder()
                .id(1)
                .email("test@sporixx.sk")
                .firstName("Test")
                .lastName("User")
                .role(Role.USER)
                .isActive(true)
                .build();

        // 2 aktívne účty pre usera + 1 saving account
        mainAccount = Account.builder()
                .ownerUserId(testUser.getId())
                .accountTypeId(Account.MAIN_ACCOUNT)
                .regionId(1)
                .defaultCurrencyCode("EUR")
                .initialBalance(1000.0)
                .currentBalance(1000.0)
                .isActive(true)
                .createdAt(LocalDateTime.now().minusDays(30))
                .build();
        accountRepo.save(mainAccount);

        savingAccount = Account.builder()
                .ownerUserId(testUser.getId())
                .accountTypeId(Account.SAVING_ACCOUNT)
                .regionId(1)
                .defaultCurrencyCode("EUR")
                .initialBalance(500.0)
                .currentBalance(500.0)
                .isActive(true)
                .createdAt(LocalDateTime.now().minusDays(30))
                .build();
        accountRepo.save(savingAccount);

        secondMainAccount = Account.builder()
                .ownerUserId(testUser.getId())
                .accountTypeId(Account.EMERGENCY_FUND)
                .regionId(1)
                .defaultCurrencyCode("EUR")
                .initialBalance(200.0)
                .currentBalance(200.0)
                .isActive(true)
                .createdAt(LocalDateTime.now().minusDays(30))
                .build();
        accountRepo.save(secondMainAccount);

        // kategórie — najprv 2 user kategórie, potom systémové s pevnymi IDs
        Category food = Category.builder()
                .userId(testUser.getId())
                .name("Food")
                .createdAt(LocalDateTime.now())
                .build();
        categoryRepo.save(food);
        categoryFoodId = food.getId();

        Category salary = Category.builder()
                .userId(testUser.getId())
                .name("Salary")
                .createdAt(LocalDateTime.now())
                .build();
        categoryRepo.save(salary);
        categorySalaryId = salary.getId();

        // SYSTÉMOVÉ kategórie — Transaction.CATEGORY_SAVING=6, CATEGORY_SAVING_EXPENSE=7
        // musíme ich uložiť tak aby mali presne tieto IDs (transfer logic ich tak používa)
        // doplníme dummy categories až do ID 5 a potom 6 + 7
        for (int i = 3; i <= 5; i++) {
            Category dummy = Category.builder()
                    .userId(null)
                    .name("dummy" + i)
                    .createdAt(LocalDateTime.now())
                    .build();
            categoryRepo.save(dummy);
        }
        Category sysSaving = Category.builder()
                .userId(null)
                .name("Saving")
                .createdAt(LocalDateTime.now())
                .build();
        categoryRepo.save(sysSaving);  // ID = 6 = CATEGORY_SAVING
        Category sysSavingExp = Category.builder()
                .userId(null)
                .name("Saving Expense")
                .createdAt(LocalDateTime.now())
                .build();
        categoryRepo.save(sysSavingExp);  // ID = 7 = CATEGORY_SAVING_EXPENSE

        // sanity check — testy spoliehaju že systémové kategórie maju ID 6 a 7
        if (sysSaving.getId() != Transaction.CATEGORY_SAVING ||
                sysSavingExp.getId() != Transaction.CATEGORY_SAVING_EXPENSE) {
            throw new IllegalStateException(
                    "Test setup: system category IDs must be 6 and 7. Got: " +
                            sysSaving.getId() + ", " + sysSavingExp.getId());
        }

        // Prihlásime usera s týmito 3 účtami
        SessionManager.getInstance().setSession(testUser,
                List.of(mainAccount, savingAccount, secondMainAccount));
    }

    @AfterEach
    void baseTearDown() {
        SessionManager.getInstance().clearSession();
    }

    // ==================== HELPERS ====================

    /** Pridá saving goal pre saving account — využíva sa v testoch transferov. */
    protected SavingGoal addSavingGoal(int accountId, double target, double current) {
        SavingGoal goal = SavingGoal.builder()
                .accountId(accountId)
                .name("Test goal")
                .goalTypeId(1)
                .targetAmount(target)
                .currentAmount(current)
                .targetDate(LocalDateTime.now().plusYears(1))
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
        return savingGoalRepo.save(goal);
    }

    /** Vytvorí transakciu priamo cez repo (bypass service validáciu). */
    protected Transaction insertRawTransaction(int accountId, int typeId,
                                               Integer categoryId, double amount,
                                               String desc, LocalDateTime date) {
        Transaction t = Transaction.builder()
                .accountId(accountId)
                .transactionTypeId(typeId)
                .categoryId(categoryId)
                .amount(amount)
                .currencyCode("EUR")
                .description(desc)
                .completeDate(date)
                .createdAt(LocalDateTime.now())
                .build();
        return transactionRepo.save(t);
    }
}
