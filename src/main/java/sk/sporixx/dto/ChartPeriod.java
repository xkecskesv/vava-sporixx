package sk.sporixx.dto;

import lombok.Getter;

import java.time.LocalDate;

/**
 * Obdobia pre Analytics graf na Overview obrazovke.
 * Zodpovedá dropdown hodnotám: 1 Week, 1 Month, 6 Months, 12 Months.
 * Každé obdobie vie:
 * - vypočítať svoj začiatočný dátum (rešpektuje rôzne dĺžky mesiacov)
 * - či sa má graf zoskupovať po dňoch alebo mesiacoch
 */
@Getter
public enum ChartPeriod {

    ONE_WEEK(true),
    ONE_MONTH(true),
    SIX_MONTHS(false),
    TWELVE_MONTHS(false);

    private final boolean groupByDay;

    ChartPeriod(boolean groupByDay) {
        this.groupByDay = groupByDay;
    }

    /**
     * Vypočíta začiatočný dátum pre graf.
     * Používa LocalDate metódy, ktoré správne rešpektujú
     * rôzne dĺžky mesiacov (28/29/30/31 dní).
     */
    public LocalDate calculateStartDate() {
        LocalDate today = LocalDate.now();

        return switch (this) {
            case ONE_WEEK -> today.minusWeeks(1);
            case ONE_MONTH -> today.minusMonths(1);
            case SIX_MONTHS -> today.minusMonths(6).withDayOfMonth(1);
            default -> today.minusMonths(11).withDayOfMonth(1);
        };
    }
}