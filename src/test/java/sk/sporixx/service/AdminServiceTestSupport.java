package sk.sporixx.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import sk.sporixx.model.Account;
import sk.sporixx.model.GenderCode;
import sk.sporixx.model.Role;
import sk.sporixx.model.User;
import sk.sporixx.service.testovanie.InMemoryAccountAccessRepository;
import sk.sporixx.service.testovanie.InMemoryAccountRepository;
import sk.sporixx.service.testovanie.InMemoryUserRepository;
import sk.sporixx.util.PasswordUtil;

import java.util.List;

/**
 * Spolocny základ pre všetky testy AdminService.
 * Drží repozitáre, session a helpery – to čo by inak každy test opakoval.
 *
 * Poznámka k dizajnu:
 *  - používame "failing in-memory" repos namiesto Mockito (rovnaký štýl ako AuthServiceTest)
 *  - SessionManager je singleton => musíme ho v @AfterEach VZDY upratať
 *    inak jeden test ovplyvní ďalší. Very important.
 */
abstract class AdminServiceTestSupport {

    protected InMemoryUserRepository userRepo;
    protected InMemoryAccountRepository accountRepo;
    protected InMemoryAccountAccessRepository accountAccessRepo;
    protected UserService userService;
    protected AdminService adminService;

    // Preddefinovaní používatelia pre testy
    protected User admin;
    protected User regularUser;
    protected User familyManager;

    @BeforeEach
    void baseSetUp() {
        userRepo = new InMemoryUserRepository();
        accountRepo = new InMemoryAccountRepository();
        accountAccessRepo = new InMemoryAccountAccessRepository();
        userService = new UserServiceImpl();
        adminService = new AdminServiceImpl(userRepo, accountRepo, accountAccessRepo, userService);

        // Pripravime testovacích pouzivatelov
        admin = saveUser("admin@sporixx.sk", "Admin", "Adminov", Role.ADMIN, GenderCode.MALE, true);
        regularUser = saveUser("marek.mosko@stuba.sk", "Marek", "Moško", Role.USER, GenderCode.MALE, true);
        familyManager = saveUser("jana.mrkvickova@stuba.sk", "Jana", "Mrkvičková",
                Role.FAMILY_MANAGER, GenderCode.FEMALE, true);

        // kazdy user má aspoň jeden účet - deaktivácia vyžaduje mať aspoň 1 účet
        giveAccount(regularUser.getId());
        giveAccount(familyManager.getId());
        giveAccount(admin.getId());

        // Defaultne – prihlaseny je admin (väčšina testov to potrebuje)
        loginAs(admin);
    }

    @AfterEach
    void baseTearDown() {
        // DÔLEŽITÉ: singleton session sa musí VŽDY vyčistiť
        // inak nasledujuci test zdedí prihláseného použivatela
        SessionManager.getInstance().clearSession();
    }

    // ------------- helpers -------------

    /** Vytvorí a uloží používateľa s rovno zahashovaným heslom "Heslo123!". */
    protected User saveUser(String email, String firstName, String lastName,
                            Role role, String gender, boolean active) {
        User u = User.builder()
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .passwordHash(PasswordUtil.hashPassword("Heslo123!"))
                .role(role)
                .gender(gender)
                .isActive(active)
                .build();
        return userRepo.save(u);
    }

    /** Priradí používateľovi jeden fiktivny hlavny účet. */
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

    /** Prihlási daného používateľa do singleton sessionu. */
    protected void loginAs(User user) {
        SessionManager.getInstance().setSession(user, List.of());
    }

    /** Úplne odhlási. */
    protected void logout() {
        SessionManager.getInstance().clearSession();
    }

    /**
     * Helper: volaju sa 2 riadky, chceme iba taky ktory nemá accounty.
     * Používa sa keď chceme testovať setUserAccountsActive s prazdnym zoznamom.
     */
    protected User saveUserWithoutAccounts(String email) {
        return saveUser(email, "Bez", "Účtov", Role.USER, GenderCode.UNKNOWN, true);
    }
}
