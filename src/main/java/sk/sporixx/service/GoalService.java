package sk.sporixx.service;

import sk.sporixx.model.Goal;

import java.util.List;

public interface GoalService {

    boolean addGoal(Goal goal);

    boolean updateGoal(Goal goal);

    boolean deleteGoal(Long id);

    Goal getGoalById(Long id);

    List<Goal> getGoalsByUserId(Long userId);

    List<Goal> searchGoals(Long userId, String query);
}