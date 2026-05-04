package sk.sporixx.service;

import org.junit.jupiter.api.*;
import sk.sporixx.dto.UserSettings;
import sk.sporixx.model.Account;
import sk.sporixx.model.User;
import sk.sporixx.model.Role;
import sk.sporixx.service.testovanie.*;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {

    private AuthService authService;

    // ===== STUB: minimálna implementácia SettingsService pre testy =====

    static class StubSettingsService implements SettingsService {
        @Override public String getLanguageCode() { return "sk"; }
        @Override public void setLanguageCode(String c) {}
        @Override public String getCurrencyCode() { return "EUR"; }
        @Override public void setCurrencyCode(String c) {}
        @Override public boolean isUpcomingPaymentsEnabled() { return false; }
        @Override public void setUpcomingPaymentsEnabled(boolean e) {}
        @Override public boolean isBudgetLimitAlertsEnabled() { return false; }
        @Override public void setBudgetLimitAlertsEnabled(boolean e) {}
        @Override public boolean isSavingRemindersEnabled() { return false; }
        @Override public void setSavingRemindersEnabled(boolean e) {}
        @Override public boolean isSavingGoalsUpdatesEnabled() { return false; }
        @Override public void setSavingGoalsUpdatesEnabled(boolean e) {}
        @Override public boolean isAchievementsEnabled() { return false; }
        @Override public void setAchievementsEnabled(boolean e) {}
        @Override public UserSettings getSettingsSnapshot() { return null; }
        @Override public void reload() {}
    }

    /**
     * Inicializuje ServiceLocator s minimálnym stubom SettingsService cez reflexiu.
     * Obíde požiadavku na plný ServiceLocator.init() bez spustenia celej aplikácie.
     */
    private static void initServiceLocatorForTests() throws Exception {
        Field initializedField = ServiceLocator.class.getDeclaredField("initialized");
        initializedField.setAccessible(true);
        initializedField.set(null, true);

        Field settingsField = ServiceLocator.class.getDeclaredField("settingsService");
        settingsField.setAccessible(true);
        settingsField.set(null, new StubSettingsService());
    }

    @BeforeEach
    void setUp() throws Exception {
        initServiceLocatorForTests();
        authService = new AuthServiceImpl(
                new InMemoryUserRepository(),
                new InMemoryAccountRepository()
        );
    }

    @AfterEach
    void tearDown() throws Exception {
        SessionManager.getInstance().clearSession();

        Field initializedField = ServiceLocator.class.getDeclaredField("initialized");
        initializedField.setAccessible(true);
        initializedField.set(null, false);

        Field settingsField = ServiceLocator.class.getDeclaredField("settingsService");
        settingsField.setAccessible(true);
        settingsField.set(null, null);
    }

    // ===== HELPER: "Failing" repozitáre =====

    static class FailingFindByEmailUserRepository extends InMemoryUserRepository {
        @Override
        public Optional<User> findByEmail(String email) {
            throw new RuntimeException("Simulated DB error on findByEmail");
        }
    }

    static class FailingSaveUserRepository extends InMemoryUserRepository {
        @Override
        public User save(User user) {
            throw new RuntimeException("Simulated DB error on save");
        }
    }

    static class FailingFindAccountsRepository extends InMemoryAccountRepository {
        @Override
        public List<Account> findByOwnerUserId(int userId) {
            throw new RuntimeException("Simulated DB error on findByOwnerUserId");
        }
    }

    static class FailingSaveAccountRepository extends InMemoryAccountRepository {
        @Override
        public Account save(Account account) {
            throw new RuntimeException("Simulated DB error on account save");
        }
    }

    // ===== REGISTER — základné testy =====

    @Test
    @DisplayName("Registrácia s platnými údajmi")
    void register_validInput_shouldSucceed() {
        assertDoesNotThrow(() ->
                authService.register("Ján", "Novák", "jan@test.sk", "Heslo123!", "Heslo123!")
        );
    }

    @Test
    @DisplayName("Registrácia s prázdnym menom")
    void register_emptyFirstName_shouldThrow() {
        assertThrows(AuthException.class, () ->
                authService.register("", "Novák", "jan@test.sk", "Heslo123!", "Heslo123!")
        );
    }

    @Test
    @DisplayName("Registrácia s neplatným emailom")
    void register_invalidEmail_shouldThrow() {
        assertThrows(AuthException.class, () ->
                authService.register("Ján", "Novák", "neplatny-email", "Heslo123!", "Heslo123!")
        );
    }

    @Test
    @DisplayName("Registrácia - heslá sa nezhodujú")
    void register_passwordMismatch_shouldThrow() {
        assertThrows(AuthException.class, () ->
                authService.register("Ján", "Novák", "jan@test.sk", "Heslo123!", "IneHeslo!")
        );
    }

    @Test
    @DisplayName("Registrácia duplicitného emailu")
    void register_duplicateEmail_shouldThrow() throws AuthException {
        authService.register("Ján", "Novák", "jan@test.sk", "Heslo123!", "Heslo123!");
        assertThrows(AuthException.class, () ->
                authService.register("Peter", "Kováč", "jan@test.sk", "Heslo123!", "Heslo123!")
        );
    }

    @Test
    @DisplayName("Registrácia s prázdnym priezviskom")
    void register_emptyLastName_shouldThrow() {
        assertThrows(AuthException.class, () ->
                authService.register("Ján", "", "jan@test.sk", "Heslo123!", "Heslo123!")
        );
    }

    @Test
    @DisplayName("Registrácia s neplatnými znakmi v mene")
    void register_invalidCharactersInFirstName_shouldThrow() {
        assertThrows(AuthException.class, () ->
                authService.register("J@n123", "Novák", "jan@test.sk", "Heslo123!", "Heslo123!")
        );
    }

    @Test
    @DisplayName("Registrácia s neplatnými znakmi v priezvisku")
    void register_invalidCharactersInLastName_shouldThrow() {
        assertThrows(AuthException.class, () ->
                authService.register("Ján", "Nov@k123", "jan@test.sk", "Heslo123!", "Heslo123!")
        );
    }

    @Test
    @DisplayName("Registrácia s krátkym heslom")
    void register_passwordTooShort_shouldThrow() {
        assertThrows(AuthException.class, () ->
                authService.register("Ján", "Novák", "jan@test.sk", "abc", "abc")
        );
    }

    @Test
    @DisplayName("Registrácia s prázdnym heslom")
    void register_emptyPassword_shouldThrow() {
        assertThrows(AuthException.class, () ->
                authService.register("Ján", "Novák", "jan@test.sk", "", "")
        );
    }

    @Test
    @DisplayName("Registrácia s priezviskom obsahujúcim viac ako 2 slová")
    void register_lastNameTooManyParts_shouldThrow() {
        assertThrows(AuthException.class, () ->
                authService.register("Ján", "Novák Veľký Tretí", "jan@test.sk", "Heslo123!", "Heslo123!")
        );
    }

    @Test
    @DisplayName("Registrácia s prázdnym emailom - null")
    void register_nullEmail_shouldThrow() {
        assertThrows(AuthException.class, () ->
                authService.register("Ján", "Novák", null, "Heslo123!", "Heslo123!")
        );
    }

    @Test
    @DisplayName("Registrácia s prázdnym emailom - prázdny reťazec")
    void register_blankEmail_shouldThrow() {
        assertThrows(AuthException.class, () ->
                authService.register("Ján", "Novák", "   ", "Heslo123!", "Heslo123!")
        );
    }

    @Test
    @DisplayName("Registrácia s menom obsahujúcim viac ako 2 slová")
    void register_firstNameTooManyParts_shouldThrow() {
        assertThrows(AuthException.class, () ->
                authService.register("Ján Pavol Peter", "Novák", "jan@test.sk", "Heslo123!", "Heslo123!")
        );
    }

    @Test
    @DisplayName("Registrácia uloží používateľa do repozitára")
    void register_validInput_userSavedToRepository() throws AuthException {
        InMemoryUserRepository userRepo = new InMemoryUserRepository();
        InMemoryAccountRepository accountRepo = new InMemoryAccountRepository();
        AuthService service = new AuthServiceImpl(userRepo, accountRepo);

        service.register("Ján", "Novák", "jan@test.sk", "Heslo123!", "Heslo123!");

        Optional<User> saved = userRepo.findByEmail("jan@test.sk");
        assertTrue(saved.isPresent(), "Používateľ by mal byť uložený v repozitári");
        assertEquals("Ján", saved.get().getFirstName());
        assertEquals("Novák", saved.get().getLastName());
        assertTrue(saved.get().isActive());
        assertEquals(Role.USER, saved.get().getRole());
    }

    // ===== REGISTER — DB error testy =====

    @Test
    @DisplayName("Registrácia - DB chyba pri kontrole duplicity emailu")
    void register_dbErrorOnDuplicateCheck_shouldThrow() {
        AuthService failingService = new AuthServiceImpl(
                new FailingFindByEmailUserRepository(),
                new InMemoryAccountRepository()
        );
        assertThrows(AuthException.class, () ->
                failingService.register("Ján", "Novák", "jan@test.sk", "Heslo123!", "Heslo123!")
        );
    }

    @Test
    @DisplayName("Registrácia - DB chyba pri ukladaní používateľa")
    void register_dbErrorOnSave_shouldThrow() {
        AuthService failingService = new AuthServiceImpl(
                new FailingSaveUserRepository(),
                new InMemoryAccountRepository()
        );
        assertThrows(AuthException.class, () ->
                failingService.register("Ján", "Novák", "jan@test.sk", "Heslo123!", "Heslo123!")
        );
    }

    @Test
    @DisplayName("Registrácia - zlyhanie vytvorenia default účtov nehodí výnimku")
    void register_accountCreationFails_shouldNotThrow() {
        AuthService failingService = new AuthServiceImpl(
                new InMemoryUserRepository(),
                new FailingSaveAccountRepository()
        );
        assertDoesNotThrow(() ->
                failingService.register("Ján", "Novák", "jan@test.sk", "Heslo123!", "Heslo123!")
        );
    }

    @Test
    @DisplayName("Registrácia - zlyhanie auto-loginu po registrácii nehodí výnimku")
    void register_autoLoginFails_shouldNotThrow() {
        AuthService failingService = new AuthServiceImpl(
                new InMemoryUserRepository(),
                new FailingFindAccountsRepository()
        );
        assertDoesNotThrow(() ->
                failingService.register("Ján", "Novák", "jan@test.sk", "Heslo123!", "Heslo123!")
        );
    }

    // ===== LOGIN — základné testy =====

    @Test
    @DisplayName("Prihlásenie so správnymi údajmi")
    void login_validCredentials_shouldSucceed() throws AuthException {
        authService.register("Ján", "Novák", "jan@test.sk", "Heslo123!", "Heslo123!");
        assertDoesNotThrow(() ->
                authService.login("jan@test.sk", "Heslo123!")
        );
    }

    @Test
    @DisplayName("Prihlásenie so zlým heslom")
    void login_wrongPassword_shouldThrow() throws AuthException {
        authService.register("Ján", "Novák", "jan@test.sk", "Heslo123!", "Heslo123!");
        assertThrows(AuthException.class, () ->
                authService.login("jan@test.sk", "ZleHeslo!")
        );
    }

    @Test
    @DisplayName("Prihlásenie neexistujúceho používateľa")
    void login_nonExistentUser_shouldThrow() {
        assertThrows(AuthException.class, () ->
                authService.login("nikto@test.sk", "Heslo123!")
        );
    }

    @Test
    @DisplayName("Prihlásenie s prázdnym emailom")
    void login_emptyEmail_shouldThrow() {
        assertThrows(AuthException.class, () ->
                authService.login("", "Heslo123!")
        );
    }

    @Test
    @DisplayName("Prihlásenie s prázdnym heslom")
    void login_emptyPassword_shouldThrow() {
        assertThrows(AuthException.class, () ->
                authService.login("jan@test.sk", "")
        );
    }

    @Test
    @DisplayName("Prihlásenie s oboma prázdnymi poľami")
    void login_emptyEmailAndPassword_shouldThrow() {
        assertThrows(AuthException.class, () ->
                authService.login("", "")
        );
    }

    @Test
    @DisplayName("Prihlásenie - neaktívny účet")
    void login_inactiveAccount_shouldThrow() {
        InMemoryUserRepository userRepo = new InMemoryUserRepository();
        InMemoryAccountRepository accountRepo = new InMemoryAccountRepository();

        User inactiveUser = User.builder()
                .firstName("Ján").lastName("Novák")
                .email("jan@test.sk")
                .passwordHash(sk.sporixx.util.PasswordUtil.hashPassword("Heslo123!"))
                .isActive(false)
                .build();
        userRepo.save(inactiveUser);

        AuthService service = new AuthServiceImpl(userRepo, accountRepo);

        assertThrows(AuthException.class, () ->
                service.login("jan@test.sk", "Heslo123!")
        );
    }

    @Test
    @DisplayName("Prihlásenie - user s null rolou dostane default USER rolu")
    void login_nullRole_shouldSetDefaultRole() throws AuthException {
        InMemoryUserRepository userRepo = new InMemoryUserRepository();
        InMemoryAccountRepository accountRepo = new InMemoryAccountRepository();

        User userNoRole = User.builder()
                .firstName("Ján").lastName("Novák")
                .email("jan@test.sk")
                .passwordHash(sk.sporixx.util.PasswordUtil.hashPassword("Heslo123!"))
                .role(null)
                .isActive(true)
                .build();
        User saved = userRepo.save(userNoRole);
        accountRepo.save(Account.builder()
                .ownerUserId(saved.getId())
                .accountTypeId(Account.MAIN_ACCOUNT)
                .regionId(1).description("Main")
                .initialBalance(0.0).currentBalance(0.0)
                .isActive(true).build());

        AuthService service = new AuthServiceImpl(userRepo, accountRepo);

        assertDoesNotThrow(() -> service.login("jan@test.sk", "Heslo123!"));
    }

    @Test
    @DisplayName("Prihlásenie - DB chyba pri hľadaní používateľa")
    void login_dbErrorOnFindByEmail_shouldThrow() {
        AuthService failingService = new AuthServiceImpl(
                new FailingFindByEmailUserRepository(),
                new InMemoryAccountRepository()
        );
        assertThrows(AuthException.class, () ->
                failingService.login("jan@test.sk", "Heslo123!")
        );
    }

    @Test
    @DisplayName("Prihlásenie - DB chyba pri načítaní účtov")
    void login_dbErrorOnLoadAccounts_shouldThrow() {
        InMemoryUserRepository userRepo = new InMemoryUserRepository();
        userRepo.save(User.builder()
                .firstName("Ján").lastName("Novák")
                .email("jan@test.sk")
                .passwordHash(sk.sporixx.util.PasswordUtil.hashPassword("Heslo123!"))
                .isActive(true).build());

        AuthService failingService = new AuthServiceImpl(
                userRepo,
                new FailingFindAccountsRepository()
        );
        assertThrows(AuthException.class, () ->
                failingService.login("jan@test.sk", "Heslo123!")
        );
    }

    // ===== LOGOUT =====

    @Test
    @DisplayName("Odhlásenie bez aktívnej session")
    void logout_withoutActiveSession_shouldNotThrow() {
        assertDoesNotThrow(() -> authService.logout());
    }

    @Test
    @DisplayName("Odhlásenie s aktívnou session - session je vyčistená")
    void logout_withActiveSession_shouldClearSession() throws AuthException {
        authService.register("Ján", "Novák", "jan@test.sk", "Heslo123!", "Heslo123!");
        authService.login("jan@test.sk", "Heslo123!");

        assertDoesNotThrow(() -> authService.logout());

        assertNull(SessionManager.getInstance().getCurrentUserInternal(),
                "Po odhlásení by mal byť currentUser null");
    }

    // ===== ADMIN ACCOUNT TESTY =====

    @Test
    @DisplayName("Konštruktor vytvorí admin účet ak neexistuje")
    void constructor_shouldCreateAdminAccount() {
        InMemoryUserRepository userRepo = new InMemoryUserRepository();
        InMemoryAccountRepository accountRepo = new InMemoryAccountRepository();

        new AuthServiceImpl(userRepo, accountRepo);

        Optional<User> admin = userRepo.findByEmail("admin@sporixx.sk");
        assertTrue(admin.isPresent(), "Admin účet by mal byť vytvorený");
        assertEquals(Role.ADMIN, admin.get().getRole());
        assertTrue(admin.get().isActive());
    }

    @Test
    @DisplayName("Konštruktor nevytvorí duplicitného admina")
    void constructor_adminAlreadyExists_shouldNotDuplicate() {
        InMemoryUserRepository userRepo = new InMemoryUserRepository();
        InMemoryAccountRepository accountRepo = new InMemoryAccountRepository();

        new AuthServiceImpl(userRepo, accountRepo);
        Optional<User> firstAdmin = userRepo.findByEmail("admin@sporixx.sk");

        new AuthServiceImpl(userRepo, accountRepo);
        Optional<User> secondAdmin = userRepo.findByEmail("admin@sporixx.sk");

        assertTrue(firstAdmin.isPresent());
        assertTrue(secondAdmin.isPresent());
        assertEquals(firstAdmin.get().getId(), secondAdmin.get().getId());
    }

    @Test
    @DisplayName("Admin účet má vytvorený aktívny Main Account")
    void constructor_shouldCreateActiveAccountForAdmin() {
        InMemoryUserRepository userRepo = new InMemoryUserRepository();
        InMemoryAccountRepository accountRepo = new InMemoryAccountRepository();

        new AuthServiceImpl(userRepo, accountRepo);

        Optional<User> admin = userRepo.findByEmail("admin@sporixx.sk");
        assertTrue(admin.isPresent());

        List<Account> accounts = accountRepo.findByOwnerUserId(admin.get().getId());
        assertFalse(accounts.isEmpty(), "Admin by mal mať aspoň jeden účet");
        assertTrue(accounts.stream().anyMatch(Account::isActive), "Admin by mal mať aktívny účet");
    }

    @Test
    @DisplayName("Konštruktor - DB chyba pri vytváraní admina nehodí výnimku")
    void constructor_dbError_shouldNotThrow() {
        assertDoesNotThrow(() ->
                new AuthServiceImpl(
                        new FailingFindByEmailUserRepository(),
                        new InMemoryAccountRepository()
                )
        );
    }
}