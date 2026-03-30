package sk.sporixx.repository;

import sk.sporixx.model.Transaction;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repository rozhranie pre prístup k transakciám.
 */
public interface TransactionRepository {

    Optional<Transaction> findById(int id);

    /**
     * Nájde transakcie pre daný účet v časovom rozsahu.
     */
    List<Transaction> findByAccountIdAndDateRange(int accountId, LocalDateTime from, LocalDateTime to);

    /**
     * Nájde transakcie pre zoznam účtov v časovom rozsahu.
     */
    List<Transaction> findByAccountIdsAndDateRange(List<Integer> accountIds, LocalDateTime from, LocalDateTime to);

    /**
     * Sumarizuje sumy podľa mesiacov (pre 6 Months, 12 Months graf).
     * Za posledných 6/12 mesiacov.
     */
    Map<String, Double> sumByTypeAndMonth(List<Integer> accountIds, int transactionTypeId, LocalDateTime from);

    /**
     * Sumarizuje sumy podľa dní (pre 1 Week, 1 Month graf).
     */
    Map<String, Double> sumByTypeAndDay(List<Integer> accountIds, int transactionTypeId, LocalDateTime from);

    Transaction save(Transaction transaction);

    void update(Transaction transaction);

    void deleteById(int id);
}