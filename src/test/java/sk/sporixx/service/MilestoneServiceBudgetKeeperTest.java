package sk.sporixx.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sk.sporixx.dto.BudgetData;
import sk.sporixx.dto.MilestoneData;
import sk.sporixx.model.Transaction;
import sk.sporixx.model.User;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy pre getBudgetKeeperMilestone().
 *
 * Budget Keeper sa počíta z počtu po sebe idúcich mesiacov, kde používateľ
 * dodržal budget.
 *
 * Level pravidlá:
 *   0 mesiacov   → Level 0
 *   >= 1 mesiac  → Level 1
 *   >= 3 mesiace → Level 2
 *   >= 6 mesiacov → Level 3
 *   >= 12 mesiacov → Level 4
 *   >= 24 mesiacov → Level 5
 */
@DisplayName("MilestoneService – Budget Keeper")
class MilestoneServiceBudgetKeeperTest extends MilestoneServiceTestSupport {

    private static final LocalDateTime NOW = LocalDateTime.now();

    private BudgetData simpleBudget(double income, double savings, double emergencyFund,
                                    double toInvest, double essential, double funMoney) {
        return BudgetData.builder()
                .monthlyIncome(income)
                .savings(savings)
                .emergencyFund(emergencyFund)
                .toInvest(toInvest)
                .essentialExpenses(essential)
                .funMoney(funMoney)
                .build();
    }

    private void addExpense(double amount, LocalDateTime date) {
        transactionRepo.save(Transaction.builder()
                .accountId(mainAccount.getId())
                .transactionTypeId(Transaction.TYPE_EXPENSE)
                .amount(amount)
                .completeDate(date).createdAt(date).build());
    }

    @Test
    @DisplayName("Žiadny budget nastavený → Level 0 s 'no_budget' popisom")
    void noBudget_returnsLevel0() {
        fakeBudgetService.setBudgetData(null);
        MilestoneData result = milestoneService.getBudgetKeeperMilestone();
        assertEquals(0, result.getLevel());
        assertEquals("milestone.budget_keeper.desc.no_budget", result.getDescription());
    }

    @Test
    @DisplayName("Budget s monthlyIncome = 0 → Level 0")
    void zeroIncomeBudget_returnsLevel0() {
        fakeBudgetService.setBudgetData(simpleBudget(0, 0, 0, 0, 0, 0));
        MilestoneData result = milestoneService.getBudgetKeeperMilestone();
        assertEquals(0, result.getLevel());
    }

    @Test
    @DisplayName("Žiadne transakcie → Level 0")
    void noTransactions_level0() {
        fakeBudgetService.setBudgetData(simpleBudget(2000, 0, 0, 0, 500, 200));
        MilestoneData result = milestoneService.getBudgetKeeperMilestone();
        assertEquals(0, result.getLevel());
    }

    @Test
    @DisplayName("1 mesiac dodržaného budgetu (výdavky v limite) → Level 1")
    void oneMonth_level1() {
        fakeBudgetService.setBudgetData(simpleBudget(2000, 0, 0, 0, 500, 200));
        addExpense(600.0, NOW.minusMonths(1).withDayOfMonth(15));

        MilestoneData result = milestoneService.getBudgetKeeperMilestone();

        assertEquals(1, result.getLevel());
        assertEquals("milestone.budget_keeper.level.1", result.getLevelName());
        assertEquals("Budget Keeper", result.getCategory());
    }

    @Test
    @DisplayName("Výdavky nad limitom → Level 0")
    void expensesOverLimit_level0() {
        fakeBudgetService.setBudgetData(simpleBudget(2000, 0, 0, 0, 300, 100));
        addExpense(500.0, NOW.minusMonths(1).withDayOfMonth(15));

        assertEquals(0, milestoneService.getBudgetKeeperMilestone().getLevel());
    }

    @Test
    @DisplayName("Savings požiadavka nesplnená → Level 0")
    void savingsNotMet_level0() {
        fakeBudgetService.setBudgetData(simpleBudget(2000, 300, 0, 0, 500, 200));
        addSavingIncome(100.0, NOW.minusMonths(1).withDayOfMonth(15));
        addExpense(400.0, NOW.minusMonths(1).withDayOfMonth(15));

        assertEquals(0, milestoneService.getBudgetKeeperMilestone().getLevel());
    }

    @Test
    @DisplayName("Emergency fund nesplnený → Level 0")
    void emergencyNotMet_level0() {
        fakeBudgetService.setBudgetData(simpleBudget(2000, 0, 200, 0, 500, 200));
        addEmergencyIncome(100.0, NOW.minusMonths(1).withDayOfMonth(15));
        addExpense(400.0, NOW.minusMonths(1).withDayOfMonth(15));

        assertEquals(0, milestoneService.getBudgetKeeperMilestone().getLevel());
    }

    @Test
    @DisplayName("Investícia nesplnená → Level 0")
    void investmentNotMet_level0() {
        fakeBudgetService.setBudgetData(simpleBudget(2000, 0, 0, 500, 500, 200));
        addInvestmentExpense(100.0, NOW.minusMonths(1).withDayOfMonth(15));

        assertEquals(0, milestoneService.getBudgetKeeperMilestone().getLevel());
    }

    @Test
    @DisplayName("3 mesiace dodržaného budgetu → Level 2")
    void threeMonths_level2() {
        fakeBudgetService.setBudgetData(simpleBudget(2000, 0, 0, 0, 500, 200));
        for (int i = 1; i <= 3; i++) {
            addExpense(600.0, NOW.minusMonths(i).withDayOfMonth(15));
        }

        MilestoneData result = milestoneService.getBudgetKeeperMilestone();

        assertEquals(2, result.getLevel());
        assertEquals("milestone.budget_keeper.level.2", result.getLevelName());
    }

    @Test
    @DisplayName("Medzera v mesiacoch preruší reťazec")
    void gapInMonths_breaksChain() {
        fakeBudgetService.setBudgetData(simpleBudget(2000, 0, 0, 0, 500, 200));
        addExpense(600.0, NOW.minusMonths(1).withDayOfMonth(15));
        addExpense(600.0, NOW.minusMonths(3).withDayOfMonth(15));

        assertEquals(1, milestoneService.getBudgetKeeperMilestone().getLevel());
    }

    @Test
    @DisplayName("Výdavky presne na limite → mesiac OK")
    void expensesAtExactLimit() {
        fakeBudgetService.setBudgetData(simpleBudget(2000, 0, 0, 0, 400, 100));
        addExpense(500.0, NOW.minusMonths(1).withDayOfMonth(15));

        assertEquals(1, milestoneService.getBudgetKeeperMilestone().getLevel());
    }

    @Test
    @DisplayName("Transfer kategória sa NEpočíta do výdavkov")
    void transferCategory_excluded() {
        fakeBudgetService.setBudgetData(simpleBudget(2000, 0, 0, 0, 300, 100));

        LocalDateTime lastMonth = NOW.minusMonths(1).withDayOfMonth(15);
        addExpense(300.0, lastMonth);

        transactionRepo.save(Transaction.builder()
                .accountId(mainAccount.getId())
                .transactionTypeId(Transaction.TYPE_EXPENSE)
                .categoryId(Transaction.CATEGORY_TRANSFER)
                .amount(9_999.0)
                .completeDate(lastMonth).createdAt(lastMonth).build());

        assertEquals(1, milestoneService.getBudgetKeeperMilestone().getLevel());
    }

    @Test
    @DisplayName("UserRepository dostane update pri zmene levelu")
    void xpUpdatedInUserRepo() {
        fakeBudgetService.setBudgetData(simpleBudget(2000, 0, 0, 0, 500, 200));
        addExpense(600.0, NOW.minusMonths(1).withDayOfMonth(15));

        milestoneService.getBudgetKeeperMilestone();

        User updated = userRepo.findById(testUser.getId()).orElseThrow();
        assertEquals(1, updated.getBudgetLevel());
        assertEquals(10.0, updated.getBudgetXp());
    }
}
