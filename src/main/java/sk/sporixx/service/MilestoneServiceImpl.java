package sk.sporixx.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sk.sporixx.dto.ChartPeriod;
import sk.sporixx.dto.MilestoneData;
import sk.sporixx.dto.WantNeedData;


public class MilestoneServiceImpl implements MilestoneService {

    private static final Logger logger = LoggerFactory.getLogger(MilestoneServiceImpl.class);

    private final ReportsService reportsService;

    public MilestoneServiceImpl(ReportsService reportsService) {
        this.reportsService = reportsService;
    }

    @Override
    public MilestoneData getSmartSpenderMilestone() {
        logger.info("Calculating Smart Spender milestone");

        try {
            WantNeedData data = reportsService.loadWantNeedData(ChartPeriod.TWELVE_MONTHS);

            double wantPercentage = data.getWantPercentage();
            int level = calculateSmartSpenderLevel(wantPercentage);
            double progress = calculateSmartSpenderProgress(wantPercentage, level);

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
                    .levelName("Getting Started")
                    .xp(0)
                    .progress(0.0)
                    .description("Start tracking your expenses.")
                    .build();
        }
    }

    private int calculateSmartSpenderLevel(double wantPercentage) {
        if (wantPercentage == 0) return 0;        // žiadne transakcie
        if (wantPercentage > 70) return 1;        // Impulse Buyer
        if (wantPercentage > 50) return 2;        // Careful Spender
        if (wantPercentage > 30) return 3;        // Mindful Spender
        if (wantPercentage > 10) return 4;        // Disciplined Spender
        return 5;                                  // Smart Spender
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
            case 1 -> "Impulse Buyer";
            case 2 -> "Careful Spender";
            case 3 -> "Mindful Spender";
            case 4 -> "Disciplined Spender";
            case 5 -> "Smart Spender";
            default -> "Getting Started";
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
        // TODO
        return null;
    }

    @Override
    public MilestoneData getInvestorMilestone() {
        // TODO
        return null;
    }

    @Override
    public MilestoneData getBudgetKeeperMilestone() {
        // TODO
        return null;
    }
}