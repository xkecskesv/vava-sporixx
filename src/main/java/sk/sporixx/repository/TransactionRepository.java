package sk.sporixx.repository;

import sk.sporixx.dto.SearchCriteria;
import sk.sporixx.model.Transaction;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repository rozhranie pre prístup k transakciám.
 * Optimalizované pre dopytovanie podľa jedného konkrétneho účtu.
 */
public interface TransactionRepository {

    Optional<Transaction> findById(int id);

    /**
     * Nájde transakcie pre daný účet v časovom rozsahu (pre Activities panel).
     */
    List<Transaction> findByAccountIdAndDateRange(int accountId, LocalDateTime from, LocalDateTime to);


    List<Transaction> findByAccountId(int accountId);

    /**
     * Sumarizuje sumy podľa mesiacov (pre 6 Months, 12 Months graf).
     * Za posledných 6/12 mesiacov pre jeden konkrétny účet.
     */
    Map<String, Double> sumByTypeAndMonth(int accountId, int transactionTypeId, LocalDateTime from);

    /**
     * Sumarizuje sumy podľa dní (pre 1 Week, 1 Month graf).
     * Za posledný týždeň/mesiac pre jeden konkrétny účet.
     */
    Map<String, Double> sumByTypeAndDay(int accountId, int transactionTypeId, LocalDateTime from);

    /**
     * Sumarizuje výdavky podľa kategórie za dané obdobie.
     * Kľúč: názov kategórie, hodnota: celková suma.
     * Používa sa pre Expenses by Category report.
     */
    Map<String, Double> sumByCategoryAndDateRange(int accountId, LocalDateTime from, LocalDateTime to);

    /**
     * Sumarizuje výdavky podľa spending classification (WANT/NEED) za dané obdobie.
     * Kľúč: "WANT" alebo "NEED", hodnota: celková suma.
     * Používa sa pre Want vs Need report.
     */
    Map<String, Double> sumByClassificationAndDateRange(int accountId, LocalDateTime from, LocalDateTime to);

    /**
     * Načíta transakcie podľa filtrovacích kritérií pre daný účet.
     * SQL dynamicky stavia WHERE klauzuly podľa vyplnených polí v SearchCriteria.
     */
    List<Transaction> findByFilters(int accountId,
                                    Integer categoryId,
                                    LocalDateTime dateFrom,
                                    LocalDateTime dateTo,
                                    Double amountFrom,
                                    Double amountTo,
                                    Integer transactionTypeId);

    Transaction save(Transaction transaction);

    void update(Transaction transaction);

    void deleteById(int id);
}