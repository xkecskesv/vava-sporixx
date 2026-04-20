package sk.sporixx.service.testovanie;

import sk.sporixx.model.Budget;
import sk.sporixx.repository.BudgetRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class InMemoryBudgetRepository implements BudgetRepository {

    private final List<Budget> budgets = new ArrayList<>();
    private final AtomicInteger idGenerator = new AtomicInteger(0);

    @Override
    public Optional<Budget> findByUserId(int userId) {
        return budgets.stream()
                .filter(b -> b.getUserId() == userId && b.isActive())
                .findFirst();
    }

    @Override
    public Budget save(Budget budget) {
        if (budget.getId() == 0) {
            budget.setId(idGenerator.incrementAndGet());
        }
        budgets.add(budget);
        return budget;
    }

    @Override
    public void update(Budget budget) {
        budgets.removeIf(b -> b.getId() == budget.getId());
        budgets.add(budget);
    }

    public List<Budget> findAll() {
        return new ArrayList<>(budgets);
    }
}