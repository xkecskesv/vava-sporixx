package sk.sporixx.dto;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

/**
 * DTO pre Analytics panel na Overview obrazovke.
 * Obsahuje dáta pre graf a zvolené obdobie.
 */
@Data
@Builder
public class AnalyticsData {

    /** Aké obdobie bolo zvolené pre graf */
    private ChartPeriod chartPeriod;

    /**
     * Dáta pre Analytics graf.
     * Kľúč závisí od zvoleného obdobia:
     * - 1 Week / 1 Month: "2026-03-25" (po dňoch)
     * - 6 Months / 12 Months: "2026-03" (po mesiacoch)
     */
    private Map<String, Double> chartData;

    private double totalIncome;
}
