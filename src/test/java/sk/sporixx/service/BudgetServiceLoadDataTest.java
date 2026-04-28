package sk.sporixx.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sk.sporixx.dto.BudgetData;
import sk.sporixx.model.Transaction;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy pre {@link BudgetService#loadBudgetData()}.
 * <p>
 * Pokrývajú:
 * <ul>
 *     <li>defaultné hodnoty keď budget neexistuje</li>
 *     <li>mapping všetkých polí z Budget na BudgetData</li>
 *     <li>výpočet percentuálnych hodnôt (zaokrúhlenie na 1 desatinu, division-by-zero)</li>
 *     <li>essentialExpensesTotal = food+rent+transport+utilities+other</li>
 *     <li>emergencyFundCurrent: balance prvého EMERGENCY_FUND účtu (alebo 0)</li>
 *     <li>emergencyFundHistory: kumulatívne saldo</li>
 * </ul>
 */
class BudgetServiceLoadDataTest extends BudgetServiceTestSupport {

    // ====================== DEFAULT (žiadny budget) ======================

    @Test
    @DisplayName("Bez budgetu — všetky polia 0 okrem emergency fund dát")
    void noBudget_returnsDefaults() {
        BudgetData data = budgetService.loadBudgetData();

        assertNotNull(data);
        assertEquals(0.0, data.getMonthlyIncome(), 0.001);
        assertEquals(0.0, data.getFood(), 0.001);
        assertEquals(0.0, data.getRent(), 0.001);
        assertEquals(0.0, data.getTransport(), 0.001);
        assertEquals(0.0, data.getUtilities(), 0.001);
        assertEquals(0.0, data.getOther(), 0.001);
        assertEquals(0.0, data.getEssentialExpensesTotal(), 0.001);
        assertEquals(0.0, data.getEssentialExpenses(), 0.001);
        assertEquals(0.0, data.getEmergencyFund(), 0.001);
        assertEquals(0.0, data.getSavings(), 0.001);
        assertEquals(0.0, data.getToInvest(), 0.001);
        assertEquals(0.0, data.getFunMoney(), 0.001);
        // Percentá tiež 0
        assertEquals(0.0, data.getEssentialExpensesPercent(), 0.001);
        assertEquals(0.0, data.getEmergencyFundPercent(), 0.001);
    }

    @Test
    @DisplayName("Bez budgetu — emergencyFundCurrent SA STÁLE načítava (z account-u)")
    void noBudget_emergencyFundCurrentLoaded() {
        // Emergency account v setUp má currentBalance = 500
        BudgetData data = budgetService.loadBudgetData();

        assertEquals(500.0, data.getEmergencyFundCurrent(), 0.001,
                "emergencyFundCurrent sa načítava nezávisle od existencie budgetu");
    }

    @Test
    @DisplayName("Bez budgetu a bez emergency účtu — emergencyFundCurrent = 0")
    void noBudget_noEmergencyAccount_returnsZero() {
        logInWithoutEmergencyAccount();

        BudgetData data = budgetService.loadBudgetData();

        assertEquals(0.0, data.getEmergencyFundCurrent(), 0.001);
    }

    @Test
    @DisplayName("Bez budgetu — emergencyFundHistory je vždy non-null mapa")
    void noBudget_emergencyFundHistoryIsNonNull() {
        BudgetData data = budgetService.loadBudgetData();

        assertNotNull(data.getEmergencyFundHistory(),
                "emergencyFundHistory nesmie byť null");
    }

    // ====================== MAPPING ======================

    @Test
    @DisplayName("Po setup — všetky polia (Setup + Allocation) sú v BudgetData")
    void afterSetup_allFieldsMapped() {
        budgetService.saveBudgetSetup(2000, 100, 50, 30, 20, 0);
        // standard alokácia: emergency=200, savings=600, toInvest=800, funMoney=200

        BudgetData data = budgetService.loadBudgetData();

        // Setup polia
        assertEquals(2000.0, data.getMonthlyIncome(), 0.001);
        assertEquals(100.0, data.getFood(), 0.001);
        assertEquals(50.0, data.getRent(), 0.001);
        assertEquals(30.0, data.getTransport(), 0.001);
        assertEquals(20.0, data.getUtilities(), 0.001);
        assertEquals(0.0, data.getOther(), 0.001);

        // Allocation polia
        assertEquals(200.0, data.getEssentialExpenses(), 0.001);
        assertEquals(200.0, data.getEmergencyFund(), 0.001);
        assertEquals(600.0, data.getSavings(), 0.001);
        assertEquals(800.0, data.getToInvest(), 0.001);
        assertEquals(200.0, data.getFunMoney(), 0.001);
    }

    @Test
    @DisplayName("essentialExpensesTotal = súčet food+rent+transport+utilities+other")
    void essentialExpensesTotal_isSum() {
        budgetService.saveBudgetSetup(2000, 100, 50, 30, 20, 0);

        BudgetData data = budgetService.loadBudgetData();

        assertEquals(200.0, data.getEssentialExpensesTotal(), 0.001,
                "essentialExpensesTotal = 100+50+30+20+0");
    }

    @Test
    @DisplayName("Emergency fund limity sa mapujú z Budget objektu")
    void emergencyFundLimits_mapped() {
        budgetService.saveBudgetSetup(2000, 100, 200, 50, 30, 20);
        // essentialTotal = 400, minimal = 1200, optimal = 2400

        BudgetData data = budgetService.loadBudgetData();

        assertEquals(1200.0, data.getMinimalEmergencyFund(), 0.001);
        assertEquals(2400.0, data.getOptimalEmergencyFund(), 0.001);
    }

    // ====================== PERCENTÁ ======================

    @Test
    @DisplayName("Percentá: monthlyIncome=1000, essential=100 → 10%")
    void percent_basicCalculation() {
        budgetService.saveBudgetSetup(1000, 50, 30, 10, 5, 5);
        // standard: essential=100, emergency=100, savings=300, toInvest=400, funMoney=100

        BudgetData data = budgetService.loadBudgetData();

        assertEquals(10.0, data.getEssentialExpensesPercent(), 0.001);
        assertEquals(10.0, data.getEmergencyFundPercent(), 0.001);
        assertEquals(30.0, data.getSavingsPercent(), 0.001);
        assertEquals(40.0, data.getToInvestPercent(), 0.001);
        assertEquals(10.0, data.getFunMoneyPercent(), 0.001);
    }

    @Test
    @DisplayName("Percentá sumarizujú na 100% (všetky alokácie spolu)")
    void percent_sumTo100() {
        budgetService.saveBudgetSetup(1500, 100, 50, 30, 15, 5);

        BudgetData data = budgetService.loadBudgetData();

        double total = data.getEssentialExpensesPercent()
                + data.getEmergencyFundPercent()
                + data.getSavingsPercent()
                + data.getToInvestPercent()
                + data.getFunMoneyPercent();
        // Tolerancia kvôli zaokrúhleniu na 1 desatinné miesto
        assertEquals(100.0, total, 0.5,
                "súčet všetkých percent = 100% (tolerancia 0.5 kvôli zaokrúhleniu)");
    }

    @Test
    @DisplayName("Percentá zaokrúhlené na 1 desatinné miesto")
    void percent_roundedTo1Decimal() {
        // Income=300, essential=100 → 33.333...%
        // Math.round(33.333 * 100 * 10) / 10 = Math.round(33333.33) / 10 = 3333/10 = 333.3
        // Hmm, kód: Math.round(amount/income * 100 * 10) / 10
        //   = Math.round(0.333... * 100 * 10) / 10 = Math.round(333.3) / 10 = 333/10 = 33.3
        budgetService.saveBudgetSetup(300, 50, 30, 10, 5, 5);
        // essential = 100, monthlyIncome=300 → 33.3%

        BudgetData data = budgetService.loadBudgetData();

        assertEquals(33.3, data.getEssentialExpensesPercent(), 0.001,
                "33.333% sa zaokrúhli na 33.3");
    }

    // ====================== EMERGENCY FUND CURRENT ======================

    @Test
    @DisplayName("emergencyFundCurrent = balance prvého EMERGENCY_FUND účtu")
    void emergencyFundCurrent_takesFirstEmergencyAccount() {
        // setUp dal emergency account s balance 500
        BudgetData data = budgetService.loadBudgetData();

        assertEquals(500.0, data.getEmergencyFundCurrent(), 0.001);
    }

    @Test
    @DisplayName("Bez emergency účtu — emergencyFundCurrent = 0")
    void noEmergencyAccount_emergencyFundCurrentZero() {
        logInWithoutEmergencyAccount();

        BudgetData data = budgetService.loadBudgetData();

        assertEquals(0.0, data.getEmergencyFundCurrent(), 0.001);
    }

    // ====================== EMERGENCY FUND HISTORY ======================

    @Test
    @DisplayName("Emergency fund history obsahuje 12 mesiacov (od -11 do current)")
    void emergencyFundHistory_has12Months() {
        BudgetData data = budgetService.loadBudgetData();

        assertNotNull(data.getEmergencyFundHistory());
        // Kód generuje od (now - 11 months, withDayOfMonth(1)) po now (withDayOfMonth(1))
        // To je 12 mesiacov inkluzívne
        assertEquals(12, data.getEmergencyFundHistory().size(),
                "history má 12 mesiacov");
    }

    @Test
    @DisplayName("History bez transakcií — všetky hodnoty = initialBalance (kumulatívne)")
    void emergencyFundHistory_noTransactions_flatAtInitial() {
        // emergency account má initialBalance = 500, žiadne transakcie

        BudgetData data = budgetService.loadBudgetData();

        // Všetky mesiace musia byť 500 (initialBalance, žiadny zmena)
        assertTrue(data.getEmergencyFundHistory().values().stream()
                        .allMatch(v -> Math.abs(v - 500.0) < 0.001),
                "bez transakcií je history flat na initialBalance");
    }

    @Test
    @DisplayName("History s INCOME — kumulatívne rastie")
    void emergencyFundHistory_withIncome_growsCumulatively() {
        // Vlož income transakciu pred 1 mesiac (v rámci history rangu)
        insertEmergencyTransaction(Transaction.TYPE_INCOME, 200.0,
                LocalDateTime.now().minusMonths(1).withDayOfMonth(15));

        BudgetData data = budgetService.loadBudgetData();
        var history = data.getEmergencyFundHistory();

        // Posledná hodnota = 500 (init) + 200 (income) = 700
        double lastValue = history.values().stream().reduce((a, b) -> b).orElseThrow();
        assertEquals(700.0, lastValue, 0.001);
    }

    @Test
    @DisplayName("History bez emergency účtu — prázdna mapa (graf nezobrazí nič)")
    void emergencyFundHistory_noEmergencyAccount_emptyMap() {
        logInWithoutEmergencyAccount();

        BudgetData data = budgetService.loadBudgetData();

        assertTrue(data.getEmergencyFundHistory().isEmpty(),
                "bez emergency účtu je history prázdna");
    }

    // ====================== KEEP RUNNING WHEN BUDGET EXISTS ======================

    @Test
    @DisplayName("Po custom alloc — loadBudgetData vráti nové custom hodnoty")
    void afterCustomAlloc_dataReflectsCustom() {
        budgetService.saveBudgetSetup(1000, 50, 30, 10, 5, 5);
        budgetService.saveCustomAllocation(500, 200, 100, 50);

        BudgetData data = budgetService.loadBudgetData();

        assertEquals(500.0, data.getEssentialExpenses(), 0.001);
        assertEquals(200.0, data.getEmergencyFund(), 0.001);
        assertEquals(100.0, data.getSavings(), 0.001);
        assertEquals(50.0, data.getToInvest(), 0.001);
        assertEquals(150.0, data.getFunMoney(), 0.001);
    }
}
