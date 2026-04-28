package sk.sporixx.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sk.sporixx.dto.BudgetWarning;
import sk.sporixx.model.Budget;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy pre {@link BudgetService#saveBudgetSetup}.
 * <p>
 * Pokrývajú:
 * <ul>
 *     <li>všetky validácie vstupu (income, záporné, prekročenie)</li>
 *     <li>standard allocation (10/30/40/zvyšok = funMoney)</li>
 *     <li>fallback allocation (15/40/30/15) keď essential príliš vysoké</li>
 *     <li>edge case essential = monthlyIncome → všetko 0 + warning</li>
 *     <li>výpočet emergency fund limitov (3× a 6× essential)</li>
 *     <li>insert (prvé volanie) vs update (druhé volanie pre toho istého usera)</li>
 *     <li>uloženie všetkých polí do repo</li>
 * </ul>
 */
class BudgetServiceSaveSetupTest extends BudgetServiceTestSupport {

    // ====================== VALIDÁCIE VSTUPU ======================

    @Test
    @DisplayName("monthlyIncome = 0 hodí invalid_income")
    void validation_zeroIncome_throws() {
        BudgetException ex = assertThrows(BudgetException.class,
                () -> budgetService.saveBudgetSetup(0, 0, 0, 0, 0, 0));
        assertEquals("budget.error.invalid_income", ex.getMessageKey());
    }

    @Test
    @DisplayName("Záporný monthlyIncome hodí invalid_income")
    void validation_negativeIncome_throws() {
        BudgetException ex = assertThrows(BudgetException.class,
                () -> budgetService.saveBudgetSetup(-100, 0, 0, 0, 0, 0));
        assertEquals("budget.error.invalid_income", ex.getMessageKey());
    }

    @Test
    @DisplayName("Záporné food hodí negative_expense")
    void validation_negativeFood_throws() {
        BudgetException ex = assertThrows(BudgetException.class,
                () -> budgetService.saveBudgetSetup(1000, -10, 0, 0, 0, 0));
        assertEquals("budget.error.negative_expense", ex.getMessageKey());
    }

    @Test
    @DisplayName("Záporné rent hodí negative_expense")
    void validation_negativeRent_throws() {
        BudgetException ex = assertThrows(BudgetException.class,
                () -> budgetService.saveBudgetSetup(1000, 0, -5, 0, 0, 0));
        assertEquals("budget.error.negative_expense", ex.getMessageKey());
    }

    @Test
    @DisplayName("Záporné transport hodí negative_expense")
    void validation_negativeTransport_throws() {
        assertThrows(BudgetException.class,
                () -> budgetService.saveBudgetSetup(1000, 0, 0, -1, 0, 0));
    }

    @Test
    @DisplayName("Záporné utilities hodí negative_expense")
    void validation_negativeUtilities_throws() {
        assertThrows(BudgetException.class,
                () -> budgetService.saveBudgetSetup(1000, 0, 0, 0, -1, 0));
    }

    @Test
    @DisplayName("Záporné other hodí negative_expense")
    void validation_negativeOther_throws() {
        assertThrows(BudgetException.class,
                () -> budgetService.saveBudgetSetup(1000, 0, 0, 0, 0, -1));
    }

    @Test
    @DisplayName("Súčet výdavkov > monthlyIncome hodí expenses_exceed_income")
    void validation_essentialExceedsIncome_throws() {
        // 100+100+100+100+700 = 1100 > 1000
        BudgetException ex = assertThrows(BudgetException.class,
                () -> budgetService.saveBudgetSetup(1000, 100, 100, 100, 100, 700));
        assertEquals("budget.error.expenses_exceed_income", ex.getMessageKey());
    }

    @Test
    @DisplayName("essential == monthlyIncome je VALIDNÉ (na hranici)")
    void validation_essentialEqualsIncome_ok() {
        // Hraničný prípad — je to <= takže prejde
        assertDoesNotThrow(() ->
                budgetService.saveBudgetSetup(1000, 200, 300, 200, 200, 100));
    }

    // ====================== STANDARD ALLOCATION ======================

    @Test
    @DisplayName("Standard alokácia: monthlyIncome=1000, essential=100 → 10%/30%/40%/funMoney=zvyšok")
    void standardAllocation_lowEssential_appliesPercentages() {
        // monthlyIncome=1000, essential=100
        // emergency = 10% × 1000 = 100
        // savings = 30% × 1000 = 300
        // toInvest = 40% × 1000 = 400
        // totalAllocated = 100 + 100 + 300 + 400 = 900 ≤ 1000 → standard
        // funMoney = 1000 - 100 - 100 - 300 - 400 = 100
        BudgetWarning warning = budgetService.saveBudgetSetup(
                1000, 50, 30, 10, 5, 5);

        assertEquals(BudgetWarning.NONE, warning,
                "standard alokácia nemá vrátiť warning");

        Optional<Budget> saved = budgetRepo.findByUserId(testUser.getId());
        assertTrue(saved.isPresent());
        Budget b = saved.get();
        assertEquals(100.0, b.getEmergencyFund(), 0.001);
        assertEquals(300.0, b.getSavings(), 0.001);
        assertEquals(400.0, b.getToInvest(), 0.001);
        assertEquals(100.0, b.getFunMoney(), 0.001);
        assertEquals(100.0, b.getEssentialExpenses(), 0.001);
    }

    @Test
    @DisplayName("Standard: súčet všetkých zložiek = monthlyIncome (žiadne 'stratené' eur)")
    void standardAllocation_sumsToMonthlyIncome() {
        budgetService.saveBudgetSetup(2000, 100, 50, 30, 20, 0);

        Budget b = budgetRepo.findByUserId(testUser.getId()).orElseThrow();
        double sum = b.getEssentialExpenses() + b.getEmergencyFund()
                + b.getSavings() + b.getToInvest() + b.getFunMoney();
        assertEquals(2000.0, sum, 0.001,
                "essential + emergency + savings + invest + fun = monthlyIncome");
    }

    // ====================== FALLBACK ALLOCATION ======================

    @Test
    @DisplayName("Fallback alokácia: monthlyIncome=1000, essential=800 → fallback 15/40/30/15 zo zvyšku 200")
    void fallbackAllocation_highEssential_usesRemaining() {
        // monthlyIncome=1000, essential=800
        // standard by bolo: 800 + 100 + 300 + 400 = 1600 > 1000 → fallback
        // remaining = 200
        // emergency = 15% × 200 = 30
        // savings = 40% × 200 = 80
        // toInvest = 30% × 200 = 60
        // funMoney = 200 - 30 - 80 - 60 = 30
        BudgetWarning warning = budgetService.saveBudgetSetup(
                1000, 200, 300, 200, 50, 50);

        assertEquals(BudgetWarning.FALLBACK_ALLOCATION_APPLIED, warning,
                "fallback musí vrátiť warning");

        Budget b = budgetRepo.findByUserId(testUser.getId()).orElseThrow();
        assertEquals(30.0, b.getEmergencyFund(), 0.001);
        assertEquals(80.0, b.getSavings(), 0.001);
        assertEquals(60.0, b.getToInvest(), 0.001);
        assertEquals(30.0, b.getFunMoney(), 0.001);
    }

    @Test
    @DisplayName("Fallback: na hranici — essential = monthlyIncome → remaining=0 → všetko 0 + warning")
    void fallbackAllocation_essentialEqualsIncome_allZero() {
        BudgetWarning warning = budgetService.saveBudgetSetup(
                1000, 200, 300, 200, 200, 100);

        assertEquals(BudgetWarning.FALLBACK_ALLOCATION_APPLIED, warning);

        Budget b = budgetRepo.findByUserId(testUser.getId()).orElseThrow();
        assertEquals(0.0, b.getEmergencyFund(), 0.001);
        assertEquals(0.0, b.getSavings(), 0.001);
        assertEquals(0.0, b.getToInvest(), 0.001);
        assertEquals(0.0, b.getFunMoney(), 0.001);
        assertEquals(1000.0, b.getEssentialExpenses(), 0.001);
    }

    @Test
    @DisplayName("Fallback: súčet alokácií = remaining (žiadne 'stratené' eur)")
    void fallbackAllocation_sumsToRemaining() {
        budgetService.saveBudgetSetup(1000, 200, 300, 200, 50, 50);

        Budget b = budgetRepo.findByUserId(testUser.getId()).orElseThrow();
        double allocated = b.getEmergencyFund() + b.getSavings()
                + b.getToInvest() + b.getFunMoney();
        double remaining = b.getMonthlyIncome() - b.getEssentialExpenses();
        assertEquals(remaining, allocated, 0.001,
                "fallback alokácia musí pokryť celý zvyšok");
    }

    @Test
    @DisplayName("Hranica standard/fallback: presný prípad keď totalAllocated == monthlyIncome")
    void boundary_totalEqualsIncome_isStandardNotFallback() {
        // Štandardné: 80% z monthlyIncome ide do alokácií (10+30+40)
        // Ak essential = 20% z monthlyIncome, presne na hranici
        // monthlyIncome=1000, essential=200 → totalAllocated = 200+100+300+400 = 1000 == monthlyIncome
        // Kód má `if (totalAllocated <= monthlyIncome)` → standard
        BudgetWarning warning = budgetService.saveBudgetSetup(
                1000, 100, 50, 25, 15, 10);

        assertEquals(BudgetWarning.NONE, warning,
                "totalAllocated == monthlyIncome stále patrí pod standard");

        Budget b = budgetRepo.findByUserId(testUser.getId()).orElseThrow();
        // funMoney = 0 v tomto prípade
        assertEquals(0.0, b.getFunMoney(), 0.001);
    }

    // ====================== EMERGENCY FUND LIMITY ======================

    @Test
    @DisplayName("minimalEmergencyFund = 3× essentialTotal")
    void emergencyFundLimits_minimalIs3x() {
        budgetService.saveBudgetSetup(2000, 100, 200, 50, 30, 20);
        // essentialTotal = 400

        Budget b = budgetRepo.findByUserId(testUser.getId()).orElseThrow();
        assertEquals(1200.0, b.getMinimalEmergencyFund(), 0.001,
                "minimal = 3 × 400 = 1200");
    }

    @Test
    @DisplayName("optimalEmergencyFund = 6× essentialTotal")
    void emergencyFundLimits_optimalIs6x() {
        budgetService.saveBudgetSetup(2000, 100, 200, 50, 30, 20);

        Budget b = budgetRepo.findByUserId(testUser.getId()).orElseThrow();
        assertEquals(2400.0, b.getOptimalEmergencyFund(), 0.001,
                "optimal = 6 × 400 = 2400");
    }

    @Test
    @DisplayName("Emergency fund limity sa prepočítajú aj pri update")
    void emergencyFundLimits_recalculatedOnUpdate() {
        // Prvé volanie
        budgetService.saveBudgetSetup(1000, 50, 50, 25, 15, 10);
        Budget first = budgetRepo.findByUserId(testUser.getId()).orElseThrow();
        double firstMin = first.getMinimalEmergencyFund();

        // Druhé volanie s vyššími výdavkami
        budgetService.saveBudgetSetup(2000, 100, 200, 50, 30, 20);
        Budget second = budgetRepo.findByUserId(testUser.getId()).orElseThrow();

        assertNotEquals(firstMin, second.getMinimalEmergencyFund(),
                "limity sa musia prepočítať pri update");
        assertEquals(1200.0, second.getMinimalEmergencyFund(), 0.001);
    }

    // ====================== INSERT vs UPDATE ======================

    @Test
    @DisplayName("Prvé volanie pre nového usera vytvorí NOVÝ záznam (insert)")
    void firstCall_createsNewRecord() {
        assertEquals(0, budgetRepo.findAll().size());

        budgetService.saveBudgetSetup(1000, 50, 30, 10, 5, 5);

        assertEquals(1, budgetRepo.findAll().size());
        Budget b = budgetRepo.findAll().get(0);
        assertEquals(testUser.getId(), b.getUserId());
        assertTrue(b.isActive());
        assertNotNull(b.getCreatedAt());
        assertTrue(b.getId() > 0, "id musí byť priradené");
    }

    @Test
    @DisplayName("Druhé volanie aktualizuje existujúci záznam (update, nie ďalší insert)")
    void secondCall_updatesExisting() {
        budgetService.saveBudgetSetup(1000, 50, 30, 10, 5, 5);
        int firstSize = budgetRepo.findAll().size();
        int firstId = budgetRepo.findByUserId(testUser.getId()).orElseThrow().getId();

        // Druhé volanie s novými hodnotami
        budgetService.saveBudgetSetup(2000, 100, 200, 50, 30, 20);

        assertEquals(firstSize, budgetRepo.findAll().size(),
                "počet záznamov sa nemá zvýšiť");

        Budget updated = budgetRepo.findByUserId(testUser.getId()).orElseThrow();
        assertEquals(firstId, updated.getId(), "ID musí ostať rovnaké (update, nie nový záznam)");
        assertEquals(2000.0, updated.getMonthlyIncome(), 0.001);
        assertEquals(100.0, updated.getFood(), 0.001);
        assertEquals(200.0, updated.getRent(), 0.001);
    }

    @Test
    @DisplayName("Update prepíše VŠETKY polia (vrátane alokácií, nie iba income)")
    void update_overwritesAllFields() {
        budgetService.saveBudgetSetup(1000, 50, 30, 10, 5, 5);
        // → standard allocation: emergency=100, savings=300, toInvest=400, funMoney=100

        budgetService.saveBudgetSetup(2000, 200, 300, 100, 50, 50);
        // → standard allocation: emergency=200, savings=600, toInvest=800, funMoney=300 (zo zvyšku)

        Budget b = budgetRepo.findByUserId(testUser.getId()).orElseThrow();
        assertEquals(700.0, b.getEssentialExpenses(), 0.001);
        assertEquals(195.0, b.getEmergencyFund(), 0.001);
        assertEquals(520.0, b.getSavings(), 0.001);
        assertEquals(390.0, b.getToInvest(), 0.001);
    }

    // ====================== ZACHOVÁVANÉ POLIA ======================

    @Test
    @DisplayName("Po save sú v repo všetky polia (food, rent, transport, utilities, other)")
    void allInputFieldsArePersisted() {
        budgetService.saveBudgetSetup(2000, 50, 100, 30, 25, 15);

        Budget b = budgetRepo.findByUserId(testUser.getId()).orElseThrow();
        assertEquals(2000.0, b.getMonthlyIncome(), 0.001);
        assertEquals(50.0, b.getFood(), 0.001);
        assertEquals(100.0, b.getRent(), 0.001);
        assertEquals(30.0, b.getTransport(), 0.001);
        assertEquals(25.0, b.getUtilities(), 0.001);
        assertEquals(15.0, b.getOther(), 0.001);
        assertEquals(220.0, b.getEssentialExpenses(), 0.001,
                "essentialExpenses = 50+100+30+25+15");
    }

    @Test
    @DisplayName("Save s 0 výdavkami (všetky kategórie 0) je validné")
    void saveSetup_allZeroExpenses_ok() {
        BudgetWarning warning = budgetService.saveBudgetSetup(
                1000, 0, 0, 0, 0, 0);

        assertEquals(BudgetWarning.NONE, warning);
        Budget b = budgetRepo.findByUserId(testUser.getId()).orElseThrow();
        assertEquals(0.0, b.getEssentialExpenses(), 0.001);
        // Standard alokácia: 100+300+400+200=1000
        assertEquals(100.0, b.getEmergencyFund(), 0.001);
        assertEquals(300.0, b.getSavings(), 0.001);
        assertEquals(400.0, b.getToInvest(), 0.001);
        assertEquals(200.0, b.getFunMoney(), 0.001);
    }
}
