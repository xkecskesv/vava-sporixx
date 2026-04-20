package sk.stuba.fiit.sporixx.service;

import org.junit.jupiter.api.*;
import sk.sporixx.service.AuthException;
import sk.sporixx.service.AuthService;
import sk.sporixx.service.AuthServiceImpl;
import sk.sporixx.service.testovanie.*;

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

    // ===== REGISTER =====

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

    // ===== LOGIN =====

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

    // ===== LOGOUT =====

    @Test
    @DisplayName("Odhlásenie prebehne bez chyby")
    void logout_shouldSucceed() throws AuthException {
        authService.register("Ján", "Novák", "jan@test.sk", "Heslo123!", "Heslo123!");
        authService.login("jan@test.sk", "Heslo123!");
        assertDoesNotThrow(() -> authService.logout());
    }

    // ===== CHÝBAJÚCE REGISTER TESTY =====

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

// ===== CHÝBAJÚCE LOGIN TESTY =====

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

// ===== CHÝBAJÚCE LOGOUT TESTY =====

    @Test
    @DisplayName("Odhlásenie bez aktívnej session")
    void logout_withoutActiveSession_shouldNotThrow() {
        assertDoesNotThrow(() -> authService.logout());
    }
}

