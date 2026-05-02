package sk.sporixx.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy pre getFinancialTitleKey(int totalXp).
 *
 * XP→title mapovanie:
 *   0–39   → milestone.financial_level.started
 *   40–79  → milestone.financial_level.aware
 *   80–119 → milestone.financial_level.skilled
 *   120–159→ milestone.financial_level.expert
 *   160–200→ milestone.financial_level.pro
 */
@DisplayName("MilestoneService – Financial Title Key")
class MilestoneServiceFinancialTitleTest extends MilestoneServiceTestSupport {

    @ParameterizedTest(name = "XP={0} → {1}")
    @CsvSource({
            "0,   milestone.financial_level.started",
            "39,  milestone.financial_level.started",
            "40,  milestone.financial_level.aware",
            "79,  milestone.financial_level.aware",
            "80,  milestone.financial_level.skilled",
            "119, milestone.financial_level.skilled",
            "120, milestone.financial_level.expert",
            "159, milestone.financial_level.expert",
            "160, milestone.financial_level.pro",
            "200, milestone.financial_level.pro"
    })
    void financialTitle_boundaries(int totalXp, String expectedKey) {
        String result = milestoneService.getFinancialTitleKey(totalXp);
        assertEquals(expectedKey, result);
    }

    @Test
    @DisplayName("XP = 0 → started (prázdny profil)")
    void xp0_started() {
        assertEquals("milestone.financial_level.started",
                milestoneService.getFinancialTitleKey(0));
    }

    @Test
    @DisplayName("XP = 200 → pro (max level)")
    void xp200_pro() {
        assertEquals("milestone.financial_level.pro",
                milestoneService.getFinancialTitleKey(200));
    }

    @Test
    @DisplayName("Priamo na hranici 40 → aware (nie started)")
    void xp40_aware() {
        assertEquals("milestone.financial_level.aware",
                milestoneService.getFinancialTitleKey(40));
    }

    @Test
    @DisplayName("Priamo na hranici 160 → pro (nie expert)")
    void xp160_pro() {
        assertEquals("milestone.financial_level.pro",
                milestoneService.getFinancialTitleKey(160));
    }

    @Test
    @DisplayName("XP nad 200 → pro (nie je obmedzené)")
    void xpOver200_pro() {
        assertEquals("milestone.financial_level.pro",
                milestoneService.getFinancialTitleKey(999));
    }
}

