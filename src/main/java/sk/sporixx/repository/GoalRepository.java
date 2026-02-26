package sk.sporixx.repository;

import sk.sporixx.model.Goal;

import java.util.List;

public interface GoalRepository {

    boolean save(Goal goal);

    boolean update(Goal goal);

    boolean deleteById(Long id);

    Goal findById(Long id);

    List<Goal> findByUserId(Long userId);

    List<Goal> search(Long userId, String query);
}