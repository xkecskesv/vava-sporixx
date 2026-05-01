package sk.sporixx.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import sk.sporixx.dto.AdminUserData;
import sk.sporixx.model.GenderCode;
import sk.sporixx.model.Role;
import sk.sporixx.model.User;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance testy pre AdminService.
 */
@DisplayName("AdminService – Performance testy")
class AdminServicePerformanceTest extends AdminServiceTestSupport {

    @Nested
    @DisplayName("getAllUsers() – výkon")
    class GetAllUsersPerformance {

        @Test
        @Timeout(value = 2, unit = TimeUnit.SECONDS)
        @DisplayName("getAllUsers() s 500 používateľmi prebehne do 2 s")
        void with500Users_withinTimeLimit() {
            for (int i = 1; i <= 497; i++) {
                saveUser("user" + i + "@sporixx.sk", "User", "Num" + i,
                        Role.USER, GenderCode.MALE, true);
            }

            long start = System.nanoTime();
            List<AdminUserData> result = adminService.getAllUsers();
            long ms = (System.nanoTime() - start) / 1_000_000;

            // admin + regularUser + familyManager + 497 nových = 500
            assertEquals(500, result.size());
            System.out.printf("[PERF] getAllUsers (500 users): %d ms%n", ms);
        }

        @Test
        @Timeout(value = 2, unit = TimeUnit.SECONDS)
        @DisplayName("getAllUsers() sa volá 1 000-krát do 2 s")
        void called1000x_withinTimeLimit() {
            long start = System.nanoTime();
            for (int i = 0; i < 1000; i++) {
                adminService.getAllUsers();
            }
            long ms = (System.nanoTime() - start) / 1_000_000;
            System.out.printf("[PERF] getAllUsers x1000 volaní: %d ms%n", ms);
        }
    }

    @Nested
    @DisplayName("updateUser() – výkon")
    class UpdateUserPerformance {

        @Test
        @Timeout(value = 2, unit = TimeUnit.SECONDS)
        @DisplayName("Aktualizácia 300 používateľov prebehne do 2 s")
        void update300Users_withinTimeLimit() {
            List<User> users = new java.util.ArrayList<>();
            for (int i = 1; i <= 300; i++) {
                users.add(saveUser("upd" + i + "@sporixx.sk", "Old", "Name" + i,
                        Role.USER, GenderCode.FEMALE, true));
            }

            long start = System.nanoTime();
            for (User u : users) {
                u.setFirstName("Updated");
                adminService.updateUser(u);
            }
            long ms = (System.nanoTime() - start) / 1_000_000;
            System.out.printf("[PERF] updateUser x300: %d ms%n", ms);
        }
    }

    @Nested
    @DisplayName("deactivateUser() / activateUser() – výkon")
    class LifecyclePerformance {

        @Test
        @Timeout(value = 3, unit = TimeUnit.SECONDS)
        @DisplayName("Deaktivácia a aktivácia 200 používateľov prebehne do 3 s")
        void deactivateAndActivate200Users_withinTimeLimit() {
            List<User> users = new java.util.ArrayList<>();
            for (int i = 1; i <= 200; i++) {
                User u = saveUser("lc" + i + "@sporixx.sk", "Life", "Cycle" + i,
                        Role.USER, GenderCode.MALE, true);
                giveAccount(u.getId());
                users.add(u);
            }

            long start = System.nanoTime();
            for (User u : users) {
                adminService.deactivateUser(u);
            }
            for (User u : users) {
                adminService.activateUser(u);
            }
            long ms = (System.nanoTime() - start) / 1_000_000;
            System.out.printf("[PERF] deactivate+activate x200: %d ms%n", ms);
        }
    }

    @Nested
    @DisplayName("changeOwnPassword() – výkon")
    class ChangePasswordPerformance {

        @Test
        @Timeout(value = 90, unit = TimeUnit.SECONDS)
        @DisplayName("Zmena hesla 100-krát prebehne do 90 s")
        void changePassword100x_withinTimeLimit() {
            loginAs(admin);

            long start = System.nanoTime();
            for (int i = 0; i < 100; i++) {
                // každý cyklus zmení heslo na nové a použije ho ako staré v ďalšom
                String oldPwd = i == 0 ? "Heslo123!" : "NewHeslo" + i + "!";
                String newPwd = "NewHeslo" + (i + 1) + "!";
                adminService.changeOwnPassword(admin, oldPwd, newPwd);
            }
            long ms = (System.nanoTime() - start) / 1_000_000;
            System.out.printf("[PERF] changeOwnPassword x100: %d ms%n", ms);
        }
    }
}

