package sk.sporixx.dto;

import lombok.Builder;
import lombok.Data;
import sk.sporixx.model.RecurringRule;
import sk.sporixx.model.Transaction;
import java.util.List;

/**
 * DTO pre Activities panel na Overview obrazovke.
 * Obsahuje upcoming payments a nedávne transakcie.
 */
@Data
@Builder
public class ActivitiesData {

    /** Blížiace sa opakované platby pre Activities panel */
    private List<RecurringRule> upcomingPayments;

    /** Nedávne transakcie pre Activities panel */
    private List<Transaction> recentTransactions;
}