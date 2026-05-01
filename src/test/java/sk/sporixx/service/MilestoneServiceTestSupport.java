package sk.sporixx.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import sk.sporixx.dto.BudgetData;
import sk.sporixx.dto.BudgetWarning;
import sk.sporixx.dto.ChartPeriod;
import sk.sporixx.dto.WantNeedData;
import sk.sporixx.model.Account;
import sk.sporixx.model.GenderCode;
import sk.sporixx.model.Role;
import sk.sporixx.model.Transaction;
import sk.sporixx.model.User;
import sk.sporixx.service.testovanie.InMemoryTransactionRepository;
import sk.sporixx.service.testovanie.InMemoryUserRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spoločný základ pre testy MilestoneService.
 *
 * Stratégia:
 *  - ReportsService a BudgetService mockujeme cez anonymné triedy (fake),
 *    lebo ich implementácie závisia od DB vrstvy.
 *  - TransactionRepository a UserRepository = InMemory implementácie.
 *  - SessionManager je singleton → vždy upratovať v @AfterEach!
 */
abstract class MilestoneServiceTestSupport {

    protected InMemoryUserRepository userRepo;
    protected InMemoryTransactionRepository transactionRepo;

    // Fake collaborators – môžeme nastaviť v konkrétnych testoch
    protected FakeReportsService fakeReportsService;
    protected FakeBudgetService fakeBudgetService;

    protected MilestoneService milestoneService;

    protected User testUser;
    protected Account mainAccount;
    protected Account savingAccount;
    protected Account emergencyAccount;

    @BeforeEach
    void baseSetUp() {
        userRepo = new InMemoryUserRepository();
        transactionRepo = new InMemoryTransactionRepository();
        fakeReportsService = new FakeReportsService();
        fakeBudgetService = new FakeBudgetService();

        // Vytvor testovacieho používateľa
        testUser = User.builder()
                .email("test@sporixx.sk")
                .firstName("Test")
                .lastName("User")
                .passwordHash("hash")
                .role(Role.USER)
                .gender(GenderCode.MALE)
                .isActive(true)
                .spenderLevel(0)
                .savingLevel(0)
                .investorLevel(0)
                .budgetLevel(0)
                .spenderXp(0)
                .savingXp(0)
                .investorXp(0)
                .budgetXp(0)
                .build();
        testUser = userRepo.save(testUser);

        // Vytvor účty
        mainAccount = Account.builder()
                .id(1)
                .ownerUserId(testUser.getId())
                .accountTypeId(Account.MAIN_ACCOUNT)
                .currentBalance(1000.0)
                .isActive(true)
                .build();

        savingAccount = Account.builder()
                .id(2)
                .ownerUserId(testUser.getId())
                .accountTypeId(Account.SAVING_ACCOUNT)
                .currentBalance(5000.0)
                .isActive(true)
                .build();

        emergencyAccount = Account.builder()
                .id(3)
                .ownerUserId(testUser.getId())
                .accountTypeId(Account.EMERGENCY_FUND)
                .currentBalance(2000.0)
                .isActive(true)
                .build();

        // Prihlás používateľa so všetkými účtami
        SessionManager.getInstance().setSession(testUser,
                List.of(mainAccount, savingAccount, emergencyAccount));

        milestoneService = new MilestoneServiceImpl(
                fakeReportsService,
                null, // accountRepository – MilestoneServiceImpl ho nepoužíva priamo
                userRepo,
                transactionRepo,
                fakeBudgetService
        );
    }

    @AfterEach
    void baseTearDown() {
        SessionManager.getInstance().clearSession();
    }

    // ====================== Helpers ======================

    /** Pridá výdavok s danou klasifikáciou (WANT/NEED) na hlavný účet. */
    protected Transaction addExpenseWithClassification(double amount, int classificationId,
                                                        LocalDateTime date) {
        Transaction t = Transaction.builder()
                .accountId(mainAccount.getId())
                .transactionTypeId(Transaction.TYPE_EXPENSE)
                .spendingClassificationId(classificationId)
                .amount(amount)
                .completeDate(date)
                .createdAt(date)
                .build();
        return transactionRepo.save(t);
    }

    /** Pridá investičný výdavok na hlavný účet. */
    protected Transaction addInvestmentExpense(double amount, LocalDateTime date) {
        Transaction t = Transaction.builder()
                .accountId(mainAccount.getId())
                .transactionTypeId(Transaction.TYPE_EXPENSE)
                .categoryId(Transaction.CATEGORY_INVESTMENT)
                .amount(amount)
                .completeDate(date)
                .createdAt(date)
                .build();
        return transactionRepo.save(t);
    }

    /** Pridá príjem na saving účet. */
    protected Transaction addSavingIncome(double amount, LocalDateTime date) {
        Transaction t = Transaction.builder()
                .accountId(savingAccount.getId())
                .transactionTypeId(Transaction.TYPE_INCOME)
                .amount(amount)
                .completeDate(date)
                .createdAt(date)
                .build();
        return transactionRepo.save(t);
    }

    /** Pridá príjem na emergency fund účet. */
    protected Transaction addEmergencyIncome(double amount, LocalDateTime date) {
        Transaction t = Transaction.builder()
                .accountId(emergencyAccount.getId())
                .transactionTypeId(Transaction.TYPE_INCOME)
                .amount(amount)
                .completeDate(date)
                .createdAt(date)
                .build();
        return transactionRepo.save(t);
    }

    // ====================== Fake implementácie ======================

    /**
     * Fake ReportsService – vráti nastaviteľnú WantNeedData.
     * Ostatné metódy hádžu UnsupportedOperationException (nie sú potrebné pre milestone testy).
     */
    static class FakeReportsService implements ReportsService {
        private WantNeedData wantNeedData = WantNeedData.builder()
                .totalWant(0).totalNeed(0).wantPercentage(0).needPercentage(0).build();

        public void setWantNeedData(WantNeedData data) {
            this.wantNeedData = data;
        }

        public void setWantPercentage(double wantPct) {
            this.wantNeedData = WantNeedData.builder()
                    .wantPercentage(wantPct)
                    .needPercentage(100 - wantPct)
                    .totalWant(wantPct)
                    .totalNeed(100 - wantPct)
                    .build();
        }

        @Override
        public WantNeedData loadWantNeedData(ChartPeriod period) {
            return wantNeedData;
        }

        @Override
        public sk.sporixx.dto.IncomeExpenseData loadIncomeExpenseData(ChartPeriod period) {
            throw new UnsupportedOperationException("Not needed for milestone tests");
        }

        @Override
        public sk.sporixx.dto.CategoryExpenseData loadCategoryExpenseData(ChartPeriod period) {
            throw new UnsupportedOperationException("Not needed for milestone tests");
        }

        @Override
        public sk.sporixx.dto.RecurringExpenseData loadRecurringExpenseData() {
            throw new UnsupportedOperationException("Not needed for milestone tests");
        }

        @Override
        public java.util.List<sk.sporixx.dto.SavingAccountReportData> loadSavingAccountsData() {
            throw new UnsupportedOperationException("Not needed for milestone tests");
        }
    }

    /**
     * Fake BudgetService – vráti nastaviteľnú BudgetData.
     */
    static class FakeBudgetService implements BudgetService {
        private BudgetData budgetData = null;

        public void setBudgetData(BudgetData data) {
            this.budgetData = data;
        }

        @Override
        public BudgetData loadBudgetData() {
            return budgetData;
        }

        @Override
        public BudgetWarning saveBudgetSetup(double monthlyIncome, double food, double rent,
                                              double transport, double utilities, double other) {
            throw new UnsupportedOperationException();
        }

        @Override
        public BudgetWarning saveCustomAllocation(double essentialExpenses, double emergencyFund,
                                                   double savings, double toInvest) {
            throw new UnsupportedOperationException();
        }
    }
}

