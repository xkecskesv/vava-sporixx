package sk.sporixx.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sk.sporixx.model.Account;
import sk.sporixx.model.User;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy lifecycle metód:
 *   - deactivateUser(User)
 *   - activateUser(User)
 *   - deleteUser(User)
 *
 * Všetky tri zdieľajú autorizáciu (requireAdmin) a validáciu null/id<=0,
 * preto ich testujeme spolu v jednej triede.
 */
class AdminServiceLifecycleTest extends AdminServiceTestSupport {

    // ===================================================================
    //                          DEACTIVATE
    // ===================================================================

    @Test
    @DisplayName("deactivate – všetky aktívne účty používateľa sa deaktivujú")
    void deactivate_deactivatesAllUserAccounts() {
        // pridame ešte jeden účet pre regularUsera
        giveAccount(regularUser.getId());
        // teraz má 2 aktivne účty (z baseSetUp + tento)

        adminService.deactivateUser(User.builder().id(regularUser.getId()).build());

        List<Account> all = accountRepo.findAllByOwnerUserId(regularUser.getId());
        assertEquals(2, all.size());
        assertTrue(all.stream().noneMatch(Account::isActive),
                "všetky účty musia byť deaktivované");
    }

    @Test
    @DisplayName("deactivate – vracia aktualizovaného usera")
    void deactivate_returnsExistingUser() {
        User result = adminService.deactivateUser(
                User.builder().id(regularUser.getId()).build());

        assertNotNull(result);
        assertEquals(regularUser.getId(), result.getId());
        assertEquals("marek.mosko@stuba.sk", result.getEmail());
    }

    @Test
    @DisplayName("BUG nález: deactivate usera bez účtov hodí no_accounts")
    void deactivate_userWithoutAccounts_throws() {
        // Vytvoríme usera bez účtov
        User orphan = saveUserWithoutAccounts("orphan@test.sk");

        // Podla kódu (setUserAccountsActive riadok 203-206): ak zoznam účtov je prázdny,
        // hodí sa ProfileException("admin.error.no_accounts").
        // TO JE NÁVRHOVÝ PROBLÉM: deactivateUser by mal fungovať aj bez účtov,
        // pretože môže existovať user ktorý ešte žiadny účet nemá.
        ProfileException ex = assertThrows(ProfileException.class,
                () -> adminService.deactivateUser(User.builder().id(orphan.getId()).build()));
        assertEquals("admin.error.no_accounts", ex.getMessageKey());
    }

    @Test
    @DisplayName("deactivate – admin NEMÔŽE deaktivovať sám seba")
    void deactivate_self_throws() {
        // admin je prihlaseny v setUp
        ProfileException ex = assertThrows(ProfileException.class,
                () -> adminService.deactivateUser(User.builder().id(admin.getId()).build()));
        assertEquals("admin.error.cannot_deactivate_self", ex.getMessageKey());
    }

    @Test
    @DisplayName("deactivate – null user hodí error.unexpected")
    void deactivate_null_throws() {
        ProfileException ex = assertThrows(ProfileException.class,
                () -> adminService.deactivateUser(null));
        assertEquals("error.unexpected", ex.getMessageKey());
    }

    @Test
    @DisplayName("deactivate – id == 0 hodí error.unexpected")
    void deactivate_zeroId_throws() {
        assertThrows(ProfileException.class,
                () -> adminService.deactivateUser(User.builder().id(0).build()));
    }

    @Test
    @DisplayName("deactivate – záporné id hodí error.unexpected")
    void deactivate_negativeId_throws() {
        assertThrows(ProfileException.class,
                () -> adminService.deactivateUser(User.builder().id(-1).build()));
    }

    @Test
    @DisplayName("deactivate – neexistujúce ID hodí error.unexpected")
    void deactivate_nonExistent_throws() {
        ProfileException ex = assertThrows(ProfileException.class,
                () -> adminService.deactivateUser(User.builder().id(99999).build()));
        assertEquals("error.unexpected", ex.getMessageKey());
    }

    @Test
    @DisplayName("Autorizácia: deactivate ako bežný user hodí invalid_credentials")
    void deactivate_notAdmin_throws() {
        loginAs(regularUser);
        ProfileException ex = assertThrows(ProfileException.class,
                () -> adminService.deactivateUser(User.builder().id(familyManager.getId()).build()));
        assertEquals("auth.error.invalid_credentials", ex.getMessageKey());
    }

    // ===================================================================
    //                          ACTIVATE
    // ===================================================================

    @Test
    @DisplayName("activate – už deaktivovaný user sa dá reaktivovať")
    void activate_reactivatesAccounts() {
        // najprv deaktivujeme
        adminService.deactivateUser(User.builder().id(regularUser.getId()).build());
        // sanity check
        List<Account> afterDeactivate = accountRepo.findAllByOwnerUserId(regularUser.getId());
        assertTrue(afterDeactivate.stream().noneMatch(Account::isActive));

        // teraz reaktivujeme
        adminService.activateUser(User.builder().id(regularUser.getId()).build());

        List<Account> afterActivate = accountRepo.findAllByOwnerUserId(regularUser.getId());
        assertTrue(afterActivate.stream().allMatch(Account::isActive),
                "všetky účty musia byť znova aktívne");
    }

    @Test
    @DisplayName("activate – je idempotentná (už aktívny user zostane aktívny)")
    void activate_idempotent() {
        // user už je aktívny z baseSetUp
        adminService.activateUser(User.builder().id(regularUser.getId()).build());

        List<Account> accounts = accountRepo.findAllByOwnerUserId(regularUser.getId());
        assertTrue(accounts.stream().allMatch(Account::isActive));
    }

    @Test
    @DisplayName("activate – admin môže aktivovať SÁM SEBA (nemá self-protection)")
    void activate_self_allowed() {
        // Zaujímavosť oproti deactivate/delete: tu nie je cannot_activate_self check
        // pretože aktivácia seba samého nemá bezpečnostný dopad.
        assertDoesNotThrow(() ->
                adminService.activateUser(User.builder().id(admin.getId()).build()));
    }

    @Test
    @DisplayName("activate – user bez účtov hodí no_accounts")
    void activate_userWithoutAccounts_throws() {
        // setUserAccountsActive rovnako kontroluje empty list aj pri aktivacii
        User orphan = saveUserWithoutAccounts("orphan2@test.sk");
        ProfileException ex = assertThrows(ProfileException.class,
                () -> adminService.activateUser(User.builder().id(orphan.getId()).build()));
        assertEquals("admin.error.no_accounts", ex.getMessageKey());
    }

    @Test
    @DisplayName("activate – null user hodí error.unexpected")
    void activate_null_throws() {
        assertThrows(ProfileException.class, () -> adminService.activateUser(null));
    }

    @Test
    @DisplayName("activate – neexistujúce ID hodí error.unexpected")
    void activate_nonExistent_throws() {
        assertThrows(ProfileException.class,
                () -> adminService.activateUser(User.builder().id(99999).build()));
    }

    @Test
    @DisplayName("Autorizácia: activate ako bežný user hodí invalid_credentials")
    void activate_notAdmin_throws() {
        loginAs(regularUser);
        assertThrows(ProfileException.class,
                () -> adminService.activateUser(User.builder().id(familyManager.getId()).build()));
    }

    // ===================================================================
    //                          DELETE
    // ===================================================================

    @Test
    @DisplayName("delete – trvalo odstráni usera z repozitára")
    void delete_removesUser() {
        int targetId = regularUser.getId();
        assertTrue(userRepo.findById(targetId).isPresent());

        adminService.deleteUser(User.builder().id(targetId).build());

        Optional<User> after = userRepo.findById(targetId);
        assertTrue(after.isEmpty(), "user musí byť zmazaný z repo");
    }

    @Test
    @DisplayName("delete – admin NEMÔŽE zmazať sám seba")
    void delete_self_throws() {
        ProfileException ex = assertThrows(ProfileException.class,
                () -> adminService.deleteUser(User.builder().id(admin.getId()).build()));
        assertEquals("admin.error.cannot_delete_self", ex.getMessageKey());
    }

    @Test
    @DisplayName("delete – null user hodí error.unexpected")
    void delete_null_throws() {
        assertThrows(ProfileException.class, () -> adminService.deleteUser(null));
    }

    @Test
    @DisplayName("delete – id == 0 hodí error.unexpected")
    void delete_zeroId_throws() {
        assertThrows(ProfileException.class,
                () -> adminService.deleteUser(User.builder().id(0).build()));
    }

    @Test
    @DisplayName("delete – záporné id hodí error.unexpected")
    void delete_negativeId_throws() {
        assertThrows(ProfileException.class,
                () -> adminService.deleteUser(User.builder().id(-5).build()));
    }

    @Test
    @DisplayName("delete – neexistujúce ID hodí error.unexpected")
    void delete_nonExistent_throws() {
        ProfileException ex = assertThrows(ProfileException.class,
                () -> adminService.deleteUser(User.builder().id(99999).build()));
        assertEquals("error.unexpected", ex.getMessageKey());
    }

    @Test
    @DisplayName("delete – user bez účtov SA DÁ zmazať (nemá no_accounts check)")
    void delete_userWithoutAccounts_ok() {
        // Zaujímavosť: deleteUser na rozdiel od deactivate NEKONTROLUJE účty.
        // Čiže user bez účtov sa DÁ zmazať, ale NEDÁ deaktivovať – asymmetric!
        User orphan = saveUserWithoutAccounts("orphan3@test.sk");

        assertDoesNotThrow(() ->
                adminService.deleteUser(User.builder().id(orphan.getId()).build()));

        assertTrue(userRepo.findById(orphan.getId()).isEmpty());
    }

    @Test
    @DisplayName("delete – účty zostanú v repozitári (orphaned accounts, dokumentujem stav)")
    void delete_leavesOrphanedAccounts() {
        // Nálezy: deleteUser volá IBA userRepository.deleteById(), neinformuje accountRepo.
        // Takže po zmazaní usera zostanú jeho účty v in-memory repo bez vlastníka.
        // V produkcii to rieši FK CASCADE v SQLite, ale na úrovni service layer
        // je to "leak" ktorý by mal byť osetreny.
        List<Account> before = accountRepo.findAllByOwnerUserId(regularUser.getId());
        assertFalse(before.isEmpty(), "setUp musí dať aspoň 1 účet");

        adminService.deleteUser(User.builder().id(regularUser.getId()).build());

        List<Account> after = accountRepo.findAllByOwnerUserId(regularUser.getId());
        // DOKUMENTUJE aktualny stav – keď sa oprava nasadí, test treba prepísať
        assertEquals(before.size(), after.size(),
                "AKTUÁLNE SPRÁVANIE: účty nie sú kaskádovo mazané na úrovni service");
    }

    @Test
    @DisplayName("Autorizácia: delete ako bežný user hodí invalid_credentials")
    void delete_notAdmin_throws() {
        loginAs(regularUser);
        ProfileException ex = assertThrows(ProfileException.class,
                () -> adminService.deleteUser(User.builder().id(familyManager.getId()).build()));
        assertEquals("auth.error.invalid_credentials", ex.getMessageKey());
    }

    @Test
    @DisplayName("Autorizácia: delete ako family manager hodí invalid_credentials")
    void delete_asFamilyManager_throws() {
        loginAs(familyManager);
        assertThrows(ProfileException.class,
                () -> adminService.deleteUser(User.builder().id(regularUser.getId()).build()));
    }
}
