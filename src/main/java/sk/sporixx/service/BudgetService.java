package sk.sporixx.service;

import sk.sporixx.model.Budget;

import java.util.List;

public interface BudgetService {

    boolean addBudget(Budget budget);

    boolean updateBudget(Budget budget);

    boolean deleteBudget(Long id);

    Budget getBudgetById(Long id);

    List<Budget> getBudgetsByUserId(Long userId);

    List<Budget> getBudgetsByMonth(Long userId, String month);
}