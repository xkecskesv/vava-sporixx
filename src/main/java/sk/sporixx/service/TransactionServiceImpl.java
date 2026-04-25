package sk.sporixx.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sk.sporixx.dto.SearchCriteria;
import sk.sporixx.model.Account;
import sk.sporixx.model.SavingGoal;
import sk.sporixx.model.Transaction;
import sk.sporixx.repository.AccountRepository;
import sk.sporixx.repository.CategoryRepository;
import sk.sporixx.repository.SavingGoalRepository;
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
 * Transakcie sú výhradne TYPE_INCOME alebo TYPE_EXPENSE.
 * Transfer medzi účtami vytvorí dve transakcie s automaticky priradenou
 * systémovou kategóriou (CATEGORY_SAVING alebo CATEGORY_SAVING_EXPENSE).
 * Pri každej zmene transakcie sa aktualizuje zostatok účtu
 * v DB (AccountRepository) aj v SessionManager.
 */
public class TransactionServiceImpl implements TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionServiceImpl.class);

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final SavingGoalRepository savingGoalRepository;

    public TransactionServiceImpl(TransactionRepository transactionRepository,
                                  AccountRepository accountRepository,
                                  CategoryRepository categoryRepository,
                                  SavingGoalRepository savingGoalRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.savingGoalRepository = savingGoalRepository;
    }

    //  NAČÍTANIE
    @Override
    public List<Transaction> getTransactions(int accountId) {
        logger.info("Loading transactions for accountId: {}", accountId);
        try {
            return transactionRepository.findByAccountId(accountId);
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
                all.addAll(transactionRepository.findByAccountId(accountId));
            }

            all.sort((a, b) -> {
                int dateCompare = b.getCompleteDate().compareTo(a.getCompleteDate());
                if (dateCompare != 0) return dateCompare;
                return b.getCreatedAt().compareTo(a.getCreatedAt());
            });
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
            List<Transaction> filtered = transactionRepository.findByFilters(
                    accountId,
                    criteria.getCategoryId(),
                    criteria.getDateFrom(),
                    criteria.getDateTo(),
                    criteria.getAmountFrom(),
                    criteria.getAmountTo(),
                    criteria.getTransactionTypeId());

            if (criteria.getSearchText() == null || criteria.getSearchText().isBlank()) {
                return filtered;
            }

            Pattern pattern;
            try {
                pattern = Pattern.compile(
                        criteria.getSearchText(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            } catch (PatternSyntaxException e) {
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

            all.sort((a, b) -> {
                int dateCompare = b.getCompleteDate().compareTo(a.getCompleteDate());
                if (dateCompare != 0) return dateCompare;
                return b.getCreatedAt().compareTo(a.getCreatedAt());
            });
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

        Account account = getAccountOrThrow(accountId);

        // Validácia kategórie — len pre štandardné transakcie, nie pre transfer
        if (targetAccountId == null && categoryRepository.findById(categoryId).isEmpty()) {
            throw new TransactionException("transaction.error.invalid_category");
        }

        try {
            LocalDateTime completeDate = date.atTime(
                    LocalDateTime.now().getHour(),
                    LocalDateTime.now().getMinute());

            if (targetAccountId != null) {
                return addTransfer(account, targetAccountId, description,
                        amount, currencyCode, completeDate);
            }

            return addStandardTransaction(account, transactionTypeId, categoryId,
                    spendingClassificationId, description, amount, currencyCode, completeDate);

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

        double newBalance = calculateNewBalance(
                account.getCurrentBalance(), transactionTypeId, amount);
        updateBalance(account, newBalance);

        logger.info("Transaction added: id={}, type={}, amount={}",
                saved.getId(), transactionTypeId, amount);
        return saved;
    }

    // ── Helper — prevod medzi účtami ──
    // Kategória sa určí automaticky podľa smeru prevodu:
    //   main → saving = CATEGORY_SAVING
    //   saving → main = CATEGORY_SAVING_EXPENSE
    private Transaction addTransfer(Account fromAccount,
                                    int targetAccountId,
                                    String description,
                                    double amount,
                                    String currencyCode,
                                    LocalDateTime completeDate) {
        Account toAccount = getAccountOrThrow(targetAccountId);

        // Kategória len pri transferoch kde je zapojený saving účet
        Integer autoCategory = Transaction.CATEGORY_TRANSFER;
        if (toAccount.isSavingAccount()) {
            autoCategory = Transaction.CATEGORY_SAVING;
        } else if (fromAccount.isSavingAccount()) {
            autoCategory = Transaction.CATEGORY_SAVING_EXPENSE;
        }

        LocalDateTime createdAt = LocalDateTime.now();

        Transaction expense = Transaction.builder()
                .accountId(fromAccount.getId())
                .transactionTypeId(Transaction.TYPE_EXPENSE)
                .categoryId(autoCategory)  // môže byť null
                .spendingClassificationId(null)
                .description(description)
                .amount(amount)
                .currencyCode(currencyCode)
                .completeDate(completeDate)
                .createdAt(createdAt)
                .build();
        transactionRepository.save(expense);

        Transaction income = Transaction.builder()
                .accountId(toAccount.getId())
                .transactionTypeId(Transaction.TYPE_INCOME)
                .categoryId(autoCategory)  // môže byť null
                .spendingClassificationId(null)
                .description(description)
                .amount(amount)
                .currencyCode(currencyCode)
                .completeDate(completeDate)
                .createdAt(createdAt)
                .build();
        transactionRepository.save(income);

        updateBalance(fromAccount, fromAccount.getCurrentBalance() - amount);
        updateBalance(toAccount, toAccount.getCurrentBalance() + amount);

        // Aktualizuj currentAmount v SavingGoal ak je zapojený saving účet
        if (toAccount.isSavingAccount()) {
            updateSavingGoalAmount(toAccount.getId(), amount);
        } else if (fromAccount.isSavingAccount()) {
            updateSavingGoalAmount(fromAccount.getId(), -amount);
        }

        logger.info("Transfer added: from={}, to={}, amount={}",
                fromAccount.getId(), toAccount.getId(), amount);
        return expense;
    }

    //  AKTUALIZÁCIA TRANSAKCIE
    @Override
    public void updateTransaction(Transaction updatedTransaction) {
        logger.info("Updating transaction id={}", updatedTransaction.getId());

        Optional<Transaction> originalOpt = transactionRepository
                .findById(updatedTransaction.getId());
        if (originalOpt.isEmpty()) {
            throw new TransactionException("transaction.error.not_found");
        }

        Transaction original = originalOpt.get();

        if (updatedTransaction.getAmount() <= 0) {
            throw new TransactionException("transaction.error.invalid_amount");
        }
        if (updatedTransaction.getDescription() == null
                || updatedTransaction.getDescription().isBlank()) {
            throw new TransactionException("transaction.error.description_required");
        }
        if (updatedTransaction.getCompleteDate().isAfter(LocalDateTime.now())) {
            throw new TransactionException("transaction.error.future_date");
        }
        if (updatedTransaction.getTransactionTypeId() != original.getTransactionTypeId()) {
            throw new TransactionException("transaction.error.type_cannot_change");
        }

        // Systémové kategórie (Saving, Saving Expense) sa nedajú manuálne nastaviť pri editácii
        Integer newCategoryId = updatedTransaction.getCategoryId();
        if (newCategoryId != null &&
                (newCategoryId == Transaction.CATEGORY_SAVING
                        || newCategoryId == Transaction.CATEGORY_SAVING_EXPENSE)) {
            if (!newCategoryId.equals(original.getCategoryId())) {
                throw new TransactionException("transaction.error.invalid_category");
            }
        }

        try {
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

            if (original.getCategoryId() != null &&
                    (original.getCategoryId() == Transaction.CATEGORY_SAVING ||
                            original.getCategoryId() == Transaction.CATEGORY_SAVING_EXPENSE ||
                            original.getCategoryId() == Transaction.CATEGORY_TRANSFER)) {

                transactionRepository.findPairedTransfer(
                        original.getAccountId(),
                        original.getAmount(),
                        original.getCreatedAt()
                ).ifPresent(paired -> {
                    double pairedDiff = updatedTransaction.getAmount() - original.getAmount();
                    paired.setAmount(updatedTransaction.getAmount());
                    paired.setDescription(updatedTransaction.getDescription());
                    paired.setCompleteDate(updatedTransaction.getCompleteDate());
                    transactionRepository.update(paired);

                    Account pairedAccount = getAccountOrThrow(paired.getAccountId());
                    double pairedNewBalance;
                    if (paired.isIncome()) {
                        pairedNewBalance = pairedAccount.getCurrentBalance() + pairedDiff;
                    } else {
                        pairedNewBalance = pairedAccount.getCurrentBalance() - pairedDiff;
                    }
                    updateBalance(pairedAccount, pairedNewBalance);

                    if (pairedAccount.isSavingAccount()) {
                        double goalDelta = paired.isIncome() ? pairedDiff : -pairedDiff;
                        updateSavingGoalAmount(pairedAccount.getId(), goalDelta);
                    }

                    logger.info("Paired transfer transaction updated: id={}", paired.getId());
                });
            }

            if (account.isSavingAccount()) {
                double goalDelta = original.isIncome() ? difference : -difference;
                updateSavingGoalAmount(account.getId(), goalDelta);
            }

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

                double revertedBalance = revertBalance(
                        account.getCurrentBalance(),
                        tx.getTransactionTypeId(),
                        tx.getAmount());
                updateBalance(account, revertedBalance);

                // Aktualizuj SavingGoal ak je to saving účet
                if (account.isSavingAccount()) {
                    if (tx.isIncome()) {
                        updateSavingGoalAmount(account.getId(), -tx.getAmount());
                    } else {
                        updateSavingGoalAmount(account.getId(), tx.getAmount());
                    }
                }

                transactionRepository.deleteById(id);
                logger.info("Transaction deleted: id={}, amount={}", id, tx.getAmount());

                if (tx.getCategoryId() != null &&
                        (tx.getCategoryId() == Transaction.CATEGORY_SAVING ||
                                tx.getCategoryId() == Transaction.CATEGORY_SAVING_EXPENSE ||
                                tx.getCategoryId() == Transaction.CATEGORY_TRANSFER)) {

                    transactionRepository.findPairedTransfer(
                            tx.getAccountId(),
                            tx.getAmount(),
                            tx.getCreatedAt()
                    ).ifPresent(paired -> {
                        Account pairedAccount = getAccountOrThrow(paired.getAccountId());
                        double revertedPairedBalance = revertBalance(
                                pairedAccount.getCurrentBalance(),
                                paired.getTransactionTypeId(),
                                paired.getAmount());
                        updateBalance(pairedAccount, revertedPairedBalance);

                        if (pairedAccount.isSavingAccount()) {
                            if (paired.isIncome()) {
                                updateSavingGoalAmount(pairedAccount.getId(), -paired.getAmount());
                            } else {
                                updateSavingGoalAmount(pairedAccount.getId(), paired.getAmount());
                            }
                        }

                        transactionRepository.deleteById(paired.getId());
                        logger.info("Paired transfer transaction deleted: id={}", paired.getId());
                    });
                }

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
        return currentBalance - amount;
    }

    private double revertBalance(double currentBalance,
                                 int transactionTypeId,
                                 double amount) {
        if (transactionTypeId == Transaction.TYPE_INCOME) {
            return currentBalance - amount;
        }
        return currentBalance + amount;
    }

    private void updateBalance(Account account, double newBalance) {
        accountRepository.updateBalance(account.getId(), newBalance);
        account.setCurrentBalance(newBalance);
    }

    private void updateSavingGoalAmount(int accountId, double delta) {
        List<SavingGoal> goals = savingGoalRepository.findActiveByAccountId(accountId);
        if (goals.isEmpty()) return;

        SavingGoal goal = goals.get(0);
        double newAmount = Math.max(0, goal.getCurrentAmount() + delta);
        savingGoalRepository.updateCurrentAmount(goal.getId(), newAmount);
        goal.setCurrentAmount(newAmount);
        logger.info("SavingGoal updated: accountId={}, newCurrentAmount={}",
                accountId, newAmount);
    }
}