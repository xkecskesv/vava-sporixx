package sk.sporixx.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import sk.sporixx.dto.SearchCriteria;
import sk.sporixx.model.*;
import sk.sporixx.service.testovanie.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Základná trieda pre Management testy.
 *
 * Každý test dostane čistý stav:
 *   - InMemory repozitáre (kategórie, účty, savingGoals, recurringRules, accountAccess)
 *   - SessionManager s prihláseným testovacím používateľom
 *   - mainAccount (MAIN_ACCOUNT, balance 1000.0 EUR)
 *   - emergencyAccount (EMERGENCY_FUND, balance 2000.0 EUR)
 *
 * Stav SessionManagera sa vyčistí pred aj po každom teste.
 */
abstract class ManagementServiceTestSupport {

    protected InMemoryCategoryRepository categoryRepo;
    protected InMemoryTransactionRepository transactionRepo;
    protected InMemoryRecurringRuleRepository recurringRuleRepo;
    protected InMemorySavingGoalRepository savingGoalRepo;
    protected InMemoryAccountRepository accountRepo;
    protected InMemoryAccountAccessRepository accountAccessRepo;

    protected CategoryService categoryService;
    protected RecurringRuleService recurringRuleService;
    protected AccountService accountService;

    protected Account mainAccount;
    protected Account emergencyAccount;

    // No-op TransactionService stub pre RecurringRuleServiceImpl
    protected final TransactionService noOpTransactionService = new TransactionService() {
        @Override
        public List<Transaction> getTransactions(int accountId) { return List.of(); }
        @Override
        public List<Transaction> getAllTransactions() { return List.of(); }
        @Override
        public List<Transaction> searchTransactions(SearchCriteria criteria) { return List.of(); }
        @Override
        public List<Transaction> searchTransactions(SearchCriteria criteria, int accountId) { return List.of(); }
        @Override
        public Transaction addTransaction(int accountId, int transactionTypeId,
                                          Integer targetAccountId, int categoryId,
                                          Integer spendingClassificationId, String description,
                                          double amount, String currencyCode, LocalDate date) {
            return null; // no-op
        }
        @Override
        public void updateTransaction(Transaction updatedTransaction) {}
        @Override
        public void deleteTransactions(List<Integer> transactionIds) {}
        @Override
        public List<Transaction> searchTransactions(String rawInput, int accountId) { return List.of(); }
        @Override
        public List<Transaction> searchTransactions(String rawInput) { return List.of(); }
    };

    @BeforeEach
    void setUpManagement() {
        categoryRepo       = new InMemoryCategoryRepository();
        transactionRepo    = new InMemoryTransactionRepository();
        recurringRuleRepo  = new InMemoryRecurringRuleRepository();
        savingGoalRepo     = new InMemorySavingGoalRepository();
        accountRepo        = new InMemoryAccountRepository();
        accountAccessRepo  = new InMemoryAccountAccessRepository();

        categoryService = new CategoryServiceImpl(categoryRepo, transactionRepo);
        recurringRuleService = new RecurringRuleServiceImpl(recurringRuleRepo, noOpTransactionService);
        accountService  = new AccountServiceImpl(accountRepo, savingGoalRepo, accountAccessRepo);

        // ---- testovací používateľ ----
        User testUser = User.builder()
                .id(1)
                .firstName("Test").lastName("User")
                .email("test@sporixx.sk")
                .gender("M")
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .build();

        // ---- povinné účty (id=0 → repo priradí ID cez idGenerator) ----
        mainAccount = accountRepo.save(Account.builder()
                .id(0).ownerUserId(1).regionId(1)
                .accountTypeId(Account.MAIN_ACCOUNT)
                .defaultCurrencyCode("EUR")
                .description("Everyday account")
                .initialBalance(1000.0).currentBalance(1000.0)
                .isActive(true).createdAt(LocalDateTime.now())
                .build());

        emergencyAccount = accountRepo.save(Account.builder()
                .id(0).ownerUserId(1).regionId(1)
                .accountTypeId(Account.EMERGENCY_FUND)
                .defaultCurrencyCode("EUR")
                .description("Emergency fund")
                .initialBalance(2000.0).currentBalance(2000.0)
                .isActive(true).createdAt(LocalDateTime.now())
                .build());

        SessionManager.getInstance().setSession(testUser,
                List.of(mainAccount, emergencyAccount));
    }

    @AfterEach
    void tearDownSession() {
        SessionManager.getInstance().clearSession();
    }

    // ---- helper metódy pre testy ----

    protected Category addSystemCategory(String name) {
        Category cat = Category.builder()
                .userId(null)
                .name(name)
                .createdAt(LocalDateTime.now())
                .build();
        return categoryRepo.save(cat);
    }

    protected Category addUserCategory(String name) {
        Category cat = Category.builder()
                .userId(1)
                .name(name)
                .createdAt(LocalDateTime.now())
                .build();
        return categoryRepo.save(cat);
    }
}


