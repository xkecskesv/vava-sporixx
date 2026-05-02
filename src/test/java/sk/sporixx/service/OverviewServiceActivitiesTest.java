package sk.sporixx.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import sk.sporixx.dto.ActivitiesData;
import sk.sporixx.model.RecurringRule;
import sk.sporixx.model.Transaction;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy pre {@link OverviewService#loadActivities(int)}.
 *
 * Pokrývame:
 *   - prázdne dáta → prázdne zoznamy
 *   - upcoming payments (len najbližšie 3, zoradené podľa nextDueDate)
 *   - recent transactions (posledné 2 týždne, nie staršie)
 *   - filtrovanie podľa accountId
 *   - zoradenie recent transactions (od najnovšej)
 *   - limit 3 pre upcoming payments
 */
@DisplayName("OverviewService – loadActivities()")
class OverviewServiceActivitiesTest extends OverviewServiceTestSupport {

    // ======================== PRÁZDNE DÁTA ========================

    @Nested
    @DisplayName("Prázdne dáta")
    class EmptyData {

        @Test
        @DisplayName("žiadne transakcie ani recurring rules → obe zoznamy prázdne")
        void noData_emptyLists() {
            ActivitiesData data = overviewService.loadActivities(mainAccount.getId());

            assertNotNull(data);
            assertTrue(data.getUpcomingPayments().isEmpty(),
                    "Bez recurring rules musí byť upcomingPayments prázdny");
            assertTrue(data.getRecentTransactions().isEmpty(),
                    "Bez transakcií musí byť recentTransactions prázdny");
        }
    }

    // ======================== UPCOMING PAYMENTS ========================

    @Nested
    @DisplayName("Upcoming payments")
    class UpcomingPayments {

        @Test
        @DisplayName("1 aktívny recurring rule → 1 upcoming payment")
        void oneRule_oneUpcoming() {
            addRecurringRule(mainAccount.getId(), 50.0, LocalDateTime.now().plusDays(5));

            ActivitiesData data = overviewService.loadActivities(mainAccount.getId());

            assertEquals(1, data.getUpcomingPayments().size());
        }

        @Test
        @DisplayName("upcoming payments sú zoradené podľa nextDueDate (od najbližšieho)")
        void upcomingPayments_sortedByNextDueDate() {
            LocalDateTime now = LocalDateTime.now();
            addRecurringRule(mainAccount.getId(), 100.0, now.plusDays(10));
            addRecurringRule(mainAccount.getId(), 50.0, now.plusDays(2));   // najbližší
            addRecurringRule(mainAccount.getId(), 200.0, now.plusDays(7));

            ActivitiesData data = overviewService.loadActivities(mainAccount.getId());

            List<RecurringRule> upcoming = data.getUpcomingPayments();
            // prvý musí mať najmenší nextDueDate
            assertTrue(upcoming.get(0).getNextDueDate().isBefore(upcoming.get(1).getNextDueDate()),
                    "Upcoming payments musia byť zoradené od najbližšieho");
            assertTrue(upcoming.get(1).getNextDueDate().isBefore(upcoming.get(2).getNextDueDate()));
        }

        @Test
        @DisplayName("limit 3 — pri 5 rules sa vrátia len 3 najbližšie")
        void moreRules_limitedToThree() {
            LocalDateTime now = LocalDateTime.now();
            addRecurringRule(mainAccount.getId(), 10.0, now.plusDays(1));
            addRecurringRule(mainAccount.getId(), 20.0, now.plusDays(3));
            addRecurringRule(mainAccount.getId(), 30.0, now.plusDays(5));
            addRecurringRule(mainAccount.getId(), 40.0, now.plusDays(8));
            addRecurringRule(mainAccount.getId(), 50.0, now.plusDays(12));

            ActivitiesData data = overviewService.loadActivities(mainAccount.getId());

            assertEquals(3, data.getUpcomingPayments().size(),
                    "loadActivities vracia max 3 upcoming payments");
        }

        @Test
        @DisplayName("minulý recurring rule (nextDueDate v minulosti) sa neobjaví")
        void pastRecurringRule_excluded() {
            // nextDueDate v minulosti
            addRecurringRule(mainAccount.getId(), 100.0, LocalDateTime.now().minusDays(1));

            ActivitiesData data = overviewService.loadActivities(mainAccount.getId());

            assertTrue(data.getUpcomingPayments().isEmpty(),
                    "Recurring rule s nextDueDate v minulosti sa nesmie objaviť v upcoming");
        }

        @Test
        @DisplayName("recurring rule iného účtu sa nezobrazí")
        void otherAccountRule_excluded() {
            addRecurringRule(emergencyAccount.getId(), 100.0, LocalDateTime.now().plusDays(3));

            ActivitiesData data = overviewService.loadActivities(mainAccount.getId());

            assertTrue(data.getUpcomingPayments().isEmpty(),
                    "Recurring rule iného účtu sa nesmie objaviť");
        }

        @Test
        @DisplayName("neaktívny recurring rule sa nezobrazí")
        void inactiveRule_excluded() {
            // Pridáme rule, potom ho deaktivujeme
            addRecurringRule(mainAccount.getId(), 100.0, LocalDateTime.now().plusDays(5));
            List<RecurringRule> all = recurringRuleRepo.findAll();
            recurringRuleRepo.deactivateById(all.get(0).getId());

            ActivitiesData data = overviewService.loadActivities(mainAccount.getId());

            assertTrue(data.getUpcomingPayments().isEmpty(),
                    "Neaktívny recurring rule sa nesmie objaviť v upcoming");
        }

        @Test
        @DisplayName("3 rules — limit 3 vracia všetky 3")
        void exactlyThreeRules_allReturned() {
            LocalDateTime now = LocalDateTime.now();
            addRecurringRule(mainAccount.getId(), 10.0, now.plusDays(1));
            addRecurringRule(mainAccount.getId(), 20.0, now.plusDays(4));
            addRecurringRule(mainAccount.getId(), 30.0, now.plusDays(7));

            ActivitiesData data = overviewService.loadActivities(mainAccount.getId());

            assertEquals(3, data.getUpcomingPayments().size());
        }
    }

    // ======================== RECENT TRANSACTIONS ========================

    @Nested
    @DisplayName("Recent transactions")
    class RecentTransactions {

        @Test
        @DisplayName("dnešná transakcia sa objaví v recent")
        void todayTransaction_included() {
            addIncome(mainAccount.getId(), 200.0, LocalDateTime.now());

            ActivitiesData data = overviewService.loadActivities(mainAccount.getId());

            assertEquals(1, data.getRecentTransactions().size());
        }

        @Test
        @DisplayName("transakcia spred 10 dní (< 2 týždne) sa objaví v recent")
        void tenDaysAgo_included() {
            addIncome(mainAccount.getId(), 100.0, LocalDateTime.now().minusDays(10));

            ActivitiesData data = overviewService.loadActivities(mainAccount.getId());

            assertEquals(1, data.getRecentTransactions().size());
        }

        @Test
        @DisplayName("transakcia spred 3 týždňov sa NEobjaví v recent")
        void threeWeeksAgo_excluded() {
            addIncome(mainAccount.getId(), 100.0, LocalDateTime.now().minusWeeks(3));

            ActivitiesData data = overviewService.loadActivities(mainAccount.getId());

            assertTrue(data.getRecentTransactions().isEmpty(),
                    "Transakcia staršia ako 2 týždne sa nesmie objaviť v recent");
        }

        @Test
        @DisplayName("recent transactions iného účtu sa NEzobrazí")
        void otherAccountTransaction_excluded() {
            addIncome(emergencyAccount.getId(), 500.0, LocalDateTime.now().minusDays(1));

            ActivitiesData data = overviewService.loadActivities(mainAccount.getId());

            assertTrue(data.getRecentTransactions().isEmpty(),
                    "Transakcia iného účtu sa nesmie objaviť");
        }

        @Test
        @DisplayName("recent transactions sú zoradené od najnovšej")
        void recentTransactions_sortedNewestFirst() {
            LocalDateTime now = LocalDateTime.now();
            addIncome(mainAccount.getId(), 100.0, now.minusDays(7));
            addIncome(mainAccount.getId(), 200.0, now.minusDays(1));
            addExpense(mainAccount.getId(), 50.0, now.minusDays(3));

            ActivitiesData data = overviewService.loadActivities(mainAccount.getId());

            List<Transaction> recent = data.getRecentTransactions();
            assertEquals(3, recent.size());
            assertTrue(recent.get(0).getCompleteDate().isAfter(recent.get(1).getCompleteDate()),
                    "Najnovšia transakcia musí byť prvá");
            assertTrue(recent.get(1).getCompleteDate().isAfter(recent.get(2).getCompleteDate()));
        }

        @Test
        @DisplayName("income aj expense sa obe objavia v recent")
        void incomeAndExpense_bothIncluded() {
            LocalDateTime now = LocalDateTime.now();
            addIncome(mainAccount.getId(), 500.0, now.minusDays(2));
            addExpense(mainAccount.getId(), 50.0, now.minusDays(4));

            ActivitiesData data = overviewService.loadActivities(mainAccount.getId());

            assertEquals(2, data.getRecentTransactions().size());
            long incomeCount = data.getRecentTransactions().stream()
                    .filter(t -> t.getTransactionTypeId() == Transaction.TYPE_INCOME).count();
            long expenseCount = data.getRecentTransactions().stream()
                    .filter(t -> t.getTransactionTypeId() == Transaction.TYPE_EXPENSE).count();
            assertEquals(1, incomeCount);
            assertEquals(1, expenseCount);
        }
    }

    // ======================== KOMBINÁCIA ========================

    @Nested
    @DisplayName("Kombinácia upcoming + recent")
    class Combined {

        @Test
        @DisplayName("upcoming + recent sa načítajú súčasne správne")
        void combined_bothLoaded() {
            LocalDateTime now = LocalDateTime.now();
            addRecurringRule(mainAccount.getId(), 99.0, now.plusDays(4));
            addIncome(mainAccount.getId(), 300.0, now.minusDays(5));

            ActivitiesData data = overviewService.loadActivities(mainAccount.getId());

            assertEquals(1, data.getUpcomingPayments().size());
            assertEquals(1, data.getRecentTransactions().size());
        }

        @Test
        @DisplayName("správny accountId — upcoming aj recent musia byť z rovnakého účtu")
        void isolation_separateAccounts() {
            LocalDateTime now = LocalDateTime.now();
            // Transakcie a rules pre emergencyAccount
            addRecurringRule(emergencyAccount.getId(), 50.0, now.plusDays(2));
            addIncome(emergencyAccount.getId(), 100.0, now.minusDays(1));

            // Transakcie pre mainAccount
            addIncome(mainAccount.getId(), 200.0, now.minusDays(3));

            ActivitiesData dataForMain = overviewService.loadActivities(mainAccount.getId());

            // Len transakcie mainAccount
            assertTrue(dataForMain.getUpcomingPayments().isEmpty());
            assertEquals(1, dataForMain.getRecentTransactions().size());
            assertEquals(200.0, dataForMain.getRecentTransactions().get(0).getAmount(), 0.001);
        }
    }

    // ======================== EDGE CASES ========================

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("Transakcia presne dnes je zahrnutá v recentTransactions")
        void transactionToday_includedInRecent() {
            addExpense(mainAccount.getId(), 55.0, LocalDateTime.now());
            ActivitiesData data = overviewService.loadActivities(mainAccount.getId());
            assertFalse(data.getRecentTransactions().isEmpty());
        }

        @Test
        @DisplayName("Platba s nextDueDate presne zajtra je zahrnutá v upcoming")
        void paymentTomorrow_includedInUpcoming() {
            addRecurringRule(mainAccount.getId(), 100.0, LocalDateTime.now().plusDays(1));
            ActivitiesData data = overviewService.loadActivities(mainAccount.getId());
            assertFalse(data.getUpcomingPayments().isEmpty());
        }

        @Test
        @DisplayName("Recent transactions sú zoradené od najnovšej po najstaršiu")
        void recentTransactions_orderedNewestFirst() {
            LocalDateTime now = LocalDateTime.now();
            addExpense(mainAccount.getId(), 10.0, now.minusDays(5));
            addExpense(mainAccount.getId(), 20.0, now.minusDays(2));
            addExpense(mainAccount.getId(), 30.0, now.minusDays(1));

            ActivitiesData data = overviewService.loadActivities(mainAccount.getId());
            List<Transaction> recent = data.getRecentTransactions();

            assertTrue(recent.get(0).getCompleteDate()
                    .isAfter(recent.get(recent.size() - 1).getCompleteDate()));
        }

        @Test
        @DisplayName("Upcoming payments sú zoradené podľa nextDueDate vzostupne")
        void upcomingPayments_orderedByDueDate() {
            LocalDateTime now = LocalDateTime.now();
            addRecurringRule(mainAccount.getId(), 50.0, now.plusDays(10));
            addRecurringRule(mainAccount.getId(), 30.0, now.plusDays(3));
            addRecurringRule(mainAccount.getId(), 80.0, now.plusDays(7));

            ActivitiesData data = overviewService.loadActivities(mainAccount.getId());
            List<RecurringRule> upcoming = data.getUpcomingPayments();

            assertTrue(upcoming.get(0).getNextDueDate()
                    .isBefore(upcoming.get(upcoming.size() - 1).getNextDueDate()));
        }
    }
}

