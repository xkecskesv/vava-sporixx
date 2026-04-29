package sk.sporixx.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sk.sporixx.model.Role;
import sk.sporixx.model.User;
import sk.sporixx.util.PasswordUtil;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy pre {@link AdminService#updateUser(User)}.
 *
 * Pokrývame:
 *   - happy path (zmena mena, priezviska, emailu, gender)
 *   - validácia – prázdne polia, zlé formáty
 *   - duplicitné emaily (vrátane vlastného – NESMIE hodiť duplicate)
 *   - heslo: nastavenie nového, prázdne = ponechať staré
 *   - normalizácia (trim, lowercase email)
 *   - edge: null user, user.id <= 0, neexistujuci user
 *   - autorizácia
 */
class AdminServiceUpdateUserTest extends AdminServiceTestSupport {

    // ====================== HAPPY PATH ======================

    @Test
    @DisplayName("updateUser – zmena firstName, lastName a emailu sa uloží")
    void updateUser_updatesBasicFields() {
        User update = User.builder()
                .id(regularUser.getId())
                .firstName("Peter")
                .lastName("Novák")
                .email("peter.novak@test.sk")
                .gender("M")
                .build();

        User result = adminService.updateUser(update);

        assertEquals("Peter", result.getFirstName());
        assertEquals("Novák", result.getLastName());
        assertEquals("peter.novak@test.sk", result.getEmail());

        // Overime ze repo bolo skutocne aktualizovane
        User persisted = userRepo.findById(regularUser.getId()).orElseThrow();
        assertEquals("Peter", persisted.getFirstName());
    }

    @Test
    @DisplayName("updateUser – email sa normalizuje na lowercase + trim")
    void updateUser_normalizesEmail() {
        User update = User.builder()
                .id(regularUser.getId())
                .firstName("Peter")
                .lastName("Novák")
                .email("  PETER.Novak@TEST.SK  ")
                .build();

        User result = adminService.updateUser(update);

        assertEquals("peter.novak@test.sk", result.getEmail(),
                "email musí byť trimnutý aj lowercase");
    }

    @Test
    @DisplayName("updateUser – meno a priezvisko sa normalizujú (viaceré medzery → jedna)")
    void updateUser_normalizesNames() {
        User update = User.builder()
                .id(regularUser.getId())
                .firstName("  Anna  Mária  ")
                .lastName("Nová")
                .email("anna@test.sk")
                .build();

        User result = adminService.updateUser(update);

        // normalizeName spája multiple spaces
        assertEquals("Anna Mária", result.getFirstName());
    }

    @Test
    @DisplayName("updateUser – keď passwordHash je prázdny, staré heslo ostane")
    void updateUser_emptyPasswordHash_keepsOldPassword() {
        String originalHash = userRepo.findById(regularUser.getId()).get().getPasswordHash();

        User update = User.builder()
                .id(regularUser.getId())
                .firstName("Marek")
                .lastName("Moško")
                .email("marek.mosko@stuba.sk")
                .passwordHash("") // prazdny
                .build();

        adminService.updateUser(update);

        String newHash = userRepo.findById(regularUser.getId()).get().getPasswordHash();
        assertEquals(originalHash, newHash, "heslo sa nemá meniť keď je pole prázdne");
    }

    @Test
    @DisplayName("updateUser – keď passwordHash obsahuje platný plaintext, heslo sa nanovo zahashuje")
    void updateUser_validPasswordHash_isReHashed() {
        // Nálezy: toto je v skutočnosti BUG v API - parameter sa volá passwordHash
        // ale service ho berie ako plaintext a hashuje ho. Test to potvrdzuje.
        User update = User.builder()
                .id(regularUser.getId())
                .firstName("Marek")
                .lastName("Moško")
                .email("marek.mosko@stuba.sk")
                .passwordHash("NoveHeslo123!") // PLAINTEXT!
                .build();

        adminService.updateUser(update);

        String newHash = userRepo.findById(regularUser.getId()).get().getPasswordHash();
        assertTrue(PasswordUtil.verifyPassword("NoveHeslo123!", newHash),
                "Nové heslo 'NoveHeslo123!' musí byť verifikovateľné proti novému hashu");
        assertFalse(PasswordUtil.verifyPassword("Heslo123!", newHash),
                "Staré heslo už nemá fungovať");
    }

    @Test
    @DisplayName("updateUser – zmena emailu na rovnaký email toho istého usera je OK")
    void updateUser_sameEmailSameUser_ok() {
        User update = User.builder()
                .id(regularUser.getId())
                .firstName("Marek")
                .lastName("Moško")
                .email("marek.mosko@stuba.sk") // sám seba
                .build();

        assertDoesNotThrow(() -> adminService.updateUser(update));
    }

    // ====================== VALIDÁCIA ======================

    @Test
    @DisplayName("Validácia: prázdne firstName hodí ProfileException(first_name_required)")
    void updateUser_blankFirstName_throws() {
        User update = User.builder()
                .id(regularUser.getId())
                .firstName("   ")
                .lastName("Novák")
                .email("x@y.sk")
                .build();

        ProfileException ex = assertThrows(ProfileException.class,
                () -> adminService.updateUser(update));
        assertEquals("auth.error.first_name_required", ex.getMessageKey());
    }

    @Test
    @DisplayName("Validácia: prázdne lastName hodí ProfileException(last_name_required)")
    void updateUser_blankLastName_throws() {
        User update = User.builder()
                .id(regularUser.getId())
                .firstName("Peter")
                .lastName("")
                .email("x@y.sk")
                .build();

        ProfileException ex = assertThrows(ProfileException.class,
                () -> adminService.updateUser(update));
        assertEquals("auth.error.last_name_required", ex.getMessageKey());
    }

    @Test
    @DisplayName("Validácia: meno s číslicami sa neakceptuje")
    void updateUser_firstNameWithDigits_throws() {
        User update = User.builder()
                .id(regularUser.getId())
                .firstName("Peter123")
                .lastName("Novák")
                .email("x@y.sk")
                .build();

        ProfileException ex = assertThrows(ProfileException.class,
                () -> adminService.updateUser(update));
        assertEquals("auth.error.invalid_first_name", ex.getMessageKey());
    }

    @Test
    @DisplayName("Validácia: meno so špeciálnymi znakmi (@#$) sa neakceptuje")
    void updateUser_firstNameWithSpecialChars_throws() {
        User update = User.builder()
                .id(regularUser.getId())
                .firstName("Peter@Novák")
                .lastName("Novák")
                .email("x@y.sk")
                .build();

        assertThrows(ProfileException.class, () -> adminService.updateUser(update));
    }

    @Test
    @DisplayName("Validácia: neplatný email (bez @) hodí invalid_email")
    void updateUser_invalidEmail_throws() {
        User update = User.builder()
                .id(regularUser.getId())
                .firstName("Peter")
                .lastName("Novák")
                .email("nieje-email")
                .build();

        ProfileException ex = assertThrows(ProfileException.class,
                () -> adminService.updateUser(update));
        assertEquals("auth.error.invalid_email", ex.getMessageKey());
    }

    @Test
    @DisplayName("Validácia: email bez TLD (x@y) je neplatný")
    void updateUser_emailWithoutTld_throws() {
        User update = User.builder()
                .id(regularUser.getId())
                .firstName("Peter")
                .lastName("Novák")
                .email("x@y")
                .build();

        assertThrows(ProfileException.class, () -> adminService.updateUser(update));
    }

    @Test
    @DisplayName("Validácia: heslo kratšie ako 8 znakov hodí password_too_short")
    void updateUser_shortPassword_throws() {
        User update = User.builder()
                .id(regularUser.getId())
                .firstName("Peter")
                .lastName("Novák")
                .email("p@n.sk")
                .passwordHash("short")
                .build();

        ProfileException ex = assertThrows(ProfileException.class,
                () -> adminService.updateUser(update));
        assertEquals("auth.error.password_too_short", ex.getMessageKey());
    }

    // ====================== DUPLICITY ======================

    @Test
    @DisplayName("Duplicity: email iného usera hodí email_exists")
    void updateUser_duplicateEmail_throws() {
        // skúsime zmeniť usera regularUser na email rovnaky ako familyManager
        User update = User.builder()
                .id(regularUser.getId())
                .firstName("Peter")
                .lastName("Novák")
                .email("jana.mrkvickova@stuba.sk") // patri familyManageri!
                .build();

        ProfileException ex = assertThrows(ProfileException.class,
                () -> adminService.updateUser(update));
        assertEquals("auth.error.email_exists", ex.getMessageKey());
    }

    @Test
    @DisplayName("Duplicity: email s inou veľkosťou písmen ale toho istého iného usera – email_exists")
    void updateUser_duplicateEmailCaseInsensitive_throws() {
        User update = User.builder()
                .id(regularUser.getId())
                .firstName("Peter")
                .lastName("Novák")
                .email("JANA.MRKVICKOVA@stuba.sk")
                .build();

        assertThrows(ProfileException.class, () -> adminService.updateUser(update));
    }

    // ====================== EDGE CASES ======================

    @Test
    @DisplayName("Edge: null user hodí error.unexpected")
    void updateUser_nullUser_throws() {
        ProfileException ex = assertThrows(ProfileException.class,
                () -> adminService.updateUser(null));
        assertEquals("error.unexpected", ex.getMessageKey());
    }

    @Test
    @DisplayName("Edge: user.id == 0 hodí error.unexpected")
    void updateUser_zeroId_throws() {
        User update = User.builder()
                .id(0)
                .firstName("Peter")
                .lastName("Novák")
                .email("x@y.sk")
                .build();

        ProfileException ex = assertThrows(ProfileException.class,
                () -> adminService.updateUser(update));
        assertEquals("error.unexpected", ex.getMessageKey());
    }

    @Test
    @DisplayName("Edge: záporný user.id hodí error.unexpected")
    void updateUser_negativeId_throws() {
        User update = User.builder()
                .id(-1)
                .firstName("Peter")
                .lastName("Novák")
                .email("x@y.sk")
                .build();

        assertThrows(ProfileException.class, () -> adminService.updateUser(update));
    }

    @Test
    @DisplayName("Edge: neexistujúce user.id hodí error.unexpected")
    void updateUser_nonExistentId_throws() {
        User update = User.builder()
                .id(99999)
                .firstName("Peter")
                .lastName("Novák")
                .email("x@y.sk")
                .build();

        ProfileException ex = assertThrows(ProfileException.class,
                () -> adminService.updateUser(update));
        assertEquals("error.unexpected", ex.getMessageKey());
    }

    // ====================== AUTORIZÁCIA ======================

    @Test
    @DisplayName("Autorizácia: bežný user nemôže editovať iného")
    void updateUser_notAdmin_throws() {
        loginAs(regularUser);

        User update = User.builder()
                .id(familyManager.getId())
                .firstName("X")
                .lastName("Y")
                .email("x@y.sk")
                .build();

        ProfileException ex = assertThrows(ProfileException.class,
                () -> adminService.updateUser(update));
        assertEquals("auth.error.invalid_credentials", ex.getMessageKey());
    }

    // ====================== INTEGRACIA S UserServiceImpl (normalizeGender) ======================

    @Test
    @DisplayName("Integrácia s UserService: gender 'male' sa normalizuje na 'M'")
    void updateUser_normalizesGenderMale() {
        User update = User.builder()
                .id(regularUser.getId())
                .firstName("Marek")
                .lastName("Moško")
                .email("marek.mosko@stuba.sk")
                .gender("male")
                .build();

        adminService.updateUser(update);

        User persisted = userRepo.findById(regularUser.getId()).get();
        assertEquals("M", persisted.getGender());
    }

    @Test
    @DisplayName("Integrácia s UserService: gender 'zena' sa normalizuje na 'F'")
    void updateUser_normalizesGenderFemaleSlovak() {
        // normalizeGender: ak začína na 'z' → FEMALE (SK slovo 'žena')
        User update = User.builder()
                .id(regularUser.getId())
                .firstName("Marek")
                .lastName("Moško")
                .email("marek.mosko@stuba.sk")
                .gender("zena")
                .build();

        adminService.updateUser(update);

        User persisted = userRepo.findById(regularUser.getId()).get();
        assertEquals("F", persisted.getGender());
    }

    @Test
    @DisplayName("Integrácia s UserService: neznámy gender → ONHSR")
    void updateUser_unknownGender_defaultsToUnknown() {
        User update = User.builder()
                .id(regularUser.getId())
                .firstName("Marek")
                .lastName("Moško")
                .email("marek.mosko@stuba.sk")
                .gender("??")
                .build();

        adminService.updateUser(update);

        User persisted = userRepo.findById(regularUser.getId()).get();
        assertEquals("ONHSR", persisted.getGender());
    }

    // ====================== KONTROLA NEČAKANYCH SIDE-EFFECTOV ======================

    @Test
    @DisplayName("Side-effect: updateUser NEMENÍ rolu používateľa (len identitu)")
    void updateUser_doesNotChangeRole() {
        User update = User.builder()
                .id(regularUser.getId())
                .firstName("Peter")
                .lastName("Novák")
                .email("p@n.sk")
                .role(Role.ADMIN) // pokúšame sa povýšiť seba na admina
                .build();

        adminService.updateUser(update);

        Optional<User> persisted = userRepo.findById(regularUser.getId());
        assertEquals(Role.USER, persisted.get().getRole(),
                "API nesmie povoliť eskaláciu roly cez updateUser()");
    }

    @Test
    @DisplayName("Side-effect: updateUser NEMENÍ active flag")
    void updateUser_doesNotChangeActiveFlag() {
        User update = User.builder()
                .id(regularUser.getId())
                .firstName("Peter")
                .lastName("Novák")
                .email("p@n.sk")
                .isActive(false) // pokús o deaktivaciu cez update
                .build();

        adminService.updateUser(update);

        User persisted = userRepo.findById(regularUser.getId()).get();
        assertTrue(persisted.isActive(),
                "active flag sa nesmie meniť cez updateUser – na to je deactivateUser()");
    }
}
