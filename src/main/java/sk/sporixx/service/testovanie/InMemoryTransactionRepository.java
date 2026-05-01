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
    public Map<String, Double> sumByCategoryAndDateRange(int accountId, LocalDateTime from,
                                                         LocalDateTime to,
                                                         List<Integer> excludeCategoryIds) {
        return transactions.stream()
                .filter(t -> t.getAccountId() == accountId)
                .filter(t -> t.getTransactionTypeId() == Transaction.TYPE_EXPENSE)
                .filter(t -> !t.getCompleteDate().isBefore(from) && !t.getCompleteDate().isAfter(to))
                .filter(t -> t.getCategoryId() != null
                        && !excludeCategoryIds.contains(t.getCategoryId()))
                .collect(Collectors.groupingBy(
                        t -> "Category_" + t.getCategoryId(),
                        LinkedHashMap::new,
                        Collectors.summingDouble(Transaction::getAmount)));
    }

    @Override
    public List<Transaction> findByAccountId(int accountId) {
        return transactions.stream()
                .filter(t -> t.getAccountId() == accountId)
                .toList();
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
    public Map<String, Double> sumByTypeAndMonth(int accountId, int transactionTypeId,
                                                 LocalDateTime from,
                                                 List<Integer> excludeCategoryIds) {
        return transactions.stream()
                .filter(t -> t.getAccountId() == accountId)
                .filter(t -> t.getTransactionTypeId() == transactionTypeId)
                .filter(t -> !t.getCompleteDate().isBefore(from))
                .filter(t -> t.getCategoryId() != null
                        && !excludeCategoryIds.contains(t.getCategoryId()))
                .collect(Collectors.groupingBy(
                        t -> t.getCompleteDate().format(MONTH_FORMAT),
                        LinkedHashMap::new,
                        Collectors.summingDouble(Transaction::getAmount)));
    }

    @Override
    public Map<String, Double> sumByTypeAndDay(int accountId, int transactionTypeId,
                                               LocalDateTime from,
                                               List<Integer> excludeCategoryIds) {
        return transactions.stream()
                .filter(t -> t.getAccountId() == accountId)
                .filter(t -> t.getTransactionTypeId() == transactionTypeId)
                .filter(t -> !t.getCompleteDate().isBefore(from))
                .filter(t -> t.getCategoryId() != null
                        && !excludeCategoryIds.contains(t.getCategoryId()))
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
    public void saveTransfer(Transaction expense, Transaction income,
                             int fromAccountId, double fromNewBalance,
                             int toAccountId, double toNewBalance) {
        save(expense);
        save(income);
        // balance updates sú in-memory riešené priamo cez account.setCurrentBalance() v service vrstve
    }

    @Override
    public void update(Transaction transaction) {
        transactions.removeIf(t -> t.getId() == transaction.getId());
        transactions.add(transaction);
    }

    @Override
    public void deleteById(int id) { transactions.removeIf(t -> t.getId() == id); }

    @Override
    public Map<String, Double> sumByCategoryAndDateRange(int accountId, LocalDateTime from, LocalDateTime to) {
        // V in-memory nemáme JOIN na categories — vrátime categoryId ako kľúč
        // DB kolega implementuje správny JOIN s názvom kategórie
        return transactions.stream()
                .filter(t -> t.getAccountId() == accountId)
                .filter(t -> t.getTransactionTypeId() == Transaction.TYPE_EXPENSE)
                .filter(t -> !t.getCompleteDate().isBefore(from) && !t.getCompleteDate().isAfter(to))
                .collect(Collectors.groupingBy(
                        t -> "Category_" + t.getCategoryId(), // placeholder — DB kolega vráti názov
                        LinkedHashMap::new,
                        Collectors.summingDouble(Transaction::getAmount)));
    }

    @Override
    public Map<String, Double> sumByClassificationAndDateRange(int accountId, LocalDateTime from, LocalDateTime to) {
        Map<String, Double> result = new LinkedHashMap<>();

        double wantSum = transactions.stream()
                .filter(t -> t.getAccountId() == accountId)
                .filter(t -> t.getTransactionTypeId() == Transaction.TYPE_EXPENSE)
                .filter(t -> !t.getCompleteDate().isBefore(from) && !t.getCompleteDate().isAfter(to))
                .filter(t -> t.getSpendingClassificationId() != null &&
                        t.getSpendingClassificationId() == Transaction.CLASSIFICATION_WANT)
                .mapToDouble(Transaction::getAmount)
                .sum();

        double needSum = transactions.stream()
                .filter(t -> t.getAccountId() == accountId)
                .filter(t -> t.getTransactionTypeId() == Transaction.TYPE_EXPENSE)
                .filter(t -> !t.getCompleteDate().isBefore(from) && !t.getCompleteDate().isAfter(to))
                .filter(t -> t.getSpendingClassificationId() != null &&
                        t.getSpendingClassificationId() == Transaction.CLASSIFICATION_NEED)
                .mapToDouble(Transaction::getAmount)
                .sum();

        result.put("WANT", wantSum);
        result.put("NEED", needSum);
        return result;
    }

    @Override
    public List<Transaction> findByFilters(int accountId,
                                           Integer categoryId,
                                           LocalDateTime dateFrom,
                                           LocalDateTime dateTo,
                                           Double amountFrom,
                                           Double amountTo,
                                           Integer transactionTypeId) {
        return transactions.stream()
                .filter(t -> t.getAccountId() == accountId)
                .filter(t -> categoryId == null
                        || (t.getCategoryId() != null && t.getCategoryId().equals(categoryId)))
                .filter(t -> dateFrom == null
                        || !t.getCompleteDate().isBefore(dateFrom))
                .filter(t -> dateTo == null
                        || !t.getCompleteDate().isAfter(dateTo))
                .filter(t -> amountFrom == null
                        || t.getAmount() >= amountFrom)
                .filter(t -> amountTo == null
                        || t.getAmount() <= amountTo)
                .filter(t -> transactionTypeId == null
                        || t.getTransactionTypeId() == transactionTypeId)
                .sorted((a, b) -> b.getCompleteDate().compareTo(a.getCompleteDate()))
                .collect(Collectors.toList());
    }

    public List<Transaction> findAll() { return new ArrayList<>(transactions); }

    @Override
    public Optional<Transaction> findPairedTransfer(int excludeAccountId,
                                                    double amount,
                                                    LocalDateTime createdAt) {
        return transactions.stream()
                .filter(t -> t.getAccountId() != excludeAccountId)
                .filter(t -> t.getAmount() == amount)
                .filter(t -> t.getCreatedAt().equals(createdAt))
                .filter(t -> t.getCategoryId() != null &&
                        (t.getCategoryId() == Transaction.CATEGORY_SAVING ||
                                t.getCategoryId() == Transaction.CATEGORY_SAVING_EXPENSE ||
                                t.getCategoryId() == Transaction.CATEGORY_TRANSFER))
                .findFirst();
    }

    @Override
    public boolean existsByCategoryId(int categoryId) {
        return transactions.stream()
                .anyMatch(t -> t.getCategoryId() != null
                        && t.getCategoryId() == categoryId);
    }
}
