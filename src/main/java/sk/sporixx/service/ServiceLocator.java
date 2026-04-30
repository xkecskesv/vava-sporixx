package sk.sporixx.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sk.sporixx.repository.*;

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
    private static ReportsService reportsService;
    private static ExportService exportService;
    private static ImportService importService;
    private static UserService userService;
    private static ProfileService profileService;
    private static AdminService adminService;
    private static TransactionService transactionService;
    private static CategoryService categoryService;
    private static BudgetService budgetService;
    private static SettingsService settingsService;
    private static CurrencyService currencyService;
    private static RecurringRuleService recurringRuleService;
    private static FamilyService familyService;
    private static MilestoneService milestoneService;

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

        try {
            settingsService = new SettingsServiceImpl();
            currencyService = new CurrencyServiceImpl(settingsService);

            UserRepository userRepo = new UserRepositoryImpl();
            AccountRepository accountRepo = new AccountRepositoryImpl();
            TransactionRepository transactionRepo = new TransactionRepositoryImpl();
            RecurringRuleRepository recurringRuleRepo = new RecurringRuleRepositoryImpl();
            SavingGoalRepository savingGoalRepo = new SavingGoalRepositoryImpl();
            CategoryRepository categoryRepo = new CategoryRepositoryImpl();
            BudgetRepository budgetRepo = new BudgetRepositoryImpl();
            AccountAccessRepository accountAccessRepo = new AccountAccessRepositoryImpl();
            FamilyRequestRepository familyRequestRepo = new FamilyRequestRepositoryImpl();

            authService = new AuthServiceImpl(userRepo, accountRepo);
            userService = new UserServiceImpl();
            profileService = new ProfileServiceImpl(userRepo, userService, accountRepo, accountAccessRepo, familyRequestRepo);
            adminService = new AdminServiceImpl(userRepo, accountRepo, userService);

            overviewService = new OverviewServiceImpl(transactionRepo, recurringRuleRepo, savingGoalRepo);

            accountService = new AccountServiceImpl(accountRepo, savingGoalRepo, accountAccessRepo);

            reportsService = new ReportsServiceImpl(transactionRepo, recurringRuleRepo, savingGoalRepo);

            exportService = new ExportServiceImpl(reportsService);

            importService = new ImportServiceImpl(savingGoalRepo, accountService, transactionRepo, accountRepo);

            transactionService = new TransactionServiceImpl(transactionRepo, accountRepo, categoryRepo, savingGoalRepo);

            categoryService = new CategoryServiceImpl(categoryRepo, transactionRepo);

            budgetService = new BudgetServiceImpl(budgetRepo, transactionRepo);

            recurringRuleService = new RecurringRuleServiceImpl(recurringRuleRepo, transactionService);

            familyService = new FamilyServiceImpl(accountAccessRepo, accountRepo, userRepo, familyRequestRepo, savingGoalRepo);

            milestoneService = new MilestoneServiceImpl(reportsService, accountRepo, userRepo, transactionRepo, budgetService);

        } catch (Exception e) {
            logger.error("FAILED to initialize ServiceLocator!", e);
            throw new RuntimeException("Critical failure in ServiceLocator", e);
        }

        initialized = true;
        logger.info("ServiceLocator initialized successfully.");
    }

    //  GETTERY - UI vrstva volá tieto metódy
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

    /** Pomocná používateľská služba pre prístup k aktuálnemu používateľovi a normalizáciu. */
    public static UserService getUserService() {
        checkInitialized();
        return userService;
    }

    /** Operácie profilu: úprava údajov a zmena hesla. */
    public static ProfileService getProfileService() {
        checkInitialized();
        return profileService;
    }

    /** Administrátorské operácie pre panel správy používateľov. */
    public static AdminService getAdminService() {
        checkInitialized();
        return adminService;
    }

    public static TransactionService getTransactionService() {
        checkInitialized();
        return transactionService;
    }

    public static CategoryService getCategoryService() {
        checkInitialized();
        return categoryService;
    }

    public static BudgetService getBudgetService() {
        checkInitialized();
        return budgetService;
    }

    public static SettingsService getSettingsService() {
        checkInitialized();
        return settingsService;
    }

    public static CurrencyService getCurrencyService() {
        checkInitialized();
        return currencyService;
    }

    public static RecurringRuleService getRecurringRuleService() {
        checkInitialized();
        return recurringRuleService;
    }

    public static FamilyService getFamilyService() {
        checkInitialized();
        return familyService;
    }

    public static MilestoneService getMilestoneService() {
        checkInitialized();
        return milestoneService;
    }

    private static void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException(
                    "ServiceLocator not initialized! Call ServiceLocator.init() in Main.start() first.");
        }
    }
}