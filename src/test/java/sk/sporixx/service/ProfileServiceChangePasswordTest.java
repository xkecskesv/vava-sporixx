package sk.sporixx.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import sk.sporixx.util.PasswordUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy pre ProfileService.changePassword() – zmena hesla.
 */
@DisplayName("ProfileService – changePassword")
class ProfileServiceChangePasswordTest extends ProfileServiceTestSupport {

    @Nested
    @DisplayName("Happy path")
    class HappyPath {

        @Test
        @DisplayName("správne staré heslo a platné nové heslo – heslo sa zmení")
        void changePassword_validOldAndNew_passwordUpdated() {
            loginAs(regularUser);
            profileService.changePassword(DEFAULT_PASSWORD, "NoveHeslo1!");
            var saved = userRepo.findByEmail(regularUser.getEmail()).get();
            assertTrue(PasswordUtil.verifyPassword("NoveHeslo1!", saved.getPasswordHash()));
        }
    }

    @Nested
    @DisplayName("Chybné vstupy")
    class Validation {

        @Test
        @DisplayName("prázdne staré heslo hodí výnimku")
        void changePassword_blankOld_throws() {
            loginAs(regularUser);
            assertThrows(ProfileException.class, () ->
                    profileService.changePassword("", "NoveHeslo1!"));
        }

        @Test
        @DisplayName("nesprávne staré heslo hodí výnimku")
        void changePassword_wrongOldPassword_throws() {
            loginAs(regularUser);
            assertThrows(ProfileException.class, () ->
                    profileService.changePassword("ZleHeslo1!", "NoveHeslo1!"));
        }

        @Test
        @DisplayName("nové heslo príliš krátke hodí výnimku")
        void changePassword_newPasswordTooShort_throws() {
            loginAs(regularUser);
            assertThrows(ProfileException.class, () ->
                    profileService.changePassword(DEFAULT_PASSWORD, "abc"));
        }

        @Test
        @DisplayName("nové heslo rovnaké ako staré hodí výnimku")
        void changePassword_sameAsOld_throws() {
            loginAs(regularUser);
            assertThrows(ProfileException.class, () ->
                    profileService.changePassword(DEFAULT_PASSWORD, DEFAULT_PASSWORD));
        }
    }

    @Nested
    @DisplayName("Bez prihlásenia")
    class NotLoggedIn {

        @Test
        @DisplayName("changePassword bez session hodí ProfileException")
        void changePassword_notLoggedIn_throws() {
            assertThrows(ProfileException.class, () ->
                    profileService.changePassword(DEFAULT_PASSWORD, "NoveHeslo1!"));
        }
    }
}

