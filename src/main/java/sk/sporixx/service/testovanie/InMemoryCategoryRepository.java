package sk.sporixx.service.testovanie;

import sk.sporixx.model.Category;
import sk.sporixx.repository.CategoryRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class InMemoryCategoryRepository implements CategoryRepository {

    private final List<Category> categories = new ArrayList<>();
    private final AtomicInteger idGenerator = new AtomicInteger(0);

    public InMemoryCategoryRepository() {

    }

    @Override
    public List<Category> findByUserIdOrSystem(int userId) {
        return categories.stream()
                .filter(c -> c.getUserId() == null || c.getUserId() == userId)
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Category> findById(int categoryId) {
        return categories.stream()
                .filter(c -> c.getId() == categoryId)
                .findFirst();
    }

    @Override
    public Category save(Category category) {
        if (category.getId() == 0) {
            category.setId(idGenerator.incrementAndGet());
        }
        categories.add(category);
        return category;
    }

    @Override
    public void update(Category category) {
        categories.removeIf(c -> c.getId() == category.getId());
        categories.add(category);
    }

    @Override
    public void deleteById(int categoryId) {
        categories.removeIf(c -> c.getId() == categoryId);
    }

    public List<Category> findAll() {
        return new ArrayList<>(categories);
    }
}
