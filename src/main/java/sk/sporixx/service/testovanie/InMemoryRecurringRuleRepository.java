package sk.sporixx.service.testovanie;

import sk.sporixx.model.RecurringRule;
import sk.sporixx.repository.RecurringRuleRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class InMemoryRecurringRuleRepository implements RecurringRuleRepository {

    private final List<RecurringRule> rules = new ArrayList<>();
    private final AtomicInteger idGenerator = new AtomicInteger(0);

    @Override
    public List<RecurringRule> findActiveByAccountId(int accountId) {
        return rules.stream()
                .filter(r -> r.getAccountId() == accountId && r.getIsActive() == 1)
                .sorted((a, b) -> a.getNextDueDate().compareTo(b.getNextDueDate()))
                .collect(Collectors.toList());
    }

    @Override
    public List<RecurringRule> findUpcomingByAccountId(int accountId, LocalDateTime from, int limit) {
        return rules.stream()
                .filter(r -> r.getAccountId() == accountId && r.getIsActive() == 1) // Len 1 účet
                .filter(r -> !r.getNextDueDate().isBefore(from))
                .sorted((a, b) -> a.getNextDueDate().compareTo(b.getNextDueDate()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public RecurringRule save(RecurringRule rule) {
        if (rule.getId() == 0) { rule.setId(idGenerator.incrementAndGet()); }
        rules.add(rule);
        return rule;
    }

    @Override
    public void updateNextDueDate(int ruleId, LocalDateTime nextDueDate, int generatedCount) {
        rules.stream().filter(r -> r.getId() == ruleId).findFirst().ifPresent(r -> {
            r.setNextDueDate(nextDueDate);
            r.setGeneratedCount(generatedCount);
        });
    }

    public List<RecurringRule> findAll() { return new ArrayList<>(rules); }
}
