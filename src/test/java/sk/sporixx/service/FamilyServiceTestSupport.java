package sk.sporixx.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import sk.sporixx.model.*;
import sk.sporixx.service.testovanie.*;
import sk.sporixx.util.PasswordUtil;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spoločný základ pre všetky testy FamilyService.
 */
abstract class FamilyServiceTestSupport {

    protected InMemoryAccountAccessRepository accountAccessRepo;
    protected InMemoryAccountRepository accountRepo;
    protected InMemoryUserRepository userRepo;
    protected InMemoryFamilyRequestRepository familyRequestRepo;
    protected InMemorySavingGoalRepository savingGoalRepo;
    protected FamilyService familyService;

    // Predpripravení používatelia
    protected User manager;   // FAMILY_MANAGER
    protected User child;     // USER – dieťa managera
    protected User child2;    // USER – druhé dieťa
    protected User stranger;  // USER – cudzí user (nie v rodine)

    // Účty
    protected Account childMainAccount;
    protected Account child2MainAccount;

    @BeforeEach
    void baseSetUp() {
        accountAccessRepo = new InMemoryAccountAccessRepository();
        accountRepo       = new InMemoryAccountRepository();
        userRepo          = new InMemoryUserRepository();
        familyRequestRepo = new InMemoryFamilyRequestRepository();
        savingGoalRepo    = new InMemorySavingGoalRepository();

        familyService = new FamilyServiceImpl(
                accountAccessRepo, accountRepo, userRepo,
                familyRequestRepo, savingGoalRepo);

        manager  = saveUser("manager@test.sk", "Jana",  "Mrkvičková", Role.FAMILY_MANAGER);
        child    = saveUser("child@test.sk",   "Marek", "Malý",       Role.USER);
        child2   = saveUser("child2@test.sk",  "Eva",   "Malá",       Role.USER);
        stranger = saveUser("stranger@test.sk", "Cudzí", "User",      Role.USER);

        // child má hlavný účet
        childMainAccount  = giveMainAccount(child.getId());
        child2MainAccount = giveMainAccount(child2.getId());

        // manager má prístup k child's accountu (= child je v rodine)
        accountAccessRepo.grantAccess(manager.getId(), childMainAccount.getId(),
                Role.USER.getAccessLevel());

        loginAs(manager);
    }

    @AfterEach
    void baseTearDown() {
        SessionManager.getInstance().clearSession();
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    protected User saveUser(String email, String firstName, String lastName, Role role) {
        User u = User.builder()
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .passwordHash(PasswordUtil.hashPassword("Heslo123!"))
                .role(role)
                .gender(GenderCode.UNKNOWN)
                .isActive(true)
                .build();
        return userRepo.save(u);
    }

    protected Account giveMainAccount(int userId) {
        Account a = Account.builder()
                .ownerUserId(userId)
                .accountTypeId(Account.MAIN_ACCOUNT)
                .regionId(1)
                .defaultCurrencyCode("EUR")
                .initialBalance(0.0)
                .currentBalance(100.0)
                .isActive(true)
                .build();
        return accountRepo.save(a);
    }

    protected Account giveSavingAccount(int userId, double currentBalance) {
        Account a = Account.builder()
                .ownerUserId(userId)
                .accountTypeId(Account.SAVING_ACCOUNT)
                .regionId(1)
                .defaultCurrencyCode("EUR")
                .initialBalance(0.0)
                .currentBalance(currentBalance)
                .isActive(true)
                .build();
        return accountRepo.save(a);
    }

    protected SavingGoal giveSavingGoal(int accountId, double targetAmount) {
        SavingGoal goal = SavingGoal.builder()
                .accountId(accountId)
                .targetAmount(targetAmount)
                .currentAmount(0.0)
                .isActive(true)
                .build();
        return savingGoalRepo.save(goal);
    }

    protected FamilyRequest sendRequest(int fromId, int toId) {
        FamilyRequest req = FamilyRequest.builder()
                .fromUserId(fromId)
                .toUserId(toId)
                .status(FamilyRequest.STATUS_PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        return familyRequestRepo.save(req);
    }

    protected void loginAs(User user) {
        SessionManager.getInstance().setSession(user, List.of());
    }

    protected void logout() {
        SessionManager.getInstance().clearSession();
    }
}

