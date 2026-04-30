package sk.sporixx.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import sk.sporixx.model.Account;
import sk.sporixx.model.Role;
import sk.sporixx.model.Transaction;
import sk.sporixx.model.User;
import sk.sporixx.service.testovanie.InMemoryBudgetRepository;
import sk.sporixx.service.testovanie.InMemoryTransactionRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Spoločný základ pre testy {@link BudgetService}.
 * <p>
 * Setup pripraví:
 *  <ul>
 *      <li>1 prihláseného usera (ID = 1)</li>
 *      <li>1 main account, 1 emergency fund account (oba aktívne)</li>
 *      <li>prázdne in-memory repos (Budget aj Transaction)</li>
 *  </ul>
 * <p>
 * Tearovanie čistí session — singleton {@code SessionManager} je proces-wide.
 */
abstract class BudgetServiceTestSupport {

    protected InMemoryBudgetRepository budgetRepo;
    protected InMemoryTransactionRepository transactionRepo;
    protected BudgetService budgetService;

    protected User testUser;
    protected Account mainAccount;
    protected Account emergencyAccount;

    @BeforeEach
    void baseSetUp() {
        budgetRepo = new InMemoryBudgetRepository();
        transactionRepo = new InMemoryTransactionRepository();
        budgetService = new BudgetServiceImpl(budgetRepo, transactionRepo);

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
                .initialBalance(1000.0)
                .currentBalance(1000.0)
                .isActive(true)
                .createdAt(LocalDateTime.now().minusYears(1))
                .build();

        emergencyAccount = Account.builder()
                .id(2)
                .ownerUserId(testUser.getId())
                .accountTypeId(Account.EMERGENCY_FUND)
                .regionId(1)
                .defaultCurrencyCode("EUR")
                .initialBalance(500.0)
                .currentBalance(500.0)
                .isActive(true)
                .createdAt(LocalDateTime.now().minusYears(1))
                .build();

        SessionManager.getInstance().setSession(testUser,
                List.of(mainAccount, emergencyAccount));
    }

    @AfterEach
    void baseTearDown() {
        SessionManager.getInstance().clearSession();
    }

    // ============ Helpers ============

    /** Prelogovanie iba s main account-om (bez emergency). */
    protected void logInWithoutEmergencyAccount() {
        SessionManager.getInstance().clearSession();
        SessionManager.getInstance().setSession(testUser, List.of(mainAccount));
    }

    /** Prelogovanie bez žiadnych účtov. */
    protected void logInWithoutAccounts() {
        SessionManager.getInstance().clearSession();
        SessionManager.getInstance().setSession(testUser, new ArrayList<>());
    }

    /** Vlož transakciu na emergency account priamo cez repo (pre history testy). */
    protected void insertEmergencyTransaction(int typeId, double amount, LocalDateTime date) {
        Transaction t = Transaction.builder()
                .accountId(emergencyAccount.getId())
                .transactionTypeId(typeId)
                .amount(amount)
                .currencyCode("EUR")
                .description("Test")
                .completeDate(date)
                .createdAt(LocalDateTime.now())
                .build();
        transactionRepo.save(t);
    }
}
