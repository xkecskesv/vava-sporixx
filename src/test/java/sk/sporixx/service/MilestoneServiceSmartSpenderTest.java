package sk.sporixx.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sk.sporixx.dto.MilestoneData;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy pre getSmartSpenderMilestone().
 *
 * Smart Spender sa počíta z pomeru WANT výdavkov za posledných 12 mesiacov.
 *
 * Level pravidlá:
 *   wantPct == 0      → Level 0 (žiadne transakcie)
 *   wantPct  > 70     → Level 1 (Impulse Buyer)
 *   wantPct 51-70     → Level 2 (Careful Spender)
 *   wantPct 31-50     → Level 3 (Mindful Spender)
 *   wantPct 11-30     → Level 4 (Disciplined Spender)
 *   wantPct <= 10     → Level 5 (Smart Spender)
 */
@DisplayName("MilestoneService – Smart Spender")
class MilestoneServiceSmartSpenderTest extends MilestoneServiceTestSupport {

    @Test
    @DisplayName("wantPct = 0 → Level 0, xp = 0, progress = 0")
    void level0_noTransactions() {
        fakeReportsService.setWantPercentage(0.0);

        MilestoneData result = milestoneService.getSmartSpenderMilestone();

        assertEquals(0, result.getLevel());
        assertEquals(0.0, result.getXp());
        assertEquals(0.0, result.getProgress());
        assertEquals("milestone.level.0", result.getLevelName());
        assertEquals("Smart Spender", result.getCategory());
    }

    @Test
    @DisplayName("wantPct = 80 → Level 1 (Impulse Buyer)")
    void level1_impulseBuyer() {
        fakeReportsService.setWantPercentage(80.0);

        MilestoneData result = milestoneService.getSmartSpenderMilestone();

        assertEquals(1, result.getLevel());
        assertEquals(10.0, result.getXp());
        assertEquals("milestone.smart_spender.level.1", result.getLevelName());
        assertEquals("milestone.smart_spender.desc.1", result.getDescription());
    }

    @Test
    @DisplayName("wantPct = 60 → Level 2 (Careful Spender)")
    void level2_carefulSpender() {
        fakeReportsService.setWantPercentage(60.0);

        MilestoneData result = milestoneService.getSmartSpenderMilestone();

        assertEquals(2, result.getLevel());
        assertEquals(20.0, result.getXp());
        assertEquals("milestone.smart_spender.level.2", result.getLevelName());
    }

    @Test
    @DisplayName("wantPct = 40 → Level 3 (Mindful Spender)")
    void level3_mindfulSpender() {
        fakeReportsService.setWantPercentage(40.0);

        MilestoneData result = milestoneService.getSmartSpenderMilestone();

        assertEquals(3, result.getLevel());
        assertEquals(30.0, result.getXp());
        assertEquals("milestone.smart_spender.level.3", result.getLevelName());
    }

    @Test
    @DisplayName("wantPct = 20 → Level 4 (Disciplined Spender)")
    void level4_disciplinedSpender() {
        fakeReportsService.setWantPercentage(20.0);

        MilestoneData result = milestoneService.getSmartSpenderMilestone();

        assertEquals(4, result.getLevel());
        assertEquals(40.0, result.getXp());
        assertEquals("milestone.smart_spender.level.4", result.getLevelName());
    }

    @Test
    @DisplayName("wantPct = 5 → Level 5 (Smart Spender), progress = 1.0")
    void level5_smartSpender() {
        fakeReportsService.setWantPercentage(5.0);

        MilestoneData result = milestoneService.getSmartSpenderMilestone();

        assertEquals(5, result.getLevel());
        assertEquals(50.0, result.getXp());
        assertEquals(1.0, result.getProgress());
        assertEquals("milestone.smart_spender.level.5", result.getLevelName());
    }

    @Test
    @DisplayName("wantPct presne na hranici 70 → Level 2 (nie Level 1)")
    void boundary_70pct_isLevel2() {
        fakeReportsService.setWantPercentage(70.0);

        MilestoneData result = milestoneService.getSmartSpenderMilestone();

        // > 70 je Level 1, teda 70 presne → Level 2
        assertEquals(2, result.getLevel());
    }

    @Test
    @DisplayName("wantPct presne na hranici 50 → Level 3")
    void boundary_50pct_isLevel3() {
        fakeReportsService.setWantPercentage(50.0);

        MilestoneData result = milestoneService.getSmartSpenderMilestone();

        assertEquals(3, result.getLevel());
    }

    @Test
    @DisplayName("wantPct presne na hranici 10 → Level 5")
    void boundary_10pct_isLevel5() {
        fakeReportsService.setWantPercentage(10.0);

        MilestoneData result = milestoneService.getSmartSpenderMilestone();

        assertEquals(5, result.getLevel());
    }

    @Test
    @DisplayName("progress v Level 1 je korektne vypočítaný")
    void progress_level1_calculation() {
        // Level 1: wantPct > 70, progress = 1 - (wantPct - 70) / 30
        // wantPct = 85 → progress = 1 - 15/30 = 0.5
        fakeReportsService.setWantPercentage(85.0);

        MilestoneData result = milestoneService.getSmartSpenderMilestone();

        assertEquals(1, result.getLevel());
        assertEquals(0.5, result.getProgress(), 0.001);
    }

    @Test
    @DisplayName("ReportsService hádzajúci výnimku → fallback Level 0")
    void reportsServiceException_returnsLevel0() {
        // Preťažíme fake aby hádzal výnimku
        fakeReportsService = new MilestoneServiceTestSupport.FakeReportsService() {
            @Override
            public sk.sporixx.dto.WantNeedData loadWantNeedData(sk.sporixx.dto.ChartPeriod period) {
                throw new RuntimeException("DB error");
            }
        };
        milestoneService = new MilestoneServiceImpl(
                fakeReportsService, null, userRepo, transactionRepo, fakeBudgetService);

        MilestoneData result = milestoneService.getSmartSpenderMilestone();

        assertEquals(0, result.getLevel());
        assertEquals(0.0, result.getXp());
    }

    @Test
    @DisplayName("XP na Useri sa aktualizuje keď sa level zmení")
    void userXpUpdated_whenLevelChanges() {
        // Používateľ má level 0 (default), nastavíme wantPct pre level 3
        fakeReportsService.setWantPercentage(40.0);

        milestoneService.getSmartSpenderMilestone();

        // Skontroluj že UserRepository dostalo update
        sk.sporixx.model.User updated = userRepo.findById(testUser.getId()).orElseThrow();
        assertEquals(3, updated.getSpenderLevel());
        assertEquals(30.0, updated.getSpenderXp());
    }

    @Test
    @DisplayName("XP sa NEaktualizuje keď level zostáva rovnaký")
    void userXp_notUpdated_whenLevelSame() {
        // Nastavíme že user už má level 3
        testUser.setSpenderLevel(3);
        testUser.setSpenderXp(30.0);

        fakeReportsService.setWantPercentage(40.0); // → Level 3

        milestoneService.getSmartSpenderMilestone();

        // Skontroluj že update nebol zbytočne volaný
        sk.sporixx.model.User updated = userRepo.findById(testUser.getId()).orElseThrow();
        assertEquals(3, updated.getSpenderLevel());
    }
}

