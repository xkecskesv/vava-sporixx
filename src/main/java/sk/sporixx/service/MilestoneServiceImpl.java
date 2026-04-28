package sk.sporixx.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sk.sporixx.dto.ChartPeriod;
import sk.sporixx.dto.MilestoneData;
import sk.sporixx.dto.WantNeedData;
import sk.sporixx.model.Account;
import sk.sporixx.model.Transaction;
import sk.sporixx.model.User;
import sk.sporixx.repository.AccountRepository;
import sk.sporixx.repository.TransactionRepository;
import sk.sporixx.repository.UserRepository;

import java.util.List;


public class MilestoneServiceImpl implements MilestoneService {

    private static final Logger logger = LoggerFactory.getLogger(MilestoneServiceImpl.class);

    private final ReportsService reportsService;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    public MilestoneServiceImpl(ReportsService reportsService, AccountRepository accountRepository,
                                UserRepository userRepository, TransactionRepository transactionRepository) {
        this.reportsService = reportsService;
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public MilestoneData getSmartSpenderMilestone() {
        logger.info("Calculating Smart Spender milestone");

        try {
            WantNeedData data = reportsService.loadWantNeedData(ChartPeriod.TWELVE_MONTHS);

            double wantPercentage = data.getWantPercentage();
            int level = calculateSmartSpenderLevel(wantPercentage);
            double progress = calculateSmartSpenderProgress(wantPercentage, level);

            int userId = SessionManager.getInstance().getCurrentUserId();
            User currentUser = SessionManager.getInstance().getCurrentUserInternal();

            if (level != currentUser.getSpenderLevel()) {
                double xp = level * 10.0;
                userRepository.updateXpAndLevel(userId, "spender", xp, level);
                currentUser.setSpenderLevel(level);
                currentUser.setSpenderXp(xp);
            }

            return MilestoneData.builder()
                    .category("Smart Spender")
                    .level(level)
                    .levelName(getSmartSpenderLevelName(level))
                    .xp(level * 10.0)
                    .progress(progress)
                    .description(getSmartSpenderDescription(level))
                    .build();

        } catch (Exception e) {
            logger.error("Failed to calculate Smart Spender milestone", e);
            return MilestoneData.builder()
                    .category("Smart Spender")
                    .level(0)
                    .levelName("milestone.level.0")
                    .xp(0)
                    .progress(0.0)
                    .description("milestone.smart_spender.desc.0")
                    .build();
        }
    }

    private int calculateSmartSpenderLevel(double wantPercentage) {
        if (wantPercentage == 0) return 0;        // žiadne transakcie
        if (wantPercentage > 70) return 1;        // Impulse Buyer
        if (wantPercentage > 50) return 2;        // Careful Spender
        if (wantPercentage > 30) return 3;        // Mindful Spender
        if (wantPercentage > 10) return 4;        // Disciplined Spender
        return 5;                                 // Smart Spender
    }

    private double calculateSmartSpenderProgress(double wantPercentage, int level) {
        // Progress v rámci aktuálneho levelu (0.0 - 1.0)
        return switch (level) {
            case 1 -> 1.0 - ((wantPercentage - 70) / 30.0);  // 70-100%
            case 2 -> 1.0 - ((wantPercentage - 50) / 20.0);  // 50-70%
            case 3 -> 1.0 - ((wantPercentage - 30) / 20.0);  // 30-50%
            case 4 -> 1.0 - ((wantPercentage - 10) / 20.0);  // 10-30%
            case 5 -> 1.0;
            default -> 0.0;
        };
    }

    private String getSmartSpenderLevelName(int level) {
        return switch (level) {
            case 1 -> "milestone.smart_spender.level.1";
            case 2 -> "milestone.smart_spender.level.2";
            case 3 -> "milestone.smart_spender.level.3";
            case 4 -> "milestone.smart_spender.level.4";
            case 5 -> "milestone.smart_spender.level.5";
            default -> "milestone.level.0";
        };
    }

    private String getSmartSpenderDescription(int level) {
        return switch (level) {
            case 1 -> "milestone.smart_spender.desc.1";
            case 2 -> "milestone.smart_spender.desc.2";
            case 3 -> "milestone.smart_spender.desc.3";
            case 4 -> "milestone.smart_spender.desc.4";
            case 5 -> "milestone.smart_spender.desc.5";
            default -> "milestone.smart_spender.desc.0";
        };
    }

    @Override
    public MilestoneData getSavingMasterMilestone() {
        logger.info("Calculating Saving Master milestone");

        try {
            // sčíta currentAmount naprieč všetkými saving účtami
            double totalSaved = SessionManager.getInstance().getAccounts().stream()
                    .filter(Account::isSavingAccount)
                    .mapToDouble(Account::getCurrentBalance)
                    .sum();

            int level = calculateSavingLevel(totalSaved);
            double progress = calculateSavingProgress(totalSaved, level);

            // aktualizuj XP a level v DB ak sa zmenil
            int userId = SessionManager.getInstance().getCurrentUserId();
            User currentUser = SessionManager.getInstance().getCurrentUserInternal();

            if (level != currentUser.getSavingLevel()) {
                double xp = level * 10.0;
                userRepository.updateXpAndLevel(userId, "saving", xp, level);
                currentUser.setSavingLevel(level);
                currentUser.setSavingXp(xp);
            }

            return MilestoneData.builder()
                    .category("Saving Master")
                    .level(level)
                    .levelName(getSavingLevelName(level))
                    .xp(level * 10.0)
                    .progress(progress)
                    .description(getSavingDescription(level))
                    .build();

        } catch (Exception e) {
            logger.error("Failed to calculate Saving Master milestone", e);
            return MilestoneData.builder()
                    .category("Saving Master")
                    .level(0)
                    .levelName("milestone.level.0")
                    .xp(0)
                    .progress(0.0)
                    .description("milestone.saving_master.desc.0")
                    .build();
        }
    }

    private int calculateSavingLevel(double totalSaved) {
        if (totalSaved >= 100_000) return 5;
        if (totalSaved >= 50_000)  return 4;
        if (totalSaved >= 10_000)  return 3;
        if (totalSaved >= 5_000)   return 2;
        if (totalSaved >= 1_000)   return 1;
        return 0;
    }

    private double calculateSavingProgress(double totalSaved, int level) {
        return switch (level) {
            case 0 -> totalSaved / 1_000.0;
            case 1 -> (totalSaved - 1_000) / 4_000.0;
            case 2 -> (totalSaved - 5_000) / 5_000.0;
            case 3 -> (totalSaved - 10_000) / 40_000.0;
            case 4 -> (totalSaved - 50_000) / 50_000.0;
            case 5 -> 1.0;
            default -> 0.0;
        };
    }

    private String getSavingLevelName(int level) {
        return switch (level) {
            case 1 -> "milestone.saving_master.level.1";
            case 2 -> "milestone.saving_master.level.2";
            case 3 -> "milestone.saving_master.level.3";
            case 4 -> "milestone.saving_master.level.4";
            case 5 -> "milestone.saving_master.level.5";
            default -> "milestone.level.0";
        };
    }

    private String getSavingDescription(int level) {
        return switch (level) {
            case 0 -> "milestone.saving_master.desc.0";
            case 1 -> "milestone.saving_master.desc.1";
            case 2 -> "milestone.saving_master.desc.2";
            case 3 -> "milestone.saving_master.desc.3";
            case 4 -> "milestone.saving_master.desc.4";
            default -> "milestone.saving_master.desc.5";
        };
    }

    @Override
    public MilestoneData getInvestorMilestone() {
        logger.info("Calculating Investor milestone");

        try {
            // Sčítaj všetky Income transakcie s kategóriou Investment
            List<Integer> accountIds = SessionManager.getInstance().getAccountIds();
            double totalInvested = 0.0;

            for (int accountId : accountIds) {
                List<Transaction> transactions = transactionRepository
                        .findByAccountId(accountId);
                totalInvested += transactions.stream()
                        .filter(t -> t.getCategoryId() != null
                                && t.getCategoryId() == Transaction.CATEGORY_INVESTMENT
                                && t.getTransactionTypeId() == Transaction.TYPE_EXPENSE)
                        .mapToDouble(Transaction::getAmount)
                        .sum();
            }

            int level = calculateInvestorLevel(totalInvested);
            double progress = calculateInvestorProgress(totalInvested, level);

            int userId = SessionManager.getInstance().getCurrentUserId();
            User currentUser = SessionManager.getInstance().getCurrentUserInternal();

            if (level != currentUser.getInvestorLevel()) {
                double xp = level * 10.0;
                userRepository.updateXpAndLevel(userId, "investor", xp, level);
                currentUser.setInvestorLevel(level);
                currentUser.setInvestorXp(xp);
            }

            return MilestoneData.builder()
                    .category("Investor")
                    .level(level)
                    .levelName(getInvestorLevelName(level))
                    .xp(level * 10.0)
                    .progress(progress)
                    .description(getInvestorDescription(level))
                    .build();

        } catch (Exception e) {
            logger.error("Failed to calculate Investor milestone", e);
            return MilestoneData.builder()
                    .category("Investor")
                    .level(0)
                    .levelName("milestone.level.0")
                    .xp(0)
                    .progress(0.0)
                    .description("milestone.investor.desc.0")
                    .build();
        }
    }

    private int calculateInvestorLevel(double totalInvested) {
        if (totalInvested >= 100_000) return 5;
        if (totalInvested >= 50_000)  return 4;
        if (totalInvested >= 10_000)  return 3;
        if (totalInvested >= 5_000)   return 2;
        if (totalInvested >= 1_000)   return 1;
        return 0;
    }

    private double calculateInvestorProgress(double totalInvested, int level) {
        return switch (level) {
            case 0 -> totalInvested / 1_000.0;
            case 1 -> (totalInvested - 1_000) / 4_000.0;
            case 2 -> (totalInvested - 5_000) / 5_000.0;
            case 3 -> (totalInvested - 10_000) / 40_000.0;
            case 4 -> (totalInvested - 50_000) / 50_000.0;
            case 5 -> 1.0;
            default -> 0.0;
        };
    }

    private String getInvestorLevelName(int level) {
        return switch (level) {
            case 1 -> "milestone.investor.level.1";
            case 2 -> "milestone.investor.level.2";
            case 3 -> "milestone.investor.level.3";
            case 4 -> "milestone.investor.level.4";
            case 5 -> "milestone.investor.level.5";
            default -> "milestone.level.0";
        };
    }

    private String getInvestorDescription(int level) {
        return switch (level) {
            case 0 -> "milestone.investor.desc.0";
            case 1 -> "milestone.investor.desc.1";
            case 2 -> "milestone.investor.desc.2";
            case 3 -> "milestone.investor.desc.3";
            case 4 -> "milestone.investor.desc.4";
            default -> "milestone.investor.desc.5";
        };
    }

    @Override
    public MilestoneData getBudgetKeeperMilestone() {
        // TODO
        return null;
    }
}