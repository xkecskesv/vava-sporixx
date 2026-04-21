package sk.sporixx.ui;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import sk.sporixx.model.Account;
import sk.sporixx.model.Category;
import sk.sporixx.model.RecurringRule;
import sk.sporixx.model.Transaction;
import sk.sporixx.service.ServiceLocator;
import sk.sporixx.service.SessionManager;
import sk.sporixx.util.Localization;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ManagementController {

    private static final int MAX_VISIBLE_ACCOUNT_CARDS = 2;

    // Categories
    @FXML private VBox categoriesList;
    @FXML private Button categoryAddBtn;
    @FXML private Button categoryEditBtn;
    @FXML private Button categoryDeleteBtn;
    @FXML private Label categoryErrorLabel;

    // Category modal
    @FXML private StackPane categoryModalOverlay;
    @FXML private Label categoryModalTitle;
    @FXML private TextField categoryNameField;
    @FXML private Label categoryModalErrorLabel;

    // Accounts
    @FXML private Label accountManagerSubtitle;
    @FXML private Label accountManagerCurrency;
    @FXML private HBox defaultAccountsRow;
    @FXML private HBox savingAccountsRow;
    @FXML private Button accountAddBtn;
    @FXML private Label accountErrorLabel;

    // Account modal
    @FXML private StackPane accountModalOverlay;
    @FXML private Label accountModalTitle;
    @FXML private ComboBox<String> accountTypeComboBox;
    @FXML private TextField accountDescField;
    @FXML private TextField accountAmountField;
    @FXML private VBox accountGoalSection;
    @FXML private TextField accountGoalField;
    @FXML private DatePicker accountDatePicker;
    @FXML private Label accountModalErrorLabel;

    // Recurring list
    @FXML private VBox recurringList;
    @FXML private Button recurringAddBtn;
    @FXML private Button recurringEditBtn;
    @FXML private Button recurringDeleteBtn;
    @FXML private Label recurringErrorLabel;

    // Recurring modal
    @FXML private StackPane recurringModalOverlay;
    @FXML private Label recurringModalTitle;
    @FXML private TextField recurringNameField;
    @FXML private ComboBox<String> recurringCategoryCombo;
    @FXML private TextField recurringAmountField;
    @FXML private ComboBox<String> recurringTypeCombo;
    @FXML private ComboBox<String> recurringClassificationCombo;
    @FXML private ComboBox<String> recurringFrequencyCombo;
    @FXML private TextField recurringIntervalField;
    @FXML private DatePicker recurringStartDatePicker;
    @FXML private TextField recurringMaxOccurrencesField;
    @FXML private Label recurringModalErrorLabel;

    // State — categories
    private List<Category> categories;
    private List<Category> selectableCategories;
    private Category selectedCategory = null;
    private Category editingCategory = null;

    // State — accounts pagination
    private List<Account> allDefaultAccounts = new ArrayList<>();
    private List<Account> allSavingAccounts = new ArrayList<>();
    private int defaultAccountOffset = 0;
    private int savingAccountOffset = 0;

    // State — recurring
    private List<RecurringRule> recurringRules;
    private RecurringRule selectedRecurringRule = null;
    private RecurringRule editingRecurringRule = null;

    @FXML
    public void initialize() {
        fixButtonSize(categoryDeleteBtn);
        fixButtonSize(categoryEditBtn);
        fixButtonSize(recurringDeleteBtn);
        fixButtonSize(recurringEditBtn);

        loadCategories();
        loadAccounts();
        loadRecurring();
    }

    private void fixButtonSize(Button btn) {
        btn.setMinWidth(36);
        btn.setMaxWidth(36);
        btn.setPrefWidth(36);
        btn.setMinHeight(36);
        btn.setMaxHeight(36);
        btn.setPrefHeight(36);
    }

    // ============================================================
    //  CATEGORIES
    // ============================================================
    private void loadCategories() {
        try {
            categories = ServiceLocator.getCategoryService().getCategories();
        } catch (Exception e) {
            categories = List.of();
            showCategoryError("error.db_error");
        }
        renderCategories();
        fixButtonSize(categoryDeleteBtn);
        fixButtonSize(categoryEditBtn);
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
        row.getStyleClass().add("table-row");
        row.setUserData(cat);
        row.setOnMouseClicked(e -> selectCategoryRow(row, cat));
        Label name = new Label(cat.getName());
        name.getStyleClass().add(cat.isSystemCategory() ? "analytics-subtitle" : "activity-name");
        HBox.setHgrow(name, Priority.ALWAYS);
        row.getChildren().add(name);
        return row;
    }

    private void selectCategoryRow(HBox row, Category cat) {
        categoriesList.getChildren().forEach(n -> {
            if (n instanceof HBox r) r.getStyleClass().setAll("table-row");
        });
        if (selectedCategory == cat) { selectedCategory = null; return; }
        selectedCategory = cat;
        row.getStyleClass().setAll("table-row-selected");
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
        if (selectedCategory == null) { showCategoryError("management.categories.error.select_first"); return; }
        if (selectedCategory.isSystemCategory()) { showCategoryError("category.error.cannot_modify_system"); return; }
        editingCategory = selectedCategory;
        categoryModalTitle.setText(Localization.get("management.categories.modal.title_edit"));
        categoryNameField.setText(editingCategory.getName());
        clearCategoryModalError();
        openCategoryModal();
    }

    @FXML
    private void handleCategoryDelete() {
        clearCategoryError();
        if (selectedCategory == null) { showCategoryError("management.categories.error.select_first"); return; }
        if (selectedCategory.isSystemCategory()) { showCategoryError("category.error.cannot_modify_system"); return; }
        try {
            ServiceLocator.getCategoryService().deleteCategory(selectedCategory.getId());
            selectedCategory = null;
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
        if (name.isEmpty()) { showCategoryModalError("category.error.name_required"); return; }
        try {
            if (editingCategory == null) {
                ServiceLocator.getCategoryService().addCategory(name);
            } else {
                ServiceLocator.getCategoryService().updateCategory(editingCategory.getId(), name);
            }
            closeCategoryModal();
            resetCategoryState();
            loadCategories();
        } catch (Exception e) {
            String msg = e.getMessage();
            showCategoryModalError(msg != null && msg.startsWith("category.error.") ? msg : "error.db_error");
        }
    }

    @FXML
    private void onCategoryModalClose() {
        closeCategoryModal();
        resetCategoryState();
    }

    private void resetCategoryState() {
        selectedCategory = null;
        editingCategory = null;
        categoryEditBtn.getStyleClass().remove("btn-icon-active");
        if (!categoryEditBtn.getStyleClass().contains("btn-icon-edit"))
            categoryEditBtn.getStyleClass().add("btn-icon-edit");
        categoryDeleteBtn.getStyleClass().remove("btn-icon-danger-active");
        if (!categoryDeleteBtn.getStyleClass().contains("btn-icon-danger"))
            categoryDeleteBtn.getStyleClass().add("btn-icon-danger");
        fixButtonSize(categoryEditBtn);
        fixButtonSize(categoryDeleteBtn);
        categoriesList.getChildren().forEach(n -> {
            if (n instanceof HBox r) r.getStyleClass().setAll("table-row");
        });
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
    //  ACCOUNTS
    // ============================================================
    private void loadAccounts() {
        try {
            List<Account> accounts = SessionManager.getInstance().getAccounts();
            accountManagerSubtitle.setText(
                    Localization.get("management.accounts.total") + ": " + accounts.size());
            accountManagerCurrency.setText(
                    Localization.get("management.accounts.currency") + ": Eur");
            allDefaultAccounts = accounts.stream()
                    .filter(a -> !a.isSavingAccount())
                    .collect(Collectors.toList());
            allSavingAccounts = accounts.stream()
                    .filter(Account::isSavingAccount)
                    .collect(Collectors.toList());
            defaultAccountOffset = 0;
            savingAccountOffset = 0;
        } catch (Exception e) {
            showAccountError("error.db_error");
            allDefaultAccounts = List.of();
            allSavingAccounts = List.of();
        }
        renderDefaultAccounts();
        renderSavingAccounts();
    }

    private void renderDefaultAccounts() {
        defaultAccountsRow.getChildren().clear();

        boolean canLeft = defaultAccountOffset > 0;
        defaultAccountsRow.getChildren().add(createArrow(true, canLeft, () -> {
            defaultAccountOffset = Math.max(0, defaultAccountOffset - 1);
            renderDefaultAccounts();
        }));

        int from = defaultAccountOffset;
        int to = Math.min(from + MAX_VISIBLE_ACCOUNT_CARDS, allDefaultAccounts.size());
        for (int i = from; i < to; i++) {
            defaultAccountsRow.getChildren().add(createAccountCard(allDefaultAccounts.get(i)));
        }

        boolean isLastPage = to >= allDefaultAccounts.size();
        if (isLastPage) {
            // + karta — vždy otvára modal fixne na "Account" (private)
            defaultAccountsRow.getChildren().add(createAddCard(() -> openAccountModal(false)));
        }

        boolean canRight = !isLastPage;
        defaultAccountsRow.getChildren().add(createArrow(false, canRight, () -> {
            defaultAccountOffset++;
            renderDefaultAccounts();
        }));
    }

    private void renderSavingAccounts() {
        savingAccountsRow.getChildren().clear();

        boolean canLeft = savingAccountOffset > 0;
        savingAccountsRow.getChildren().add(createArrow(true, canLeft, () -> {
            savingAccountOffset = Math.max(0, savingAccountOffset - 1);
            renderSavingAccounts();
        }));

        int from = savingAccountOffset;
        int to = Math.min(from + MAX_VISIBLE_ACCOUNT_CARDS, allSavingAccounts.size());
        for (int i = from; i < to; i++) {
            savingAccountsRow.getChildren().add(createAccountCard(allSavingAccounts.get(i)));
        }

        boolean isLastPage = to >= allSavingAccounts.size();
        if (isLastPage) {
            // + karta — vždy otvára modal fixne na "Saving Account"
            savingAccountsRow.getChildren().add(createAddCard(() -> openAccountModal(true)));
        }

        boolean canRight = !isLastPage;
        savingAccountsRow.getChildren().add(createArrow(false, canRight, () -> {
            savingAccountOffset++;
            renderSavingAccounts();
        }));
    }

    private VBox createArrow(boolean isLeft, boolean visible, Runnable onClick) {
        VBox arrow = new VBox();
        arrow.setPadding(new Insets(0, 8, 0, 8));
        arrow.getStyleClass().add("accounts-arrow");
        arrow.setAlignment(Pos.CENTER);
        try {
            String iconPath = isLeft
                    ? "/assets/icons/icon_arrow_left.png"
                    : "/assets/icons/icon_arrow_right.png";
            ImageView icon = new ImageView(new Image(
                    Objects.requireNonNull(getClass().getResourceAsStream(iconPath))));
            icon.setFitWidth(24);
            icon.setFitHeight(24);
            icon.setPreserveRatio(true);
            icon.getStyleClass().add("accounts-arrow-icon");
            arrow.getChildren().add(icon);
        } catch (Exception ignored) {}
        arrow.setOnMouseClicked(e -> onClick.run());
        arrow.setVisible(visible);
        arrow.setManaged(visible);
        return arrow;
    }

    private VBox createAddCard(Runnable onClick) {
        VBox card = new VBox();
        card.getStyleClass().add("account-card-add");
        card.setAlignment(Pos.CENTER);
        HBox.setHgrow(card, Priority.ALWAYS);
        Label plus = new Label("+");
        plus.getStyleClass().add("account-card-plus");
        card.getChildren().add(plus);
        card.setOnMouseClicked(e -> onClick.run());
        return card;
    }

    private VBox createAccountCard(Account account) {
        boolean canDelete = !account.isMainAccount() && !account.isEmergencyFund();

        VBox card = new VBox(8);
        card.getStyleClass().add("account-card");
        HBox.setHgrow(card, Priority.ALWAYS);

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(getAccountDisplayName(account));
        title.getStyleClass().add("account-card-title");
        HBox.setHgrow(title, Priority.ALWAYS);
        header.getChildren().add(title);

        if (canDelete) {
            Button deleteBtn = new Button();
            deleteBtn.getStyleClass().add("btn-icon-danger");
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

        Button editBtn = new Button();
        editBtn.getStyleClass().add("btn-icon");
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

        Label desc = new Label(account.getDescription());
        desc.getStyleClass().add("account-card-desc");

        Label created = new Label(Localization.get("management.accounts.created") + ": "
                + account.getCreatedAt().toLocalDate()
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
        created.getStyleClass().add("analytics-subtitle");

        Region vspacer = new Region();
        VBox.setVgrow(vspacer, Priority.ALWAYS);

        Label amount = new Label(formatCurrency(account.getCurrentBalance()));
        amount.getStyleClass().add("account-card-amount");

        card.getChildren().addAll(header, desc, created, vspacer, amount);

        if (account.isMainAccount() || account.isEmergencyFund()) {
            Label cantDelete = new Label(Localization.get("management.accounts.cannot_delete"));
            cantDelete.getStyleClass().add("modal-error-label");
            card.getChildren().add(cantDelete);
        }

        return card;
    }

    // ============================================================
    //  ACCOUNT MODAL
    // ============================================================

    /**
     * Volaný cez ADD button hore — zobrazí dropdown s oboma typmi.
     */
    @FXML
    private void handleAccountAdd() {
        openAccountModal(null);
    }

    /**
     * @param forceSaving null = dropdown (Add button), true = len Saving, false = len Account
     */
    private void openAccountModal(Boolean forceSaving) {
        clearAccountModalError();
        accountModalTitle.setText(Localization.get("dashboard.modal.title"));

        if (forceSaving == null) {
            accountTypeComboBox.getItems().setAll(
                    Localization.get("dashboard.account.saving"),
                    Localization.get("dashboard.account.default"));
            accountTypeComboBox.setValue(Localization.get("dashboard.account.default"));
            accountTypeComboBox.setDisable(false);
        } else if (forceSaving) {
            accountTypeComboBox.getItems().setAll(Localization.get("dashboard.account.saving"));
            accountTypeComboBox.setValue(Localization.get("dashboard.account.saving"));
            accountTypeComboBox.setDisable(true);
        } else {
            accountTypeComboBox.getItems().setAll(Localization.get("dashboard.account.default"));
            accountTypeComboBox.setValue(Localization.get("dashboard.account.default"));
            accountTypeComboBox.setDisable(true);
        }

        updateGoalSectionVisibility();
        accountTypeComboBox.setOnAction(e -> updateGoalSectionVisibility());

        accountDescField.clear();
        accountAmountField.clear();
        accountGoalField.clear();
        accountDatePicker.setValue(null);
        accountDatePicker.setConverter(new javafx.util.StringConverter<>() {
            private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            @Override public String toString(LocalDate d) { return d != null ? d.format(fmt) : ""; }
            @Override public LocalDate fromString(String s) {
                return (s != null && !s.isEmpty()) ? LocalDate.parse(s, fmt) : null;
            }
        });

        final boolean[] pressedOnOverlay = {false};
        accountModalOverlay.setOnMousePressed(e -> pressedOnOverlay[0] = e.getTarget() == accountModalOverlay);
        accountModalOverlay.setOnMouseReleased(e -> {
            if (pressedOnOverlay[0] && e.getTarget() == accountModalOverlay) closeAccountModal();
        });

        accountModalOverlay.setVisible(true);
        accountModalOverlay.setManaged(true);
    }

    private void updateGoalSectionVisibility() {
        boolean isSaving = Localization.get("dashboard.account.saving")
                .equals(accountTypeComboBox.getValue());
        accountGoalSection.setVisible(isSaving);
        accountGoalSection.setManaged(isSaving);
    }

    @FXML
    private void onAccountModalConfirm() {
        clearAccountModalError();

        String desc = accountDescField.getText().trim();
        String amountText = accountAmountField.getText().trim();
        boolean isSaving = Localization.get("dashboard.account.saving")
                .equals(accountTypeComboBox.getValue());

        if (desc.isEmpty()) { showAccountModalError("account.error.description_required"); return; }
        if (amountText.isEmpty()) { showAccountModalError("account.error.negative_amount"); return; }

        double amount;
        try {
            amount = Double.parseDouble(amountText.replace(",", "."));
            if (amount < 0) { showAccountModalError("account.error.negative_amount"); return; }
        } catch (NumberFormatException e) {
            showAccountModalError("account.error.negative_amount"); return;
        }

        try {
            if (isSaving) {
                String goalText = accountGoalField.getText().trim();
                if (goalText.isEmpty()) { showAccountModalError("account.error.goal_amount_invalid"); return; }
                if (accountDatePicker.getValue() == null) { showAccountModalError("account.error.goal_date_invalid"); return; }
                double goalAmount;
                try {
                    goalAmount = Double.parseDouble(goalText.replace(",", "."));
                } catch (NumberFormatException e) {
                    showAccountModalError("account.error.goal_amount_invalid"); return;
                }
                ServiceLocator.getAccountService()
                        .createSavingAccount(desc, amount, goalAmount, accountDatePicker.getValue());
            } else {
                ServiceLocator.getAccountService().createPrivateAccount(desc, amount);
            }
            closeAccountModal();
            loadAccounts();
        } catch (Exception e) {
            String msg = e.getMessage();
            showAccountModalError(msg != null && msg.startsWith("account.error.") ? msg : "account.error.invalid_type");
        }
    }

    @FXML
    private void onAccountModalClose() {
        closeAccountModal();
    }

    private void closeAccountModal() {
        accountModalOverlay.setVisible(false);
        accountModalOverlay.setManaged(false);
    }

    private void handleAccountEdit(Account account) {
        // TODO: keď Adelka dodá AccountService rozhranie pre management
    }

    private void handleAccountDelete(Account account) {
        try {
            ServiceLocator.getAccountService().deleteAccount(account.getId());
            loadAccounts();
        } catch (Exception e) {
            String msg = e.getMessage();
            showAccountError(msg != null && msg.startsWith("account.error.") ? msg : "error.db_error");
        }
    }

    private void showAccountError(String key) {
        accountErrorLabel.setText(Localization.get(key));
        accountErrorLabel.setVisible(true);
        accountErrorLabel.setManaged(true);
    }

    private void showAccountModalError(String key) {
        accountModalErrorLabel.setText(Localization.get(key));
        accountModalErrorLabel.setVisible(true);
        accountModalErrorLabel.setManaged(true);
    }

    private void clearAccountModalError() {
        accountModalErrorLabel.setVisible(false);
        accountModalErrorLabel.setManaged(false);
    }

    // ============================================================
    //  RECURRING
    // ============================================================
    private void loadRecurring() {
        try {
            recurringRules = ServiceLocator.getRecurringRuleService().getRecurringRules();
        } catch (Exception e) {
            recurringRules = List.of();
            showRecurringError("error.db_error");
        }
        renderRecurring();
        fixButtonSize(recurringDeleteBtn);
        fixButtonSize(recurringEditBtn);
    }

    private void renderRecurring() {
        recurringList.getChildren().clear();
        selectedRecurringRule = null;
        if (recurringRules.isEmpty()) {
            Label empty = new Label(Localization.get("management.recurring.empty"));
            empty.getStyleClass().add("analytics-subtitle");
            recurringList.getChildren().add(empty);
            return;
        }
        for (RecurringRule rule : recurringRules) {
            recurringList.getChildren().add(createRecurringRow(rule));
        }
    }

    private HBox createRecurringRow(RecurringRule rule) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("table-row");
        row.setUserData(rule);

        VBox info = new VBox(2);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label name = new Label(rule.getDescription());
        name.getStyleClass().add("activity-name");
        Label freq = new Label(formatFrequency(rule.getFrequencyType(), rule.getFrequencyInterval()));
        freq.getStyleClass().add("activity-type");
        info.getChildren().addAll(name, freq);
        row.getChildren().add(info);

        Label amount = new Label("- " + formatCurrency(rule.getAmount()));
        amount.getStyleClass().add("table-cell-amount-negative");
        row.getChildren().add(amount);

        row.setOnMouseClicked(e -> selectRecurringRow(row, rule));
        return row;
    }

    private String formatFrequency(String frequencyType, int interval) {
        if (frequencyType == null) return "";
        return switch (frequencyType.toUpperCase()) {
            case "DAILY"   -> Localization.get("management.recurring.frequency.daily").replace("{n}", String.valueOf(interval));
            case "WEEKLY"  -> Localization.get("management.recurring.frequency.weekly").replace("{n}", String.valueOf(interval));
            case "MONTHLY" -> Localization.get("management.recurring.frequency.monthly").replace("{n}", String.valueOf(interval));
            case "YEARLY"  -> Localization.get("management.recurring.frequency.yearly").replace("{n}", String.valueOf(interval));
            default -> frequencyType;
        };
    }

    private void selectRecurringRow(HBox row, RecurringRule rule) {
        recurringList.getChildren().forEach(n -> {
            if (n instanceof HBox r) r.getStyleClass().setAll("table-row");
        });
        if (selectedRecurringRule == rule) { selectedRecurringRule = null; return; }
        selectedRecurringRule = rule;
        row.getStyleClass().setAll("table-row-selected");
    }

    @FXML
    private void handleRecurringAdd() {
        clearRecurringError();
        editingRecurringRule = null;
        recurringModalTitle.setText(Localization.get("management.recurring.modal.title_add"));
        clearRecurringModal();
        openRecurringModal();
    }

    @FXML
    private void handleRecurringEdit() {
        clearRecurringError();
        if (selectedRecurringRule == null) { showRecurringError("management.recurring.error.select_first"); return; }
        editingRecurringRule = selectedRecurringRule;
        recurringModalTitle.setText(Localization.get("management.recurring.modal.title_edit"));
        populateRecurringModal(editingRecurringRule);
        clearRecurringModalError();
        openRecurringModal();
    }

    @FXML
    private void handleRecurringDelete() {
        clearRecurringError();
        if (selectedRecurringRule == null) { showRecurringError("management.recurring.error.select_first"); return; }
        try {
            ServiceLocator.getRecurringRuleService().deleteRecurringRule(selectedRecurringRule.getId());
            resetRecurringState();
            loadRecurring();
        } catch (Exception e) {
            String msg = e.getMessage();
            showRecurringError(msg != null && msg.startsWith("recurring.error.") ? msg : "error.db_error");
        }
    }

    @FXML
    private void onRecurringModalConfirm() {
        clearRecurringModalError();

        String name = recurringNameField.getText().trim();
        String amountText = recurringAmountField.getText().trim();
        String frequency = recurringFrequencyCombo.getValue();
        String intervalText = recurringIntervalField.getText().trim();
        LocalDate startDate = recurringStartDatePicker.getValue();
        String categoryName = recurringCategoryCombo.getValue();
        String typeName = recurringTypeCombo.getValue();

        if (name.isEmpty()) { showRecurringModalError("recurring.error.description_required"); return; }
        if (amountText.isEmpty()) { showRecurringModalError("recurring.error.invalid_amount"); return; }
        if (frequency == null) { showRecurringModalError("recurring.error.invalid_interval"); return; }
        if (startDate == null) { showRecurringModalError("recurring.error.start_date_required"); return; }

        double amount;
        try {
            amount = Double.parseDouble(amountText.replace(",", "."));
            if (amount <= 0) { showRecurringModalError("recurring.error.invalid_amount"); return; }
        } catch (NumberFormatException e) { showRecurringModalError("recurring.error.invalid_amount"); return; }

        int interval;
        try {
            interval = intervalText.isEmpty() ? 1 : Integer.parseInt(intervalText);
            if (interval <= 0) { showRecurringModalError("recurring.error.invalid_interval"); return; }
        } catch (NumberFormatException e) { showRecurringModalError("recurring.error.invalid_interval"); return; }

        Integer maxOccurrences = null;
        String maxText = recurringMaxOccurrencesField.getText().trim();
        if (!maxText.isEmpty()) {
            try { maxOccurrences = Integer.parseInt(maxText); }
            catch (NumberFormatException e) { showRecurringModalError("recurring.error.invalid_interval"); return; }
        }

        int categoryId = selectableCategories.stream()
                .filter(c -> c.getName().equals(categoryName))
                .findFirst().map(Category::getId).orElse(0);

        int transactionTypeId = Localization.get("management.recurring.modal.type_expense")
                .equals(typeName) ? Transaction.TYPE_EXPENSE : Transaction.TYPE_INCOME;

        String classificationName = recurringClassificationCombo.getValue();
        Integer spendingClassificationId = null;
        if (transactionTypeId == Transaction.TYPE_EXPENSE && classificationName != null) {
            if (Localization.get("management.recurring.modal.classification_need").equals(classificationName))
                spendingClassificationId = Transaction.CLASSIFICATION_NEED;
            else if (Localization.get("management.recurring.modal.classification_want").equals(classificationName))
                spendingClassificationId = Transaction.CLASSIFICATION_WANT;
        }

        Account mainAccount = SessionManager.getInstance().getAccounts().stream()
                .filter(Account::isMainAccount).findFirst().orElse(null);
        if (mainAccount == null) { showRecurringModalError("account.error.no_main_account"); return; }

        String frequencyType = getFrequencyType(frequency);

        try {
            if (editingRecurringRule == null) {
                ServiceLocator.getRecurringRuleService().addRecurringRule(
                        mainAccount.getId(), categoryId, transactionTypeId,
                        spendingClassificationId, name, amount, frequencyType,
                        interval, startDate, maxOccurrences);
            } else {
                ServiceLocator.getRecurringRuleService().updateRecurringRule(
                        editingRecurringRule.getId(), categoryId, spendingClassificationId,
                        name, amount, frequencyType, interval, maxOccurrences);
            }
            closeRecurringModal();
            resetRecurringState();
            loadRecurring();
        } catch (Exception e) {
            String msg = e.getMessage();
            showRecurringModalError(msg != null && msg.startsWith("recurring.error.") ? msg : "error.db_error");
        }
    }

    private String getFrequencyType(String localizedFrequency) {
        if (localizedFrequency.equals(Localization.get("management.recurring.modal.frequency_daily"))) return "DAILY";
        if (localizedFrequency.equals(Localization.get("management.recurring.modal.frequency_weekly"))) return "WEEKLY";
        if (localizedFrequency.equals(Localization.get("management.recurring.modal.frequency_yearly"))) return "YEARLY";
        return "MONTHLY";
    }

    @FXML
    private void onRecurringModalClose() {
        closeRecurringModal();
        resetRecurringState();
    }

    private void clearRecurringModal() {
        recurringNameField.clear();
        recurringAmountField.clear();
        recurringIntervalField.clear();
        recurringMaxOccurrencesField.clear();
        recurringStartDatePicker.setValue(LocalDate.now());
        recurringStartDatePicker.setConverter(new javafx.util.StringConverter<>() {
            private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            @Override public String toString(LocalDate d) { return d != null ? d.format(fmt) : ""; }
            @Override public LocalDate fromString(String s) {
                return (s != null && !s.isEmpty()) ? LocalDate.parse(s, fmt) : null;
            }
        });
        try {
            selectableCategories = ServiceLocator.getCategoryService().getCategories();
        } catch (Exception e) {
            selectableCategories = List.of();
        }
        recurringCategoryCombo.getItems().setAll(
                selectableCategories.stream().map(Category::getName).toList());
        if (!recurringCategoryCombo.getItems().isEmpty())
            recurringCategoryCombo.setValue(recurringCategoryCombo.getItems().get(0));

        recurringTypeCombo.getItems().setAll(
                Localization.get("management.recurring.modal.type_expense"),
                Localization.get("management.recurring.modal.type_income"));
        recurringTypeCombo.setValue(Localization.get("management.recurring.modal.type_expense"));

        recurringClassificationCombo.getItems().setAll(
                Localization.get("management.recurring.modal.classification_none"),
                Localization.get("management.recurring.modal.classification_need"),
                Localization.get("management.recurring.modal.classification_want"));
        recurringClassificationCombo.setValue(Localization.get("management.recurring.modal.classification_none"));

        recurringTypeCombo.setOnAction(e -> {
            boolean isExpense = Localization.get("management.recurring.modal.type_expense")
                    .equals(recurringTypeCombo.getValue());
            recurringClassificationCombo.setDisable(!isExpense);
            if (!isExpense)
                recurringClassificationCombo.setValue(Localization.get("management.recurring.modal.classification_none"));
        });

        recurringFrequencyCombo.getItems().setAll(
                Localization.get("management.recurring.modal.frequency_daily"),
                Localization.get("management.recurring.modal.frequency_weekly"),
                Localization.get("management.recurring.modal.frequency_monthly"),
                Localization.get("management.recurring.modal.frequency_yearly"));
        recurringFrequencyCombo.setValue(Localization.get("management.recurring.modal.frequency_monthly"));

        clearRecurringModalError();
    }

    private void populateRecurringModal(RecurringRule rule) {
        clearRecurringModal();
        recurringNameField.setText(rule.getDescription());
        recurringAmountField.setText(String.format("%.2f", rule.getAmount()));
        recurringIntervalField.setText(String.valueOf(rule.getFrequencyInterval()));
        if (rule.getMaxOccurrences() != null)
            recurringMaxOccurrencesField.setText(String.valueOf(rule.getMaxOccurrences()));
        if (rule.getStartDate() != null)
            recurringStartDatePicker.setValue(rule.getStartDate().toLocalDate());

        String freqKey = switch (rule.getFrequencyType().toUpperCase()) {
            case "DAILY"  -> "management.recurring.modal.frequency_daily";
            case "WEEKLY" -> "management.recurring.modal.frequency_weekly";
            case "YEARLY" -> "management.recurring.modal.frequency_yearly";
            default -> "management.recurring.modal.frequency_monthly";
        };
        recurringFrequencyCombo.setValue(Localization.get(freqKey));
        recurringTypeCombo.setValue(rule.getTransactionTypeId() == Transaction.TYPE_INCOME
                ? Localization.get("management.recurring.modal.type_income")
                : Localization.get("management.recurring.modal.type_expense"));

        if (rule.getSpendingClassificationId() == Transaction.CLASSIFICATION_NEED)
            recurringClassificationCombo.setValue(Localization.get("management.recurring.modal.classification_need"));
        else if (rule.getSpendingClassificationId() == Transaction.CLASSIFICATION_WANT)
            recurringClassificationCombo.setValue(Localization.get("management.recurring.modal.classification_want"));
        else
            recurringClassificationCombo.setValue(Localization.get("management.recurring.modal.classification_none"));

        selectableCategories.stream()
                .filter(c -> c.getId() == rule.getCategoryId())
                .findFirst()
                .ifPresent(c -> recurringCategoryCombo.setValue(c.getName()));
    }

    private void openRecurringModal() {
        final boolean[] pressedOnOverlay = {false};
        recurringModalOverlay.setOnMousePressed(e -> pressedOnOverlay[0] = e.getTarget() == recurringModalOverlay);
        recurringModalOverlay.setOnMouseReleased(e -> {
            if (pressedOnOverlay[0] && e.getTarget() == recurringModalOverlay) closeRecurringModal();
        });
        recurringModalOverlay.setVisible(true);
        recurringModalOverlay.setManaged(true);
    }

    private void closeRecurringModal() {
        recurringModalOverlay.setVisible(false);
        recurringModalOverlay.setManaged(false);
    }

    private void resetRecurringState() {
        selectedRecurringRule = null;
        editingRecurringRule = null;
        recurringEditBtn.getStyleClass().remove("btn-icon-active");
        if (!recurringEditBtn.getStyleClass().contains("btn-icon-edit"))
            recurringEditBtn.getStyleClass().add("btn-icon-edit");
        recurringDeleteBtn.getStyleClass().remove("btn-icon-danger-active");
        if (!recurringDeleteBtn.getStyleClass().contains("btn-icon-danger"))
            recurringDeleteBtn.getStyleClass().add("btn-icon-danger");
        fixButtonSize(recurringEditBtn);
        fixButtonSize(recurringDeleteBtn);
        recurringList.getChildren().forEach(n -> {
            if (n instanceof HBox r) r.getStyleClass().setAll("table-row");
        });
    }

    private void showRecurringError(String key) {
        recurringErrorLabel.setText(Localization.get(key));
        recurringErrorLabel.setVisible(true);
        recurringErrorLabel.setManaged(true);
    }

    private void clearRecurringError() {
        recurringErrorLabel.setVisible(false);
        recurringErrorLabel.setManaged(false);
    }

    private void showRecurringModalError(String key) {
        recurringModalErrorLabel.setText(Localization.get(key));
        recurringModalErrorLabel.setVisible(true);
        recurringModalErrorLabel.setManaged(true);
    }

    private void clearRecurringModalError() {
        recurringModalErrorLabel.setVisible(false);
        recurringModalErrorLabel.setManaged(false);
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