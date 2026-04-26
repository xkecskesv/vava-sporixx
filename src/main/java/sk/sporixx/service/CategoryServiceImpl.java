package sk.sporixx.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sk.sporixx.model.Category;
import sk.sporixx.model.Transaction;
import sk.sporixx.repository.CategoryRepository;
import sk.sporixx.repository.TransactionRepository;
import sk.sporixx.util.ValidationUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Implementácia CategoryService.
 * Spravuje systémové aj používateľské kategórie.
 * Systémové kategórie (userId = null) sa nedajú upravovať ani mazať.
 * Saving a Saving Expense sú systémové a skryté pred používateľom —
 * priraďujú sa automaticky pri transferoch medzi účtami.
 * Investment je systémová ale viditeľná — používateľ si ju môže vybrať.
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
     * Používa sa napr. pri filtrovaní v Transactions screene.
     */
    @Override
    public List<Category> getCategories() {
        int userId = SessionManager.getInstance().getCurrentUserId();
        logger.info("Loading categories for userId: {}", userId);

        try {
            return categoryRepository.findByUserIdOrSystem(userId);
        } catch (Exception e) {
            logger.error("Failed to load categories for userId: {}", userId, e);
            throw new CategoryException("error.db_error", e);
        }
    }

    /**
     * Kategórie dostupné používateľovi pri pridávaní/editácii transakcie.
     * Vylučuje Saving a Saving Expense — tie sa priraďujú automaticky pri transferoch.
     * Investment je zahrnutý — používateľ si ho môže vybrať manuálne.
     */
    @Override
    public List<Category> getSelectableCategories() {
        int userId = SessionManager.getInstance().getCurrentUserId();
        logger.info("Loading selectable categories for userId: {}", userId);

        try {
            return categoryRepository.findByUserIdOrSystem(userId).stream()
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
    public Category addCategory(String name) {
        logger.info("Adding category: {}", name);

        if (!ValidationUtil.isNotBlank(name)) {
            throw new CategoryException("category.error.name_required");
        }

        int userId = SessionManager.getInstance().getCurrentUserId();

        List<Category> existing = categoryRepository.findByUserIdOrSystem(userId);
        boolean duplicate = existing.stream()
                .anyMatch(c -> c.getName().equalsIgnoreCase(name.trim()));

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
            return saved;

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
        boolean duplicate = existing.stream()
                .filter(c -> c.getId() != categoryId)
                .anyMatch(c -> c.getName().equalsIgnoreCase(newName.trim()));

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

        try {
            if (transactionRepository.existsByCategoryId(categoryId)) {
                throw new CategoryException("category.error.in_use");
            }
            categoryRepository.deleteById(categoryId);
            logger.info("Category deleted: id={}", categoryId);

        } catch (Exception e) {
            logger.error("Failed to delete category id={}", categoryId, e);
            throw new CategoryException("error.db_error", e);
        }
    }
}