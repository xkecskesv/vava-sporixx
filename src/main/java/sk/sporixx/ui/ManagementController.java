package sk.sporixx.ui;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import sk.sporixx.model.Account;
import sk.sporixx.model.Category;
import sk.sporixx.service.ServiceLocator;
import sk.sporixx.service.SessionManager;
import sk.sporixx.util.Localization;

import java.util.List;
import java.util.Objects;

public class ManagementController {

    @FXML private VBox categoriesList;
    @FXML private Button categoryAddBtn;
    @FXML private Button categoryEditBtn;
    @FXML private Button categoryDeleteBtn;
    @FXML private Label categoryErrorLabel;

    @FXML private StackPane categoryModalOverlay;
    @FXML private Label categoryModalTitle;
    @FXML private TextField categoryNameField;
    @FXML private Label categoryModalErrorLabel;

    @FXML private Label accountManagerSubtitle;
    @FXML private Label accountManagerCurrency;
    @FXML private HBox defaultAccountsRow;
    @FXML private HBox savingAccountsRow;
    @FXML private Button accountAddBtn;
    @FXML private Label accountErrorLabel;

    @FXML private VBox recurringList;
    @FXML private Button recurringAddBtn;
    @FXML private Button recurringEditBtn;
    @FXML private Button recurringDeleteBtn;
    @FXML private Label recurringErrorLabel;

    private List<Category> categories;
    private Category selectedCategory = null;
    private boolean categoryEditMode = false;
    private Category editingCategory = null;

    @FXML
    public void initialize() {
        loadCategories();
        loadAccounts();
        loadRecurring();
    }

    // ============================================================
    //  CATEGORIES
    // ============================================================
    private void loadCategories() {
        try {
            categories = ServiceLocator.getCategoryService().getCategories().stream()
                    .filter(c -> !c.isSystemCategory())
                    .toList();
        } catch (Exception e) {
            categories = List.of();
            showCategoryError("error.db_error");
        }
        renderCategories();
    }

    private void renderCategories() {
        categoriesList.getChildren().clear();
        selectedCategory = null;

        for (Category cat : categories) {
            categoriesList.getChildren().add(createCategoryRow(cat));
        }

        if (categories.isEmpty()) {
            Label empty = new Label(Localization.get("management.categories.empty"));
            empty.getStyleClass().add("analytics-subtitle");
            categoriesList.getChildren().add(empty);
        }
    }

    private HBox createCategoryRow(Category cat) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("activity-row");
        row.setUserData(cat);

        Label name = new Label(cat.getName());
        name.getStyleClass().add("activity-name");
        HBox.setHgrow(name, Priority.ALWAYS);
        row.getChildren().add(name);

        row.setOnMouseClicked(e -> {
            if (!categoryEditMode) return;
            selectCategoryRow(row, cat);
        });

        return row;
    }

    private void selectCategoryRow(HBox row, Category cat) {
        for (var node : categoriesList.getChildren()) {
            if (node instanceof HBox r) {
                r.getStyleClass().setAll("activity-row");
                for (var child : r.getChildren()) {
                    if (child instanceof Label l)
                        l.getStyleClass().setAll("activity-name");
                }
            }
        }

        if (selectedCategory == cat) {
            selectedCategory = null;
            return;
        }

        selectedCategory = cat;
        row.getStyleClass().setAll("activity-row", "sidebar-item-active");
        for (var child : row.getChildren()) {
            if (child instanceof Label l)
                l.getStyleClass().setAll("activity-name", "sidebar-item-label-active");
        }
    }

    @FXML
    private void handleCategoryAdd() {
        editingCategory = null;
        categoryModalTitle.setText(Localization.get("management.categories.modal.title_add"));
        categoryNameField.clear();
        clearCategoryModalError();
        openCategoryModal();
    }

    @FXML
    private void handleCategoryEdit() {
        clearCategoryError();
        if (!categoryEditMode) {
            categoryEditMode = true;
            categoryEditBtn.getStyleClass().setAll("modal-action-btn");
            return;
        }
        if (selectedCategory == null) {
            showCategoryError("management.categories.error.select_first");
            return;
        }
        editingCategory = selectedCategory;
        categoryModalTitle.setText(Localization.get("management.categories.modal.title_edit"));
        categoryNameField.setText(editingCategory.getName());
        clearCategoryModalError();
        openCategoryModal();
    }

    @FXML
    private void handleCategoryDelete() {
        clearCategoryError();
        if (!categoryEditMode) {
            categoryEditMode = true;
            categoryDeleteBtn.getStyleClass().setAll("modal-action-btn");
            return;
        }
        if (selectedCategory == null) {
            showCategoryError("management.categories.error.select_first");
            return;
        }
        try {
            ServiceLocator.getCategoryService().deleteCategory(selectedCategory.getId());
            resetCategoryEditMode();
            loadCategories();
        } catch (Exception e) {
            String msg = e.getMessage();
            showCategoryError(msg != null && msg.startsWith("category.error.") ? msg : "error.db_error");
        }
    }

    @FXML
    private void onCategoryModalConfirm() {
        clearCategoryModalError();
        String name = categoryNameField.getText().trim();
        if (name.isEmpty()) {
            showCategoryModalError("category.error.name_required");
            return;
        }
        try {
            if (editingCategory == null) {
                ServiceLocator.getCategoryService().addCategory(name);
            } else {
                ServiceLocator.getCategoryService().updateCategory(editingCategory.getId(), name);
            }
            resetCategoryEditMode();
            closeCategoryModal();
            loadCategories();
        } catch (Exception e) {
            String msg = e.getMessage();
            showCategoryModalError(msg != null && msg.startsWith("category.error.") ? msg : "error.db_error");
        }
    }

    @FXML
    private void onCategoryModalClose() {
        closeCategoryModal();
        resetCategoryEditMode();
    }

    private void resetCategoryEditMode() {
        categoryEditMode = false;
        selectedCategory = null;
        editingCategory = null;
        categoryEditBtn.getStyleClass().setAll("btn-icon-edit");
        categoryDeleteBtn.getStyleClass().setAll("btn-icon-delete");
    }

    private void openCategoryModal() {
        final boolean[] pressedOnOverlay = {false};
        categoryModalOverlay.setOnMousePressed(e -> pressedOnOverlay[0] = e.getTarget() == categoryModalOverlay);
        categoryModalOverlay.setOnMouseReleased(e -> {
            if (pressedOnOverlay[0] && e.getTarget() == categoryModalOverlay) closeCategoryModal();
        });
        categoryModalOverlay.setVisible(true);
        categoryModalOverlay.setManaged(true);
    }

    private void closeCategoryModal() {
        categoryModalOverlay.setVisible(false);
        categoryModalOverlay.setManaged(false);
    }

    private void showCategoryError(String key) {
        categoryErrorLabel.setText(Localization.get(key));
        categoryErrorLabel.setVisible(true);
        categoryErrorLabel.setManaged(true);
    }

    private void clearCategoryError() {
        categoryErrorLabel.setVisible(false);
        categoryErrorLabel.setManaged(false);
    }

    private void showCategoryModalError(String key) {
        categoryModalErrorLabel.setText(Localization.get(key));
        categoryModalErrorLabel.setVisible(true);
        categoryModalErrorLabel.setManaged(true);
    }

    private void clearCategoryModalError() {
        categoryModalErrorLabel.setVisible(false);
        categoryModalErrorLabel.setManaged(false);
    }

    // ============================================================
    //  ACCOUNTS — TODO: zadrôtovať keď Adelka dodá AccountService
    // ============================================================
    private void loadAccounts() {
        try {
            List<Account> accounts = SessionManager.getInstance().getAccounts();
            accountManagerSubtitle.setText(
                    Localization.get("management.accounts.total") + ": " + accounts.size());
            accountManagerCurrency.setText(
                    Localization.get("management.accounts.currency") + ": Eur");

            defaultAccountsRow.getChildren().clear();
            savingAccountsRow.getChildren().clear();

            for (Account acc : accounts) {
                if (acc.isSavingAccount()) {
                    savingAccountsRow.getChildren().add(createAccountCard(acc, true));
                } else {
                    defaultAccountsRow.getChildren().add(createAccountCard(acc, false));
                }
            }

            VBox addCard = new VBox();
            addCard.getStyleClass().add("account-card-add");
            addCard.setAlignment(Pos.CENTER);
            HBox.setHgrow(addCard, Priority.ALWAYS);
            Label plus = new Label("+");
            plus.getStyleClass().add("account-card-plus");
            addCard.getChildren().add(plus);
            addCard.setOnMouseClicked(e -> handleAccountAdd());
            savingAccountsRow.getChildren().add(addCard);

        } catch (Exception e) {
            showAccountError("error.db_error");
        }
    }

    private VBox createAccountCard(Account account, boolean canDelete) {
        VBox card = new VBox(8);
        card.getStyleClass().add("account-card");
        HBox.setHgrow(card, Priority.ALWAYS);

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(getAccountDisplayName(account));
        title.getStyleClass().add("account-card-title");
        HBox.setHgrow(title, Priority.ALWAYS);
        header.getChildren().add(title);

        Button editBtn = new Button();
        editBtn.getStyleClass().add("btn-icon-edit");
        try {
            ImageView editIcon = new ImageView(new Image(Objects.requireNonNull(
                    getClass().getResourceAsStream("/assets/icons/icon_edit.png"))));
            editIcon.setFitWidth(14);
            editIcon.setFitHeight(14);
            editIcon.setPreserveRatio(true);
            editBtn.setGraphic(editIcon);
        } catch (Exception ignored) {}
        editBtn.setOnAction(e -> handleAccountEdit(account));
        header.getChildren().add(editBtn);

        if (canDelete) {
            Button deleteBtn = new Button();
            deleteBtn.getStyleClass().add("btn-icon-delete");
            try {
                ImageView deleteIcon = new ImageView(new Image(Objects.requireNonNull(
                        getClass().getResourceAsStream("/assets/icons/icon_delete.png"))));
                deleteIcon.setFitWidth(14);
                deleteIcon.setFitHeight(14);
                deleteIcon.setPreserveRatio(true);
                deleteBtn.setGraphic(deleteIcon);
            } catch (Exception ignored) {}
            deleteBtn.setOnAction(e -> handleAccountDelete(account));
            header.getChildren().add(deleteBtn);
        }

        Label desc = new Label(account.getDescription());
        desc.getStyleClass().add("account-card-desc");

        Label created = new Label(Localization.get("management.accounts.created") + ": "
                + account.getCreatedAt().toLocalDate()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy")));
        created.getStyleClass().add("analytics-subtitle");

        Region vspacer = new Region();
        VBox.setVgrow(vspacer, Priority.ALWAYS);

        Label amount = new Label(formatCurrency(account.getCurrentBalance()));
        amount.getStyleClass().add("account-card-amount");

        if (account.isMainAccount() || account.isEmergencyFund()) {
            Label cantDelete = new Label(Localization.get("management.accounts.cannot_delete"));
            cantDelete.getStyleClass().add("modal-error-label");
            card.getChildren().addAll(header, desc, created, vspacer, amount, cantDelete);
        } else {
            card.getChildren().addAll(header, desc, created, vspacer, amount);
        }

        return card;
    }

    @FXML
    private void handleAccountAdd() {
        // TODO: keď Adelka dodá AccountService
    }

    private void handleAccountEdit(Account account) {
        // TODO: keď Adelka dodá AccountService
    }

    private void handleAccountDelete(Account account) {
        // TODO: keď Adelka dodá AccountService
    }

    private void showAccountError(String key) {
        accountErrorLabel.setText(Localization.get(key));
        accountErrorLabel.setVisible(true);
        accountErrorLabel.setManaged(true);
    }

    // ============================================================
    //  RECURRING — TODO: zadrôtovať keď Adelka dodá RecurringService
    // ============================================================
    private void loadRecurring() {
        recurringList.getChildren().clear();
        Label placeholder = new Label(Localization.get("management.recurring.empty"));
        placeholder.getStyleClass().add("analytics-subtitle");
        recurringList.getChildren().add(placeholder);
    }

    @FXML
    private void handleRecurringAdd() {
        // TODO: keď Adelka dodá RecurringService
    }

    @FXML
    private void handleRecurringEdit() {
        // TODO: keď Adelka dodá RecurringService
    }

    @FXML
    private void handleRecurringDelete() {
        // TODO: keď Adelka dodá RecurringService
    }

    // ============================================================
    //  HELPERS
    // ============================================================
    private String getAccountDisplayName(Account account) {
        if (account.isMainAccount()) return Localization.get("dashboard.account.main");
        if (account.isEmergencyFund()) return Localization.get("dashboard.account.emergency");
        if (account.isSavingAccount()) return Localization.get("dashboard.account.saving");
        return Localization.get("dashboard.account.default");
    }

    private String formatCurrency(double value) {
        return String.format("€%,.2f", value);
    }
}