package sk.sporixx.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sk.sporixx.service.testovanie.TestDataInitializer;

/**
 * Centrálny prístupový bod pre service vrstvu.
 * UI vrstva pristupuje k službám VÝLUČNE cez túto triedu.
 * UI nikdy nevytvára service ani repository objekty priamo.
 * Prepojenie s DB vrstvou:
 *   V metode init() staci zamenit InMemory repozitare za JDBC implementacie.
 *   Zvysok aplikacie (UI, service) sa NEMENI.
 */
public final class ServiceLocator {

    private static final Logger logger = LoggerFactory.getLogger(ServiceLocator.class);

    // Service inštancie
    private static AuthService authService;
    private static OverviewService overviewService;

    private static boolean initialized = false;

    private ServiceLocator() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Inicializuje všetky služby.
     * Volá sa RAZ pri štarte aplikácie - v Main.start().
     * AKTUALNE: pouziva in-memory repozitare (TestDataInitializer) pre testovanie.
     * NESKOR: zamenit za JDBC repozitare po dokonceni DB vrstvy.
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
        TestDataInitializer testData = new TestDataInitializer();
        authService = testData.getAuthService();
        overviewService = testData.getOverviewService();

        // TODO: inicializovat dalsie sluzby ked budu implementovane
        // transactionService = ...
        // categoryService = ...
        // budgetService = ...
        // budgetTemplateService = ...
        // statisticsService = ...
        // balanceService = ...
        // recurringItemService = ...
        // currencyService = ...
        // accountService = ...
        // familyService = ...
        // userService = ...
        // adminService = ...
        // settingsService = ...
        // goalService = ...

        // ============================================================
        //  TODO: PRODUKCNY REZIM (po dokonceni DB vrstvy)
        //  - Zakomentovat TestDataInitializer vyssie
        //  - Odkomentovat JDBC repozitare nizsie
        // ============================================================
        // Connection conn = DatabaseManager.getConnection();
        //
        // // Repository instancie (JDBC)
        // UserRepository userRepo = new JdbcUserRepository(conn);
        // AccountRepository accountRepo = new JdbcAccountRepository(conn);
        //
        // // Service instancie
        // authService = new AuthServiceImpl(userRepo, accountRepo);

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

    //  HELPER
    private static void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException("ServiceLocator not initialized! Call ServiceLocator.init() in Main.start() first.");
        }
    }
}
