package sk.sporixx.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import sk.sporixx.model.Account;
import sk.sporixx.model.Category;
import sk.sporixx.model.RecurringRule;
import sk.sporixx.model.Transaction;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance testy pre Management modul.
 *
 * Overujeme, že kľúčové operácie zvládnu veľké objemy dát v prijateľnom čase.
 * Každý test má definovaný časový limit pomocou @Timeout.
 *
 * Testované scenáre:
 *   - CategoryService: načítanie / pridanie / vyhľadávanie medzi 1 000 kategóriami
 *   - RecurringRuleService: pridanie / načítanie 500 pravidiel
 *   - AccountService: vytvorenie 200 saving a private účtov
 */
@DisplayName("Management – Performance testy")
class ManagementPerformanceTest extends ManagementServiceTestSupport {

    private static final LocalDate FUTURE = LocalDate.now().plusYears(1);

    // ======================== CATEGORY SERVICE ========================

    @Nested
    @DisplayName("CategoryService – výkon")
    class CategoryPerformance {

        @Test
        @Timeout(value = 1, unit = TimeUnit.SECONDS)
        @DisplayName("Načítanie 1 000 kategórií prebehne do 1 s")
        void load1000Categories_withinTimeLimit() {
            // Naplníme repozitár: 500 systémových + 500 vlastných
            for (int i = 1; i <= 500; i++) {
                addSystemCategory("SysCategory-" + i);
            }
            for (int i = 1; i <= 500; i++) {
                addUserCategory("UserCategory-" + i);
            }

            long start = System.nanoTime();
            List<Category> result = categoryService.getCategories();
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;

            assertEquals(1000, result.size());
            System.out.printf("[PERF] getCategories (1 000 záznamov): %d ms%n", elapsedMs);
        }

        @Test
        @Timeout(value = 2, unit = TimeUnit.SECONDS)
        @DisplayName("Pridanie 500 kategórií prebehne do 2 s")
        void add500Categories_withinTimeLimit() {
            long start = System.nanoTime();
            for (int i = 1; i <= 500; i++) {
                categoryService.addCategory("PerfCat-" + i);
            }
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;

            List<Category> result = categoryService.getCategories();
            assertEquals(500, result.size());
            System.out.printf("[PERF] addCategory x500: %d ms%n", elapsedMs);
        }

        @Test
        @Timeout(value = 1, unit = TimeUnit.SECONDS)
        @DisplayName("Detekcia duplicity medzi 1 000 kategóriami prebehne do 1 s")
        void duplicateCheckIn1000Categories_withinTimeLimit() {
            for (int i = 1; i <= 999; i++) {
                addUserCategory("Cat-" + i);
            }

            long start = System.nanoTime();
            assertThrows(CategoryException.class,
                    () -> categoryService.addCategory("Cat-1")); // duplicita
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;

            System.out.printf("[PERF] duplicateCheck (1 000 záznamov): %d ms%n", elapsedMs);
        }

        @Test
        @Timeout(value = 2, unit = TimeUnit.SECONDS)
        @DisplayName("Aktualizácia kategórie medzi 1 000 kategóriami prebehne do 2 s")
        void updateCategoryIn1000_withinTimeLimit() {
            for (int i = 1; i <= 999; i++) {
                addUserCategory("Cat-" + i);
            }
            Category target = addUserCategory("UpdateTarget");

            long start = System.nanoTime();
            categoryService.updateCategory(target.getId(), "UpdatedName");
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;

            System.out.printf("[PERF] updateCategory (1 000 záznamov): %d ms%n", elapsedMs);
        }

        @Test
        @Timeout(value = 1, unit = TimeUnit.SECONDS)
        @DisplayName("Načítanie selectable kategórií medzi 1 000 prebehne do 1 s")
        void getSelectableCategories1000_withinTimeLimit() {
            for (int i = 1; i <= 1000; i++) {
                addUserCategory("SelectCat-" + i);
            }

            long start = System.nanoTime();
            List<Category> result = categoryService.getSelectableCategories();
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;

            // getSelectableCategories vylučuje systémové kategórie Saving/SavingExpense/Transfer
            // (podľa ich ID) – počet môže byť mierne nižší ak sa ID zhodujú
            assertFalse(result.isEmpty());
            assertTrue(result.size() >= 990,
                    "Očakávame aspoň 990 selectable kategórií, dostali sme: " + result.size());
            System.out.printf("[PERF] getSelectableCategories (1 000 záznamov): %d ms%n", elapsedMs);
        }
    }

    // ======================== RECURRING RULE SERVICE ========================

    @Nested
    @DisplayName("RecurringRuleService – výkon")
    class RecurringRulePerformance {

        @Test
        @Timeout(value = 3, unit = TimeUnit.SECONDS)
        @DisplayName("Pridanie 300 pravidiel prebehne do 3 s")
        void add300Rules_withinTimeLimit() {
            long start = System.nanoTime();
            for (int i = 1; i <= 300; i++) {
                recurringRuleService.addRecurringRule(
                        mainAccount.getId(), 1, Transaction.CLASSIFICATION_NEED,
                        "Rule-" + i, 10.0 * i, "MONTHLY", 1, FUTURE, null);
            }
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;

            List<RecurringRule> rules = recurringRuleService.getRecurringRules();
            assertEquals(300, rules.size());
            System.out.printf("[PERF] addRecurringRule x300: %d ms%n", elapsedMs);
        }

        @Test
        @Timeout(value = 2, unit = TimeUnit.SECONDS)
        @DisplayName("Načítanie 500 pravidiel prebehne do 2 s")
        void load500Rules_withinTimeLimit() {
            // Naplníme priamo repozitár (obídeme validáciu pre rýchlosť plnenia)
            for (int i = 1; i <= 500; i++) {
                recurringRuleRepo.save(sk.sporixx.model.RecurringRule.builder()
                        .accountId(mainAccount.getId())
                        .categoryId(1)
                        .transactionTypeId(Transaction.TYPE_EXPENSE)
                        .spendingClassificationId(Transaction.CLASSIFICATION_NEED)
                        .description("Rule-" + i)
                        .amount(100.0)
                        .frequencyType("MONTHLY").frequencyInterval(1)
                        .startDate(FUTURE.atStartOfDay())
                        .nextDueDate(FUTURE.atStartOfDay())
                        .isActive(true).generatedCount(0).statusId(1)
                        .createdAt(java.time.LocalDateTime.now())
                        .build());
            }

            long start = System.nanoTime();
            List<RecurringRule> rules = recurringRuleService.getRecurringRules();
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;

            assertEquals(500, rules.size());
            System.out.printf("[PERF] getRecurringRules (500 záznamov): %d ms%n", elapsedMs);
        }

        @Test
        @Timeout(value = 2, unit = TimeUnit.SECONDS)
        @DisplayName("Deaktivácia 200 pravidiel prebehne do 2 s")
        void deactivate200Rules_withinTimeLimit() {
            List<RecurringRule> created = new java.util.ArrayList<>();
            for (int i = 1; i <= 200; i++) {
                created.add(recurringRuleService.addRecurringRule(
                        mainAccount.getId(), 1, Transaction.CLASSIFICATION_NEED,
                        "Rule-" + i, 50.0, "MONTHLY", 1, FUTURE, null));
            }

            long start = System.nanoTime();
            for (RecurringRule rule : created) {
                recurringRuleService.deleteRecurringRule(rule.getId());
            }
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;

            assertTrue(recurringRuleService.getRecurringRules().isEmpty());
            System.out.printf("[PERF] deleteRecurringRule x200: %d ms%n", elapsedMs);
        }
    }

    // ======================== ACCOUNT SERVICE ========================

    @Nested
    @DisplayName("AccountService – výkon")
    class AccountPerformance {

        @Test
        @Timeout(value = 3, unit = TimeUnit.SECONDS)
        @DisplayName("Vytvorenie 100 saving účtov prebehne do 3 s")
        void create100SavingAccounts_withinTimeLimit() {
            long start = System.nanoTime();
            for (int i = 1; i <= 100; i++) {
                accountService.createSavingAccount(
                        "SavingGoal-" + i, 0.0, 1000.0 * i, FUTURE);
            }
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;

            long savingCount = SessionManager.getInstance().getAccounts().stream()
                    .filter(Account::isSavingAccount)
                    .count();
            assertEquals(100, savingCount);
            System.out.printf("[PERF] createSavingAccount x100: %d ms%n", elapsedMs);
        }

        @Test
        @Timeout(value = 3, unit = TimeUnit.SECONDS)
        @DisplayName("Vytvorenie 100 private účtov prebehne do 3 s")
        void create100PrivateAccounts_withinTimeLimit() {
            long start = System.nanoTime();
            for (int i = 1; i <= 100; i++) {
                accountService.createPrivateAccount("PrivateAcc-" + i, 100.0 * i);
            }
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;

            long privateCount = SessionManager.getInstance().getAccounts().stream()
                    .filter(Account::isPrivateAccount)
                    .count();
            assertEquals(100, privateCount);
            System.out.printf("[PERF] createPrivateAccount x100: %d ms%n", elapsedMs);
        }

        @Test
        @Timeout(value = 3, unit = TimeUnit.SECONDS)
        @DisplayName("Vytvorenie a vymazanie 100 saving účtov prebehne do 3 s")
        void createAndDelete100Accounts_withinTimeLimit() {
            List<Account> created = new java.util.ArrayList<>();
            for (int i = 1; i <= 100; i++) {
                accountService.createSavingAccount("Fund-" + i, 0.0, 500.0, FUTURE);
            }
            created.addAll(SessionManager.getInstance().getAccounts().stream()
                    .filter(Account::isSavingAccount).toList());

            long start = System.nanoTime();
            for (Account acc : created) {
                accountService.deleteAccount(acc.getId());
            }
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;

            long activeSaving = SessionManager.getInstance().getAccounts().stream()
                    .filter(Account::isSavingAccount).count();
            assertEquals(0, activeSaving);
            System.out.printf("[PERF] deleteAccount x100: %d ms%n", elapsedMs);
        }

        @Test
        @Timeout(value = 2, unit = TimeUnit.SECONDS)
        @DisplayName("Načítanie saving goal pre 100 účtov prebehne do 2 s")
        void getSavingGoalFor100Accounts_withinTimeLimit() {
            List<Account> accounts = new java.util.ArrayList<>();
            for (int i = 1; i <= 100; i++) {
                accountService.createSavingAccount("Goal-" + i, 0.0, 1000.0, FUTURE);
            }
            accounts.addAll(SessionManager.getInstance().getAccounts().stream()
                    .filter(Account::isSavingAccount).toList());

            long start = System.nanoTime();
            for (Account acc : accounts) {
                assertTrue(accountService.getSavingGoal(acc.getId()).isPresent());
            }
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;

            System.out.printf("[PERF] getSavingGoal x100: %d ms%n", elapsedMs);
        }
    }
}


