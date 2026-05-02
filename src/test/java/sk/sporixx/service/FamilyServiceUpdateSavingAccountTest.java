package sk.sporixx.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import sk.sporixx.model.Account;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy pre FamilyService.updateChildSavingAccount().
 */
@DisplayName("FamilyService – updateChildSavingAccount")
class FamilyServiceUpdateSavingAccountTest extends FamilyServiceTestSupport {

    private static final LocalDate FUTURE_DATE = LocalDate.now().plusMonths(6);

    // ─── Happy path ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Happy path")
    class HappyPath {

        @Test
        @DisplayName("platná aktualizácia saving accountu dieťaťa sa uloží")
        void update_valid_succeeds() {
            Account savingAcc = giveSavingAccount(child.getId(), 0.0);
            accountAccessRepo.grantAccess(manager.getId(), savingAcc.getId(),
                    sk.sporixx.model.Role.USER.getAccessLevel());
            giveSavingGoal(savingAcc.getId(), 500.0);

            assertDoesNotThrow(() ->
                    familyService.updateChildSavingAccount(
                            savingAcc.getId(), "Na dovolenku", 300.0, FUTURE_DATE));
        }

        @Test
        @DisplayName("popis saving accountu sa aktualizuje v repe")
        void update_valid_descriptionPersisted() {
            Account savingAcc = giveSavingAccount(child.getId(), 0.0);
            accountAccessRepo.grantAccess(manager.getId(), savingAcc.getId(),
                    sk.sporixx.model.Role.USER.getAccessLevel());
            giveSavingGoal(savingAcc.getId(), 500.0);

            familyService.updateChildSavingAccount(savingAcc.getId(), "Nový popis", 300.0, FUTURE_DATE);

            Account updated = accountRepo.findById(savingAcc.getId()).get();
            assertEquals("Nový popis", updated.getDescription());
        }

        @Test
        @DisplayName("targetAmount saving goalu sa aktualizuje")
        void update_valid_targetAmountUpdated() {
            Account savingAcc = giveSavingAccount(child.getId(), 0.0);
            accountAccessRepo.grantAccess(manager.getId(), savingAcc.getId(),
                    sk.sporixx.model.Role.USER.getAccessLevel());
            var goal = giveSavingGoal(savingAcc.getId(), 500.0);

            familyService.updateChildSavingAccount(savingAcc.getId(), "Popis", 999.0, FUTURE_DATE);

            assertEquals(999.0, savingGoalRepo.findById(goal.getId()).get().getTargetAmount());
        }
    }

    // ─── Prístupové chyby ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("Prístup a existencia")
    class AccessErrors {

        @Test
        @DisplayName("účet bez prístupu managera hodí FamilyException")
        void update_noAccess_throws() {
            Account savingAcc = giveSavingAccount(stranger.getId(), 0.0);
            // manager nemá prístup k stranger's accountu

            FamilyException ex = assertThrows(FamilyException.class,
                    () -> familyService.updateChildSavingAccount(
                            savingAcc.getId(), "Popis", 300.0, FUTURE_DATE));
            assertEquals("family.error.no_access", ex.getMessageKey());
        }

        @Test
        @DisplayName("vlastný účet managera sa nepovažuje za child saving account")
        void update_ownAccount_throws() {
            Account ownSaving = giveSavingAccount(manager.getId(), 0.0);
            // manager má prístup k vlastnému účtu cez self-ref
            accountAccessRepo.grantAccess(manager.getId(), ownSaving.getId(), 1);

            FamilyException ex = assertThrows(FamilyException.class,
                    () -> familyService.updateChildSavingAccount(
                            ownSaving.getId(), "Popis", 300.0, FUTURE_DATE));
            assertEquals("family.error.no_access", ex.getMessageKey());
        }

        @Test
        @DisplayName("nie je saving account (je main) → not_saving_account")
        void update_notSavingAccount_throws() {
            // child's main account – manager má k nemu prístup (nastavený v baseSetUp)
            // kód prejde access check a potom zistí že nie je saving account
            FamilyException ex = assertThrows(FamilyException.class,
                    () -> familyService.updateChildSavingAccount(
                            childMainAccount.getId(), "Popis", 300.0, FUTURE_DATE));
            assertEquals("family.error.not_saving_account", ex.getMessageKey());
        }
    }

    // ─── Validácia vstupov ────────────────────────────────────────────────────

    @Nested
    @DisplayName("Validácia vstupov")
    class Validation {

        private Account savingAcc;

        @org.junit.jupiter.api.BeforeEach
        void setUp() {
            savingAcc = giveSavingAccount(child.getId(), 0.0);
            accountAccessRepo.grantAccess(manager.getId(), savingAcc.getId(),
                    sk.sporixx.model.Role.USER.getAccessLevel());
            giveSavingGoal(savingAcc.getId(), 500.0);
        }

        @Test
        @DisplayName("prázdny popis hodí FamilyException")
        void update_blankDescription_throws() {
            FamilyException ex = assertThrows(FamilyException.class,
                    () -> familyService.updateChildSavingAccount(
                            savingAcc.getId(), "", 300.0, FUTURE_DATE));
            assertEquals("family.error.description_required", ex.getMessageKey());
        }

        @Test
        @DisplayName("targetAmount <= 0 hodí FamilyException")
        void update_invalidAmount_throws() {
            FamilyException ex = assertThrows(FamilyException.class,
                    () -> familyService.updateChildSavingAccount(
                            savingAcc.getId(), "Popis", 0.0, FUTURE_DATE));
            assertEquals("family.error.invalid_amount", ex.getMessageKey());
        }

        @Test
        @DisplayName("dátum v minulosti hodí FamilyException")
        void update_pastDate_throws() {
            FamilyException ex = assertThrows(FamilyException.class,
                    () -> familyService.updateChildSavingAccount(
                            savingAcc.getId(), "Popis", 300.0, LocalDate.now().minusDays(1)));
            assertEquals("family.error.invalid_date", ex.getMessageKey());
        }

        @Test
        @DisplayName("null dátum hodí FamilyException")
        void update_nullDate_throws() {
            FamilyException ex = assertThrows(FamilyException.class,
                    () -> familyService.updateChildSavingAccount(
                            savingAcc.getId(), "Popis", 300.0, null));
            assertEquals("family.error.invalid_date", ex.getMessageKey());
        }

        @Test
        @DisplayName("targetAmount menší ako currentBalance hodí FamilyException")
        void update_targetBelowCurrentBalance_throws() {
            // currentBalance = 0.0 z giveSavingAccount, zmeniť na vyššiu
            Account richSaving = giveSavingAccount(child.getId(), 200.0);
            accountAccessRepo.grantAccess(manager.getId(), richSaving.getId(),
                    sk.sporixx.model.Role.USER.getAccessLevel());
            giveSavingGoal(richSaving.getId(), 500.0);

            FamilyException ex = assertThrows(FamilyException.class,
                    () -> familyService.updateChildSavingAccount(
                            richSaving.getId(), "Popis", 100.0, FUTURE_DATE));
            assertEquals("family.error.target_below_current", ex.getMessageKey());
        }
    }
}


