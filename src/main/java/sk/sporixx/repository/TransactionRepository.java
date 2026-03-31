package sk.sporixx.repository;

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

    Transaction save(Transaction transaction);

    void update(Transaction transaction);

    void deleteById(int id);
}