package sk.sporixx.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import sk.sporixx.dto.BudgetData;
import sk.sporixx.dto.MilestoneData;
import sk.sporixx.dto.WantNeedData;
import sk.sporixx.model.Transaction;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance testy pre MilestoneService.
 *
 * Keďže MilestoneService robí čisté in-memory výpočty (žiadny BCrypt, žiadna DB),
 * sú tieto testy rýchle a spoľahlivé.
 *
 * Testované scenáre:
 *   - SmartSpender: 1 000 volaní getSmartSpenderMilestone()
 *   - SavingMaster: 1 000 volaní getSavingMasterMilestone()
 *   - Investor:     1 000 volaní getInvestorMilestone() pri 500 transakciách
 *   - BudgetKeeper: 100 volaní getBudgetKeeperMilestone() pri 24 mesiacoch transakcií
 *   - AllMilestones: sekvenčné načítanie všetkých 4 milestonov 500×
 */
@DisplayName("MilestoneService – Performance testy")
class MilestoneServicePerformanceTest extends MilestoneServiceTestSupport {

    private static final LocalDateTime NOW = LocalDateTime.now();

    // ======================== SMART SPENDER ========================

    @Nested
    @DisplayName("SmartSpender – výkon")
    class SmartSpenderPerformance {

        @Test
        @Timeout(value = 1, unit = TimeUnit.SECONDS)
        @DisplayName("1 000 volaní getSmartSpenderMilestone() prebehne do 1 s")
        void smartSpender1000Calls_withinTimeLimit() {
            fakeReportsService.setWantPercentage(35.0); // Level 3

            for (int i = 0; i < 1_000; i++) {
                MilestoneData result = milestoneService.getSmartSpenderMilestone();
                assertNotNull(result);
                assertEquals(3, result.getLevel());
            }
        }

        @Test
        @Timeout(value = 1, unit = TimeUnit.SECONDS)
        @DisplayName("1 000 volaní s rôznymi wantPercentage prebehne do 1 s")
        void smartSpender1000CallsVaryingInput_withinTimeLimit() {
            double[] wantPercentages = {5.0, 15.0, 35.0, 55.0, 75.0, 0.0};

            for (int i = 0; i < 1_000; i++) {
                fakeReportsService.setWantPercentage(wantPercentages[i % wantPercentages.length]);
                MilestoneData result = milestoneService.getSmartSpenderMilestone();
                assertNotNull(result);
                assertTrue(result.getLevel() >= 0 && result.getLevel() <= 5);
            }
        }
    }

    // ======================== SAVING MASTER ========================

    @Nested
    @DisplayName("SavingMaster – výkon")
    class SavingMasterPerformance {

        @Test
        @Timeout(value = 1, unit = TimeUnit.SECONDS)
        @DisplayName("1 000 volaní getSavingMasterMilestone() prebehne do 1 s")
        void savingMaster1000Calls_withinTimeLimit() {
            // savingAccount má currentBalance = 5 000 → Level 2
            for (int i = 0; i < 1_000; i++) {
                MilestoneData result = milestoneService.getSavingMasterMilestone();
                assertNotNull(result);
                assertEquals(2, result.getLevel());
            }
        }
    }

    // ======================== INVESTOR ========================

    @Nested
    @DisplayName("Investor – výkon")
    class InvestorPerformance {

        @Test
        @Timeout(value = 1, unit = TimeUnit.SECONDS)
        @DisplayName("getInvestorMilestone() pri 500 investičných transakciách prebehne do 1 s")
        void investor500Transactions_withinTimeLimit() {
            // Naplníme repozitár 500 investičnými transakciami
            for (int i = 0; i < 500; i++) {
                addInvestmentExpense(10.0, NOW.minusDays(i));
            }
            // 500 × 10 = 5 000 € → Level 2

            MilestoneData result = milestoneService.getInvestorMilestone();
            assertNotNull(result);
            assertEquals(2, result.getLevel());
        }

        @Test
        @Timeout(value = 2, unit = TimeUnit.SECONDS)
        @DisplayName("100 volaní getInvestorMilestone() pri 500 transakciách prebehne do 2 s")
        void investor100Calls500Transactions_withinTimeLimit() {
            for (int i = 0; i < 500; i++) {
                addInvestmentExpense(10.0, NOW.minusDays(i));
            }

            for (int i = 0; i < 100; i++) {
                MilestoneData result = milestoneService.getInvestorMilestone();
                assertNotNull(result);
                assertEquals(2, result.getLevel());
            }
        }
    }

    // ======================== BUDGET KEEPER ========================

    @Nested
    @DisplayName("BudgetKeeper – výkon")
    class BudgetKeeperPerformance {

        @Test
        @Timeout(value = 2, unit = TimeUnit.SECONDS)
        @DisplayName("getBudgetKeeperMilestone() s 24 mesiacmi transakcií prebehne do 2 s")
        void budgetKeeper24MonthsData_withinTimeLimit() {
            BudgetData budget = BudgetData.builder()
                    .monthlyIncome(2000.0)
                    .savings(200.0)
                    .emergencyFund(100.0)
                    .toInvest(0.0)
                    .essentialExpenses(1200.0)
                    .funMoney(300.0)
                    .build();
            fakeBudgetService.setBudgetData(budget);

            // Naplníme 24 mesiacov po 30 transakcií = 720 transakcií
            for (int month = 1; month <= 24; month++) {
                LocalDateTime base = NOW.minusMonths(month).withDayOfMonth(15);
                addSavingIncome(200.0, base);
                addEmergencyIncome(100.0, base);
                for (int day = 1; day <= 28; day++) {
                    Transaction t = Transaction.builder()
                            .accountId(mainAccount.getId())
                            .transactionTypeId(Transaction.TYPE_EXPENSE)
                            .amount(50.0)
                            .completeDate(base.withDayOfMonth(day))
                            .createdAt(base.withDayOfMonth(day))
                            .build();
                    transactionRepo.save(t);
                }
            }

            MilestoneData result = milestoneService.getBudgetKeeperMilestone();
            assertNotNull(result);
            assertTrue(result.getLevel() >= 0);
        }

        @Test
        @Timeout(value = 3, unit = TimeUnit.SECONDS)
        @DisplayName("10 volaní getBudgetKeeperMilestone() s 24 mesiacmi dát prebehne do 3 s")
        void budgetKeeper10Calls24Months_withinTimeLimit() {
            BudgetData budget = BudgetData.builder()
                    .monthlyIncome(2000.0)
                    .savings(200.0)
                    .emergencyFund(100.0)
                    .toInvest(0.0)
                    .essentialExpenses(1200.0)
                    .funMoney(300.0)
                    .build();
            fakeBudgetService.setBudgetData(budget);

            for (int month = 1; month <= 24; month++) {
                LocalDateTime base = NOW.minusMonths(month).withDayOfMonth(15);
                addSavingIncome(200.0, base);
                addEmergencyIncome(100.0, base);
                for (int day = 1; day <= 10; day++) {
                    Transaction t = Transaction.builder()
                            .accountId(mainAccount.getId())
                            .transactionTypeId(Transaction.TYPE_EXPENSE)
                            .amount(50.0)
                            .completeDate(base.withDayOfMonth(day))
                            .createdAt(base.withDayOfMonth(day))
                            .build();
                    transactionRepo.save(t);
                }
            }

            for (int i = 0; i < 10; i++) {
                MilestoneData result = milestoneService.getBudgetKeeperMilestone();
                assertNotNull(result);
            }
        }
    }

    // ======================== ALL MILESTONES ========================

    @Nested
    @DisplayName("Všetky milestony – výkon")
    class AllMilestonesPerformance {

        @Test
        @Timeout(value = 2, unit = TimeUnit.SECONDS)
        @DisplayName("500× načítanie všetkých 4 milestonov prebehne do 2 s")
        void allMilestones500Calls_withinTimeLimit() {
            fakeReportsService.setWantPercentage(25.0);
            fakeBudgetService.setBudgetData(BudgetData.builder()
                    .monthlyIncome(2000.0).savings(200.0).emergencyFund(100.0)
                    .toInvest(0.0).essentialExpenses(1200.0).funMoney(300.0).build());

            for (int i = 0; i < 500; i++) {
                assertNotNull(milestoneService.getSmartSpenderMilestone());
                assertNotNull(milestoneService.getSavingMasterMilestone());
                assertNotNull(milestoneService.getInvestorMilestone());
                assertNotNull(milestoneService.getBudgetKeeperMilestone());
            }
        }
    }
}

