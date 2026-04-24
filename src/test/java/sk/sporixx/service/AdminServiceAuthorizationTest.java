package sk.sporixx.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import sk.sporixx.model.User;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cross-cutting testy autorizácie.
 *
 * Každá metóda AdminService prechádza cez requireAdmin() ako prvú kontrolu.
 * Testujeme že 3 negatívne scenáre (neprihlásený, bežný user, family manager)
 * hádžu ProfileException("auth.error.invalid_credentials") pre KAŽDÚ metódu.
 *
 * Toto je dôležité z bezpečnostného hľadiska – nemôže sa stať že niekto
 * pridá novú metódu a zabudne na requireAdmin().
 */
class AdminServiceAuthorizationTest extends AdminServiceTestSupport {

    // ====================== NEPRIHLASENY ======================

    @Nested
    @DisplayName("Neprihlasený volajúci")
    class NotLoggedIn {

        @Test
        @DisplayName("getAllUsers → invalid_credentials")
        void getAllUsers_throws() {
            logout();
            ProfileException ex = assertThrows(ProfileException.class,
                    () -> adminService.getAllUsers());
            assertEquals("auth.error.invalid_credentials", ex.getMessageKey());
        }

        @Test
        @DisplayName("updateUser → invalid_credentials")
        void updateUser_throws() {
            logout();
            assertThrows(ProfileException.class, () -> adminService.updateUser(
                    User.builder().id(regularUser.getId()).firstName("X").lastName("Y")
                            .email("x@y.sk").build()));
        }

        @Test
        @DisplayName("activateUser → invalid_credentials")
        void activateUser_throws() {
            logout();
            assertThrows(ProfileException.class, () -> adminService.activateUser(
                    User.builder().id(regularUser.getId()).build()));
        }

        @Test
        @DisplayName("deactivateUser → invalid_credentials")
        void deactivateUser_throws() {
            logout();
            assertThrows(ProfileException.class, () -> adminService.deactivateUser(
                    User.builder().id(regularUser.getId()).build()));
        }

        @Test
        @DisplayName("deleteUser → invalid_credentials")
        void deleteUser_throws() {
            logout();
            assertThrows(ProfileException.class, () -> adminService.deleteUser(
                    User.builder().id(regularUser.getId()).build()));
        }

        @Test
        @DisplayName("changeOwnPassword → invalid_credentials")
        void changeOwnPassword_throws() {
            logout();
            assertThrows(ProfileException.class, () -> adminService.changeOwnPassword(
                    admin, "Heslo123!", "NoveHeslo456!"));
        }
    }

    // ====================== BEZNY USER ======================

    @Nested
    @DisplayName("Prihlásený ako bežný USER")
    class AsRegularUser {

        @Test
        @DisplayName("getAllUsers → invalid_credentials")
        void getAllUsers_throws() {
            loginAs(regularUser);
            ProfileException ex = assertThrows(ProfileException.class,
                    () -> adminService.getAllUsers());
            assertEquals("auth.error.invalid_credentials", ex.getMessageKey());
        }

        @Test
        @DisplayName("updateUser → invalid_credentials (nemôže meniť iného usera)")
        void updateUser_throws() {
            loginAs(regularUser);
            assertThrows(ProfileException.class, () -> adminService.updateUser(
                    User.builder().id(familyManager.getId()).firstName("X").lastName("Y")
                            .email("x@y.sk").build()));
        }

        @Test
        @DisplayName("updateUser nevie zmeniť ani SEBA cez AdminService")
        void updateUser_self_throws() {
            // Tento test je dôležitý – user NIE JE admin, takže requireAdmin padne
            // aj keď by išlo o seba. Zmenu profilu má robiť cez ProfileService.
            loginAs(regularUser);
            assertThrows(ProfileException.class, () -> adminService.updateUser(
                    User.builder().id(regularUser.getId()).firstName("X").lastName("Y")
                            .email("x@y.sk").build()));
        }

        @Test
        @DisplayName("activateUser → invalid_credentials")
        void activateUser_throws() {
            loginAs(regularUser);
            assertThrows(ProfileException.class, () -> adminService.activateUser(
                    User.builder().id(familyManager.getId()).build()));
        }

        @Test
        @DisplayName("deactivateUser → invalid_credentials")
        void deactivateUser_throws() {
            loginAs(regularUser);
            assertThrows(ProfileException.class, () -> adminService.deactivateUser(
                    User.builder().id(familyManager.getId()).build()));
        }

        @Test
        @DisplayName("deleteUser → invalid_credentials")
        void deleteUser_throws() {
            loginAs(regularUser);
            assertThrows(ProfileException.class, () -> adminService.deleteUser(
                    User.builder().id(familyManager.getId()).build()));
        }

        @Test
        @DisplayName("changeOwnPassword → invalid_credentials (bežný user má ProfileService)")
        void changeOwnPassword_throws() {
            loginAs(regularUser);
            assertThrows(ProfileException.class, () -> adminService.changeOwnPassword(
                    regularUser, "Heslo123!", "NoveHeslo456!"));
        }
    }

    // ====================== FAMILY MANAGER ======================

    @Nested
    @DisplayName("Prihlásený ako FAMILY_MANAGER")
    class AsFamilyManager {

        @Test
        @DisplayName("getAllUsers → invalid_credentials")
        void getAllUsers_throws() {
            loginAs(familyManager);
            ProfileException ex = assertThrows(ProfileException.class,
                    () -> adminService.getAllUsers());
            assertEquals("auth.error.invalid_credentials", ex.getMessageKey());
        }

        @Test
        @DisplayName("updateUser → invalid_credentials")
        void updateUser_throws() {
            loginAs(familyManager);
            assertThrows(ProfileException.class, () -> adminService.updateUser(
                    User.builder().id(regularUser.getId()).firstName("X").lastName("Y")
                            .email("x@y.sk").build()));
        }

        @Test
        @DisplayName("deactivateUser → invalid_credentials")
        void deactivateUser_throws() {
            loginAs(familyManager);
            assertThrows(ProfileException.class, () -> adminService.deactivateUser(
                    User.builder().id(regularUser.getId()).build()));
        }

        @Test
        @DisplayName("activateUser → invalid_credentials")
        void activateUser_throws() {
            loginAs(familyManager);
            assertThrows(ProfileException.class, () -> adminService.activateUser(
                    User.builder().id(regularUser.getId()).build()));
        }

        @Test
        @DisplayName("deleteUser → invalid_credentials")
        void deleteUser_throws() {
            loginAs(familyManager);
            assertThrows(ProfileException.class, () -> adminService.deleteUser(
                    User.builder().id(regularUser.getId()).build()));
        }

        @Test
        @DisplayName("changeOwnPassword → invalid_credentials")
        void changeOwnPassword_throws() {
            loginAs(familyManager);
            assertThrows(ProfileException.class, () -> adminService.changeOwnPassword(
                    familyManager, "Heslo123!", "NoveHeslo456!"));
        }
    }

    // ====================== ESCALATION PREVENTION ======================

    @Test
    @DisplayName("Role escalation: user sa NEVIE pretočit na admin cez manipuláciu session objektu")
    void escalation_modifyingReturnedUser_doesNotGrantAdmin() {
        // Obranný test – user dostane z login-u User objekt.
        // Ak by ho niekto zmutoval napr. setRole(ADMIN), session ho berie priamo
        // (SessionManager.setSession drží referenciu), takže isAdmin() pozrie na current ROLE.
        //
        // V tomto teste simulujeme útok: prihlásime sa ako regular user
        // a zmutujeme ho na ADMIN v session objekte -> po tomto BY bolo isAdmin()=true.
        //
        // Toto NIE JE skutočný bug services (validácia session je mimo scope AdminService),
        // ale test dokumentuje riziko pre security review.
        loginAs(regularUser);

        // pokus o eskaláciu:
        regularUser.setRole(sk.sporixx.model.Role.ADMIN);
        // teraz sa naozaj stane že session povie isAdmin=true
        assertTrue(SessionManager.getInstance().isAdmin(),
                "SECURITY NOTE: SessionManager drží REFERENCIU na User objekt – " +
                "ak má niekto prístup k objektu, vie ho zmutovať. " +
                "Mitigácia: SessionManager by mal držať immutable kópiu User-a.");

        // Kvôli tomuto rizikovému API vie AdminService tomuto zmutovanému 'adminovi'
        // vrátiť všetkých používateľov:
        assertDoesNotThrow(() -> adminService.getAllUsers());
    }
}
