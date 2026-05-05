package sk.sporixx.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sk.sporixx.dto.BudgetWarning;
import sk.sporixx.model.Budget;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy pre {@link BudgetService#saveCustomAllocation}.
 * <p>
 * Pokrývajú:
 * <ul>
 *     <li>setup_required ak budget ešte neexistuje</li>
 *     <li>všetky validácie (záporné, prekročenie monthlyIncome)</li>
 *     <li>výpočet funMoney = monthlyIncome - (essential + emergency + savings + toInvest)</li>
 *     <li>warning ESSENTIAL_BELOW_ACTUAL keď essential &lt; sum(food+rent+...)</li>
 *     <li>perzistencia zmeny do repo</li>
 * </ul>
 */
class BudgetServiceCustomAllocationTest extends BudgetServiceTestSupport {

    /** Helper: pripraví budget pomocou setup, aby sme mali na čom volať custom. */
    private void setupBaseBudget() {
        // monthlyIncome=1000, essential=100 → standard alloc: 100/300/400/100
        budgetService.saveBudgetSetup(1000, 50, 30, 10, 5, 5);
    }

    // ====================== PRECONDITION ======================

    @Test
    @DisplayName("Custom alloc bez existujúceho budgetu hodí setup_required")
    void noBudget_throwsSetupRequired() {
        BudgetException ex = assertThrows(BudgetException.class,
                () -> budgetService.saveCustomAllocation(100, 100, 100, 100));
        assertEquals("budget.error.setup_required", ex.getMessageKey());
    }

    // ====================== VALIDÁCIE ======================

    @Test
    @DisplayName("Záporné essential hodí negative_expense")
    void negativeEssential_throws() {
        setupBaseBudget();

        BudgetException ex = assertThrows(BudgetException.class,
                () -> budgetService.saveCustomAllocation(-1, 100, 100, 100));
        assertEquals("budget.error.negative_expense", ex.getMessageKey());
    }

    @Test
    @DisplayName("Záporné emergencyFund hodí negative_expense")
    void negativeEmergency_throws() {
        setupBaseBudget();

        assertThrows(BudgetException.class,
                () -> budgetService.saveCustomAllocation(100, -1, 100, 100));
    }

    @Test
    @DisplayName("Záporné savings hodí negative_expense")
    void negativeSavings_throws() {
        setupBaseBudget();

        assertThrows(BudgetException.class,
                () -> budgetService.saveCustomAllocation(100, 100, -1, 100));
    }

    @Test
    @DisplayName("Záporné toInvest hodí negative_expense")
    void negativeToInvest_throws() {
        setupBaseBudget();

        assertThrows(BudgetException.class,
                () -> budgetService.saveCustomAllocation(100, 100, 100, -1));
    }

    @Test
    @DisplayName("Súčet > monthlyIncome hodí expenses_exceed_income")
    void totalExceedsIncome_throws() {
        setupBaseBudget(); // monthlyIncome = 1000

        // 400 + 300 + 200 + 200 = 1100 > 1000
        BudgetException ex = assertThrows(BudgetException.class,
                () -> budgetService.saveCustomAllocation(400, 300, 200, 200));
        assertEquals("budget.error.expenses_exceed_income", ex.getMessageKey());
    }

    @Test
    @DisplayName("Súčet == monthlyIncome je OK (na hranici)")
    void totalEqualsIncome_ok() {
        setupBaseBudget(); // monthlyIncome = 1000

        // 400 + 300 + 200 + 100 = 1000 == 1000
        BudgetWarning warning = assertDoesNotThrow(
                () -> budgetService.saveCustomAllocation(400, 300, 200, 100));

        Budget b = budgetRepo.findByUserId(testUser.getId()).orElseThrow();
        assertEquals(0.0, b.getFunMoney(), 0.001,
                "funMoney = monthlyIncome - total = 0");
    }

    // ====================== ULOŽENIE A FUN MONEY ======================

    @Test
    @DisplayName("funMoney = monthlyIncome - (essential + emergency + savings + toInvest)")
    void funMoney_isCalculatedAsRemainder() {
        setupBaseBudget(); // monthlyIncome = 1000

        // essential=200, emergency=100, savings=200, toInvest=300
        // total = 800, funMoney = 200
        budgetService.saveCustomAllocation(200, 100, 200, 300);

        Budget b = budgetRepo.findByUserId(testUser.getId()).orElseThrow();
        assertEquals(200.0, b.getFunMoney(), 0.001);
    }

    @Test
    @DisplayName("Custom alloc prepíše essential, emergency, savings, toInvest a funMoney")
    void customAlloc_overridesAllocationFields() {
        setupBaseBudget();
        // Pred: essential=100 (zo setup), emergency=100, savings=300, toInvest=400, funMoney=100

        budgetService.saveCustomAllocation(500, 200, 100, 50);

        Budget b = budgetRepo.findByUserId(testUser.getId()).orElseThrow();
        assertEquals(500.0, b.getEssentialExpenses(), 0.001);
        assertEquals(200.0, b.getEmergencyFund(), 0.001);
        assertEquals(100.0, b.getSavings(), 0.001);
        assertEquals(50.0, b.getToInvest(), 0.001);
        assertEquals(150.0, b.getFunMoney(), 0.001);
    }

    @Test
    @DisplayName("Custom alloc NEMENÍ Budget Setup polia (food, rent, transport, utilities, other)")
    void customAlloc_doesNotChangeSetupFields() {
        setupBaseBudget();
        Budget before = budgetRepo.findByUserId(testUser.getId()).orElseThrow();
        double foodBefore = before.getFood();
        double rentBefore = before.getRent();
        double transportBefore = before.getTransport();

        budgetService.saveCustomAllocation(500, 200, 100, 50);

        Budget after = budgetRepo.findByUserId(testUser.getId()).orElseThrow();
        assertEquals(foodBefore, after.getFood(), 0.001);
        assertEquals(rentBefore, after.getRent(), 0.001);
        assertEquals(transportBefore, after.getTransport(), 0.001);
    }

    @Test
    @DisplayName("Custom alloc NEMENÍ monthlyIncome a emergency fund limity")
    void customAlloc_doesNotChangeIncomeOrLimits() {
        setupBaseBudget();
        Budget before = budgetRepo.findByUserId(testUser.getId()).orElseThrow();

        budgetService.saveCustomAllocation(500, 200, 100, 50);

        Budget after = budgetRepo.findByUserId(testUser.getId()).orElseThrow();
        assertEquals(before.getMonthlyIncome(), after.getMonthlyIncome(), 0.001);
        assertEquals(before.getMinimalEmergencyFund(), after.getMinimalEmergencyFund(), 0.001);
        assertEquals(before.getOptimalEmergencyFund(), after.getOptimalEmergencyFund(), 0.001);
    }

    // ====================== WARNING ESSENTIAL_BELOW_ACTUAL ======================

    @Test
    @DisplayName("Warning ESSENTIAL_BELOW_ACTUAL keď essential < skutočné výdavky zo setup")
    void warning_essentialBelowActual() {
        // Setup: essentialTotal = 100 (z food+rent+transport+utilities+other)
        budgetService.saveBudgetSetup(1000, 50, 30, 10, 5, 5);

        // Custom: essential = 50 < actualEssential = 100 → WARNING
        BudgetWarning warning = budgetService.saveCustomAllocation(50, 100, 200, 300);

        assertEquals(BudgetWarning.ESSENTIAL_BELOW_ACTUAL, warning);
    }

    @Test
    @DisplayName("Žiadny warning keď essential >= actual")
    void noWarning_essentialMatchesActual() {
        // Setup: essentialTotal = 100
        budgetService.saveBudgetSetup(1000, 50, 30, 10, 5, 5);

        // Custom: essential = 150 > 100 → bez warningu
        BudgetWarning warning = budgetService.saveCustomAllocation(150, 100, 200, 300);

        assertEquals(BudgetWarning.NONE, warning);
    }

    @Test
    @DisplayName("Žiadny warning keď essential == actual")
    void noWarning_essentialEqualsActual() {
        // Setup: essentialTotal = 100
        budgetService.saveBudgetSetup(1000, 50, 30, 10, 5, 5);

        // Custom: essential = 100 == 100 → bez warningu (kód má `if essential < actual`)
        BudgetWarning warning = budgetService.saveCustomAllocation(100, 100, 200, 300);

        assertEquals(BudgetWarning.NONE, warning);
    }

    @Test
    @DisplayName("Warning sa vráti aj pri uložení (allocation sa stále persistuje)")
    void warning_allocationStillPersisted() {
        budgetService.saveBudgetSetup(1000, 50, 30, 10, 5, 5);

        BudgetWarning warning = budgetService.saveCustomAllocation(50, 100, 200, 300);

        assertEquals(BudgetWarning.ESSENTIAL_BELOW_ACTUAL, warning);

        // Údaje sa MAJÚ uložiť aj keď je warning (warning != error)
        Budget b = budgetRepo.findByUserId(testUser.getId()).orElseThrow();
        assertEquals(50.0, b.getEssentialExpenses(), 0.001);
        assertEquals(100.0, b.getEmergencyFund(), 0.001);
    }

    // ====================== EDGE CASES ======================

    @Test
    @DisplayName("Všetko 0 (essential=0, emergency=0, savings=0, toInvest=0) — funMoney = monthlyIncome")
    void allZero_funMoneyEqualsIncome() {
        setupBaseBudget(); // monthlyIncome = 1000

        BudgetWarning warning = budgetService.saveCustomAllocation(0, 0, 0, 0);

        // essential=0 < actualEssential=100 → warning
        assertEquals(BudgetWarning.ESSENTIAL_BELOW_ACTUAL, warning);

        Budget b = budgetRepo.findByUserId(testUser.getId()).orElseThrow();
        assertEquals(1000.0, b.getFunMoney(), 0.001,
                "celý monthlyIncome ide do funMoney");
    }

    @Test
    @DisplayName("Po viacerých custom allocations sa zachová posledný stav")
    void multipleCustomAllocations_lastWins() {
        setupBaseBudget();

        budgetService.saveCustomAllocation(200, 100, 100, 100);
        budgetService.saveCustomAllocation(300, 200, 150, 50);

        Budget b = budgetRepo.findByUserId(testUser.getId()).orElseThrow();
        assertEquals(300.0, b.getEssentialExpenses(), 0.001);
        assertEquals(200.0, b.getEmergencyFund(), 0.001);
        assertEquals(150.0, b.getSavings(), 0.001);
        assertEquals(50.0, b.getToInvest(), 0.001);
        assertEquals(300.0, b.getFunMoney(), 0.001, "1000 - 700 = 300");

        // Stále len 1 záznam v repo
        assertEquals(1, budgetRepo.findAll().size());
    }
}
