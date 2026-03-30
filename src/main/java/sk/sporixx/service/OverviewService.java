package sk.sporixx.service;

import sk.sporixx.dto.ChartPeriod;
import sk.sporixx.dto.OverviewData;

/**
 * Service rozhranie pre Overview obrazovku.
 * Poskytuje všetky dáta potrebné na zobrazenie hlavnej obrazovky.
 */
public interface OverviewService {

    /**
     * Načíta kompletné dáta pre Overview obrazovku.
     *
     * @param userId ID prihláseného používateľa
     * @param chartPeriod obdobie pre Analytics graf (1 Week, 1 Month, 6 Months, 12 Months)
     * @param daysForRecent počet dní pre nedávne transakcie (typicky 2 = dnes + včera)
     * @return kompletný OverviewData objekt
     */
    OverviewData loadOverviewData(int userId, ChartPeriod chartPeriod, int daysForRecent);
}
