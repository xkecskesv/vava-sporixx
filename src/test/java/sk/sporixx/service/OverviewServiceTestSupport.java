package sk.sporixx.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import sk.sporixx.model.*;
import sk.sporixx.service.testovanie.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Základná trieda pre OverviewService testy.
 *
 * Každý test dostane čistý stav:
 *   - InMemory repozitáre (transakcie, recurring rules, saving goals)
 *   - SessionManager s prihláseným testovacím používateľom
 *   - mainAccount (MAIN_ACCOUNT, balance 1000.0 EUR)
 *   - savingAccount (SAVING_ACCOUNT, balance 500.0 EUR)
 *   - emergencyAccount (EMERGENCY_FUND, balance 2000.0 EUR)
 *
 * Stav SessionManagera sa vyčistí pred aj po každom teste.
 */
abstract class OverviewServiceTestSupport {

    protected InMemoryTransactionRepository transactionRepo;
    protected InMemoryRecurringRuleRepository recurringRuleRepo;
    protected InMemorySavingGoalRepository savingGoalRepo;

    protected OverviewService overviewService;

    protected Account mainAccount;
    protected Account savingAccount;
    protected Account emergencyAccount;

    protected static final int CAT_FOOD       = 1;
    protected static final int CAT_CLOTHING    = 2;
    protected static final int CAT_ENTERTAIN   = 3;
    protected static final int CAT_RENT        = 4;
    protected static final int CAT_TRANSPORT   = 5;
    protected static final int CAT_SALARY      = 9;

    @BeforeEach
    void setUpOverview() {
        transactionRepo    = new InMemoryTransactionRepository();
        recurringRuleRepo  = new InMemoryRecurringRuleRepository();
        savingGoalRepo     = new InMemorySavingGoalRepository();

        overviewService = new OverviewServiceImpl(
                transactionRepo, recurringRuleRepo, savingGoalRepo);

        // ---- testovací používateľ ----
        User testUser = User.builder()
                .id(1)
                .firstName("Test").lastName("User")
                .email("test@sporixx.sk")
                .gender("M")
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .build();

        // ---- účty ----
        mainAccount = Account.builder()
                .id(1)
                .ownerUserId(1).regionId(1)
                .accountTypeId(Account.MAIN_ACCOUNT)
                .defaultCurrencyCode("EUR")
                .description("Everyday account")
                .initialBalance(1000.0).currentBalance(1000.0)
                .isActive(true).createdAt(LocalDateTime.now())
                .build();

        emergencyAccount = Account.builder()
                .id(2)
                .ownerUserId(1).regionId(1)
                .accountTypeId(Account.EMERGENCY_FUND)
                .defaultCurrencyCode("EUR")
                .description("Use in need")
                .initialBalance(2000.0).currentBalance(2000.0)
                .isActive(true).createdAt(LocalDateTime.now())
                .build();

        savingAccount = Account.builder()
                .id(3)
                .ownerUserId(1).regionId(1)
                .accountTypeId(Account.SAVING_ACCOUNT)
                .defaultCurrencyCode("EUR")
                .description("Saving for Porsche")
                .initialBalance(500.0).currentBalance(500.0)
                .isActive(true).createdAt(LocalDateTime.now())
                .build();

        SessionManager.getInstance().setSession(testUser,
                List.of(mainAccount, emergencyAccount, savingAccount));
    }

    @AfterEach
    void tearDownSession() {
        SessionManager.getInstance().clearSession();
    }

    // ---- helper metódy pre testy ----

    protected void addIncome(int accountId, double amount, LocalDateTime date) {
        transactionRepo.save(Transaction.builder()
                .accountId(accountId)
                .transactionTypeId(Transaction.TYPE_INCOME)
                .categoryId(CAT_SALARY)
                .amount(amount).currencyCode("EUR")
                .description("Income")
                .completeDate(date).createdAt(date)
                .build());
    }

    protected void addExpense(int accountId, double amount, LocalDateTime date) {
        transactionRepo.save(Transaction.builder()
                .accountId(accountId)
                .transactionTypeId(Transaction.TYPE_EXPENSE)
                .spendingClassificationId(Transaction.CLASSIFICATION_NEED)
                .categoryId(CAT_FOOD)
                .amount(amount).currencyCode("EUR")
                .description("Expense")
                .completeDate(date).createdAt(date)
                .build());
    }

    protected void addRecurringRule(int accountId, double amount, LocalDateTime nextDueDate) {
        recurringRuleRepo.save(RecurringRule.builder()
                .accountId(accountId)
                .categoryId(CAT_RENT)
                .statusId(1)
                .transactionTypeId(Transaction.TYPE_EXPENSE)
                .spendingClassificationId(Transaction.CLASSIFICATION_NEED)
                .amount(amount).description("Recurring payment")
                .frequencyType("MONTHLY").frequencyInterval(1)
                .startDate(LocalDateTime.now().minusMonths(1))
                .nextDueDate(nextDueDate)
                .isActive(true).generatedCount(1)
                .createdAt(LocalDateTime.now())
                .build());
    }

    protected void addSavingGoal(int accountId, double target, double current) {
        savingGoalRepo.save(SavingGoal.builder()
                .accountId(accountId)
                .name("Test goal")
                .goalTypeId(1)
                .targetAmount(target).currentAmount(current)
                .targetDate(LocalDateTime.now().plusYears(2))
                .isActive(true).createdAt(LocalDateTime.now())
                .build());
    }
}

