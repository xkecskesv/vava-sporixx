package sk.sporixx.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import sk.sporixx.model.GenderCode;
import sk.sporixx.model.Role;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy pre ProfileService.updateProfile() – aktualizácia identifikačných údajov.
 */
@DisplayName("ProfileService – updateProfile")
class ProfileServiceUpdateProfileTest extends ProfileServiceTestSupport {

    @Nested
    @DisplayName("Happy path")
    class HappyPath {

        @Test
        @DisplayName("zmena mena, priezviska a emailu")
        void updateProfile_validData_shouldPersist() {
            loginAs(regularUser);
            profileService.updateProfile("Ján", "Novák", "jan.novak@test.sk", GenderCode.MALE, false);
            var saved = userRepo.findByEmail("jan.novak@test.sk");
            assertTrue(saved.isPresent());
            assertEquals("Ján", saved.get().getFirstName());
            assertEquals("Novák", saved.get().getLastName());
        }

        @Test
        @DisplayName("email sa ukladá ako lowercase")
        void updateProfile_emailNormalized_toLowercase() {
            loginAs(regularUser);
            profileService.updateProfile("Marek", "Moško", "MAREK@Test.SK", GenderCode.MALE, false);
            assertTrue(userRepo.findByEmail("marek@test.sk").isPresent());
        }

        @Test
        @DisplayName("USER môže povýšiť na FAMILY_MANAGER keď nie je dieťa")
        void updateProfile_userBecomesParent_roleChangedToFamilyManager() {
            loginAs(regularUser);
            profileService.updateProfile("Marek", "Moško", regularUser.getEmail(), GenderCode.MALE, true);
            var saved = userRepo.findByEmail(regularUser.getEmail());
            assertEquals(Role.FAMILY_MANAGER, saved.get().getRole());
        }

        @Test
        @DisplayName("FAMILY_MANAGER bez detí môže degradovať na USER")
        void updateProfile_parentWithNoChildren_becomesUser() {
            loginAs(familyManager);
            profileService.updateProfile("Jana", "Mrkvičková", familyManager.getEmail(), GenderCode.FEMALE, false);
            var saved = userRepo.findByEmail(familyManager.getEmail());
            assertEquals(Role.USER, saved.get().getRole());
        }

        @Test
        @DisplayName("ADMIN nemôže zmeniť svoju rolu")
        void updateProfile_admin_roleUnchanged() {
            loginAs(admin);
            profileService.updateProfile("Admin", "Adminov", admin.getEmail(), GenderCode.MALE, false);
            var saved = userRepo.findByEmail(admin.getEmail());
            assertEquals(Role.ADMIN, saved.get().getRole());
        }

        @Test
        @DisplayName("pohlavie sa normalizuje pri uložení")
        void updateProfile_genderNormalized() {
            loginAs(regularUser);
            profileService.updateProfile("Marek", "Moško", regularUser.getEmail(), "male", false);
            var saved = userRepo.findByEmail(regularUser.getEmail());
            assertEquals(GenderCode.MALE, saved.get().getGender());
        }
    }

    @Nested
    @DisplayName("Validácia vstupov")
    class Validation {

        @Test
        @DisplayName("prázdne meno hodí výnimku")
        void updateProfile_blankFirstName_throws() {
            loginAs(regularUser);
            assertThrows(ProfileException.class, () ->
                    profileService.updateProfile("", "Moško", "x@y.sk", GenderCode.MALE, false));
        }

        @Test
        @DisplayName("prázdne priezvisko hodí výnimku")
        void updateProfile_blankLastName_throws() {
            loginAs(regularUser);
            assertThrows(ProfileException.class, () ->
                    profileService.updateProfile("Marek", "", "x@y.sk", GenderCode.MALE, false));
        }

        @Test
        @DisplayName("neplatný email hodí výnimku")
        void updateProfile_invalidEmail_throws() {
            loginAs(regularUser);
            assertThrows(ProfileException.class, () ->
                    profileService.updateProfile("Marek", "Moško", "nie-email", GenderCode.MALE, false));
        }

        @Test
        @DisplayName("email obsadený iným userom hodí výnimku")
        void updateProfile_emailTakenByOther_throws() {
            loginAs(regularUser);
            // familyManager@test.sk patrí inému userovi
            assertThrows(ProfileException.class, () ->
                    profileService.updateProfile("Marek", "Moško", familyManager.getEmail(), GenderCode.MALE, false));
        }

        @Test
        @DisplayName("ten istý email (vlastný) je povolený")
        void updateProfile_sameOwnEmail_allowed() {
            loginAs(regularUser);
            assertDoesNotThrow(() ->
                    profileService.updateProfile("Marek", "Moško", regularUser.getEmail(), GenderCode.MALE, false));
        }
    }

    @Nested
    @DisplayName("Bez prihlásenia")
    class NotLoggedIn {

        @Test
        @DisplayName("updateProfile bez session hodí ProfileException")
        void updateProfile_notLoggedIn_throws() {
            assertThrows(ProfileException.class, () ->
                    profileService.updateProfile("X", "Y", "x@y.sk", GenderCode.MALE, false));
        }
    }
}

