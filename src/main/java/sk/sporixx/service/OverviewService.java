package sk.sporixx.service;

import sk.sporixx.dto.*;
import sk.sporixx.model.Account;

import java.util.List;

/**
 * Service rozhranie pre Overview obrazovku.
 * Poskytuje všetky dáta potrebné na zobrazenie hlavnej obrazovky.
 */
public interface OverviewService {

    /**
     * Načíta sumár účtov: total balance, zoznam účtov, saving goals.
     */
    AccountsSummaryData loadAccountsSummary();

    /**
     * Načíta dáta pre Analytics graf podľa zvoleného obdobia.
     * @param chartPeriod obdobie (1 Week, 1 Month, 6 Months, 12 Months)
     */
    AnalyticsData loadAnalytics(ChartPeriod chartPeriod);

    /**
     * Načíta aktivity: upcoming payments a nedávne transakcie.
     */
    ActivitiesData loadActivities();
}
