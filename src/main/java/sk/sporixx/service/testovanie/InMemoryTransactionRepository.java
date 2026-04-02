package sk.sporixx.service.testovanie;

import sk.sporixx.model.Transaction;
import sk.sporixx.repository.TransactionRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class InMemoryTransactionRepository implements TransactionRepository {

    private final List<Transaction> transactions = new ArrayList<>();
    private final AtomicInteger idGenerator = new AtomicInteger(0);

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public Optional<Transaction> findById(int id) {
        return transactions.stream().filter(t -> t.getId() == id).findFirst();
    }

    @Override
    public List<Transaction> findByAccountIdAndDateRange(int accountId, LocalDateTime from, LocalDateTime to) {
        return transactions.stream()
                .filter(t -> t.getAccountId() == accountId)
                .filter(t -> !t.getCompleteDate().isBefore(from) && !t.getCompleteDate().isAfter(to))
                .sorted((a, b) -> b.getCompleteDate().compareTo(a.getCompleteDate()))
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Double> sumByTypeAndMonth(int accountId, int transactionTypeId, LocalDateTime from) {
        return transactions.stream()
                .filter(t -> t.getAccountId() == accountId) // Filtrujeme už len 1 účet
                .filter(t -> t.getTransactionTypeId() == transactionTypeId)
                .filter(t -> !t.getCompleteDate().isBefore(from))
                .collect(Collectors.groupingBy(
                        t -> t.getCompleteDate().format(MONTH_FORMAT),
                        LinkedHashMap::new,
                        Collectors.summingDouble(Transaction::getAmount)));
    }

    @Override
    public Map<String, Double> sumByTypeAndDay(int accountId, int transactionTypeId, LocalDateTime from) {
        return transactions.stream()
                .filter(t -> t.getAccountId() == accountId) // Filtrujeme už len 1 účet
                .filter(t -> t.getTransactionTypeId() == transactionTypeId)
                .filter(t -> !t.getCompleteDate().isBefore(from))
                .collect(Collectors.groupingBy(
                        t -> t.getCompleteDate().format(DAY_FORMAT),
                        LinkedHashMap::new,
                        Collectors.summingDouble(Transaction::getAmount)));
    }

    @Override
    public Transaction save(Transaction transaction) {
        if (transaction.getId() == 0) { transaction.setId(idGenerator.incrementAndGet()); }
        transactions.add(transaction);
        return transaction;
    }

    @Override
    public void update(Transaction transaction) {
        transactions.removeIf(t -> t.getId() == transaction.getId());
        transactions.add(transaction);
    }

    @Override
    public void deleteById(int id) { transactions.removeIf(t -> t.getId() == id); }

    public List<Transaction> findAll() { return new ArrayList<>(transactions); }
}
