package sk.sporixx.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import sk.sporixx.model.*;
import sk.sporixx.service.testovanie.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spoločný základ pre testy ReportsService.
 * Pripravuje:
 *  - prihláseného usera + main + saving + emergency účet
 *  - prázdne in-memory repos (transakcie a recurring rules sa pridávajú v testoch)
 *
 * Reports neaktualizujú balance — sú to read-only reporty, takže oproti
 * TransactionServiceTestSupport sme jednoduchší (nie je tu CategoryRepository).
 */
abstract class ReportsServiceTestSupport {

    protected InMemoryTransactionRepository transactionRepo;
    protected InMemoryRecurringRuleRepository recurringRepo;
    protected InMemorySavingGoalRepository savingGoalRepo;
    protected ReportsService reportsService;

    protected User testUser;
    protected Account mainAccount;
    protected Account savingAccount;

    @BeforeEach
    void baseSetUp() {
        transactionRepo = new InMemoryTransactionRepository();
        recurringRepo = new InMemoryRecurringRuleRepository();
        savingGoalRepo = new InMemorySavingGoalRepository();

        reportsService = new ReportsServiceImpl(
                transactionRepo, recurringRepo, savingGoalRepo);

        testUser = User.builder()
                .id(1)
                .email("test@sporixx.sk")
                .firstName("Test")
                .lastName("User")
                .role(Role.USER)
                .isActive(true)
                .build();

        mainAccount = Account.builder()
                .id(1)
                .ownerUserId(testUser.getId())
                .accountTypeId(Account.MAIN_ACCOUNT)
                .regionId(1)
                .defaultCurrencyCode("EUR")
                .description("Main")
                .initialBalance(1000.0)
                .currentBalance(1000.0)
                .isActive(true)
                .createdAt(LocalDateTime.now().minusYears(1))
                .build();

        savingAccount = Account.builder()
                .id(2)
                .ownerUserId(testUser.getId())
                .accountTypeId(Account.SAVING_ACCOUNT)
                .regionId(1)
                .defaultCurrencyCode("EUR")
                .description("Sporenie na auto")
                .initialBalance(500.0)
                .currentBalance(500.0)
                .isActive(true)
                .createdAt(LocalDateTime.now().minusYears(1))
                .build();

        SessionManager.getInstance().setSession(testUser,
                List.of(mainAccount, savingAccount));
    }

    @AfterEach
    void baseTearDown() {
        SessionManager.getInstance().clearSession();
    }

    /** Vytvorí transakciu priamo cez repo (bypass akejkoľvek service validácie). */
    protected Transaction tx(int accountId, int typeId, Integer categoryId,
                             Integer classification, double amount, LocalDateTime date) {
        Transaction t = Transaction.builder()
                .accountId(accountId)
                .transactionTypeId(typeId)
                .categoryId(categoryId)
                .spendingClassificationId(classification)
                .amount(amount)
                .currencyCode("EUR")
                .description("Test")
                .completeDate(date)
                .createdAt(LocalDateTime.now())
                .build();
        return transactionRepo.save(t);
    }

    /** Recurring rule pre Recurring Expenses report. */
    protected RecurringRule rule(int accountId, double amount, int classificationId,
                                 String description) {
        RecurringRule r = RecurringRule.builder()
                .accountId(accountId)
                .amount(amount)
                .description(description)
                .spendingClassificationId(classificationId)
                .transactionTypeId(Transaction.TYPE_EXPENSE)
                .frequencyType("MONTHLY")
                .frequencyInterval(1)
                .startDate(LocalDateTime.now().minusMonths(1))
                .nextDueDate(LocalDateTime.now().plusDays(5))
                .isActive(true)
                .createdAt(LocalDateTime.now().minusMonths(1))
                .build();
        return recurringRepo.save(r);
    }

    /** Saving goal — pre Saving Accounts report. */
    protected SavingGoal goal(int accountId, double target, double current,
                              LocalDateTime created, LocalDateTime targetDate) {
        SavingGoal g = SavingGoal.builder()
                .accountId(accountId)
                .name("Goal")
                .goalTypeId(1)
                .targetAmount(target)
                .currentAmount(current)
                .targetDate(targetDate)
                .createdAt(created)
                .isActive(true)
                .build();
        return savingGoalRepo.save(g);
    }
}
