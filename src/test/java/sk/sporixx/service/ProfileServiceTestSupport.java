package sk.sporixx.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import sk.sporixx.model.Account;
import sk.sporixx.model.GenderCode;
import sk.sporixx.model.Role;
import sk.sporixx.model.User;
import sk.sporixx.service.testovanie.InMemoryAccountAccessRepository;
import sk.sporixx.service.testovanie.InMemoryAccountRepository;
import sk.sporixx.service.testovanie.InMemoryFamilyRequestRepository;
import sk.sporixx.service.testovanie.InMemoryUserRepository;
import sk.sporixx.util.PasswordUtil;

import java.util.List;

/**
 * Spoločný základ pre všetky testy ProfileService.
 */
abstract class ProfileServiceTestSupport {

    protected InMemoryUserRepository userRepo;
    protected InMemoryAccountRepository accountRepo;
    protected InMemoryAccountAccessRepository accountAccessRepo;
    protected InMemoryFamilyRequestRepository familyRequestRepo;
    protected UserService userService;
    protected ProfileService profileService;

    protected User regularUser;
    protected User familyManager;
    protected User admin;

    static final String DEFAULT_PASSWORD = "Heslo123!";

    @BeforeEach
    void baseSetUp() {
        userRepo = new InMemoryUserRepository();
        accountRepo = new InMemoryAccountRepository();
        accountAccessRepo = new InMemoryAccountAccessRepository();
        familyRequestRepo = new InMemoryFamilyRequestRepository();
        userService = new UserServiceImpl();

        profileService = new ProfileServiceImpl(
                userRepo, userService, accountRepo, accountAccessRepo, familyRequestRepo);

        regularUser = saveUser("marek@test.sk", "Marek", "Moško", Role.USER, GenderCode.MALE);
        familyManager = saveUser("jana@test.sk", "Jana", "Mrkvičková", Role.FAMILY_MANAGER, GenderCode.FEMALE);
        admin = saveUser("admin@test.sk", "Admin", "Adminov", Role.ADMIN, GenderCode.MALE);
    }

    @AfterEach
    void baseTearDown() {
        SessionManager.getInstance().clearSession();
    }

    protected User saveUser(String email, String firstName, String lastName, Role role, String gender) {
        User u = User.builder()
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .passwordHash(PasswordUtil.hashPassword(DEFAULT_PASSWORD))
                .role(role)
                .gender(gender)
                .isActive(true)
                .build();
        return userRepo.save(u);
    }

    protected Account giveAccount(int userId) {
        Account a = Account.builder()
                .ownerUserId(userId)
                .accountTypeId(Account.MAIN_ACCOUNT)
                .regionId(1)
                .defaultCurrencyCode("EUR")
                .initialBalance(0.0)
                .currentBalance(0.0)
                .isActive(true)
                .build();
        return accountRepo.save(a);
    }

    protected void loginAs(User user) {
        SessionManager.getInstance().setSession(user, List.of());
    }

    protected void logout() {
        SessionManager.getInstance().clearSession();
    }
}

