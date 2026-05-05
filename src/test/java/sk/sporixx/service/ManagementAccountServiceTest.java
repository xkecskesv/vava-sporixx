package sk.sporixx.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import sk.sporixx.model.Account;
import sk.sporixx.model.SavingGoal;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy pre {@link AccountService}.
 *
 * Pokrývame:
 *   - vytvorenie saving účtu (validácia: popis, targetAmount, targetDate, duplicita)
 *   - vytvorenie private účtu (validácia: popis, záporná suma, duplicita)
 *   - vymazanie účtu (ochrana Main/Emergency, neznáme ID)
 *   - aktualizácia popisku účtu
 *   - aktualizácia saving účtu a jeho goal
 *   - načítanie saving goal pre účet
 */
@DisplayName("AccountService – Management")
class ManagementAccountServiceTest extends ManagementServiceTestSupport {

    private static final LocalDate FUTURE_DATE = LocalDate.now().plusYears(1);
    private static final LocalDate PAST_DATE   = LocalDate.now().minusDays(1);

    // ======================== CREATE SAVING ACCOUNT ========================

    @Nested
    @DisplayName("Vytvorenie saving účtu")
    class CreateSavingAccount {

        @Test
        @DisplayName("Platné údaje → saving účet a goal sa vytvoria")
        void validInput_accountAndGoalCreated() {
            accountService.createSavingAccount("Porsche fund", 500.0, 50000.0, FUTURE_DATE);

            Account created = SessionManager.getInstance().getAccounts().stream()
                    .filter(Account::isSavingAccount)
                    .findFirst()
                    .orElseThrow();

            assertEquals("Porsche fund", created.getDescription());
            assertEquals(500.0, created.getCurrentBalance(), 0.001);

            Optional<SavingGoal> goal = accountService.getSavingGoal(created.getId());
            assertTrue(goal.isPresent());
            assertEquals(50000.0, goal.get().getTargetAmount(), 0.001);
        }

        @Test
        @DisplayName("Prázdny popis → AccountException")
        void blankDescription_throwsException() {
            assertThrows(AccountException.class,
                    () -> accountService.createSavingAccount("  ", 0.0, 1000.0, FUTURE_DATE));
        }

        @Test
        @DisplayName("targetAmount ≤ 0 → AccountException")
        void zeroTargetAmount_throwsException() {
            assertThrows(AccountException.class,
                    () -> accountService.createSavingAccount("Goal", 0.0, 0.0, FUTURE_DATE));
        }

        @Test
        @DisplayName("targetAmount ≤ initialAmount → AccountException")
        void targetBelowInitial_throwsException() {
            assertThrows(AccountException.class,
                    () -> accountService.createSavingAccount("Goal", 1000.0, 500.0, FUTURE_DATE));
        }

        @Test
        @DisplayName("targetDate v minulosti → AccountException")
        void pastTargetDate_throwsException() {
            assertThrows(AccountException.class,
                    () -> accountService.createSavingAccount("Goal", 0.0, 1000.0, PAST_DATE));
        }

        @Test
        @DisplayName("Null targetDate → AccountException")
        void nullTargetDate_throwsException() {
            assertThrows(AccountException.class,
                    () -> accountService.createSavingAccount("Goal", 0.0, 1000.0, null));
        }

        @Test
        @DisplayName("Záporný initialAmount → AccountException")
        void negativeInitialAmount_throwsException() {
            assertThrows(AccountException.class,
                    () -> accountService.createSavingAccount("Goal", -100.0, 1000.0, FUTURE_DATE));
        }

        @Test
        @DisplayName("Nulový initialAmount je povolený (začíname od nuly)")
        void zeroInitialAmount_allowed() {
            assertDoesNotThrow(() ->
                    accountService.createSavingAccount("Goal zero", 0.0, 1000.0, FUTURE_DATE));
        }

        @Test
        @DisplayName("Saving účet sa pridá do SessionManager")
        void savingAccount_addedToSession() {
            int before = SessionManager.getInstance().getAccounts().size();
            accountService.createSavingAccount("New Fund", 0.0, 500.0, FUTURE_DATE);
            int after = SessionManager.getInstance().getAccounts().size();
            assertEquals(before + 1, after);
        }
    }

    // ======================== CREATE PRIVATE ACCOUNT ========================

    @Nested
    @DisplayName("Vytvorenie private účtu")
    class CreatePrivateAccount {

        @Test
        @DisplayName("Platné údaje → private účet sa vytvorí")
        void validInput_accountCreated() {
            accountService.createPrivateAccount("Side account", 200.0);

            Account created = SessionManager.getInstance().getAccounts().stream()
                    .filter(a -> a.getAccountTypeId() == Account.PRIVATE_ACCOUNT)
                    .findFirst()
                    .orElseThrow();

            assertEquals("Side account", created.getDescription());
            assertEquals(200.0, created.getCurrentBalance(), 0.001);
        }

        @Test
        @DisplayName("Prázdny popis → AccountException")
        void blankDescription_throwsException() {
            assertThrows(AccountException.class,
                    () -> accountService.createPrivateAccount("", 0.0));
        }

        @Test
        @DisplayName("Záporná suma → AccountException")
        void negativeAmount_throwsException() {
            assertThrows(AccountException.class,
                    () -> accountService.createPrivateAccount("Side", -50.0));
        }

        @Test
        @DisplayName("Duplicitný popis (case insensitive) → AccountException")
        void duplicateDescription_throwsException() {
            accountService.createPrivateAccount("Side account", 0.0);
            assertThrows(AccountException.class,
                    () -> accountService.createPrivateAccount("side account", 0.0));
        }

        @Test
        @DisplayName("Nulový initialAmount je povolený")
        void zeroInitialAmount_allowed() {
            assertDoesNotThrow(() ->
                    accountService.createPrivateAccount("Zero account", 0.0));
        }

        @Test
        @DisplayName("Private účet sa pridá do SessionManager")
        void privateAccount_addedToSession() {
            int before = SessionManager.getInstance().getAccounts().size();
            accountService.createPrivateAccount("My account", 100.0);
            assertEquals(before + 1, SessionManager.getInstance().getAccounts().size());
        }

        @Test
        @DisplayName("Viac private účtov s rôznymi popismi je povolené")
        void multiplePrivateAccounts_allowed() {
            accountService.createPrivateAccount("Account A", 100.0);
            accountService.createPrivateAccount("Account B", 200.0);
            long count = SessionManager.getInstance().getAccounts().stream()
                    .filter(Account::isPrivateAccount).count();
            assertEquals(2, count);
        }
    }

    // ======================== DELETE ACCOUNT ========================

    @Nested
    @DisplayName("Vymazanie účtu")
    class DeleteAccount {

        @Test
        @DisplayName("Saving/Private účet sa dá vymazať")
        void savingAccount_canBeDeleted() {
            accountService.createSavingAccount("Fund", 0.0, 1000.0, FUTURE_DATE);
            Account saving = SessionManager.getInstance().getAccounts().stream()
                    .filter(Account::isSavingAccount)
                    .findFirst()
                    .orElseThrow();

            accountService.deleteAccount(saving.getId());

            boolean stillActive = SessionManager.getInstance().getAccounts().stream()
                    .anyMatch(a -> a.getId() == saving.getId());
            assertFalse(stillActive);
        }

        @Test
        @DisplayName("Main Account sa nedá vymazať → AccountException")
        void mainAccount_cannotBeDeleted() {
            assertThrows(AccountException.class,
                    () -> accountService.deleteAccount(mainAccount.getId()));
        }

        @Test
        @DisplayName("Emergency Fund sa nedá vymazať → AccountException")
        void emergencyFund_cannotBeDeleted() {
            assertThrows(AccountException.class,
                    () -> accountService.deleteAccount(emergencyAccount.getId()));
        }

        @Test
        @DisplayName("Neznáme ID → AccountException")
        void unknownId_throwsException() {
            assertThrows(AccountException.class,
                    () -> accountService.deleteAccount(999));
        }
    }

    // ======================== UPDATE ACCOUNT DESCRIPTION ========================

    @Nested
    @DisplayName("Aktualizácia popisku účtu")
    class UpdateAccountDescription {

        @Test
        @DisplayName("Platný nový popis → popis sa zmení")
        void validDescription_updated() {
            accountService.updateAccountDescription(mainAccount.getId(), "Updated Main");
            Account updated = SessionManager.getInstance().getAccountById(mainAccount.getId());
            assertEquals("Updated Main", updated.getDescription());
        }

        @Test
        @DisplayName("Prázdny popis → AccountException")
        void blankDescription_throwsException() {
            assertThrows(AccountException.class,
                    () -> accountService.updateAccountDescription(mainAccount.getId(), "   "));
        }

        @Test
        @DisplayName("Neznáme ID → AccountException")
        void unknownId_throwsException() {
            assertThrows(AccountException.class,
                    () -> accountService.updateAccountDescription(999, "New name"));
        }
    }

    // ======================== UPDATE SAVING ACCOUNT ========================

    @Nested
    @DisplayName("Aktualizácia saving účtu")
    class UpdateSavingAccount {

        @Test
        @DisplayName("Platná aktualizácia → popis a goal sa zmenia")
        void validUpdate_accountAndGoalUpdated() {
            accountService.createSavingAccount("Old Goal", 0.0, 5000.0, FUTURE_DATE);
            Account saving = SessionManager.getInstance().getAccounts().stream()
                    .filter(Account::isSavingAccount)
                    .findFirst()
                    .orElseThrow();

            accountService.updateSavingAccount(saving.getId(), "New Goal", 10000.0, FUTURE_DATE);

            Account updated = SessionManager.getInstance().getAccountById(saving.getId());
            assertEquals("New Goal", updated.getDescription());

            Optional<SavingGoal> goal = accountService.getSavingGoal(saving.getId());
            assertTrue(goal.isPresent());
            assertEquals(10000.0, goal.get().getTargetAmount(), 0.001);
        }

        @Test
        @DisplayName("Prázdny popis → AccountException")
        void blankDescription_throwsException() {
            accountService.createSavingAccount("Fund", 0.0, 5000.0, FUTURE_DATE);
            Account saving = SessionManager.getInstance().getAccounts().stream()
                    .filter(Account::isSavingAccount).findFirst().orElseThrow();

            assertThrows(AccountException.class,
                    () -> accountService.updateSavingAccount(saving.getId(), "", 5000.0, FUTURE_DATE));
        }

        @Test
        @DisplayName("targetAmount ≤ 0 → AccountException")
        void zeroTargetAmount_throwsException() {
            accountService.createSavingAccount("Fund", 0.0, 5000.0, FUTURE_DATE);
            Account saving = SessionManager.getInstance().getAccounts().stream()
                    .filter(Account::isSavingAccount).findFirst().orElseThrow();

            assertThrows(AccountException.class,
                    () -> accountService.updateSavingAccount(saving.getId(), "Fund", 0.0, FUTURE_DATE));
        }

        @Test
        @DisplayName("targetDate v minulosti → AccountException")
        void pastTargetDate_throwsException() {
            accountService.createSavingAccount("Fund", 0.0, 5000.0, FUTURE_DATE);
            Account saving = SessionManager.getInstance().getAccounts().stream()
                    .filter(Account::isSavingAccount).findFirst().orElseThrow();

            assertThrows(AccountException.class,
                    () -> accountService.updateSavingAccount(saving.getId(), "Fund", 5000.0, PAST_DATE));
        }

        @Test
        @DisplayName("Non-saving účet → AccountException")
        void nonSavingAccount_throwsException() {
            assertThrows(AccountException.class,
                    () -> accountService.updateSavingAccount(mainAccount.getId(), "X", 5000.0, FUTURE_DATE));
        }
    }

    // ======================== GET SAVING GOAL ========================

    @Nested
    @DisplayName("Načítanie saving goal")
    class GetSavingGoal {

        @Test
        @DisplayName("Saving účet má aktívny goal")
        void savingAccount_hasGoal() {
            accountService.createSavingAccount("My Goal", 100.0, 1000.0, FUTURE_DATE);
            Account saving = SessionManager.getInstance().getAccounts().stream()
                    .filter(Account::isSavingAccount).findFirst().orElseThrow();

            Optional<SavingGoal> goal = accountService.getSavingGoal(saving.getId());
            assertTrue(goal.isPresent());
            assertEquals(1000.0, goal.get().getTargetAmount(), 0.001);
        }

        @Test
        @DisplayName("Účet bez goal → Optional.empty()")
        void accountWithoutGoal_returnsEmpty() {
            Optional<SavingGoal> goal = accountService.getSavingGoal(mainAccount.getId());
            assertTrue(goal.isEmpty());
        }
    }
}

