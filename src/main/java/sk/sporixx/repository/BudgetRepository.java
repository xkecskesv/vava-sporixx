package sk.sporixx.repository;

import sk.sporixx.model.Budget;

import java.util.List;

public interface BudgetRepository {

    boolean save(Budget budget);

    boolean update(Budget budget);

    boolean deleteById(Long id);

    Budget findById(Long id);

    List<Budget> findByUserId(Long userId);

    List<Budget> findByUserIdAndMonth(Long userId, String month);
}