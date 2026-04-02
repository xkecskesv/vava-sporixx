package sk.sporixx.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sk.sporixx.repository.*;
import sk.sporixx.service.testovanie.InMemorySavingGoalRepository;
import sk.sporixx.service.testovanie.TestDataInitializer;

/**
 * Centrálny prístupový bod pre service vrstvu.
 * UI vrstva pristupuje k službám VÝLUČNE cez túto triedu.
 * UI nikdy nevytvára service ani repository objekty priamo.
 */
public final class ServiceLocator {

    private static final Logger logger = LoggerFactory.getLogger(ServiceLocator.class);

    // Service inštancie
    private static AuthService authService;
    private static OverviewService overviewService;
    private static AccountService accountService;

    private static boolean initialized = false;

    private ServiceLocator() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Inicializuje všetky služby.
     * Volá sa RAZ pri štarte aplikácie - v Main.start().
     */
    public static void init() {
        if (initialized) {
            logger.warn("ServiceLocator already initialized, skipping.");
            return;
        }

        logger.info("Initializing ServiceLocator...");

        // ============================================================
        //  TESTOVACI REZIM (in-memory, bez DB)
        //  - TestDataInitializer vytvori InMemory repozitare
        //    a naplni ich testovacimi datami
        // ============================================================
        // TestDataInitializer testData = new TestDataInitializer();
        //authService = testData.getAuthService();
        //overviewService = testData.getOverviewService();

        try {
            UserRepository userRepo = new UserRepositoryImpl();
            AccountRepository accountRepo = new AccountRepositoryImpl();

            authService = new AuthServiceImpl(userRepo, accountRepo);
            // TODO: nahradiť za reálne repozitáre po dokončení DB vrstvy
            // (TransactionRepository, RecurringRuleRepository, SavingGoalRepository)
            TestDataInitializer testData = new TestDataInitializer();
            overviewService = testData.getOverviewService();
            accountService = new AccountServiceImpl(accountRepo, testData.getSavingGoalRepository());

            logger.info("Real DB repositories (User, Account) initialized successfully.");

        } catch (Exception e) {
            logger.error("FAILED to initialize production repositories!", e);
            throw new RuntimeException("Critical failure in ServiceLocator", e);
        }


        // TODO: inicializovat dalsie sluzby ked budu implementovane
        // transactionService = ...
        // categoryService = ...
        // budgetService = ...
        // budgetTemplateService = ...
        // statisticsService = ...
        // balanceService = ...
        // recurringItemService = ...
        // currencyService = ...
        // familyService = ...
        // userService = ...
        // adminService = ...
        // settingsService = ...
        // goalService = ...

        initialized = true;
        logger.info("ServiceLocator initialized successfully.");
    }


    //  GETTERY - UI vrstva vola tieto metody
    /** Autentifikacia: login, register, logout */
    public static AuthService getAuthService() {
        checkInitialized();
        return authService;
    }

    /** Overview obrazovka: zostatky, účty, graf, aktivity */
    public static OverviewService getOverviewService() {
        checkInitialized();
        return overviewService;
    }

    public static AccountService getAccountService() {
        checkInitialized();
        return accountService;
    }

    //  HELPER
    private static void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException("ServiceLocator not initialized! Call ServiceLocator.init() in Main.start() first.");
        }
    }
}
