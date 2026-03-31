package sk.sporixx.dto;

import lombok.Builder;
import lombok.Data;
import sk.sporixx.model.Account;
import sk.sporixx.model.SavingGoal;
import java.util.List;
import java.util.Map;

/**
 * DTO pre horný panel Overview obrazovky.
 * Obsahuje: total balance, zoznam účtov a saving goals pre saving účty.
 */
@Data
@Builder
public class AccountsSummaryData {

    /** Celkový zostatok všetkých účtov */
    private double totalBalance;

    /** Všetky aktívne účty používateľa (každý má vlastný description) */
    private List<Account> accounts;

    /** Saving goals podľa accountId (jeden goal per saving účet)*/
    private Map<Integer, SavingGoal> savingGoalByAccountId;
}