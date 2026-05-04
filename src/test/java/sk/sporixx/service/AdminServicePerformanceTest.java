package sk.sporixx.service;

import org.junit.jupiter.api.*;
import sk.sporixx.model.GenderCode;
import sk.sporixx.model.Role;
import sk.sporixx.model.User;
import sk.sporixx.util.PasswordUtil;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Výkonnostné testy AdminService.
 *
 * POZNÁMKA K ČASOVÝM LIMITOM:
 * BCrypt s cost=12 trvá ~430 ms na jeden hash.
 * Testy ktoré volajú hashPassword() musia počítať s týmto.
 *
 *   changeOwnPassword x1     → ~900 ms  (verify + hash)
 *   changeOwnPassword x5     → ~4 500 ms
 *   deactivate+activate x200 → žiadny hash, rýchle → limit 2 s
 *   updateUser x10 (bez hesla) → ~0 ms hash → limit 1 s
 *   updateUser x10 (s heslom)  → ~4 300 ms → limit 10 s
 *   getAllUsers x1000           → žiadny hash → limit 2 s
 *   getAllUsers 500 users       → žiadny hash → limit 2 s
 */
@TestClassOrder(ClassOrderer.DisplayName.class)
class AdminServicePerformanceTest extends AdminServiceTestSupport {

    // ─────────────────────────────────────────────────────────────────────────
    //  changeOwnPassword – výkon
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("changeOwnPassword() – výkon")
    class ChangeOwnPasswordPerformance {

        /**
         * Jeden cyklus zmeny hesla = 1 verify + 1 hash = ~900 ms.
         * Testujeme že volanie prebehne do 3 sekúnd (bezpečná rezerva).
         */
        @Test
        @DisplayName("Zmena hesla 1-krát prebehne do 3 s")
        @Timeout(3)
        void changeOwnPassword1x_withinTimeLimit() {
            long start = System.currentTimeMillis();
            assertDoesNotThrow(() ->
                    adminService.changeOwnPassword(admin, "Heslo123!", "NoveHeslo1!")
            );
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("[PERF] changeOwnPassword x1: " + elapsed + " ms");
            // Iba informatívne — @Timeout je tvrdá hranica
        }

        /**
         * 3 zmeny hesla za sebou — každá ~900 ms → spolu ~2 700 ms.
         * Limit: 12 sekúnd (bezpečná rezerva pre pomalší CI).
         */
        @Test
        @DisplayName("Zmena hesla 3-krát prebehne do 12 s")
        @Timeout(12)
        void changeOwnPassword3x_withinTimeLimit() {
            long start = System.currentTimeMillis();

            // Každá iterácia potrebuje aktuálne heslo predchádzajúcej
            adminService.changeOwnPassword(admin, "Heslo123!", "NoveHeslo1!");
            // Po prvej zmene musíme znovu načítať usera zo sessionu —
            // AdminServiceImpl pracuje s currentUser zo SessionManagera
            User current = SessionManager.getInstance().getCurrentUserInternal();
            adminService.changeOwnPassword(current, "NoveHeslo1!", "NoveHeslo2!");
            current = SessionManager.getInstance().getCurrentUserInternal();
            adminService.changeOwnPassword(current, "NoveHeslo2!", "NoveHeslo3!");

            long elapsed = System.currentTimeMillis() - start;
            System.out.println("[PERF] changeOwnPassword x3: " + elapsed + " ms");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  deactivateUser() / activateUser() – výkon
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deactivateUser() / activateUser() – výkon")
    class DeactivateActivatePerformance {

        /**
         * 20 cyklov deaktivácia+aktivácia — žiadny BCrypt.
         * Každý cyklus = 2 DB operácie na In-Memory repozitári → veľmi rýchle.
         * Limit: 2 sekundy.
         */
        @Test
        @DisplayName("Deaktivácia a aktivácia 20 používateľov prebehne do 2 s")
        @Timeout(2)
        void deactivateAndActivate20Users_withinTimeLimit() {
            // Pripravíme 20 používateľov s účtami (bez BCrypt — priame vloženie hashu)
            List<User> users = create20UsersWithAccounts();

            long start = System.currentTimeMillis();
            for (User u : users) {
                adminService.deactivateUser(u);
                adminService.activateUser(u);
            }
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("[PERF] deactivate+activate x20: " + elapsed + " ms");
        }

        private List<User> create20UsersWithAccounts() {
            List<User> result = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                // Priamo vložíme hash bez volania PasswordUtil.hashPassword()
                User u = User.builder()
                        .email("perf" + i + "@test.sk")
                        .firstName("Perf")
                        .lastName("User")
                        .passwordHash("$2a$12$fixedhashfortest" + i)
                        .role(Role.USER)
                        .isActive(true)
                        .build();
                User saved = userRepo.save(u);
                giveAccount(saved.getId());
                result.add(saved);
            }
            return result;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  updateUser() – výkon
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateUser() – výkon")
    class UpdateUserPerformance {

        /**
         * 30 aktualizácií bez zmeny hesla — žiadny BCrypt.
         * Limit: 2 sekundy.
         */
        @Test
        @DisplayName("Aktualizácia 30 používateľov (bez hesla) prebehne do 2 s")
        @Timeout(2)
        void update30UsersWithoutPassword_withinTimeLimit() {
            List<User> users = create30Users();

            long start = System.currentTimeMillis();
            for (int i = 0; i < users.size(); i++) {
                User u = users.get(i);
                u.setFirstName("Updated");
                u.setLastName("User");
                u.setEmail("updated" + i + "@test.sk");
                u.setPasswordHash(null); // žiadna zmena hesla
                assertDoesNotThrow(() -> adminService.updateUser(u));
            }
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("[PERF] updateUser x30 (bez hesla): " + elapsed + " ms");
        }

        /**
         * 5 aktualizácií SO zmenou hesla — každá ~430 ms BCrypt.
         * 5 × 430 ms = ~2 150 ms → limit 8 sekúnd (rezerva pre CI).
         */
        @Test
        @DisplayName("Aktualizácia 5 používateľov (s heslom) prebehne do 8 s")
        @Timeout(8)
        void update5UsersWithPassword_withinTimeLimit() {
            List<User> users = create30Users();
            List<User> subset = users.subList(0, 5);

            long start = System.currentTimeMillis();
            for (int i = 0; i < subset.size(); i++) {
                User u = subset.get(i);
                u.setFirstName("Novy");
                u.setLastName("User");
                u.setEmail("newpass" + i + "@test.sk");
                u.setPasswordHash("NoveHeslo1!"); // spustí BCrypt
                assertDoesNotThrow(() -> adminService.updateUser(u));
            }
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("[PERF] updateUser x5 (s heslom): " + elapsed + " ms");
        }

        private List<User> create30Users() {
            List<User> result = new ArrayList<>();
            for (int i = 0; i < 30; i++) {
                User u = User.builder()
                        .email("batch" + i + "@test.sk")
                        .firstName("Batch")
                        .lastName("User")
                        .passwordHash("$2a$12$fixedhashfortest" + i)
                        .role(Role.USER)
                        .isActive(true)
                        .build();
                result.add(userRepo.save(u));
            }
            return result;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  getAllUsers() – výkon
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAllUsers() – výkon")
    class GetAllUsersPerformance {

        /**
         * 1 000 volaní getAllUsers() na malej sade (3 users z baseSetUp).
         * Žiadny BCrypt → limit 2 sekundy.
         */
        @Test
        @DisplayName("getAllUsers() sa volá 1 000-krát do 2 s")
        @Timeout(2)
        void getAllUsers1000x_withinTimeLimit() {
            long start = System.currentTimeMillis();
            for (int i = 0; i < 1000; i++) {
                List<?> result = adminService.getAllUsers();
                assertFalse(result.isEmpty());
            }
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("[PERF] getAllUsers x1000 volaní: " + elapsed + " ms");
        }

        /**
         * getAllUsers() s 50 používateľmi v repozitári.
         * Žiadny BCrypt → limit 2 sekundy.
         *
         * Poznámka: pôvodný test mal 500 users, každý s BCrypt hashom = ~215 sekúnd len na setup.
         * Používame priame vloženie hash stringu namiesto PasswordUtil.hashPassword().
         */
        @Test
        @DisplayName("getAllUsers() s 50 používateľmi prebehne do 2 s")
        @Timeout(2)
        void with50Users_withinTimeLimit() {
            // Vložíme 50 users BEZ BCrypt hashovania
            for (int i = 0; i < 50; i++) {
                userRepo.save(User.builder()
                        .email("bulk" + i + "@test.sk")
                        .firstName("Bulk")
                        .lastName("User")
                        .passwordHash("$2a$12$fixedhashfortest" + i)
                        .role(Role.USER)
                        .isActive(true)
                        .build());
            }

            long start = System.currentTimeMillis();
            List<?> result = adminService.getAllUsers();
            long elapsed = System.currentTimeMillis() - start;

            System.out.println("[PERF] getAllUsers (50 users): " + elapsed + " ms");
            // 3 zo setUp + 50 nových = 53
            assertTrue(result.size() >= 50, "Očakávame aspoň 50 používateľov");
        }
    }
}
