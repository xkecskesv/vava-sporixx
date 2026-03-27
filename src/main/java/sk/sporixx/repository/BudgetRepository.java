package sk.sporixx.repository;

import sk.sporixx.model.UserBudget;

import java.util.List;

public interface BudgetRepository {

    boolean save(UserBudget userBudget);

    boolean update(UserBudget userBudget);

    boolean deleteById(Long id);

    UserBudget findById(Long id);

    List<UserBudget> findByUserId(Long userId);

    List<UserBudget> findByUserIdAndMonth(Long userId, String month);
}