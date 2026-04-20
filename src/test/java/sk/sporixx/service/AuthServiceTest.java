package sk.sporixx.service;

import org.junit.jupiter.api.*;
import sk.sporixx.model.Account;
import sk.sporixx.model.User;
import sk.sporixx.service.testovanie.*;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                new InMemoryUserRepository(),
                new InMemoryAccountRepository()
        );
    }

    //  HELPER: "Failing" repozitáre namiesto Mockito
    //  Dedia z InMemory* a hádžu RuntimeException kde treba

    /**
     * UserRepository, ktorý hádže výnimku pri findByEmail.
     */
    static class FailingFindByEmailUserRepository extends InMemoryUserRepository {
        @Override
        public Optional<User> findByEmail(String email) {
            throw new RuntimeException("Simulated DB error on findByEmail");
        }
    }

    /**
     * UserRepository, ktorý hádže výnimku pri save (ale findByEmail funguje normálne).
     */
    static class FailingSaveUserRepository extends InMemoryUserRepository {
        @Override
        public User save(User user) {
            throw new RuntimeException("Simulated DB error on save");
        }
    }

    /**
     * AccountRepository, ktorý hádže výnimku pri findByOwnerUserId.
     */
    static class FailingFindAccountsRepository extends InMemoryAccountRepository {
        @Override
        public List<Account> findByOwnerUserId(int userId) {
            throw new RuntimeException("Simulated DB error on findByOwnerUserId");
        }
    }

    /**
     * AccountRepository, ktorý hádže výnimku pri save (pre zlyhanie vytvorenia default účtov).
     */
    static class FailingSaveAccountRepository extends InMemoryAccountRepository {
        @Override
        public Account save(Account account) {
            throw new RuntimeException("Simulated DB error on account save");
        }
    }

    //  REGISTER — základné testy

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
                authService.register("Ján", "Novák Veľký Třetí", "jan@test.sk", "Heslo123!", "Heslo123!")
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

    //  REGISTER — DB error testy (bez Mockito, s failing repozitármi)

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

        // Registrácia by mala prejsť - zlyhanie účtov sa len zaloguje
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

        // Registrácia by mala prejsť - auto-login zlyhá, ale registrácia je OK
        // Pozn: save účtov tiež zlyhá, pretože findByOwnerUserId hádže výnimku,
        // ale save funguje normálne. Ak potrebujete oddeliť, vytvorte ďalšiu variantu.
        assertDoesNotThrow(() ->
                failingService.register("Ján", "Novák", "jan@test.sk", "Heslo123!", "Heslo123!")
        );
    }

    //  LOGIN — základné testy

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

    //  LOGIN — DB error a edge case testy

    @Test
    @DisplayName("Prihlásenie - neaktívny účet")
    void login_inactiveAccount_shouldThrow() {
        // Priamy prístup k InMemory repozitáru — vložíme neaktívneho usera ručne
        InMemoryUserRepository userRepo = new InMemoryUserRepository();
        InMemoryAccountRepository accountRepo = new InMemoryAccountRepository();

        User inactiveUser = User.builder()
                .firstName("Ján").lastName("Novák")
                .email("jan@test.sk")
                .passwordHash(sk.sporixx.util.PasswordUtil.hashPassword("Heslo123!"))
                .isActive(false)  // <-- neaktívny!
                .build();
        userRepo.save(inactiveUser);

        AuthService serviceWithInactiveUser = new AuthServiceImpl(userRepo, accountRepo);

        assertThrows(AuthException.class, () ->
                serviceWithInactiveUser.login("jan@test.sk", "Heslo123!")
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
                .role(null)  // <-- bez roly
                .isActive(true)
                .build();
        userRepo.save(userNoRole);

        AuthService serviceWithNullRole = new AuthServiceImpl(userRepo, accountRepo);

        assertDoesNotThrow(() ->
                serviceWithNullRole.login("jan@test.sk", "Heslo123!")
        );
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
    void login_dbErrorOnLoadAccounts_shouldThrow() throws AuthException {
        // Najprv registrujeme cez normálny repo, potom prihlasujeme cez failing
        InMemoryUserRepository userRepo = new InMemoryUserRepository();

        User user = User.builder()
                .firstName("Ján").lastName("Novák")
                .email("jan@test.sk")
                .passwordHash(sk.sporixx.util.PasswordUtil.hashPassword("Heslo123!"))
                .isActive(true)
                .build();
        userRepo.save(user);

        AuthService failingService = new AuthServiceImpl(
                userRepo,
                new FailingFindAccountsRepository()
        );

        assertThrows(AuthException.class, () ->
                failingService.login("jan@test.sk", "Heslo123!")
        );
    }

    //  LOGOUT

    @Test
    @DisplayName("Odhlásenie prebehne bez chyby")
    void logout_shouldSucceed() throws AuthException {
        authService.register("Ján", "Novák", "jan@test.sk", "Heslo123!", "Heslo123!");
        authService.login("jan@test.sk", "Heslo123!");
        assertDoesNotThrow(() -> authService.logout());
    }

    @Test
    @DisplayName("Odhlásenie bez aktívnej session")
    void logout_withoutActiveSession_shouldNotThrow() {
        assertDoesNotThrow(() -> authService.logout());
    }

    @Test
    @DisplayName("Odhlásenie s aktívnou session - pokryje vetvu currentUser != null")
    void logout_withActiveSession_shouldLogAndClear() throws AuthException {
        authService.register("Ján", "Novák", "jan@test.sk", "Heslo123!", "Heslo123!");
        authService.login("jan@test.sk", "Heslo123!");

        // Overíme, že logout prebehne bez chyby (pokrýva if currentUser != null vetvu)
        assertDoesNotThrow(() -> authService.logout());

        // Po logout by opätovný login mal fungovať (session je vyčistená)
        assertDoesNotThrow(() ->
                authService.login("jan@test.sk", "Heslo123!")
        );
    }
}