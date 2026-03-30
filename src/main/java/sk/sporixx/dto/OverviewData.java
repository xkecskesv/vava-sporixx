package sk.sporixx.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import sk.sporixx.model.Account;
import sk.sporixx.model.RecurringRule;
import sk.sporixx.model.SavingGoal;
import sk.sporixx.model.Transaction;

import java.util.List;
import java.util.Map;

/**
 * DTO pre Overview obrazovku.
 * Obsahuje všetky dáta potrebné na zobrazenie celej Overview obrazovky.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OverviewData {

    /** Celkový zostatok všetkých účtov (vypočítaný v DB cez SUM) */
    private double totalBalance;

    /** Všetky aktívne účty používateľa (každý má vlastný description) */
    private List<Account> accounts;

    /** Saving goals zoskupené podľa accountId */
    private Map<Integer, List<SavingGoal>> savingGoalsByAccountId;

    /**
     * Dáta pre Analytics graf.
     * Kľúč závisí od zvoleného obdobia:
     * - 1 Week / 1 Month: "2026-03-25" (po dňoch)
     * - 6 Months / 12 Months: "2026-03" (po mesiacoch)
     */
    private Map<String, Double> chartData;

    /** Aké obdobie bolo zvolené pre graf */
    private ChartPeriod chartPeriod;

    /** Blížiace sa opakované platby pre Activities panel */
    private List<RecurringRule> upcomingPayments;

    /** Nedávne transakcie (dnes, včera) pre Activities panel */
    private List<Transaction> recentTransactions;
}
