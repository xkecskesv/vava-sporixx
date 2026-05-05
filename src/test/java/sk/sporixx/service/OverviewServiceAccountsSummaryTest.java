package sk.sporixx.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import sk.sporixx.dto.AccountsSummaryData;
import sk.sporixx.model.Account;
import sk.sporixx.model.Role;
import sk.sporixx.model.User;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy pre {@link OverviewService#loadAccountsSummary()}.
 *
 * Pokrývame:
 *   - total balance (súčet všetkých účtov)
 *   - prázdna session (žiadne účty)
 *   - jeden účet, viac účtov
 *   - saving goal sa načíta len pre saving účty
 *   - emergency fond a main account nemajú saving goal
 */
@DisplayName("OverviewService – loadAccountsSummary()")
class OverviewServiceAccountsSummaryTest extends OverviewServiceTestSupport {

    // ======================== TOTAL BALANCE ========================

    @Nested
    @DisplayName("Total balance")
    class TotalBalance {

        @Test
        @DisplayName("3 účty → total = súčet ich currentBalance")
        void totalBalance_threeAccounts_summed() {
            // main=1000, emergency=2000, saving=500 → 3500
            AccountsSummaryData data = overviewService.loadAccountsSummary();

            assertEquals(3500.0, data.getTotalBalance(), 0.001,
                    "Total balance musí byť súčet všetkých currentBalance");
        }

        @Test
        @DisplayName("1 účet → total = jeho currentBalance")
        void totalBalance_oneAccount_equals() {
            User user = User.builder().id(10).firstName("X").lastName("Y")
                    .email("x@y.sk").gender("M").role(Role.USER)
                    .createdAt(LocalDateTime.now()).build();
            Account only = Account.builder().id(10).ownerUserId(10).regionId(1)
                    .accountTypeId(Account.MAIN_ACCOUNT).defaultCurrencyCode("EUR")
                    .initialBalance(500.0).currentBalance(777.50)
                    .isActive(true).createdAt(LocalDateTime.now()).build();

            SessionManager.getInstance().setSession(user, List.of(only));

            AccountsSummaryData data = overviewService.loadAccountsSummary();

            assertEquals(777.50, data.getTotalBalance(), 0.001);
        }

        @Test
        @DisplayName("nulové zostatky → total = 0")
        void totalBalance_zeroes_isZero() {
            User user = User.builder().id(20).firstName("Z").lastName("Z")
                    .email("z@z.sk").gender("M").role(Role.USER)
                    .createdAt(LocalDateTime.now()).build();
            Account a = Account.builder().id(20).ownerUserId(20).regionId(1)
                    .accountTypeId(Account.MAIN_ACCOUNT).defaultCurrencyCode("EUR")
                    .initialBalance(0.0).currentBalance(0.0)
                    .isActive(true).createdAt(LocalDateTime.now()).build();
            Account b = Account.builder().id(21).ownerUserId(20).regionId(1)
                    .accountTypeId(Account.EMERGENCY_FUND).defaultCurrencyCode("EUR")
                    .initialBalance(0.0).currentBalance(0.0)
                    .isActive(true).createdAt(LocalDateTime.now()).build();

            SessionManager.getInstance().setSession(user, List.of(a, b));

            AccountsSummaryData data = overviewService.loadAccountsSummary();

            assertEquals(0.0, data.getTotalBalance(), 0.001);
        }
    }

    // ======================== ZOZNAM ÚČTOV ========================

    @Nested
    @DisplayName("Zoznam účtov")
    class AccountList {

        @Test
        @DisplayName("prázdna session → prázdny zoznam účtov, balance = 0")
        void noAccounts_emptyResult() {
            SessionManager.getInstance().clearSession();

            // Keď nie je session, loadAccountsSummary vráti prázdny sumár
            AccountsSummaryData data = overviewService.loadAccountsSummary();

            assertEquals(0.0, data.getTotalBalance(), 0.001);
            assertTrue(data.getAccounts().isEmpty());
            assertTrue(data.getSavingGoalByAccountId().isEmpty());
        }

        @Test
        @DisplayName("3 účty → accounts list má 3 položky")
        void accounts_returnedAll() {
            AccountsSummaryData data = overviewService.loadAccountsSummary();

            assertEquals(3, data.getAccounts().size());
        }

        @Test
        @DisplayName("výsledok obsahuje správne ID účtov")
        void accounts_correctIds() {
            AccountsSummaryData data = overviewService.loadAccountsSummary();

            List<Integer> ids = data.getAccounts().stream()
                    .map(Account::getId)
                    .toList();

            assertTrue(ids.contains(mainAccount.getId()));
            assertTrue(ids.contains(emergencyAccount.getId()));
            assertTrue(ids.contains(savingAccount.getId()));
        }
    }

    // ======================== SAVING GOALS ========================

    @Nested
    @DisplayName("Saving goals")
    class SavingGoals {

        @Test
        @DisplayName("saving goal existuje → načíta sa pre saving účet")
        void savingGoal_loaded_forSavingAccount() {
            addSavingGoal(savingAccount.getId(), 10000.0, 500.0);

            AccountsSummaryData data = overviewService.loadAccountsSummary();

            assertTrue(data.getSavingGoalByAccountId().containsKey(savingAccount.getId()),
                    "Saving goal musí byť v mape pre saving účet");
        }

        @Test
        @DisplayName("saving goal pre saving účet má správne hodnoty")
        void savingGoal_correctValues() {
            addSavingGoal(savingAccount.getId(), 10000.0, 500.0);

            AccountsSummaryData data = overviewService.loadAccountsSummary();

            var goal = data.getSavingGoalByAccountId().get(savingAccount.getId());
            assertNotNull(goal);
            assertEquals(10000.0, goal.getTargetAmount(), 0.001);
            assertEquals(500.0, goal.getCurrentAmount(), 0.001);
        }

        @Test
        @DisplayName("žiadny saving goal → mapa je prázdna")
        void noSavingGoal_emptyMap() {
            AccountsSummaryData data = overviewService.loadAccountsSummary();

            assertTrue(data.getSavingGoalByAccountId().isEmpty(),
                    "Bez saving goals musí byť mapa prázdna");
        }

        @Test
        @DisplayName("saving goal sa NEOBJAVÍ pre main account (len pre saving účty)")
        void savingGoal_notLoadedForMainAccount() {
            // Pridáme goal s accountId = mainAccount (čo je neštandardné, ale testujeme logiku)
            savingGoalRepo.save(sk.sporixx.model.SavingGoal.builder()
                    .accountId(mainAccount.getId())
                    .name("Wrong goal")
                    .goalTypeId(1)
                    .targetAmount(1000.0).currentAmount(100.0)
                    .targetDate(LocalDateTime.now().plusYears(1))
                    .isActive(true).createdAt(LocalDateTime.now())
                    .build());

            AccountsSummaryData data = overviewService.loadAccountsSummary();

            // Main account nie je saving → goal sa nesmie načítať
            assertFalse(data.getSavingGoalByAccountId().containsKey(mainAccount.getId()),
                    "Main account nie je saving → saving goal sa nesmie objaviť v mape");
        }

        @Test
        @DisplayName("viac saving účtov → každý má svoj goal")
        void multiSavingAccounts_eachHasGoal() {
            User user = User.builder().id(30).firstName("A").lastName("B")
                    .email("ab@sk.sk").gender("M").role(Role.USER)
                    .createdAt(LocalDateTime.now()).build();
            Account s1 = Account.builder().id(31).ownerUserId(30).regionId(1)
                    .accountTypeId(Account.SAVING_ACCOUNT).defaultCurrencyCode("EUR")
                    .initialBalance(0.0).currentBalance(1000.0)
                    .isActive(true).createdAt(LocalDateTime.now()).build();
            Account s2 = Account.builder().id(32).ownerUserId(30).regionId(1)
                    .accountTypeId(Account.SAVING_ACCOUNT).defaultCurrencyCode("EUR")
                    .initialBalance(0.0).currentBalance(2000.0)
                    .isActive(true).createdAt(LocalDateTime.now()).build();

            SessionManager.getInstance().setSession(user, List.of(s1, s2));
            addSavingGoal(s1.getId(), 5000.0, 1000.0);
            addSavingGoal(s2.getId(), 8000.0, 2000.0);

            AccountsSummaryData data = overviewService.loadAccountsSummary();

            assertEquals(2, data.getSavingGoalByAccountId().size());
            assertTrue(data.getSavingGoalByAccountId().containsKey(s1.getId()));
            assertTrue(data.getSavingGoalByAccountId().containsKey(s2.getId()));
        }
    }

    // ======================== EDGE CASES ========================

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("Účty so záporným zostatkom → total môže byť záporné")
        void negativeBalance_reflectedInTotal() {
            User user = User.builder().id(50).firstName("X").lastName("Y")
                    .email("xy@sk.sk").gender("M").role(Role.USER)
                    .createdAt(LocalDateTime.now()).build();
            Account acc = Account.builder().id(51).ownerUserId(50).regionId(1)
                    .accountTypeId(Account.MAIN_ACCOUNT).defaultCurrencyCode("EUR")
                    .initialBalance(0.0).currentBalance(-500.0)
                    .isActive(true).createdAt(LocalDateTime.now()).build();
            SessionManager.getInstance().setSession(user, List.of(acc));

            AccountsSummaryData data = overviewService.loadAccountsSummary();

            assertEquals(-500.0, data.getTotalBalance(), 0.001);
        }

        @Test
        @DisplayName("Saving goal s 100% progress → stav dosiahnutý")
        void savingGoal100Percent_reflected() {
            addSavingGoal(savingAccount.getId(), 1000.0, 1000.0);
            AccountsSummaryData data = overviewService.loadAccountsSummary();

            var goal = data.getSavingGoalByAccountId().get(savingAccount.getId());
            assertNotNull(goal);
            assertEquals(1000.0, goal.getCurrentAmount(), 0.001);
            assertEquals(1000.0, goal.getTargetAmount(), 0.001);
        }

        @Test
        @DisplayName("loadAccountsSummary() vracia správny počet účtov zo session")
        void accountCount_matchesSession() {
            AccountsSummaryData data = overviewService.loadAccountsSummary();
            // main + emergency + saving = 3
            assertEquals(3, data.getAccounts().size());
        }
    }
}

