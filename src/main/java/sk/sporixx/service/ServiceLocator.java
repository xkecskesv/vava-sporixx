package sk.sporixx.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sk.sporixx.repository.*;
import sk.sporixx.service.testovanie.TestDataInitializer;

/**
 * Centrálny prístupový bod pre service vrstvu.
 * UI vrstva pristupuje k službám VÝLUČNE cez túto triedu.
 * UI nikdy nevytvára service ani repository objekty priamo.
 */
public final class ServiceLocator {

    private static final Logger logger = LoggerFactory.getLogger(ServiceLocator.class);

    /**
     * Prepínač medzi testovacím a produkčným režimom.
     * TRUE  — in-memory repozitáre (TestDataInitializer), bez DB
     * FALSE — reálne JDBC repozitáre, vyžaduje hotovú DB vrstvu
     * Po dokončení všetkých JDBC implementácií zmeniť.
     */
    private static final boolean USE_TEST_DATA = true;

    // Service inštancie
    private static AuthService authService;
    private static OverviewService overviewService;
    private static AccountService accountService;
    private static ReportsService reportsService;
    private static ExportService exportService;
    private static ImportService importService;

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

        logger.info("Initializing ServiceLocator (testMode={})...", USE_TEST_DATA);

        try {
            if (USE_TEST_DATA) {
                initTestMode();
            } else {
                initProductionMode();
            }
        } catch (Exception e) {
            logger.error("FAILED to initialize ServiceLocator!", e);
            throw new RuntimeException("Critical failure in ServiceLocator", e);
        }

        initialized = true;
        logger.info("ServiceLocator initialized successfully.");
    }


    //  TESTOVACI REZIM — in-memory repozitáre, bez DB
    private static void initTestMode() {
        logger.info("Using TEST DATA (in-memory repositories)");
        logger.info("Login: marek.mosko@stuba.sk / Heslo123!");
        logger.info("Login: admin@sporixx.sk / Admin123!");
        logger.info("Login: jana.mrkvickova@stuba.sk / Rodic123!");

        TestDataInitializer testData = new TestDataInitializer();

        authService    = testData.getAuthService();
        overviewService = testData.getOverviewService();
        accountService = new AccountServiceImpl(
                testData.getAccountRepository(),
                testData.getSavingGoalRepository());
        reportsService = new ReportsServiceImpl(
                testData.getTransactionRepository(),
                testData.getRecurringRuleRepository(),
                testData.getSavingGoalRepository());
        exportService  = new ExportServiceImpl(reportsService);
        importService  = new ImportServiceImpl(testData.getSavingGoalRepository());
    }

    //  PRODUKCNY REZIM — reálne JDBC repozitáre
    //  TODO: doplniť po dokončení DB vrstvy
    private static void initProductionMode() {
        logger.info("Using PRODUCTION repositories (JDBC + SQLite)");

        // ── Hotové JDBC repozitáre ──
        UserRepository userRepo = new UserRepositoryImpl();
        AccountRepository accountRepo = new AccountRepositoryImpl();

        authService = new AuthServiceImpl(userRepo, accountRepo);

        // ── Zvyšok stále in-memory
        TestDataInitializer testData = new TestDataInitializer();
        overviewService = testData.getOverviewService();
        accountService = new AccountServiceImpl(
                accountRepo,
                testData.getSavingGoalRepository());
        reportsService = new ReportsServiceImpl(
                testData.getTransactionRepository(),
                testData.getRecurringRuleRepository(),
                testData.getSavingGoalRepository());
        exportService = new ExportServiceImpl(reportsService);
        importService = new ImportServiceImpl(testData.getSavingGoalRepository());

        // TODO: nahradiť za reálne repozitáre po dokončení DB vrstvy:
        // TransactionRepository transactionRepo = new TransactionRepositoryImpl();
        // RecurringRuleRepository recurringRepo  = new RecurringRuleRepositoryImpl();
        // SavingGoalRepository savingGoalRepo    = new SavingGoalRepositoryImpl();
        // CategoryRepository categoryRepo        = new CategoryRepositoryImpl();

        // overviewService = new OverviewServiceImpl(transactionRepo, recurringRepo, savingGoalRepo);
        // accountService  = new AccountServiceImpl(accountRepo, savingGoalRepo);
        // reportsService  = new ReportsServiceImpl(transactionRepo, recurringRepo, savingGoalRepo);
        // exportService   = new ExportServiceImpl(reportsService);
        // importService   = new ImportServiceImpl(savingGoalRepo);

        // TODO: ostatné služby:
        // transactionService = ...
        // categoryService    = ...
        // budgetService      = ...
        // settingsService    = ...
        // userService        = ...
        // adminService       = ...
        // familyService      = ...
        // goalService        = ...
    }

    //  GETTERY — UI vrstva volá tieto metódy
    /** Autentifikácia: login, register, logout */
    public static AuthService getAuthService() {
        checkInitialized();
        return authService;
    }

    /** Overview obrazovka: zostatky, účty, graf, aktivity */
    public static OverviewService getOverviewService() {
        checkInitialized();
        return overviewService;
    }

    /** Správa účtov: vytvorenie, mazanie, editácia */
    public static AccountService getAccountService() {
        checkInitialized();
        return accountService;
    }

    /** Reports obrazovka: income/expense, kategórie, recurring, want/need, saving accounts */
    public static ReportsService getReportsService() {
        checkInitialized();
        return reportsService;
    }

    /** Export reportov do XML */
    public static ExportService getExportService() {
        checkInitialized();
        return exportService;
    }

    /** Import dát z XML */
    public static ImportService getImportService() {
        checkInitialized();
        return importService;
    }

    //  HELPER
    private static void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException(
                    "ServiceLocator not initialized! Call ServiceLocator.init() in Main.start() first.");
        }
    }
}