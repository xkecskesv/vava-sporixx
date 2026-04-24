package sk.sporixx.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sk.sporixx.model.User;
import sk.sporixx.util.PasswordUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy pre {@link AdminService#changeOwnPassword(User, String, String)}.
 *
 * Špecifikum: admin si môže zmeniť IBA svoje vlastné heslo – kontroluje sa
 * že user.id === currentUser.id.
 *
 * Pokrývame:
 *   - happy path
 *   - old password zle / prázdne
 *   - new password krátke
 *   - same-as-old password
 *   - pokus meniť heslo niekomu inému (user.id != currentUser.id)
 *   - neprihlaseny vs. neadmin
 */
class AdminServiceChangeOwnPasswordTest extends AdminServiceTestSupport {

    // ====================== HAPPY PATH ======================

    @Test
    @DisplayName("changeOwnPassword – úspešne zmení heslo adminovi")
    void changeOwnPassword_happyPath() {
        adminService.changeOwnPassword(admin, "Heslo123!", "NoveHeslo456!");

        // Po zmene musí byť v DB hash ktorému zodpovedá nové heslo
        User persisted = userRepo.findById(admin.getId()).orElseThrow();
        assertTrue(PasswordUtil.verifyPassword("NoveHeslo456!", persisted.getPasswordHash()),
                "nové heslo musí byť verifikovateľné");
        assertFalse(PasswordUtil.verifyPassword("Heslo123!", persisted.getPasswordHash()),
                "staré heslo už nemá fungovať");
    }

    @Test
    @DisplayName("changeOwnPassword – session user dostane nový hash (mutácia)")
    void changeOwnPassword_mutatesSessionUser() {
        // BUG/warning: UserValidationSupport.applyPasswordChange() mutuje currentUser
        // PRED uložením do repo. Ak by update zlyhal, session a DB by sa rozišli.
        // Tento test dokumentuje aktualne spravanie.
        String originalHash = admin.getPasswordHash();

        adminService.changeOwnPassword(admin, "Heslo123!", "NoveHeslo456!");

        assertNotEquals(originalHash, admin.getPasswordHash(),
                "session user objekt má mať aktualizovaný hash");
    }

    // ====================== OLD PASSWORD ======================

    @Test
    @DisplayName("Old password prázdny hodí old_password_required")
    void changeOwnPassword_blankOld_throws() {
        ProfileException ex = assertThrows(ProfileException.class,
                () -> adminService.changeOwnPassword(admin, "", "NoveHeslo456!"));
        assertEquals("auth.error.old_password_required", ex.getMessageKey());
    }

    @Test
    @DisplayName("Old password null hodí old_password_required")
    void changeOwnPassword_nullOld_throws() {
        ProfileException ex = assertThrows(ProfileException.class,
                () -> adminService.changeOwnPassword(admin, null, "NoveHeslo456!"));
        assertEquals("auth.error.old_password_required", ex.getMessageKey());
    }

    @Test
    @DisplayName("Old password nesprávny hodí wrong_old_password")
    void changeOwnPassword_wrongOld_throws() {
        ProfileException ex = assertThrows(ProfileException.class,
                () -> adminService.changeOwnPassword(admin, "ZleHeslo!", "NoveHeslo456!"));
        assertEquals("auth.error.wrong_old_password", ex.getMessageKey());
    }

    // ====================== NEW PASSWORD ======================

    @Test
    @DisplayName("New password kratšie ako 8 znakov hodí password_too_short")
    void changeOwnPassword_newTooShort_throws() {
        ProfileException ex = assertThrows(ProfileException.class,
                () -> adminService.changeOwnPassword(admin, "Heslo123!", "short"));
        assertEquals("auth.error.password_too_short", ex.getMessageKey());
    }

    @Test
    @DisplayName("New password null hodí password_too_short")
    void changeOwnPassword_newNull_throws() {
        ProfileException ex = assertThrows(ProfileException.class,
                () -> adminService.changeOwnPassword(admin, "Heslo123!", null));
        assertEquals("auth.error.password_too_short", ex.getMessageKey());
    }

    @Test
    @DisplayName("New password rovnaký ako old hodí same_password")
    void changeOwnPassword_sameAsOld_throws() {
        ProfileException ex = assertThrows(ProfileException.class,
                () -> adminService.changeOwnPassword(admin, "Heslo123!", "Heslo123!"));
        assertEquals("auth.error.same_password", ex.getMessageKey());
    }

    // ====================== IDENTITA ======================

    @Test
    @DisplayName("Cudzí user object (iný id) hodí error.unexpected")
    void changeOwnPassword_differentUser_throws() {
        // admin je prihlaseny, ale pokus meniť heslo regularUserovi
        ProfileException ex = assertThrows(ProfileException.class,
                () -> adminService.changeOwnPassword(regularUser, "Heslo123!", "NoveHeslo456!"));
        assertEquals("error.unexpected", ex.getMessageKey());

        // Sanity: heslo regularUsera sa nezmenilo
        User persisted = userRepo.findById(regularUser.getId()).orElseThrow();
        assertTrue(PasswordUtil.verifyPassword("Heslo123!", persisted.getPasswordHash()),
                "regular user nemá dostať zmenu hesla!");
    }

    @Test
    @DisplayName("null user hodí error.unexpected")
    void changeOwnPassword_nullUser_throws() {
        ProfileException ex = assertThrows(ProfileException.class,
                () -> adminService.changeOwnPassword(null, "Heslo123!", "NoveHeslo456!"));
        assertEquals("error.unexpected", ex.getMessageKey());
    }

    // ====================== AUTORIZÁCIA ======================

    @Test
    @DisplayName("Neprihlásený volajúci hodí invalid_credentials (requireAdmin first)")
    void changeOwnPassword_notLoggedIn_throws() {
        logout();
        ProfileException ex = assertThrows(ProfileException.class,
                () -> adminService.changeOwnPassword(admin, "Heslo123!", "NoveHeslo456!"));
        assertEquals("auth.error.invalid_credentials", ex.getMessageKey());
    }

    @Test
    @DisplayName("Autorizácia: regularUser (nie admin) nedostane sa ani ku svojmu vlastnemu heslu cez AdminService")
    void changeOwnPassword_asRegularUser_throws() {
        // Presne kvôli requireAdmin() – AdminService je určená IBA pre adminov,
        // bežní useri si menia heslo cez ProfileService.
        loginAs(regularUser);
        ProfileException ex = assertThrows(ProfileException.class,
                () -> adminService.changeOwnPassword(regularUser, "Heslo123!", "NoveHeslo456!"));
        assertEquals("auth.error.invalid_credentials", ex.getMessageKey());
    }
}
