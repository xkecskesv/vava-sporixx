package sk.sporixx.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sk.sporixx.dto.AdminUserData;
import sk.sporixx.model.Account;
import sk.sporixx.model.Role;
import sk.sporixx.model.User;
import sk.sporixx.repository.AccountRepository;
import sk.sporixx.repository.UserRepository;
import sk.sporixx.service.testovanie.InMemoryAccountRepository;
import sk.sporixx.service.testovanie.InMemoryUserRepository;
import sk.sporixx.util.PasswordUtil;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lightweight integration testy – overujeme SCENARE (viaceré metódy za sebou).
 *
 * Na rozdiel od predchádzajúcich unit testov tu:
 *   - reálna spolupráca AdminService + UserRepository + AccountRepository + SessionManager
 *   - reálna UserServiceImpl (nie mock)
 *   - failing repositories ako spôsob testovania error-handling spoluprace
 */
class AdminServiceIntegrationTest extends AdminServiceTestSupport {

    // ===================================================================
    //                  END-TO-END SCENÁRE
    // ===================================================================

    @Test
    @DisplayName("E2E: admin vytvorí usera → vidí ho v zozname → upraví mu meno → deaktivuje")
    void endToEnd_userLifecycle() {
        // 1. pridáme si novy "čerstvý" user do repo (simulácia registrácie)
        User freshUser = User.builder()
                .email("cerstvy@test.sk")
                .firstName("Čerstvý")
                .lastName("User")
                .passwordHash(PasswordUtil.hashPassword("Heslo123!"))
                .role(Role.USER)
                .isActive(true)
                .build();
        userRepo.save(freshUser);
        giveAccount(freshUser.getId());

        // 2. admin ho vidí v zozname
        List<AdminUserData> list = adminService.getAllUsers();
        assertTrue(list.stream().anyMatch(u -> "cerstvy@test.sk".equalsIgnoreCase(u.getEmail())));

        // 3. admin upraví meno
        adminService.updateUser(User.builder()
                .id(freshUser.getId())
                .firstName("Upravený")
                .lastName("User")
                .email("cerstvy@test.sk")
                .build());

        User afterUpdate = userRepo.findById(freshUser.getId()).orElseThrow();
        assertEquals("Upravený", afterUpdate.getFirstName());

        // 4. admin ho deaktivuje
        adminService.deactivateUser(User.builder().id(freshUser.getId()).build());
        List<Account> accounts = accountRepo.findAllByOwnerUserId(freshUser.getId());
        assertTrue(accounts.stream().noneMatch(Account::isActive));

        // 5. reaktivácia
        adminService.activateUser(User.builder().id(freshUser.getId()).build());
        accounts = accountRepo.findAllByOwnerUserId(freshUser.getId());
        assertTrue(accounts.stream().allMatch(Account::isActive));

        // 6. delete
        adminService.deleteUser(User.builder().id(freshUser.getId()).build());
        assertTrue(userRepo.findById(freshUser.getId()).isEmpty());

        // 7. v zozname už nie je
        list = adminService.getAllUsers();
        assertFalse(list.stream().anyMatch(u -> "cerstvy@test.sk".equalsIgnoreCase(u.getEmail())));
    }

    @Test
    @DisplayName("E2E: admin si zmení heslo a staré prestane platiť v repo")
    void endToEnd_changeOwnPassword_persistsToRepo() {
        adminService.changeOwnPassword(admin, "Heslo123!", "ÚplneNoveHeslo456!");

        User persisted = userRepo.findById(admin.getId()).orElseThrow();
        assertFalse(PasswordUtil.verifyPassword("Heslo123!", persisted.getPasswordHash()));
        assertTrue(PasswordUtil.verifyPassword("ÚplneNoveHeslo456!", persisted.getPasswordHash()));
    }

    @Test
    @DisplayName("E2E: deactivate → reactivate → údaje v session zostanú konzistentné")
    void endToEnd_sessionStaysValidAfterLifecycle() {
        User before = SessionManager.getInstance().getCurrentUserInternal();
        assertNotNull(before);
        assertEquals(admin.getId(), before.getId());

        adminService.deactivateUser(User.builder().id(regularUser.getId()).build());
        adminService.activateUser(User.builder().id(regularUser.getId()).build());

        User after = SessionManager.getInstance().getCurrentUserInternal();
        assertNotNull(after);
        assertEquals(admin.getId(), after.getId(),
                "operácie nad iným userom nesmú pokaziť session prihláseného admina");
    }

    // ===================================================================
    //                  FAILING REPOSITORIES
    // ===================================================================

    @Test
    @DisplayName("Ak userRepository.update() zlyhá, zmena hesla sa nepreukáže ako úspech")
    void changeOwnPassword_repoUpdateFails_propagates() {
        // Failing repo hodí výnimku na update
        UserRepository failing = new InMemoryUserRepository() {
            @Override
            public User update(User user) {
                throw new RuntimeException("Simulated DB failure on update");
            }
        };

        // Najprv tam uložíme admina, aby to bola rovnaká session
        failing.save(admin);
        // Reset session: session sa odkazuje na 'admin' objekt, ktorý bol uložený aj sem

        AdminService svc = new AdminServiceImpl(failing, accountRepo, userService);

        // Pokus o zmenu hesla – update zlyhá
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> svc.changeOwnPassword(admin, "Heslo123!", "NoveHeslo456!"));
        assertEquals("Simulated DB failure on update", ex.getMessage());

        // BUG dokumentácia: SessionManager má aktualizovaný hash aj keď DB zlyhala
        // (applyPasswordChange beží PRED userRepository.update)
        assertTrue(PasswordUtil.verifyPassword("NoveHeslo456!", admin.getPasswordHash()),
                "session user má nové heslo, hoci DB update zlyhal → inconsistency medzi " +
                "session a repo. Ak admin zostane prihlásený, funguje mu 'nové' heslo, " +
                "ale po odhlásení/reštarte sa vráti staré.");
    }

    @Test
    @DisplayName("Ak accountRepository.findAllByOwnerUserId zlyhá, deactivate padne cleanly")
    void deactivate_accountRepoFails_propagates() {
        AccountRepository failingAccounts = new InMemoryAccountRepository() {
            @Override
            public List<Account> findAllByOwnerUserId(int userId) {
                throw new RuntimeException("Simulated DB failure on findAllByOwnerUserId");
            }
        };
        AdminService svc = new AdminServiceImpl(userRepo, failingAccounts, userService);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> svc.deactivateUser(User.builder().id(regularUser.getId()).build()));
        assertEquals("Simulated DB failure on findAllByOwnerUserId", ex.getMessage());
    }

    @Test
    @DisplayName("Ak deactivateById na jednom účte zlyhá, ostatné účty môžu zostať v mixed state")
    void deactivate_partialFailure_leavesMixedState() {
        // Pridáme regularUserovi druhý účet
        giveAccount(regularUser.getId());
        List<Account> accounts = accountRepo.findAllByOwnerUserId(regularUser.getId());
        assertEquals(2, accounts.size());
        int firstAccountId = accounts.get(0).getId();

        AccountRepository mixedFail = new InMemoryAccountRepository() {
            {
                // kopirujeme data do tohto novéh repo
                for (Account a : accounts) {
                    super.save(a);
                }
            }
            @Override
            public void deactivateById(int accountId) {
                // prvý účet sa NEdeaktivuje – simulujeme transient failure
                if (accountId == firstAccountId) {
                    throw new RuntimeException("Simulated failure on first account");
                }
                super.deactivateById(accountId);
            }
        };
        AdminService svc = new AdminServiceImpl(userRepo, mixedFail, userService);

        // BUG nález: deactivate nie je transactional
        // Ak prvý účet zlyhá, pre druhý sa nikdy neprejde - throw je fail-fast
        assertThrows(RuntimeException.class,
                () -> svc.deactivateUser(User.builder().id(regularUser.getId()).build()));
    }

    // ===================================================================
    //                  COLLABORATION MEDZI METÓDAMI
    // ===================================================================

    @Test
    @DisplayName("getAllUsers po updateUser reflektuje zmenu okamžite")
    void getAllUsers_reflectsUpdate() {
        adminService.updateUser(User.builder()
                .id(regularUser.getId())
                .firstName("Čerstvé")
                .lastName("Meno")
                .email("novyEmail@test.sk")
                .build());

        List<AdminUserData> users = adminService.getAllUsers();
        Optional<AdminUserData> found = users.stream()
                .filter(u -> u.getId() == regularUser.getId())
                .findFirst();

        assertTrue(found.isPresent());
        assertEquals("Čerstvé", found.get().getFirstName());
        assertEquals("novyemail@test.sk", found.get().getEmail());
    }

    @Test
    @DisplayName("getAllUsers po deactivateUser zobrazí active=false")
    void getAllUsers_reflectsDeactivation() {
        // Pozor: AdminUserData.active sa mapuje z User.isActive
        // Ale deactivateUser meni iba Account.isActive, NIE User.isActive!
        //
        // Čiže v tabuľke admin panelu bude user stále zobrazený ako active=true
        // hoci jeho účty sú deaktivované. Je to BUG/konzistenčný problém:
        // UI zobrazuje 'user status', zatiaľ čo business deaktivuje 'account status'.

        adminService.deactivateUser(User.builder().id(regularUser.getId()).build());

        List<AdminUserData> users = adminService.getAllUsers();
        Optional<AdminUserData> dto = users.stream()
                .filter(u -> u.getId() == regularUser.getId())
                .findFirst();

        assertTrue(dto.isPresent());
        // AKTUÁLNE SPRÁVANIE: user.active ostáva true hoci účty sú deaktivované
        assertTrue(dto.get().isActive(),
                "AKTUALNE: User.isActive zostáva true aj po deaktivácii účtov. " +
                "POTENCIÁLNY BUG: UI zobrazí nekonzistentný stav.");
    }

    @Test
    @DisplayName("getAllUsers po deleteUser daného usera už neobsahuje")
    void getAllUsers_reflectsDelete() {
        int sizeBefore = adminService.getAllUsers().size();

        adminService.deleteUser(User.builder().id(regularUser.getId()).build());

        List<AdminUserData> after = adminService.getAllUsers();
        assertEquals(sizeBefore - 1, after.size());
        assertTrue(after.stream().noneMatch(u -> u.getId() == regularUser.getId()));
    }

    @Test
    @DisplayName("Po updateUser s novým emailom je nový email dostupný pre findByEmail")
    void updateUser_emailChange_reflectedInFindByEmail() {
        adminService.updateUser(User.builder()
                .id(regularUser.getId())
                .firstName("Marek")
                .lastName("Moško")
                .email("uplne.novy@email.sk")
                .build());

        Optional<User> byNew = userRepo.findByEmail("uplne.novy@email.sk");
        assertTrue(byNew.isPresent());
        assertEquals(regularUser.getId(), byNew.get().getId());

        // InMemoryUserRepository pouziva equalsIgnoreCase, takze staré email uz nenájde
        Optional<User> byOld = userRepo.findByEmail("marek.mosko@stuba.sk");
        assertTrue(byOld.isEmpty());
    }

    // ===================================================================
    //                  SESSION SINGLETON ISOLATION
    // ===================================================================

    @Test
    @DisplayName("Session isolation: každý test začína s čistou session (tearDown funguje)")
    void sessionIsolation_cleanStart() {
        // Test verifies že baseSetUp naozaj dá session do známeho stavu.
        // Ak by predchádzajúci test nevyčistil session, tento by zlyhal.
        User current = SessionManager.getInstance().getCurrentUserInternal();
        assertNotNull(current);
        assertEquals(admin.getId(), current.getId());
        assertEquals(Role.ADMIN, current.getRole());
    }
}
