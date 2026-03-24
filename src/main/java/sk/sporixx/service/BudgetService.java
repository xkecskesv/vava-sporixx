package sk.sporixx.service;

import sk.sporixx.model.UserBudget;

import java.util.List;

public interface BudgetService {

    boolean addBudget(UserBudget userBudget);

    boolean updateBudget(UserBudget userBudget);

    boolean deleteBudget(Long id);

    UserBudget getBudgetById(Long id);

    List<UserBudget> getBudgetsByUserId(Long userId);

    List<UserBudget> getBudgetsByMonth(Long userId, String month);
}