package sk.sporixx.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sk.sporixx.dto.BudgetData;
import sk.sporixx.dto.BudgetWarning;
import sk.sporixx.model.Budget;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration / scenario testy pre {@link BudgetService}.
 * <p>
 * Overujú reálne použitie API — viacero volaní za sebou ako keby používateľ
 * skutočne používal aplikáciu:
 *  <ul>
 *      <li>load → setup → load (vidí zmeny)</li>
 *      <li>setup → custom alloc → load (custom prepíše recommended)</li>
 *      <li>setup → setup (update toho istého záznamu)</li>
 *      <li>custom alloc po custom alloc (last-wins)</li>
 *  </ul>
 */
class BudgetServiceScenarioTest extends BudgetServiceTestSupport {

    @Test
    @DisplayName("E2E: nový user → load (defaults) → setup → load (vidí zmeny)")
    void newUser_setupFlow() {
        // 1. Load default
        BudgetData before = budgetService.loadBudgetData();
        assertEquals(0.0, before.getMonthlyIncome(), 0.001);
        assertEquals(0.0, before.getEssentialExpenses(), 0.001);

        // 2. Setup
        BudgetWarning warning = budgetService.saveBudgetSetup(
                2000, 100, 50, 30, 20, 0);
        assertEquals(BudgetWarning.NONE, warning);

        // 3. Load — vidí zmeny
        BudgetData after = budgetService.loadBudgetData();
        assertEquals(2000.0, after.getMonthlyIncome(), 0.001);
        assertEquals(200.0, after.getEssentialExpenses(), 0.001);
        assertEquals(200.0, after.getEmergencyFund(), 0.001);
        assertEquals(600.0, after.getSavings(), 0.001);
        assertEquals(800.0, after.getToInvest(), 0.001);
        assertEquals(200.0, after.getFunMoney(), 0.001);
    }

    @Test
    @DisplayName("E2E: setup → custom alloc → load (vidí custom hodnoty)")
    void setupThenCustomAlloc_customWins() {
        // Setup → recommended alokácia
        budgetService.saveBudgetSetup(1000, 50, 30, 10, 5, 5);
        BudgetData afterSetup = budgetService.loadBudgetData();
        assertEquals(100.0, afterSetup.getEmergencyFund(), 0.001);
        assertEquals(300.0, afterSetup.getSavings(), 0.001);

        // Custom alloc → user prepíše
        budgetService.saveCustomAllocation(150, 50, 500, 200);
        BudgetData afterCustom = budgetService.loadBudgetData();

        assertEquals(150.0, afterCustom.getEssentialExpenses(), 0.001);
        assertEquals(50.0, afterCustom.getEmergencyFund(), 0.001);
        assertEquals(500.0, afterCustom.getSavings(), 0.001);
        assertEquals(200.0, afterCustom.getToInvest(), 0.001);
        assertEquals(100.0, afterCustom.getFunMoney(), 0.001,
                "1000 - 150 - 50 - 500 - 200 = 100");

        // Setup polia ostávajú nezmenené
        assertEquals(50.0, afterCustom.getFood(), 0.001);
        assertEquals(30.0, afterCustom.getRent(), 0.001);
    }

    @Test
    @DisplayName("E2E: setup → setup (rovnaké údaje 2×) — len 1 záznam, žiadny duplicitny insert")
    void doubleSetup_singleRecord() {
        budgetService.saveBudgetSetup(1000, 50, 30, 10, 5, 5);
        budgetService.saveBudgetSetup(1000, 50, 30, 10, 5, 5);

        assertEquals(1, budgetRepo.findAll().size(),
                "rovnaké údaje 2× = stále len 1 záznam");
    }

    @Test
    @DisplayName("E2E: setup → setup s inými hodnotami — update existujúceho")
    void setupAfterSetup_updatesExisting() {
        budgetService.saveBudgetSetup(1000, 50, 30, 10, 5, 5);
        Budget first = budgetRepo.findByUserId(testUser.getId()).orElseThrow();

        budgetService.saveBudgetSetup(2000, 200, 100, 50, 30, 20);
        Budget second = budgetRepo.findByUserId(testUser.getId()).orElseThrow();

        assertEquals(first.getId(), second.getId(),
                "update musí zachovať pôvodné ID");
        assertEquals(2000.0, second.getMonthlyIncome(), 0.001);
        assertEquals(400.0, second.getEssentialExpenses(), 0.001);
    }

    @Test
    @DisplayName("E2E: setup → fallback warning → custom alloc → recover")
    void fallbackThenCustom_recoverFromHighEssential() {
        // Setup s vysokými výdavkami → fallback
        BudgetWarning warning1 = budgetService.saveBudgetSetup(
                1000, 200, 300, 200, 50, 50);
        assertEquals(BudgetWarning.FALLBACK_ALLOCATION_APPLIED, warning1);

        Budget afterSetup = budgetRepo.findByUserId(testUser.getId()).orElseThrow();
        assertEquals(800.0, afterSetup.getEssentialExpenses(), 0.001);
        assertEquals(30.0, afterSetup.getEmergencyFund(), 0.001);  // fallback 15% z 200

        // Custom alloc — user manuálne nastaví rozumnejšie hodnoty
        BudgetWarning warning2 = budgetService.saveCustomAllocation(
                800, 100, 50, 25);
        // 800 == actualEssential → bez warningu
        assertEquals(BudgetWarning.NONE, warning2);

        Budget afterCustom = budgetRepo.findByUserId(testUser.getId()).orElseThrow();
        assertEquals(800.0, afterCustom.getEssentialExpenses(), 0.001);
        assertEquals(100.0, afterCustom.getEmergencyFund(), 0.001);
        assertEquals(25.0, afterCustom.getFunMoney(), 0.001,
                "1000 - 800 - 100 - 50 - 25 = 25");
    }

    @Test
    @DisplayName("E2E: cyklus setup → custom → setup → custom (each saves correctly)")
    void multipleCycles_lastStateWins() {
        budgetService.saveBudgetSetup(1000, 50, 30, 10, 5, 5);
        budgetService.saveCustomAllocation(150, 100, 200, 200);
        budgetService.saveBudgetSetup(2000, 100, 50, 30, 20, 0);
        budgetService.saveCustomAllocation(500, 200, 300, 100);

        assertEquals(1, budgetRepo.findAll().size(),
                "stále 1 záznam pre toho istého usera");

        BudgetData data = budgetService.loadBudgetData();
        assertEquals(2000.0, data.getMonthlyIncome(), 0.001);
        assertEquals(500.0, data.getEssentialExpenses(), 0.001);
        assertEquals(200.0, data.getEmergencyFund(), 0.001);
        assertEquals(300.0, data.getSavings(), 0.001);
        assertEquals(100.0, data.getToInvest(), 0.001);
        assertEquals(900.0, data.getFunMoney(), 0.001,
                "2000 - 500 - 200 - 300 - 100 = 900");
    }

    @Test
    @DisplayName("E2E: failed setup (validation error) NESMIE meniť stav repo")
    void failedSetup_doesNotModifyState() {
        // 1. Validný setup
        budgetService.saveBudgetSetup(1000, 50, 30, 10, 5, 5);
        Budget original = budgetRepo.findByUserId(testUser.getId()).orElseThrow();

        // 2. Pokus o setup so záporným income → exception
        assertThrows(BudgetException.class,
                () -> budgetService.saveBudgetSetup(-100, 0, 0, 0, 0, 0));

        // 3. Stav v repo musí byť nezmenený
        Budget afterFailedAttempt = budgetRepo.findByUserId(testUser.getId()).orElseThrow();
        assertEquals(original.getId(), afterFailedAttempt.getId());
        assertEquals(original.getMonthlyIncome(), afterFailedAttempt.getMonthlyIncome(), 0.001);
        assertEquals(original.getFood(), afterFailedAttempt.getFood(), 0.001);
    }

    @Test
    @DisplayName("E2E: failed custom alloc (záporné) NESMIE meniť stav")
    void failedCustomAlloc_doesNotModifyState() {
        budgetService.saveBudgetSetup(1000, 50, 30, 10, 5, 5);
        budgetService.saveCustomAllocation(150, 100, 200, 200);
        Budget original = budgetRepo.findByUserId(testUser.getId()).orElseThrow();

        // Záporné savings → exception
        assertThrows(BudgetException.class,
                () -> budgetService.saveCustomAllocation(100, 100, -1, 100));

        Budget after = budgetRepo.findByUserId(testUser.getId()).orElseThrow();
        assertEquals(original.getEssentialExpenses(), after.getEssentialExpenses(), 0.001);
        assertEquals(original.getSavings(), after.getSavings(), 0.001);
    }
}
