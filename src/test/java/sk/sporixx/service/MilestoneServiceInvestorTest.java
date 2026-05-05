package sk.sporixx.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sk.sporixx.dto.MilestoneData;
import sk.sporixx.model.Transaction;
import sk.sporixx.model.User;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy pre getInvestorMilestone().
 *
 * Investor milestone sa počíta zo súčtu výdavkov s kategóriou CATEGORY_INVESTMENT
 * na všetkých účtoch používateľa.
 *
 * Level pravidlá (rovnaké ako SavingMaster):
 *   < 1000     → Level 0
 *   >= 1000    → Level 1
 *   >= 5000    → Level 2
 *   >= 10000   → Level 3
 *   >= 50000   → Level 4
 *   >= 100000  → Level 5
 */
@DisplayName("MilestoneService – Investor")
class MilestoneServiceInvestorTest extends MilestoneServiceTestSupport {

    private static final LocalDateTime NOW = LocalDateTime.now();

    @Test
    @DisplayName("Žiadne investičné transakcie → Level 0")
    void level0_noInvestments() {
        MilestoneData result = milestoneService.getInvestorMilestone();

        assertEquals(0, result.getLevel());
        assertEquals(0.0, result.getXp());
        assertEquals(1_000.0, result.getNextTarget());
        assertEquals("Investor", result.getCategory());
    }

    @Test
    @DisplayName("Celkovo 1000 EUR investícií → Level 1")
    void level1_1000invested() {
        addInvestmentExpense(1_000.0, NOW.minusDays(10));

        MilestoneData result = milestoneService.getInvestorMilestone();

        assertEquals(1, result.getLevel());
        assertEquals(10.0, result.getXp());
        assertEquals(5_000.0, result.getNextTarget());
        assertEquals("milestone.investor.level.1", result.getLevelName());
    }

    @Test
    @DisplayName("Celkovo 5000 EUR investícií → Level 2")
    void level2_5000invested() {
        addInvestmentExpense(5_000.0, NOW.minusDays(5));

        MilestoneData result = milestoneService.getInvestorMilestone();

        assertEquals(2, result.getLevel());
        assertEquals(20.0, result.getXp());
        assertEquals(10_000.0, result.getNextTarget());
    }

    @Test
    @DisplayName("Celkovo 10000 EUR investícií → Level 3")
    void level3_10000invested() {
        addInvestmentExpense(10_000.0, NOW.minusDays(5));

        MilestoneData result = milestoneService.getInvestorMilestone();

        assertEquals(3, result.getLevel());
        assertEquals(50_000.0, result.getNextTarget());
    }

    @Test
    @DisplayName("Celkovo 50000 EUR investícií → Level 4")
    void level4_50000invested() {
        addInvestmentExpense(50_000.0, NOW.minusDays(5));

        MilestoneData result = milestoneService.getInvestorMilestone();

        assertEquals(4, result.getLevel());
        assertEquals(100_000.0, result.getNextTarget());
    }

    @Test
    @DisplayName("Celkovo 100000+ EUR investícií → Level 5, progress = 1.0")
    void level5_100000invested() {
        addInvestmentExpense(100_000.0, NOW.minusDays(5));

        MilestoneData result = milestoneService.getInvestorMilestone();

        assertEquals(5, result.getLevel());
        assertEquals(50.0, result.getXp());
        assertEquals(0.0, result.getNextTarget());
        assertEquals(1.0, result.getProgress());
    }

    @Test
    @DisplayName("Viacero investičných transakcií sa sčítajú")
    void multipleInvestments_summed() {
        addInvestmentExpense(2_000.0, NOW.minusDays(10));
        addInvestmentExpense(3_000.0, NOW.minusDays(5));

        MilestoneData result = milestoneService.getInvestorMilestone();

        // 2000 + 3000 = 5000 → Level 2
        assertEquals(2, result.getLevel());
    }

    @Test
    @DisplayName("Bežné výdavky (nie investícia) sa NEpočítajú")
    void regularExpenses_notCounted() {
        // Bežný výdavok bez kategórie investícia
        Transaction t = Transaction.builder()
                .accountId(mainAccount.getId())
                .transactionTypeId(Transaction.TYPE_EXPENSE)
                .categoryId(5) // nejaká iná kategória
                .amount(9_000.0)
                .completeDate(NOW.minusDays(1))
                .createdAt(NOW.minusDays(1))
                .build();
        transactionRepo.save(t);

        MilestoneData result = milestoneService.getInvestorMilestone();

        // Žiadne investičné transakcie → Level 0
        assertEquals(0, result.getLevel());
    }

    @Test
    @DisplayName("Príjmy s CATEGORY_INVESTMENT sa NEpočítajú (len výdavky)")
    void investmentIncome_notCounted() {
        Transaction t = Transaction.builder()
                .accountId(mainAccount.getId())
                .transactionTypeId(Transaction.TYPE_INCOME) // príjem, nie výdavok
                .categoryId(Transaction.CATEGORY_INVESTMENT)
                .amount(9_999.0)
                .completeDate(NOW.minusDays(1))
                .createdAt(NOW.minusDays(1))
                .build();
        transactionRepo.save(t);

        MilestoneData result = milestoneService.getInvestorMilestone();

        assertEquals(0, result.getLevel());
    }

    @Test
    @DisplayName("progress v Level 1 – korektný výpočet")
    void progress_level1() {
        // Level 1: (totalInvested - 1000) / 4000
        // invested = 3000 → progress = 2000/4000 = 0.5
        addInvestmentExpense(3_000.0, NOW.minusDays(5));

        MilestoneData result = milestoneService.getInvestorMilestone();

        assertEquals(1, result.getLevel());
        assertEquals(0.5, result.getProgress(), 0.001);
    }

    @Test
    @DisplayName("UserRepository dostane update pri zmene levelu")
    void xpUpdatedInUserRepo() {
        addInvestmentExpense(1_000.0, NOW.minusDays(5));

        milestoneService.getInvestorMilestone();

        User updated = userRepo.findById(testUser.getId()).orElseThrow();
        assertEquals(1, updated.getInvestorLevel());
        assertEquals(10.0, updated.getInvestorXp());
    }
}

