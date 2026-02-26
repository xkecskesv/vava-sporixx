package sk.sporixx.service;

import sk.sporixx.model.Transaction;

import java.util.List;

public interface TransactionService {

    boolean addTransaction(Transaction transaction);

    boolean updateTransaction(Transaction transaction);

    boolean deleteTransaction(Long id);

    Transaction getTransactionById(Long id);

    List<Transaction> getTransactionsByMonth(Long userId, String month);

    List<Transaction> searchTransactions(Long userId, String query);
}