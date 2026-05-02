package sk.sporixx.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sk.sporixx.dto.MilestoneData;
import sk.sporixx.model.Account;
import sk.sporixx.model.User;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy pre getSavingMasterMilestone().
 *
 * Saving Master sa počíta z celkového zostatku na SAVING účtoch.
 *
 * Level pravidlá:
 *   < 1000     → Level 0
 *   >= 1000    → Level 1
 *   >= 5000    → Level 2
 *   >= 10000   → Level 3
 *   >= 50000   → Level 4
 *   >= 100000  → Level 5
 */
@DisplayName("MilestoneService – Saving Master")
class MilestoneServiceSavingMasterTest extends MilestoneServiceTestSupport {

    @Test
    @DisplayName("Saving balance = 0 → Level 0, nextTarget = 1000")
    void level0_noSavings() {
        // Prihlás používateľa len s main accountom (žiadny saving)
        SessionManager.getInstance().setSession(testUser, List.of(mainAccount));

        MilestoneData result = milestoneService.getSavingMasterMilestone();

        assertEquals(0, result.getLevel());
        assertEquals(0.0, result.getXp());
        assertEquals(1_000.0, result.getNextTarget());
        assertEquals("Saving Master", result.getCategory());
    }

    @Test
    @DisplayName("Saving balance = 1000 → Level 1, nextTarget = 5000")
    void level1_1000eur() {
        Account saving = Account.builder()
                .id(10).ownerUserId(testUser.getId())
                .accountTypeId(Account.SAVING_ACCOUNT)
                .currentBalance(1_000.0).isActive(true).build();
        SessionManager.getInstance().setSession(testUser, List.of(mainAccount, saving));

        MilestoneData result = milestoneService.getSavingMasterMilestone();

        assertEquals(1, result.getLevel());
        assertEquals(10.0, result.getXp());
        assertEquals(5_000.0, result.getNextTarget());
        assertEquals("milestone.saving_master.level.1", result.getLevelName());
    }

    @Test
    @DisplayName("Saving balance = 5000 → Level 2, nextTarget = 10000")
    void level2_5000eur() {
        Account saving = Account.builder()
                .id(10).ownerUserId(testUser.getId())
                .accountTypeId(Account.SAVING_ACCOUNT)
                .currentBalance(5_000.0).isActive(true).build();
        SessionManager.getInstance().setSession(testUser, List.of(mainAccount, saving));

        MilestoneData result = milestoneService.getSavingMasterMilestone();

        assertEquals(2, result.getLevel());
        assertEquals(20.0, result.getXp());
        assertEquals(10_000.0, result.getNextTarget());
    }

    @Test
    @DisplayName("Saving balance = 10000 → Level 3, nextTarget = 50000")
    void level3_10000eur() {
        Account saving = Account.builder()
                .id(10).ownerUserId(testUser.getId())
                .accountTypeId(Account.SAVING_ACCOUNT)
                .currentBalance(10_000.0).isActive(true).build();
        SessionManager.getInstance().setSession(testUser, List.of(mainAccount, saving));

        MilestoneData result = milestoneService.getSavingMasterMilestone();

        assertEquals(3, result.getLevel());
        assertEquals(30.0, result.getXp());
        assertEquals(50_000.0, result.getNextTarget());
    }

    @Test
    @DisplayName("Saving balance = 50000 → Level 4, nextTarget = 100000")
    void level4_50000eur() {
        Account saving = Account.builder()
                .id(10).ownerUserId(testUser.getId())
                .accountTypeId(Account.SAVING_ACCOUNT)
                .currentBalance(50_000.0).isActive(true).build();
        SessionManager.getInstance().setSession(testUser, List.of(mainAccount, saving));

        MilestoneData result = milestoneService.getSavingMasterMilestone();

        assertEquals(4, result.getLevel());
        assertEquals(40.0, result.getXp());
        assertEquals(100_000.0, result.getNextTarget());
    }

    @Test
    @DisplayName("Saving balance = 100000 → Level 5, nextTarget = 0, progress = 1.0")
    void level5_100000eur() {
        Account saving = Account.builder()
                .id(10).ownerUserId(testUser.getId())
                .accountTypeId(Account.SAVING_ACCOUNT)
                .currentBalance(100_000.0).isActive(true).build();
        SessionManager.getInstance().setSession(testUser, List.of(mainAccount, saving));

        MilestoneData result = milestoneService.getSavingMasterMilestone();

        assertEquals(5, result.getLevel());
        assertEquals(50.0, result.getXp());
        assertEquals(0.0, result.getNextTarget());
        assertEquals(1.0, result.getProgress());
    }

    @Test
    @DisplayName("Viacero saving účtov – balansy sa sčítajú")
    void multipleSavingAccounts_summed() {
        Account saving1 = Account.builder()
                .id(10).ownerUserId(testUser.getId())
                .accountTypeId(Account.SAVING_ACCOUNT)
                .currentBalance(3_000.0).isActive(true).build();
        Account saving2 = Account.builder()
                .id(11).ownerUserId(testUser.getId())
                .accountTypeId(Account.SAVING_ACCOUNT)
                .currentBalance(3_000.0).isActive(true).build();
        SessionManager.getInstance().setSession(testUser, List.of(mainAccount, saving1, saving2));

        MilestoneData result = milestoneService.getSavingMasterMilestone();

        // 3000 + 3000 = 6000 → Level 2
        assertEquals(2, result.getLevel());
    }

    @Test
    @DisplayName("Main a Emergency account sa NEpočítajú do saving")
    void mainAndEmergencyNotCounted() {
        // Emergency = 999, main = 500 → žiadny saving účet
        Account emergency = Account.builder()
                .id(20).ownerUserId(testUser.getId())
                .accountTypeId(Account.EMERGENCY_FUND)
                .currentBalance(999.0).isActive(true).build();
        Account main = Account.builder()
                .id(21).ownerUserId(testUser.getId())
                .accountTypeId(Account.MAIN_ACCOUNT)
                .currentBalance(500.0).isActive(true).build();
        SessionManager.getInstance().setSession(testUser, List.of(main, emergency));

        MilestoneData result = milestoneService.getSavingMasterMilestone();

        // Žiadny saving účet → Level 0
        assertEquals(0, result.getLevel());
    }

    @Test
    @DisplayName("progress v Level 1 – korektný výpočet")
    void progress_level1() {
        // Level 1: (totalSaved - 1000) / 4000
        // Saved = 3000 → progress = (3000-1000)/4000 = 0.5
        Account saving = Account.builder()
                .id(10).ownerUserId(testUser.getId())
                .accountTypeId(Account.SAVING_ACCOUNT)
                .currentBalance(3_000.0).isActive(true).build();
        SessionManager.getInstance().setSession(testUser, List.of(saving));

        MilestoneData result = milestoneService.getSavingMasterMilestone();

        assertEquals(1, result.getLevel());
        assertEquals(0.5, result.getProgress(), 0.001);
    }

    @Test
    @DisplayName("UserRepository dostane update pri zmene levelu")
    void xpUpdatedInUserRepo() {
        Account saving = Account.builder()
                .id(10).ownerUserId(testUser.getId())
                .accountTypeId(Account.SAVING_ACCOUNT)
                .currentBalance(5_000.0).isActive(true).build();
        SessionManager.getInstance().setSession(testUser, List.of(saving));

        milestoneService.getSavingMasterMilestone();

        User updated = userRepo.findById(testUser.getId()).orElseThrow();
        assertEquals(2, updated.getSavingLevel());
        assertEquals(20.0, updated.getSavingXp());
    }
}

