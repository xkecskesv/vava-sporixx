package sk.sporixx.service.testovanie;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sk.sporixx.model.Account;
import sk.sporixx.model.RecurringRule;
import sk.sporixx.model.Role;
import sk.sporixx.model.SavingGoal;
import sk.sporixx.model.Transaction;
import sk.sporixx.model.User;
import sk.sporixx.service.AuthService;
import sk.sporixx.service.AuthServiceImpl;
import sk.sporixx.service.OverviewService;
import sk.sporixx.service.OverviewServiceImpl;
import sk.sporixx.util.PasswordUtil;

import java.time.LocalDateTime;

/**
 * Inicializátor testovacích dát pre service vrstvu.
 * Dáta zodpovedajú Overview mockupom (Marek Moško).
 *
 * Prihlasovacie údaje:
 *   BEŽNÝ POUŽÍVATEĽ:  marek.mosko@stuba.sk / Heslo123!
 *   ADMIN:             admin@sporixx.sk / Admin123!
 *   RODIČ:             jana.mrkvickova@stuba.sk / Rodic123!
 */
@Getter
public class TestDataInitializer {

    private static final Logger logger = LoggerFactory.getLogger(TestDataInitializer.class);

    private final InMemoryUserRepository userRepository;
    private final InMemoryAccountRepository accountRepository;
    private final InMemoryTransactionRepository transactionRepository;
    private final InMemoryRecurringRuleRepository recurringRuleRepository;
    private final InMemorySavingGoalRepository savingGoalRepository;

    private final AuthService authService;
    private final OverviewService overviewService;

    private static final int REGION_SK = 1;

    public TestDataInitializer() {
        this.userRepository = new InMemoryUserRepository();
        this.accountRepository = new InMemoryAccountRepository();
        this.transactionRepository = new InMemoryTransactionRepository();
        this.recurringRuleRepository = new InMemoryRecurringRuleRepository();
        this.savingGoalRepository = new InMemorySavingGoalRepository();

        this.authService = new AuthServiceImpl(userRepository, accountRepository);
        this.overviewService = new OverviewServiceImpl(
                transactionRepository, recurringRuleRepository, savingGoalRepository);

        initTestData();
    }

    private void initTestData() {
        logger.info("=== Inicializácia testovacích dát ===");

        createRegularUser();
        createAdminUser();
        createParentUser();

        logger.info("=== Testovacie dáta pripravené: {} users, {} accounts, {} transactions, {} recurring rules, {} saving goals ===",
                userRepository.findAll().size(),
                accountRepository.findAll().size(),
                transactionRepository.findAll().size(),
                recurringRuleRepository.findAll().size(),
                savingGoalRepository.findAll().size());
    }

    // =========================================================================
    //  Bežný používateľ (Marek Moško - podľa Overview mockupov)
    //  Total balance: €124,256.53
    //  Main Account: €4,101.32 | Emergency Fund: €31,487.28 | Saving: €88,667.93
    // =========================================================================
    private void createRegularUser() {
        User savedUser = userRepository.save(User.builder()
                .firstName("Marek").lastName("Moško")
                .email("marek.mosko@stuba.sk")
                .passwordHash(PasswordUtil.hashPassword("Heslo123!"))
                .gender("M").createdAt(LocalDateTime.of(2026, 1, 15, 10, 0))
                .build());

        // ---- Účty ----
        Account mainAccount = accountRepository.save(Account.builder()
                .ownerUserId(savedUser.getId()).regionId(REGION_SK)
                .accountTypeId(Account.MAIN_ACCOUNT).defaultCurrencyCode("EUR")
                .description("Everyday account")
                .initialBalance(3_000.00).currentBalance(4_101.32)
                .isActive(true).createdAt(LocalDateTime.of(2026, 1, 15, 10, 0))
                .build());

        Account emergencyAccount = accountRepository.save(Account.builder()
                .ownerUserId(savedUser.getId()).regionId(REGION_SK)
                .accountTypeId(Account.EMERGENCY_FUND).defaultCurrencyCode("EUR")
                .description("Use in need")
                .initialBalance(20_000.00).currentBalance(31_487.28)
                .isActive(true).createdAt(LocalDateTime.of(2026, 1, 20, 14, 30))
                .build());

        Account savingAccount = accountRepository.save(Account.builder()
                .ownerUserId(savedUser.getId()).regionId(REGION_SK)
                .accountTypeId(Account.SAVING_ACCOUNT).defaultCurrencyCode("EUR")
                .description("Need money for Porsche")
                .initialBalance(50_000.00).currentBalance(88_667.93)
                .isActive(true).createdAt(LocalDateTime.of(2026, 2, 1, 9, 0))
                .build());

        Account privateAccount = accountRepository.save(Account.builder()
                .ownerUserId(savedUser.getId()).regionId(REGION_SK)
                .accountTypeId(Account.PRIVATE_ACCOUNT).defaultCurrencyCode("EUR")
                .description("Secret stash & Hobbies")
                .initialBalance(2_000.00).currentBalance(5_000.00)
                .isActive(true).createdAt(LocalDateTime.of(2026, 3, 1, 8, 0))
                .build());

        // ---- Saving Goal (zobrazuje "From €182,000.00" na Overview karte) ----
        savingGoalRepository.save(SavingGoal.builder()
                .accountId(savingAccount.getId()).name("Porsche 911")
                .targetAmount(182_000.00).currentAmount(88_667.93)
                .targetDate(LocalDateTime.of(2028, 12, 31, 0, 0))
                .isActive(true).createdAt(LocalDateTime.of(2026, 2, 1, 9, 0))
                .build());

        // ---- Dnešné transakcie (Activities panel - "Today") ----
        LocalDateTime today = LocalDateTime.now().withHour(10).withMinute(0).withSecond(0);

        transactionRepository.save(Transaction.builder()
                .accountId(mainAccount.getId())
                .transactionTypeId(Transaction.TYPE_EXPENSE)
                .spendingClassificationId(Transaction.CLASSIFICATION_WANT)
                .categoryId(1).amount(121.04).currencyCode("EUR")
                .description("Porsche merch")
                .completeDate(today).createdAt(today).build());

        transactionRepository.save(Transaction.builder()
                .accountId(savingAccount.getId())
                .transactionTypeId(Transaction.TYPE_EXPENSE)
                .spendingClassificationId(Transaction.CLASSIFICATION_NEED)
                .categoryId(2).amount(89.41).currencyCode("EUR")
                .description("Groceries")
                .completeDate(today.withHour(11)).createdAt(today.withHour(11)).build());

        transactionRepository.save(Transaction.builder()
                .accountId(mainAccount.getId())
                .transactionTypeId(Transaction.TYPE_EXPENSE)
                .spendingClassificationId(Transaction.CLASSIFICATION_NEED)
                .categoryId(3).amount(104.72).currencyCode("EUR")
                .description("Fuel")
                .completeDate(today.withHour(14)).createdAt(today.withHour(14)).build());

        // ---- Včerajšie transakcie (Activities panel - "Yesterday") ----
        LocalDateTime yesterday = today.minusDays(1);

        transactionRepository.save(Transaction.builder()
                .accountId(mainAccount.getId())
                .transactionTypeId(Transaction.TYPE_INCOME)
                .spendingClassificationId(0)
                .categoryId(4).amount(14.29).currencyCode("EUR")
                .description("From Adam Aliexpress")
                .completeDate(yesterday.withHour(9)).createdAt(yesterday.withHour(9)).build());

        transactionRepository.save(Transaction.builder()
                .accountId(mainAccount.getId())
                .transactionTypeId(Transaction.TYPE_EXPENSE)
                .spendingClassificationId(Transaction.CLASSIFICATION_NEED)
                .categoryId(5).amount(28.04).currencyCode("EUR")
                .description("ChatGPT sub")
                .completeDate(yesterday.withHour(15)).createdAt(yesterday.withHour(15)).build());

        // ---- Staršie transakcie (pre Analytics graf - mesačné príjmy) ----
        createMonthlyIncomeData(mainAccount.getId());

        // ---- Upcoming payments (Activities panel - "Upcoming Payment") ----
        recurringRuleRepository.save(RecurringRule.builder()
                .accountId(mainAccount.getId()).categoryId(1).statusId(1)
                .transactionTypeId(Transaction.TYPE_EXPENSE)
                .spendingClassificationId(Transaction.CLASSIFICATION_WANT)
                .amount(121.04).description("Porsche merch")
                .frequencyType("MONTHLY").frequencyInterval(1)
                .startDate(LocalDateTime.of(2026, 1, 1, 0, 0))
                .nextDueDate(today.plusDays(3))
                .isActive(1).generatedCount(3)
                .createdAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .build());

        recurringRuleRepository.save(RecurringRule.builder()
                .accountId(mainAccount.getId()).categoryId(5).statusId(1)
                .transactionTypeId(Transaction.TYPE_EXPENSE)
                .spendingClassificationId(Transaction.CLASSIFICATION_NEED)
                .amount(28.04).description("ChatGPT sub")
                .frequencyType("MONTHLY").frequencyInterval(1)
                .startDate(LocalDateTime.of(2026, 2, 1, 0, 0))
                .nextDueDate(today.plusDays(12))
                .isActive(1).generatedCount(2)
                .createdAt(LocalDateTime.of(2026, 2, 1, 0, 0))
                .build());

        recurringRuleRepository.save(RecurringRule.builder()
                .accountId(mainAccount.getId()).categoryId(6).statusId(1)
                .transactionTypeId(Transaction.TYPE_EXPENSE)
                .spendingClassificationId(Transaction.CLASSIFICATION_NEED)
                .amount(450.00).description("Rent")
                .frequencyType("MONTHLY").frequencyInterval(1)
                .startDate(LocalDateTime.of(2026, 1, 1, 0, 0))
                .nextDueDate(today.plusDays(18))
                .isActive(1).generatedCount(3)
                .createdAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .build());

        logger.info("Vytvorený USER: {} (3 účty, {} transakcií, 3 recurring, 1 saving goal)",
                savedUser.getEmail(), transactionRepository.findAll().size());
    }

    /**
     * Generuje mesačné príjmové transakcie za posledných 12 mesiacov pre Analytics graf.
     * Sumy zodpovedajú tvarú grafu na Overview mockupe.
     */
    private void createMonthlyIncomeData(int mainAccountId) {
        double[] monthlyAmounts = {
                4_200.00,   // -11 mesiacov
                3_800.00,   // -10
                5_100.00,   // -9
                6_500.00,   // -8
                4_900.00,   // -7
                7_200.00,   // -6
                8_100.00,   // -5
                6_800.00,   // -4
                9_500.00,   // -3
                11_200.00,  // -2
                8_400.00,   // -1 (minulý mesiac)
                3_200.00    // tento mesiac (zatiaľ)
        };

        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < monthlyAmounts.length; i++) {
            int monthsAgo = 11 - i;
            LocalDateTime date = now.minusMonths(monthsAgo).withDayOfMonth(15).withHour(12);

            transactionRepository.save(Transaction.builder()
                    .accountId(mainAccountId)
                    .transactionTypeId(Transaction.TYPE_INCOME)
                    .spendingClassificationId(0)
                    .categoryId(4)
                    .amount(monthlyAmounts[i])
                    .currencyCode("EUR")
                    .description("Salary")
                    .completeDate(date).createdAt(date)
                    .build());
        }
    }

    // =========================================================================
    //  Admin
    // =========================================================================
    private void createAdminUser() {
        User savedAdmin = userRepository.save(User.builder()
                .firstName("Admin").lastName("Sporixx")
                .email("admin@sporixx.sk")
                .passwordHash(PasswordUtil.hashPassword("Admin123!"))
                .gender("M").role(Role.ADMIN)
                .createdAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .build());

        accountRepository.save(Account.builder()
                .ownerUserId(savedAdmin.getId()).regionId(REGION_SK)
                .accountTypeId(Account.MAIN_ACCOUNT).defaultCurrencyCode("EUR")
                .description("Everyday account")
                .initialBalance(0.0).currentBalance(0.0)
                .isActive(true).createdAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .build());

        logger.info("Vytvorený ADMIN: {}", savedAdmin.getEmail());
    }

    // =========================================================================
    //  Rodič / Family Manager
    // =========================================================================
    private void createParentUser() {
        User savedParent = userRepository.save(User.builder()
                .firstName("Jana").lastName("Mrkvičková")
                .email("jana.mrkvickova@stuba.sk")
                .passwordHash(PasswordUtil.hashPassword("Rodic123!"))
                .gender("F").role(Role.FAMILY_MANAGER)
                .createdAt(LocalDateTime.of(2026, 2, 10, 8, 0))
                .build());

        accountRepository.save(Account.builder()
                .ownerUserId(savedParent.getId()).regionId(REGION_SK)
                .accountTypeId(Account.MAIN_ACCOUNT).defaultCurrencyCode("EUR")
                .description("Everyday account")
                .initialBalance(10_000.00).currentBalance(12_500.00)
                .isActive(true).createdAt(LocalDateTime.of(2026, 2, 10, 8, 0))
                .build());

        accountRepository.save(Account.builder()
                .ownerUserId(savedParent.getId()).regionId(REGION_SK)
                .accountTypeId(Account.EMERGENCY_FUND).defaultCurrencyCode("EUR")
                .description("Use in need")
                .initialBalance(5_000.00).currentBalance(5_000.00)
                .isActive(true).createdAt(LocalDateTime.of(2026, 2, 15, 12, 0))
                .build());

        logger.info("Vytvorený PARENT: {}", savedParent.getEmail());
    }
}
