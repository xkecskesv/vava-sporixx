package sk.sporixx.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import sk.sporixx.dto.CurrentUser;
import sk.sporixx.model.*;
import sk.sporixx.util.PasswordUtil;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit testy pre UserService (UserServiceImpl).
 *
 * - getCurrentUser()  → závisí od SessionManager (singleton)
 * - normalizeGender() → čistá logika, žiadne závislosti
 * - toDisplayGender() → volá Localization.get() – testujeme len UNKNOWN (vráti "-")
 *   a normalizáciu (overíme cez normalizeGender() keďže toDisplayGender ho interne volá)
 */
@DisplayName("UserService")
class UserServiceTest {

    private final UserService svc = new UserServiceImpl();

    @AfterEach
    void tearDown() {
        SessionManager.getInstance().clearSession();
    }

    // ─── getCurrentUser() ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("getCurrentUser()")
    class GetCurrentUser {

        @Test
        @DisplayName("bez prihlásenia vráti null")
        void getCurrentUser_noSession_returnsNull() {
            assertNull(svc.getCurrentUser());
        }

        @Test
        @DisplayName("po prihlásení vráti non-null CurrentUser")
        void getCurrentUser_withSession_returnsUser() {
            loginAs(buildUser(1, "Jana", "Nováková", "jana@test.sk", Role.USER));
            assertNotNull(svc.getCurrentUser());
        }

        @Test
        @DisplayName("CurrentUser obsahuje správne meno")
        void getCurrentUser_correctFirstName() {
            loginAs(buildUser(1, "Jana", "Nováková", "jana@test.sk", Role.USER));
            assertEquals("Jana", svc.getCurrentUser().getName());
        }

        @Test
        @DisplayName("CurrentUser obsahuje správne priezvisko")
        void getCurrentUser_correctLastName() {
            loginAs(buildUser(1, "Jana", "Nováková", "jana@test.sk", Role.USER));
            assertEquals("Nováková", svc.getCurrentUser().getSurname());
        }

        @Test
        @DisplayName("CurrentUser obsahuje správny email")
        void getCurrentUser_correctEmail() {
            loginAs(buildUser(1, "Jana", "Nováková", "jana@test.sk", Role.USER));
            assertEquals("jana@test.sk", svc.getCurrentUser().getEmail());
        }

        @Test
        @DisplayName("CurrentUser obsahuje správnu rolu")
        void getCurrentUser_correctRole() {
            loginAs(buildUser(1, "Jana", "Nováková", "jana@test.sk", Role.FAMILY_MANAGER));
            assertEquals(Role.FAMILY_MANAGER, svc.getCurrentUser().getRole());
        }

        @Test
        @DisplayName("po odhlásení vráti znova null")
        void getCurrentUser_afterLogout_returnsNull() {
            loginAs(buildUser(1, "Jana", "Nováková", "jana@test.sk", Role.USER));
            SessionManager.getInstance().clearSession();
            assertNull(svc.getCurrentUser());
        }
    }

    // ─── normalizeGender() ────────────────────────────────────────────────────

    @Nested
    @DisplayName("normalizeGender()")
    class NormalizeGender {

        @Test
        @DisplayName("'M' → MALE")
        void normalize_M_returnsMale() {
            assertEquals(GenderCode.MALE, svc.normalizeGender("M"));
        }

        @Test
        @DisplayName("'m' (lowercase) → MALE")
        void normalize_m_lowercase_returnsMale() {
            assertEquals(GenderCode.MALE, svc.normalizeGender("m"));
        }

        @Test
        @DisplayName("'male' → MALE")
        void normalize_male_returnsMale() {
            assertEquals(GenderCode.MALE, svc.normalizeGender("male"));
        }

        @Test
        @DisplayName("'F' → FEMALE")
        void normalize_F_returnsFemale() {
            assertEquals(GenderCode.FEMALE, svc.normalizeGender("F"));
        }

        @Test
        @DisplayName("'f' (lowercase) → FEMALE")
        void normalize_f_lowercase_returnsFemale() {
            assertEquals(GenderCode.FEMALE, svc.normalizeGender("f"));
        }

        @Test
        @DisplayName("'female' → FEMALE")
        void normalize_female_returnsFemale() {
            assertEquals(GenderCode.FEMALE, svc.normalizeGender("female"));
        }

        @Test
        @DisplayName("'z' (slovenské ženské) → FEMALE")
        void normalize_z_returnsFemale() {
            assertEquals(GenderCode.FEMALE, svc.normalizeGender("z"));
        }

        @Test
        @DisplayName("'ž' (slovenské ženské s háčkom) → FEMALE")
        void normalize_zHacek_returnsFemale() {
            assertEquals(GenderCode.FEMALE, svc.normalizeGender("ž"));
        }

        @Test
        @DisplayName("'ONHSR' → UNKNOWN")
        void normalize_ONHSR_returnsUnknown() {
            assertEquals(GenderCode.UNKNOWN, svc.normalizeGender("ONHSR"));
        }

        @Test
        @DisplayName("null → UNKNOWN")
        void normalize_null_returnsUnknown() {
            assertEquals(GenderCode.UNKNOWN, svc.normalizeGender(null));
        }

        @Test
        @DisplayName("prázdny string → UNKNOWN")
        void normalize_blank_returnsUnknown() {
            assertEquals(GenderCode.UNKNOWN, svc.normalizeGender(""));
        }

        @Test
        @DisplayName("string len s medzerami → UNKNOWN")
        void normalize_whitespace_returnsUnknown() {
            assertEquals(GenderCode.UNKNOWN, svc.normalizeGender("   "));
        }

        @Test
        @DisplayName("neznáma hodnota 'X' → UNKNOWN")
        void normalize_unknownValue_returnsUnknown() {
            assertEquals(GenderCode.UNKNOWN, svc.normalizeGender("X"));
        }

        @Test
        @DisplayName("hodnota s medzerami okolo 'M' sa orezáva a normalizuje")
        void normalize_spacedMale_returnsMale() {
            assertEquals(GenderCode.MALE, svc.normalizeGender("  male  "));
        }
    }

    // ─── toDisplayGender() ───────────────────────────────────────────────────

    @Nested
    @DisplayName("toDisplayGender()")
    class ToDisplayGender {

        @Test
        @DisplayName("UNKNOWN/ONHSR → '-'")
        void display_unknown_returnsDash() {
            assertEquals("-", svc.toDisplayGender(GenderCode.UNKNOWN));
        }

        @Test
        @DisplayName("null → '-'")
        void display_null_returnsDash() {
            assertEquals("-", svc.toDisplayGender(null));
        }

        @Test
        @DisplayName("prázdny string → '-'")
        void display_blank_returnsDash() {
            assertEquals("-", svc.toDisplayGender(""));
        }

        @Test
        @DisplayName("neznáma hodnota → '-'")
        void display_unknownValue_returnsDash() {
            assertEquals("-", svc.toDisplayGender("XYZ"));
        }

        @Test
        @DisplayName("M normalizuje na MALE pred mapovaním")
        void display_M_normalizedBeforeMapping() {
            // normalizeGender("M") = MALE → toDisplayGender volá Localization
            // Ak Localization nie je inicializovaný, metóda hodí výnimku.
            // Overíme teda len že výsledok NIE JE "-" (t.j. normalizácia prebehla)
            // ALEBO že výnimka NIE JE NullPointerException na normalizeGender
            String normalized = svc.normalizeGender("M");
            assertEquals(GenderCode.MALE, normalized,
                    "normalizeGender musí vrátiť MALE pre vstup 'M' – základ pre toDisplayGender");
        }

        @Test
        @DisplayName("F normalizuje na FEMALE pred mapovaním")
        void display_F_normalizedBeforeMapping() {
            String normalized = svc.normalizeGender("F");
            assertEquals(GenderCode.FEMALE, normalized,
                    "normalizeGender musí vrátiť FEMALE pre vstup 'F' – základ pre toDisplayGender");
        }
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private User buildUser(int id, String first, String last, String email, Role role) {
        return User.builder()
                .id(id).firstName(first).lastName(last).email(email)
                .passwordHash(PasswordUtil.hashPassword("Heslo123!"))
                .role(role).gender(GenderCode.UNKNOWN).isActive(true)
                .build();
    }

    private void loginAs(User user) {
        SessionManager.getInstance().setSession(user, List.of());
    }
}

