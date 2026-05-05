package sk.sporixx.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sk.sporixx.model.Category;
import sk.sporixx.model.Transaction;
import sk.sporixx.repository.CategoryRepository;
import sk.sporixx.repository.TransactionRepository;
import sk.sporixx.util.Localization;
import sk.sporixx.util.ValidationUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Implementácia CategoryService.
 * Spravuje systémové aj používateľské kategórie.
 * Systémové kategórie (userId = null) sa nedajú upravovať ani mazať.
 */
public class CategoryServiceImpl implements CategoryService {

    private static final Logger logger = LoggerFactory.getLogger(CategoryServiceImpl.class);

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository,
                               TransactionRepository transactionRepository) {
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Všetky kategórie prihláseného používateľa vrátane systémových.
     */
    @Override
    public List<Category> getCategories() {
        int userId = SessionManager.getInstance().getCurrentUserId();
        logger.info("Loading categories for userId: {}", userId);

        try {
            return categoryRepository.findByUserIdOrSystem(userId).stream()
                    .map(this::localizeIfSystem)
                    .toList();
        } catch (Exception e) {
            logger.error("Failed to load categories for userId: {}", userId, e);
            throw new CategoryException("error.db_error", e);
        }
    }

    /**
     * Kategórie dostupné používateľovi pri pridávaní/editácii transakcie.
     */
    @Override
    public List<Category> getSelectableCategories() {
        int userId = SessionManager.getInstance().getCurrentUserId();
        logger.info("Loading selectable categories for userId: {}", userId);

        try {
            return categoryRepository.findByUserIdOrSystem(userId).stream()
                    .map(this::localizeIfSystem)
                    .filter(c -> c.getId() != Transaction.CATEGORY_SAVING
                            && c.getId() != Transaction.CATEGORY_SAVING_EXPENSE
                            && c.getId() != Transaction.CATEGORY_TRANSFER)
                    .toList();
        } catch (Exception e) {
            logger.error("Failed to load selectable categories for userId: {}", userId, e);
            throw new CategoryException("error.db_error", e);
        }
    }

    @Override
    public void addCategory(String name) {
        logger.info("Adding category: {}", name);

        if (!ValidationUtil.isNotBlank(name)) {
            throw new CategoryException("category.error.name_required");
        }

        int userId = SessionManager.getInstance().getCurrentUserId();

        List<Category> existing = categoryRepository.findByUserIdOrSystem(userId);
        String trimmedName = name.trim();
        boolean duplicate = existing.stream()
                .anyMatch(c -> matchesCategoryName(c, trimmedName));

        if (duplicate) {
            logger.warn("Category already exists: {}", name);
            throw new CategoryException("category.error.already_exists");
        }

        try {
            Category category = Category.builder()
                    .userId(userId)
                    .name(name.trim())
                    .createdAt(LocalDateTime.now())
                    .build();

            Category saved = categoryRepository.save(category);
            logger.info("Category created: id={}, name={}", saved.getId(), saved.getName());

        } catch (Exception e) {
            logger.error("Failed to save category: {}", name, e);
            throw new CategoryException("error.db_error", e);
        }
    }

    @Override
    public void updateCategory(int categoryId, String newName) {
        logger.info("Updating category id={} to name={}", categoryId, newName);

        if (!ValidationUtil.isNotBlank(newName)) {
            throw new CategoryException("category.error.name_required");
        }

        Optional<Category> categoryOpt = categoryRepository.findById(categoryId);
        if (categoryOpt.isEmpty()) {
            throw new CategoryException("category.error.not_found");
        }

        Category category = categoryOpt.get();

        if (category.isSystemCategory()) {
            logger.warn("Cannot update system category id={}", categoryId);
            throw new CategoryException("category.error.cannot_modify_system");
        }

        int userId = SessionManager.getInstance().getCurrentUserId();
        if (category.getUserId() != userId) {
            logger.warn("User {} tried to update category {} owned by {}",
                    userId, categoryId, category.getUserId());
            throw new CategoryException("category.error.not_found");
        }

        List<Category> existing = categoryRepository.findByUserIdOrSystem(userId);
        String trimmedNewName = newName.trim();
        boolean duplicate = existing.stream()
                .filter(c -> c.getId() != categoryId)
                .anyMatch(c -> matchesCategoryName(c, trimmedNewName));

        if (duplicate) {
            throw new CategoryException("category.error.already_exists");
        }

        try {
            category.setName(newName.trim());
            categoryRepository.update(category);
            logger.info("Category updated: id={}, newName={}", categoryId, newName);

        } catch (Exception e) {
            logger.error("Failed to update category id={}", categoryId, e);
            throw new CategoryException("error.db_error", e);
        }
    }

    @Override
    public void deleteCategory(int categoryId) {
        logger.info("Deleting category id={}", categoryId);

        Optional<Category> categoryOpt = categoryRepository.findById(categoryId);
        if (categoryOpt.isEmpty()) {
            throw new CategoryException("category.error.not_found");
        }

        Category category = categoryOpt.get();

        if (category.isSystemCategory()) {
            logger.warn("Cannot delete system category id={}", categoryId);
            throw new CategoryException("category.error.cannot_modify_system");
        }

        int userId = SessionManager.getInstance().getCurrentUserId();
        if (category.getUserId() != userId) {
            logger.warn("User {} tried to delete category {} owned by {}",
                    userId, categoryId, category.getUserId());
            throw new CategoryException("category.error.not_found");
        }

        if (transactionRepository.existsByCategoryId(categoryId)) {
            throw new CategoryException("category.error.in_use");
        }

        try {
            categoryRepository.deleteById(categoryId);
            logger.info("Category deleted: id={}", categoryId);

        } catch (Exception e) {
            logger.error("Failed to delete category id={}", categoryId, e);
            throw new CategoryException("error.db_error", e);
        }
    }

    private boolean matchesCategoryName(Category category, String input) {
        if (category.getName().equalsIgnoreCase(input)) return true;
        if (category.isSystemCategory()) {
            String key = "category.system." + category.getName().toLowerCase().replace(" ", "_");
            try {
                return Localization.get(key).equalsIgnoreCase(input);
            } catch (Exception ignored) {}
        }
        return false;
    }

    private Category localizeIfSystem(Category category) {
        if (!category.isSystemCategory()) return category;
        String key = "category.system." + category.getName().toLowerCase()
                .replace(" ", "_");
        try {
            String localized = Localization.get(key);
            category.setName(localized);
        } catch (Exception ignored) {}
        return category;
    }
}