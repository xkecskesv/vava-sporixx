package sk.sporixx.service.testovanie;

import sk.sporixx.model.SavingGoal;
import sk.sporixx.repository.SavingGoalRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class InMemorySavingGoalRepository implements SavingGoalRepository {

    private final List<SavingGoal> goals = new ArrayList<>();
    private final AtomicInteger idGenerator = new AtomicInteger(0);

    @Override
    public List<SavingGoal> findActiveByAccountId(int accountId) {
        return goals.stream()
                .filter(g -> g.getAccountId() == accountId && g.isActive())
                .collect(Collectors.toList());
    }

    @Override
    public List<SavingGoal> findActiveByAccountIds(List<Integer> accountIds) {
        return goals.stream()
                .filter(g -> accountIds.contains(g.getAccountId()) && g.isActive())
                .collect(Collectors.toList());
    }

    @Override
    public Optional<SavingGoal> findById(int id) {
        return goals.stream().filter(g -> g.getId() == id).findFirst();
    }

    @Override
    public SavingGoal save(SavingGoal savingGoal) {
        if (savingGoal.getId() == 0) { savingGoal.setId(idGenerator.incrementAndGet()); }
        goals.add(savingGoal);
        return savingGoal;
    }

    @Override
    public void updateCurrentAmount(int goalId, double currentAmount) {
        goals.stream().filter(g -> g.getId() == goalId).findFirst()
                .ifPresent(g -> g.setCurrentAmount(currentAmount));
    }

    @Override
    public void updateTargetAmount(int goalId, double targetAmount) {
        goals.stream().filter(g -> g.getId() == goalId).findFirst()
                .ifPresent(g -> g.setTargetAmount(targetAmount));
    }

    @Override
    public void updateTargetDate(int goalId, LocalDateTime targetDate) {
        goals.stream().filter(g -> g.getId() == goalId).findFirst()
                .ifPresent(g -> g.setTargetDate(targetDate));
    }

    public List<SavingGoal> findAll() { return new ArrayList<>(goals); }
}

