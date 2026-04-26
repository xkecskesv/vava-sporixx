package sk.sporixx.service.testovanie;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sk.sporixx.model.*;
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
 *
 * Kategórie (categoryId):
 *   1 = Clothing
 *   2 = Groceries
 *   3 = Transport
 *   4 = Paycheck / Income
 *   5 = Subscriptions
 *   6 = Rent
 *   7 = Entertainment
 *   8 = Sport
 *   9 = Utilities
 */
@Getter
public class TestDataInitializer {

    private static final Logger logger = LoggerFactory.getLogger(TestDataInitializer.class);

    private final InMemoryUserRepository userRepository;
    private final InMemoryAccountRepository accountRepository;
    private final InMemoryTransactionRepository transactionRepository;
    private final InMemoryRecurringRuleRepository recurringRuleRepository;
    private final InMemorySavingGoalRepository savingGoalRepository;
    private final InMemoryCategoryRepository categoryRepository;
    private final InMemoryBudgetRepository budgetRepository;
    private final InMemoryAccountAccessRepository accountAccessRepository;
    private final InMemoryFamilyRequestRepository familyRequestRepository;

    private final AuthService authService;
    private final OverviewService overviewService;

    private static final int REGION_SK = 1;

    // Kategórie
    private static final int CAT_FOOD           = 1;
    private static final int CAT_CLOTHING       = 2;
    private static final int CAT_ENTERTAINMENT  = 3;
    private static final int CAT_RENT           = 4;
    private static final int CAT_TRANSPORT      = 5;
    private static final int CAT_INVESTMENT     = 8;
// 6 = Saving, 7 = Saving Expense

    public TestDataInitializer() {
        this.userRepository        = new InMemoryUserRepository();
        this.accountRepository     = new InMemoryAccountRepository();
        this.transactionRepository = new InMemoryTransactionRepository();
        this.recurringRuleRepository = new InMemoryRecurringRuleRepository();
        this.savingGoalRepository  = new InMemorySavingGoalRepository();

        this.authService    = new AuthServiceImpl(userRepository, accountRepository);
        this.overviewService = new OverviewServiceImpl(
                transactionRepository, recurringRuleRepository, savingGoalRepository);
        this.categoryRepository = new InMemoryCategoryRepository();
        this.budgetRepository = new InMemoryBudgetRepository();
        this.accountAccessRepository = new InMemoryAccountAccessRepository();
        this.familyRequestRepository = new InMemoryFamilyRequestRepository();

        initTestData();
    }

    private void initTestData() {
        logger.info("=== Inicializácia testovacích dát ===");

        // Systémové kategórie — musia byť prvé, ID musia zodpovedať Transaction konštantám
        categoryRepository.save(Category.builder().userId(null).name("Food").createdAt(LocalDateTime.now()).build());
        categoryRepository.save(Category.builder().userId(null).name("Clothing").createdAt(LocalDateTime.now()).build());
        categoryRepository.save(Category.builder().userId(null).name("Entertainment").createdAt(LocalDateTime.now()).build());
        categoryRepository.save(Category.builder().userId(null).name("Rent").createdAt(LocalDateTime.now()).build());
        categoryRepository.save(Category.builder().userId(null).name("Transportation").createdAt(LocalDateTime.now()).build());
        categoryRepository.save(Category.builder().userId(null).name("Saving").createdAt(LocalDateTime.now()).build());
        categoryRepository.save(Category.builder().userId(null).name("Saving Expense").createdAt(LocalDateTime.now()).build());
        categoryRepository.save(Category.builder().userId(null).name("Investment").createdAt(LocalDateTime.now()).build());

        createRegularUser();
        createAdminUser();
        createParentUser();
        createEmptyUser();

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

        accountRepository.save(Account.builder()
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

        accountRepository.save(Account.builder()
                .ownerUserId(savedUser.getId()).regionId(REGION_SK)
                .accountTypeId(Account.PRIVATE_ACCOUNT).defaultCurrencyCode("EUR")
                .description("Secret stash & Hobbies")
                .initialBalance(2_000.00).currentBalance(5_000.00)
                .isActive(true).createdAt(LocalDateTime.of(2026, 3, 1, 8, 0))
                .build());

        // ---- Saving Goal ----
        savingGoalRepository.save(SavingGoal.builder()
                .accountId(savingAccount.getId()).name("Porsche 911")
                .targetAmount(182_000.00).currentAmount(88_667.93)
                .targetDate(LocalDateTime.of(2028, 12, 31, 0, 0))
                .isActive(true).createdAt(LocalDateTime.of(2026, 2, 1, 9, 0))
                .build());

        // ---- Dnešné transakcie ----
        LocalDateTime today = LocalDateTime.now().withHour(10).withMinute(0).withSecond(0);

        transactionRepository.save(Transaction.builder()
                .accountId(mainAccount.getId())
                .transactionTypeId(Transaction.TYPE_EXPENSE)
                .spendingClassificationId(Transaction.CLASSIFICATION_WANT)
                .categoryId(CAT_CLOTHING).amount(121.04).currencyCode("EUR")
                .description("Porsche merch")
                .completeDate(today).createdAt(today).build());

        transactionRepository.save(Transaction.builder()
                .accountId(mainAccount.getId())
                .transactionTypeId(Transaction.TYPE_EXPENSE)
                .spendingClassificationId(Transaction.CLASSIFICATION_NEED)
                .categoryId(CAT_FOOD).amount(89.41).currencyCode("EUR")
                .description("Groceries")
                .completeDate(today.withHour(11)).createdAt(today.withHour(11)).build());

        transactionRepository.save(Transaction.builder()
                .accountId(mainAccount.getId())
                .transactionTypeId(Transaction.TYPE_EXPENSE)
                .spendingClassificationId(Transaction.CLASSIFICATION_NEED)
                .categoryId(CAT_TRANSPORT).amount(104.72).currencyCode("EUR")
                .description("Fuel")
                .completeDate(today.withHour(14)).createdAt(today.withHour(14)).build());

        transactionRepository.save(Transaction.builder()
                .accountId(mainAccount.getId())
                .transactionTypeId(Transaction.TYPE_INCOME)
                .spendingClassificationId(null)
                .categoryId(CAT_ENTERTAINMENT).amount(50.00).currencyCode("EUR")
                .description("Od babky")
                .completeDate(today.withHour(9)).createdAt(today.withHour(9)).build());

        // ---- Včerajšie transakcie ----
        LocalDateTime yesterday = today.minusDays(1);

        transactionRepository.save(Transaction.builder()
                .accountId(mainAccount.getId())
                .transactionTypeId(Transaction.TYPE_INCOME)
                .spendingClassificationId(null)
                .categoryId(CAT_ENTERTAINMENT).amount(14.29).currencyCode("EUR")
                .description("From Adam Aliexpress")
                .completeDate(yesterday.withHour(9)).createdAt(yesterday.withHour(9)).build());

        transactionRepository.save(Transaction.builder()
                .accountId(mainAccount.getId())
                .transactionTypeId(Transaction.TYPE_EXPENSE)
                .spendingClassificationId(Transaction.CLASSIFICATION_NEED)
                .categoryId(CAT_TRANSPORT).amount(28.04).currencyCode("EUR")
                .description("ChatGPT sub")
                .completeDate(yesterday.withHour(15)).createdAt(yesterday.withHour(15)).build());

        // ---- Ďalšie ----
        transactionRepository.save(Transaction.builder()
                .accountId(mainAccount.getId())
                .transactionTypeId(Transaction.TYPE_INCOME)
                .spendingClassificationId(null)
                .categoryId(CAT_TRANSPORT).amount(100).currencyCode("EUR")
                .description("Len tak")
                .completeDate(yesterday.withHour(9)).createdAt(yesterday.withHour(9)).build());

        // ---- Historické transakcie za 12 mesiacov ----
        createMonthlyData(mainAccount.getId());

        // ---- Recurring rules ----
        createRecurringRules(mainAccount.getId(), today);

        logger.info("Vytvorený USER: {} ({} transakcií, 5 recurring, 1 saving goal)",
                savedUser.getEmail(), transactionRepository.findAll().size());
    }

    private void createEmptyUser() {
        User savedUser = userRepository.save(User.builder()
                .firstName("Test").lastName("User")
                .email("test@sporixx.sk")
                .passwordHash(PasswordUtil.hashPassword("Test123!"))
                .gender("M").createdAt(LocalDateTime.now())
                .build());

        accountRepository.save(Account.builder()
                .ownerUserId(savedUser.getId()).regionId(REGION_SK)
                .accountTypeId(Account.MAIN_ACCOUNT).defaultCurrencyCode("EUR")
                .description("Everyday account")
                .initialBalance(0.0).currentBalance(0.0)
                .isActive(true).createdAt(LocalDateTime.now())
                .build());

        accountRepository.save(Account.builder()
                .ownerUserId(savedUser.getId()).regionId(REGION_SK)
                .accountTypeId(Account.EMERGENCY_FUND).defaultCurrencyCode("EUR")
                .description("Use in need")
                .initialBalance(0.0).currentBalance(0.0)
                .isActive(true).createdAt(LocalDateTime.now())
                .build());

        logger.info("Vytvorený EMPTY USER: test@sporixx.sk / Test123!");
    }

    /**
     * Generuje mesačné transakcie za posledných 12 mesiacov.
     * Income: Salary + príležitostný Freelance.
     * Expense: Groceries, Rent, Transport, Clothing, Entertainment,
     *          ChatGPT, GYM, Dining out, Utilities.
     * Pokrýva rôzne kategórie aj Want/Need klasifikáciu pre Reports grafy.
     */
    private void createMonthlyData(int mainAccountId) {
        double[] salaryAmounts = {
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
                8_400.00,   // -1
                3_200.00    // tento mesiac
        };

        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < salaryAmounts.length; i++) {
            int monthsAgo = 11 - i;
            LocalDateTime base = now.minusMonths(monthsAgo).withDayOfMonth(15).withHour(12);

            // ── INCOME ──────────────────────────────────────────────
            // Salary
            saveIfNotFuture(Transaction.builder()
                    .accountId(mainAccountId)
                    .transactionTypeId(Transaction.TYPE_INCOME)
                    .spendingClassificationId(null)
                    .categoryId(CAT_RENT)
                    .amount(salaryAmounts[i]).currencyCode("EUR")
                    .description("Salary")
                    .completeDate(base).createdAt(base).build());

            // Freelance (každé 2 mesiace)
            if (i % 2 == 0) {
                saveIfNotFuture(Transaction.builder()
                        .accountId(mainAccountId)
                        .transactionTypeId(Transaction.TYPE_INCOME)
                        .spendingClassificationId(null)
                        .categoryId(CAT_RENT)
                        .amount(800.00 + (i * 50)).currencyCode("EUR")
                        .description("Freelance project")
                        .completeDate(base.withDayOfMonth(20)).createdAt(base.withDayOfMonth(20)).build());
            }

            // ── EXPENSE — NEED ───────────────────────────────────────
            // Groceries
            saveIfNotFuture(Transaction.builder()
                    .accountId(mainAccountId)
                    .transactionTypeId(Transaction.TYPE_EXPENSE)
                    .spendingClassificationId(Transaction.CLASSIFICATION_NEED)
                    .categoryId(CAT_RENT)
                    .amount(320.00 + (i * 5)).currencyCode("EUR")
                    .description("Groceries")
                    .completeDate(base.withDayOfMonth(5)).createdAt(base.withDayOfMonth(5)).build());

            // Rent
            saveIfNotFuture(Transaction.builder()
                    .accountId(mainAccountId)
                    .transactionTypeId(Transaction.TYPE_EXPENSE)
                    .spendingClassificationId(Transaction.CLASSIFICATION_NEED)
                    .categoryId(CAT_RENT)
                    .amount(450.00).currencyCode("EUR")
                    .description("Rent")
                    .completeDate(base.withDayOfMonth(1)).createdAt(base.withDayOfMonth(1)).build());

            // Transport / Fuel
            saveIfNotFuture(Transaction.builder()
                    .accountId(mainAccountId)
                    .transactionTypeId(Transaction.TYPE_EXPENSE)
                    .spendingClassificationId(Transaction.CLASSIFICATION_NEED)
                    .categoryId(CAT_TRANSPORT)
                    .amount(75.00 + (i * 2)).currencyCode("EUR")
                    .description("Fuel")
                    .completeDate(base.withDayOfMonth(10)).createdAt(base.withDayOfMonth(10)).build());

            // ChatGPT sub
            saveIfNotFuture(Transaction.builder()
                    .accountId(mainAccountId)
                    .transactionTypeId(Transaction.TYPE_EXPENSE)
                    .spendingClassificationId(Transaction.CLASSIFICATION_NEED)
                    .categoryId(CAT_RENT)
                    .amount(28.04).currencyCode("EUR")
                    .description("ChatGPT sub")
                    .completeDate(base.withDayOfMonth(18)).createdAt(base.withDayOfMonth(18)).build());

            // Utilities
            saveIfNotFuture(Transaction.builder()
                    .accountId(mainAccountId)
                    .transactionTypeId(Transaction.TYPE_EXPENSE)
                    .spendingClassificationId(Transaction.CLASSIFICATION_NEED)
                    .categoryId(CAT_RENT)
                    .amount(55.00 + (i * 2)).currencyCode("EUR")
                    .description("Electricity & Water")
                    .completeDate(base.withDayOfMonth(25)).createdAt(base.withDayOfMonth(25)).build());

            // ── EXPENSE — WANT ───────────────────────────────────────
            // GYM membership
            saveIfNotFuture(Transaction.builder()
                    .accountId(mainAccountId)
                    .transactionTypeId(Transaction.TYPE_EXPENSE)
                    .spendingClassificationId(Transaction.CLASSIFICATION_WANT)
                    .categoryId(CAT_RENT)
                    .amount(35.00).currencyCode("EUR")
                    .description("GYM membership")
                    .completeDate(base.withDayOfMonth(3)).createdAt(base.withDayOfMonth(3)).build());

            // Netflix / Spotify
            saveIfNotFuture(Transaction.builder()
                    .accountId(mainAccountId)
                    .transactionTypeId(Transaction.TYPE_EXPENSE)
                    .spendingClassificationId(Transaction.CLASSIFICATION_WANT)
                    .categoryId(CAT_ENTERTAINMENT)
                    .amount(25.98).currencyCode("EUR")
                    .description("Netflix / Spotify")
                    .completeDate(base.withDayOfMonth(8)).createdAt(base.withDayOfMonth(8)).build());

            // Clothing (každé 3 mesiace)
            if (i % 3 == 0) {
                saveIfNotFuture(Transaction.builder()
                        .accountId(mainAccountId)
                        .transactionTypeId(Transaction.TYPE_EXPENSE)
                        .spendingClassificationId(Transaction.CLASSIFICATION_WANT)
                        .categoryId(CAT_CLOTHING)
                        .amount(120.00 + (i * 8)).currencyCode("EUR")
                        .description("Porsche merch")
                        .completeDate(base.withDayOfMonth(12)).createdAt(base.withDayOfMonth(12)).build());
            }

            // Dining out (každé 2 mesiace)
            if (i % 2 == 1) {
                saveIfNotFuture(Transaction.builder()
                        .accountId(mainAccountId)
                        .transactionTypeId(Transaction.TYPE_EXPENSE)
                        .spendingClassificationId(Transaction.CLASSIFICATION_WANT)
                        .categoryId(CAT_ENTERTAINMENT)
                        .amount(75.00 + (i * 3)).currencyCode("EUR")
                        .description("Dinner with friends")
                        .completeDate(base.withDayOfMonth(22)).createdAt(base.withDayOfMonth(22)).build());
            }
        }
    }

    /**
     * Vytvorí recurring rules pre upcoming payments v Activities paneli.
     * Pokrýva rôzne frekvencie aj Want/Need klasifikáciu.
     */
    private void createRecurringRules(int mainAccountId, LocalDateTime today) {
        // Porsche merch (WANT)
        recurringRuleRepository.save(RecurringRule.builder()
                .accountId(mainAccountId).categoryId(CAT_CLOTHING).statusId(1)
                .transactionTypeId(Transaction.TYPE_EXPENSE)
                .spendingClassificationId(Transaction.CLASSIFICATION_WANT)
                .amount(121.04).description("Porsche merch")
                .frequencyType("MONTHLY").frequencyInterval(1)
                .startDate(LocalDateTime.of(2026, 1, 1, 0, 0))
                .nextDueDate(today.plusDays(3))
                .isActive(true).generatedCount(3)
                .createdAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .build());

        // ChatGPT sub (NEED)
        recurringRuleRepository.save(RecurringRule.builder()
                .accountId(mainAccountId).categoryId(CAT_RENT).statusId(1)
                .transactionTypeId(Transaction.TYPE_EXPENSE)
                .spendingClassificationId(Transaction.CLASSIFICATION_NEED)
                .amount(28.04).description("ChatGPT sub")
                .frequencyType("MONTHLY").frequencyInterval(1)
                .startDate(LocalDateTime.of(2026, 2, 1, 0, 0))
                .nextDueDate(today.plusDays(12))
                .isActive(true).generatedCount(2)
                .createdAt(LocalDateTime.of(2026, 2, 1, 0, 0))
                .build());

        // Rent (NEED)
        recurringRuleRepository.save(RecurringRule.builder()
                .accountId(mainAccountId).categoryId(CAT_RENT).statusId(1)
                .transactionTypeId(Transaction.TYPE_EXPENSE)
                .spendingClassificationId(Transaction.CLASSIFICATION_NEED)
                .amount(450.00).description("Rent")
                .frequencyType("MONTHLY").frequencyInterval(1)
                .startDate(LocalDateTime.of(2026, 1, 1, 0, 0))
                .nextDueDate(today.plusDays(18))
                .isActive(true).generatedCount(3)
                .createdAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .build());

        // GYM membership (WANT)
        recurringRuleRepository.save(RecurringRule.builder()
                .accountId(mainAccountId).categoryId(CAT_RENT).statusId(1)
                .transactionTypeId(Transaction.TYPE_EXPENSE)
                .spendingClassificationId(Transaction.CLASSIFICATION_WANT)
                .amount(35.00).description("GYM membership")
                .frequencyType("MONTHLY").frequencyInterval(1)
                .startDate(LocalDateTime.of(2026, 1, 1, 0, 0))
                .nextDueDate(today.plusDays(5))
                .isActive(true).generatedCount(3)
                .createdAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .build());

        // Netflix (WANT)
        recurringRuleRepository.save(RecurringRule.builder()
                .accountId(mainAccountId).categoryId(CAT_ENTERTAINMENT).statusId(1)
                .transactionTypeId(Transaction.TYPE_EXPENSE)
                .spendingClassificationId(Transaction.CLASSIFICATION_WANT)
                .amount(15.99).description("Netflix")
                .frequencyType("MONTHLY").frequencyInterval(1)
                .startDate(LocalDateTime.of(2026, 1, 1, 0, 0))
                .nextDueDate(today.plusDays(8))
                .isActive(true).generatedCount(3)
                .createdAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .build());

        // Spotify (NEED)
        recurringRuleRepository.save(RecurringRule.builder()
                .accountId(mainAccountId).categoryId(CAT_ENTERTAINMENT).statusId(1)
                .transactionTypeId(Transaction.TYPE_EXPENSE)
                .spendingClassificationId(Transaction.CLASSIFICATION_NEED)
                .amount(9.99).description("Spotify")
                .frequencyType("MONTHLY").frequencyInterval(1)
                .startDate(LocalDateTime.of(2026, 1, 1, 0, 0))
                .nextDueDate(today.plusDays(15))
                .isActive(true).generatedCount(3)
                .createdAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .build());

        // Phone bill (NEED)
        recurringRuleRepository.save(RecurringRule.builder()
                .accountId(mainAccountId).categoryId(CAT_RENT).statusId(1)
                .transactionTypeId(Transaction.TYPE_EXPENSE)
                .spendingClassificationId(Transaction.CLASSIFICATION_NEED)
                .amount(25.00).description("Phone bill")
                .frequencyType("MONTHLY").frequencyInterval(1)
                .startDate(LocalDateTime.of(2026, 1, 1, 0, 0))
                .nextDueDate(today.plusDays(20))
                .isActive(true).generatedCount(3)
                .createdAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .build());
    }

    // =========================================================================
    //  Admin
    // =========================================================================
    private void createAdminUser() {
        if (userRepository.findByEmail("admin@sporixx.sk").isPresent()) {
            logger.info("ADMIN already present in test repository, skipping duplicate seed.");
            return;
        }

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
    // =========================================================================
//  Rodič / Family Manager
// =========================================================================
    private void createParentUser() {
        // ── Rodič 1: Jana Mrkvičková ──
        User savedParent1 = userRepository.save(User.builder()
                .firstName("Jana").lastName("Mrkvičková")
                .email("jana.mrkvickova@stuba.sk")
                .passwordHash(PasswordUtil.hashPassword("Rodic123!"))
                .gender("F").role(Role.FAMILY_MANAGER)
                .createdAt(LocalDateTime.of(2026, 2, 10, 8, 0))
                .build());

        accountRepository.save(Account.builder()
                .ownerUserId(savedParent1.getId()).regionId(REGION_SK)
                .accountTypeId(Account.MAIN_ACCOUNT).defaultCurrencyCode("EUR")
                .description("Everyday account")
                .initialBalance(10_000.00).currentBalance(12_500.00)
                .isActive(true).createdAt(LocalDateTime.of(2026, 2, 10, 8, 0))
                .build());

        accountRepository.save(Account.builder()
                .ownerUserId(savedParent1.getId()).regionId(REGION_SK)
                .accountTypeId(Account.EMERGENCY_FUND).defaultCurrencyCode("EUR")
                .description("Use in need")
                .initialBalance(5_000.00).currentBalance(5_000.00)
                .isActive(true).createdAt(LocalDateTime.of(2026, 2, 15, 12, 0))
                .build());

        // Jana má prístup k účtom Mareka (accountId 1-4)
        for (int accountId = 1; accountId <= 4; accountId++) {
            accountAccessRepository.grantAccess(
                    savedParent1.getId(),
                    accountId,
                    Role.USER.getAccessLevel());
        }

        logger.info("Vytvorený PARENT 1: {}", savedParent1.getEmail());

        // ── Rodič 2: Jano Mrkvička ──
        User savedParent2 = userRepository.save(User.builder()
                .firstName("Jano").lastName("Mrkvička")
                .email("jano.mrkvicka@stuba.sk")
                .passwordHash(PasswordUtil.hashPassword("Rodic123!"))
                .gender("M").role(Role.FAMILY_MANAGER)
                .createdAt(LocalDateTime.of(2026, 2, 10, 8, 0))
                .build());

        accountRepository.save(Account.builder()
                .ownerUserId(savedParent2.getId()).regionId(REGION_SK)
                .accountTypeId(Account.MAIN_ACCOUNT).defaultCurrencyCode("EUR")
                .description("Everyday account")
                .initialBalance(100_000.00).currentBalance(120_500.00)
                .isActive(true).createdAt(LocalDateTime.of(2026, 2, 10, 8, 0))
                .build());

        accountRepository.save(Account.builder()
                .ownerUserId(savedParent2.getId()).regionId(REGION_SK)
                .accountTypeId(Account.EMERGENCY_FUND).defaultCurrencyCode("EUR")
                .description("Use in need")
                .initialBalance(3_000.00).currentBalance(2_000.00)
                .isActive(true).createdAt(LocalDateTime.of(2026, 2, 15, 12, 0))
                .build());

        logger.info("Vytvorený PARENT 2: {}", savedParent2.getEmail());
    }

    /**
     * Uloží transakciu len ak jej dátum nie je v budúcnosti.
     * V reálnej aplikácii používateľ nemôže zadať budúcu transakciu.
     */
    private void saveIfNotFuture(Transaction transaction) {
        if (!transaction.getCompleteDate().isAfter(LocalDateTime.now())) {
            transactionRepository.save(transaction);
        }
    }
}