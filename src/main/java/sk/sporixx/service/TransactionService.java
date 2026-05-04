package sk.sporixx.service;

import sk.sporixx.dto.SearchCriteria;
import sk.sporixx.model.Transaction;
import java.time.LocalDate;
import java.util.List;

/**
 * Service pre správu transakcií.
 * Pokrýva: pridanie, editáciu, mazanie, načítanie a vyhľadávanie transakcií.
 * Pri každej zmene transakcie sa aktualizuje zostatok účtu.
 */
public interface TransactionService {

    /**
     * Načíta všetky transakcie pre daný účet.
     * @param accountId ID účtu
     */
    List<Transaction> getTransactions(int accountId);

    /**
     * Načíta všetky transakcie naprieč všetkými účtami prihláseného používateľa.
     * Používa sa pri zobrazení ALL tab v Transactions screene.
     * Zoradené od najnovšej.
     */
    List<Transaction> getAllTransactions();

    /**
     * Vyhľadáva transakcie naprieč všetkými účtami prihláseného používateľa.
     * Používa sa keď je aktívny ALL tab so search/filter kritériami.
     */
    List<Transaction> searchTransactions(SearchCriteria criteria);

    /**
     * Vyhľadáva transakcie podľa kritérií.
     * Regex sa aplikuje na názov transakcie.
     */
    List<Transaction> searchTransactions(SearchCriteria criteria, int accountId);

    /**
     * Pridá novú transakciu a aktualizuje zostatok účtu.
     * Pre TYPE_INCOME: balance += amount
     * Pre TYPE_EXPENSE: balance -= amount
     * Pre Transfer (Transaction Between Accounts):
     *   From Account: balance -= amount
     *   To Account: balance += amount
     */
    Transaction addTransaction(int accountId,
                               int transactionTypeId,
                               Integer targetAccountId,
                               int categoryId,
                               Integer spendingClassificationId,
                               String description,
                               double amount,
                               String currencyCode,
                               LocalDate date);

    /**
     * Aktualizuje existujúcu transakciu a prepočíta zostatok účtu.
     * Vypočíta rozdiel (nová suma - stará suma) a aktualizuje balance.
     */
    void updateTransaction(Transaction updatedTransaction);

    /**
     * Vymaže zoznam transakcií a revertuje zostatky účtov.
     * Pre každú transakciu:
     *   TYPE_INCOME: balance -= amount (reverzia príjmu)
     *   TYPE_EXPENSE: balance += amount (reverzia výdavku)
     */
    void deleteTransactions(List<Integer> transactionIds);

    List<Transaction> searchTransactions(String rawInput, int accountId);

    List<Transaction> searchTransactions(String rawInput);
}