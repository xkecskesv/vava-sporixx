package sk.sporixx.repository;

import sk.sporixx.model.Transaction;

import java.util.List;

public interface TransactionRepository {

    boolean save(Transaction transaction);

    boolean update(Transaction transaction);

    boolean deleteById(Long id);

    Transaction findById(Long id);

    List<Transaction> findByUserIdAndMonth(Long userId, String month);

    List<Transaction> search(Long userId, String query);
}