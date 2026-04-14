package sk.sporixx.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sk.sporixx.dto.SearchCriteria;
import sk.sporixx.model.Account;
import sk.sporixx.model.Transaction;
import sk.sporixx.repository.AccountRepository;
import sk.sporixx.repository.CategoryRepository;
import sk.sporixx.repository.TransactionRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

/**
 * Implementácia TransactionService.
 * Pri každej zmene transakcie sa aktualizuje zostatok účtu
 * v DB (AccountRepository) aj v SessionManager.
 */
public class TransactionServiceImpl implements TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionServiceImpl.class);

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;

    public TransactionServiceImpl(TransactionRepository transactionRepository,
                                  AccountRepository accountRepository,
                                  CategoryRepository categoryRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
    }

    //  NAČÍTANIE
    @Override
    public List<Transaction> getTransactions(int accountId) {
        logger.info("Loading transactions for accountId: {}", accountId);

        try {
            return transactionRepository.findByAccountIdAndDateRange(
                    accountId,
                    LocalDateTime.of(2000, 1, 1, 0, 0),
                    LocalDateTime.now());
        } catch (Exception e) {
            logger.error("Failed to load transactions for accountId: {}", accountId, e);
            throw new TransactionException("error.db_error", e);
        }
    }

    @Override
    public List<Transaction> getAllTransactions() {
        logger.info("Loading all transactions for current user");

        try {
            List<Integer> accountIds = SessionManager.getInstance().getAccountIds();
            List<Transaction> all = new ArrayList<>();

            for (int accountId : accountIds) {
                all.addAll(transactionRepository.findByAccountIdAndDateRange(
                        accountId,
                        LocalDateTime.of(2000, 1, 1, 0, 0),
                        LocalDateTime.now()));
            }

            // Zoraď od najnovšej
            all.sort((a, b) -> b.getCompleteDate().compareTo(a.getCompleteDate()));
            return all;

        } catch (Exception e) {
            logger.error("Failed to load all transactions", e);
            throw new TransactionException("error.db_error", e);
        }
    }

    //  VYHĽADÁVANIE S REGEX
    @Override
    public List<Transaction> searchTransactions(SearchCriteria criteria, int accountId) {
        logger.info("Searching transactions for accountId: {}", accountId);

        try {
            // Repository filtruje podľa kategórie, dátumu, sumy, typu
            List<Transaction> filtered = transactionRepository.findByFilters(
                    accountId,
                    criteria.getCategoryId(),
                    criteria.getDateFrom(),
                    criteria.getDateTo(),
                    criteria.getAmountFrom(),
                    criteria.getAmountTo(),
                    criteria.getTransactionTypeId());

            // Service aplikuje regex na description
            if (criteria.getSearchText() == null || criteria.getSearchText().isBlank()) {
                return filtered;
            }

            Pattern pattern;
            try {
                pattern = Pattern.compile(
                        criteria.getSearchText(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            } catch (PatternSyntaxException e) {
                // Neplatný regex - plain text search
                logger.warn("Invalid regex '{}', falling back to plain text",
                        criteria.getSearchText());
                String lower = criteria.getSearchText().toLowerCase();
                return filtered.stream()
                        .filter(t -> t.getDescription().toLowerCase().contains(lower))
                        .collect(Collectors.toList());
            }

            Pattern finalPattern = pattern;
            return filtered.stream()
                    .filter(t -> finalPattern.matcher(t.getDescription()).find())
                    .collect(Collectors.toList());

        } catch (TransactionException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to search transactions", e);
            throw new TransactionException("error.db_error", e);
        }
    }

    @Override
    public List<Transaction> searchTransactions(SearchCriteria criteria) {
        logger.info("Searching all transactions with criteria: {}", criteria);

        try {
            List<Integer> accountIds = SessionManager.getInstance().getAccountIds();
            List<Transaction> all = new ArrayList<>();

            for (int accountId : accountIds) {
                all.addAll(searchTransactions(criteria, accountId));
            }

            all.sort((a, b) -> b.getCompleteDate().compareTo(a.getCompleteDate()));
            return all;

        } catch (TransactionException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to search all transactions", e);
            throw new TransactionException("error.db_error", e);
        }
    }

    //  PRIDANIE TRANSAKCIE
    @Override
    public Transaction addTransaction(int accountId,
                                      int transactionTypeId,
                                      Integer targetAccountId,
                                      int categoryId,
                                      Integer spendingClassificationId,
                                      String description,
                                      double amount,
                                      String currencyCode,
                                      LocalDate date) {
        logger.info("Adding transaction: accountId={}, type={}, amount={}",
                accountId, transactionTypeId, amount);

        // Validácia
        if (amount <= 0) {
            throw new TransactionException("transaction.error.invalid_amount");
        }
        if (description == null || description.isBlank()) {
            throw new TransactionException("transaction.error.description_required");
        }
        if (date == null) {
            throw new TransactionException("transaction.error.date_required");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new TransactionException("transaction.error.future_date");
        }

        // Validácia účtu
        Account account = getAccountOrThrow(accountId);

        // Validácia kategórie
        if (categoryRepository.findById(categoryId).isEmpty()) {
            throw new TransactionException("transaction.error.invalid_category");
        }

        try {
            LocalDateTime completeDate = date.atTime(
                    LocalDateTime.now().getHour(),
                    LocalDateTime.now().getMinute());

            if (targetAccountId != null) {
                // Transfer medzi účtami
                return addTransfer(account, targetAccountId, categoryId,
                        spendingClassificationId, description,
                        amount, currencyCode, completeDate);
            }

            // Štandardná transakcia
            return addStandardTransaction(account, transactionTypeId, categoryId,
                    spendingClassificationId, description,
                    amount, currencyCode, completeDate);

        } catch (TransactionException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to add transaction", e);
            throw new TransactionException("error.db_error", e);
        }
    }

    // ── Helper — štandardná transakcia (Income/Expense) ──
    private Transaction addStandardTransaction(Account account,
                                               int transactionTypeId,
                                               int categoryId,
                                               Integer spendingClassificationId,
                                               String description,
                                               double amount,
                                               String currencyCode,
                                               LocalDateTime completeDate) {
        Transaction transaction = Transaction.builder()
                .accountId(account.getId())
                .transactionTypeId(transactionTypeId)
                .categoryId(categoryId)
                .spendingClassificationId(spendingClassificationId)
                .description(description)
                .amount(amount)
                .currencyCode(currencyCode)
                .completeDate(completeDate)
                .createdAt(LocalDateTime.now())
                .build();

        Transaction saved = transactionRepository.save(transaction);

        // Aktualizuj balance
        double newBalance = calculateNewBalance(
                account.getCurrentBalance(), transactionTypeId, amount);
        updateBalance(account, newBalance);

        logger.info("Transaction added: id={}, type={}, amount={}",
                saved.getId(), transactionTypeId, amount);
        return saved;
    }

    // ── Helper — prevod medzi účtami ──
    private Transaction addTransfer(Account fromAccount,
                                    int targetAccountId,
                                    int categoryId,
                                    Integer spendingClassificationId,
                                    String description,
                                    double amount,
                                    String currencyCode,
                                    LocalDateTime completeDate) {
        Account toAccount = getAccountOrThrow(targetAccountId);

        // Expense na zdrojovom účte
        Transaction expense = Transaction.builder()
                .accountId(fromAccount.getId())
                .transactionTypeId(Transaction.TYPE_EXPENSE)
                .categoryId(categoryId)
                .spendingClassificationId(spendingClassificationId)
                .description(description)
                .amount(amount)
                .currencyCode(currencyCode)
                .completeDate(completeDate)
                .createdAt(LocalDateTime.now())
                .build();
        transactionRepository.save(expense);

        // Income na cieľovom účte
        Transaction income = Transaction.builder()
                .accountId(toAccount.getId())
                .transactionTypeId(Transaction.TYPE_INCOME)
                .categoryId(categoryId)
                .spendingClassificationId(null)
                .description(description)
                .amount(amount)
                .currencyCode(currencyCode)
                .completeDate(completeDate)
                .createdAt(LocalDateTime.now())
                .build();
        transactionRepository.save(income);

        // Aktualizuj balance oboch účtov
        updateBalance(fromAccount, fromAccount.getCurrentBalance() - amount);
        updateBalance(toAccount, toAccount.getCurrentBalance() + amount);

        logger.info("Transfer added: from={}, to={}, amount={}",
                fromAccount.getId(), toAccount.getId(), amount);
        return expense;
    }

    //  AKTUALIZÁCIA TRANSAKCIE
    @Override
    public void updateTransaction(Transaction updatedTransaction) {
        logger.info("Updating transaction id={}", updatedTransaction.getId());

        // Načítaj pôvodnú transakciu
        Optional<Transaction> originalOpt = transactionRepository
                .findById(updatedTransaction.getId());
        if (originalOpt.isEmpty()) {
            throw new TransactionException("transaction.error.not_found");
        }

        Transaction original = originalOpt.get();

        // Validácia
        if (updatedTransaction.getAmount() <= 0) {
            throw new TransactionException("transaction.error.invalid_amount");
        }
        if (updatedTransaction.getCompleteDate().isAfter(LocalDateTime.now())) {
            throw new TransactionException("transaction.error.future_date");
        }

        try {
            // Vypočítaj rozdiel súm a aktualizuj balance
            Account account = getAccountOrThrow(original.getAccountId());
            double difference = updatedTransaction.getAmount() - original.getAmount();

            double newBalance;
            if (original.getTransactionTypeId() == Transaction.TYPE_INCOME) {
                newBalance = account.getCurrentBalance() + difference;
            } else {
                newBalance = account.getCurrentBalance() - difference;
            }

            transactionRepository.update(updatedTransaction);
            updateBalance(account, newBalance);

            logger.info("Transaction updated: id={}, oldAmount={}, newAmount={}",
                    original.getId(), original.getAmount(), updatedTransaction.getAmount());

        } catch (TransactionException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to update transaction id={}", updatedTransaction.getId(), e);
            throw new TransactionException("error.db_error", e);
        }
    }

    //  MAZANIE TRANSAKCIÍ
    @Override
    public void deleteTransactions(List<Integer> transactionIds) {
        logger.info("Deleting {} transactions", transactionIds.size());

        if (transactionIds.isEmpty()) {
            throw new TransactionException("transaction.error.no_selection");
        }

        List<String> errors = new ArrayList<>();

        for (int id : transactionIds) {
            try {
                Optional<Transaction> txOpt = transactionRepository.findById(id);
                if (txOpt.isEmpty()) {
                    logger.warn("Transaction id={} not found, skipping", id);
                    continue;
                }

                Transaction tx = txOpt.get();
                Account account = getAccountOrThrow(tx.getAccountId());

                // Revertuj balance
                double revertedBalance = revertBalance(
                        account.getCurrentBalance(),
                        tx.getTransactionTypeId(),
                        tx.getAmount());
                updateBalance(account, revertedBalance);

                transactionRepository.deleteById(id);
                logger.info("Transaction deleted: id={}, amount={}", id, tx.getAmount());

            } catch (Exception e) {
                logger.error("Failed to delete transaction id={}", id, e);
                errors.add(String.valueOf(id));
            }
        }

        if (!errors.isEmpty()) {
            throw new TransactionException("transaction.error.delete_failed");
        }
    }

    //  HELPERS
    private Account getAccountOrThrow(int accountId) {
        Account account = SessionManager.getInstance().getAccountById(accountId);
        if (account == null) {
            throw new TransactionException("transaction.error.account_not_found");
        }
        return account;
    }

    private double calculateNewBalance(double currentBalance,
                                       int transactionTypeId,
                                       double amount) {
        if (transactionTypeId == Transaction.TYPE_INCOME) {
            return currentBalance + amount;
        }
        if (transactionTypeId == Transaction.TYPE_EXPENSE) {
            return currentBalance - amount;
        }
        logger.warn("Unknown transactionTypeId: {}, treating as expense", transactionTypeId);
        return currentBalance - amount;
    }

    private double revertBalance(double currentBalance,
                                 int transactionTypeId,
                                 double amount) {
        if (transactionTypeId == Transaction.TYPE_INCOME) {
            return currentBalance - amount;
        }
        if (transactionTypeId == Transaction.TYPE_EXPENSE) {
            return currentBalance + amount;
        }
        logger.warn("Unknown transactionTypeId: {}, treating as expense revert", transactionTypeId);
        return currentBalance + amount;
    }

    private void updateBalance(Account account, double newBalance) {
        accountRepository.updateBalance(account.getId(), newBalance);
        account.setCurrentBalance(newBalance);
    }
}