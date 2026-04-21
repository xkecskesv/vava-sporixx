package sk.sporixx.ui;

import javafx.fxml.FXML;
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
import java.util.List;
import java.util.Objects;

public class ManagementController {

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

    // State
    private List<Category> categories;
    private List<Category> selectableCategories;
    private List<RecurringRule> recurringRules;

    private Category selectedCategory = null;
    private boolean categoryEditMode = false;
    private Category editingCategory = null;

    private RecurringRule selectedRecurringRule = null;
    private boolean recurringEditMode = false;
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
        // Deselect všetky
        categoriesList.getChildren().forEach(n -> {
            if (n instanceof HBox r) r.getStyleClass().setAll("table-row");
        });

        if (selectedCategory == cat) {
            selectedCategory = null;
            return;
        }
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
        if (selectedCategory == null) {
            showCategoryError("management.categories.error.select_first");
            return;
        }
        if (selectedCategory.isSystemCategory()) {
            showCategoryError("category.error.cannot_modify_system");
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
        if (selectedCategory == null) {
            showCategoryError("management.categories.error.select_first");
            return;
        }
        if (selectedCategory.isSystemCategory()) {
            showCategoryError("category.error.cannot_modify_system");
            return;
        }
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
            closeCategoryModal();
            resetCategoryEditMode();
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

        // Namiesto setAll použi remove + add aby si zachoval ostatné classy
        categoryEditBtn.getStyleClass().remove("btn-icon-active");
        if (!categoryEditBtn.getStyleClass().contains("btn-icon-edit"))
            categoryEditBtn.getStyleClass().add("btn-icon-edit");

        categoryDeleteBtn.getStyleClass().remove("btn-icon-danger-active");
        if (!categoryDeleteBtn.getStyleClass().contains("btn-icon-danger"))
            categoryDeleteBtn.getStyleClass().add("btn-icon-danger");

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
        // TODO: keď Adelka dodá AccountService rozhranie pre management
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

        String freqText = formatFrequency(rule.getFrequencyType(), rule.getFrequencyInterval());
        Label freq = new Label(freqText);
        freq.getStyleClass().add("activity-type");

        info.getChildren().addAll(name, freq);
        row.getChildren().add(info);

        Label amount = new Label("- " + formatCurrency(rule.getAmount()));
        amount.getStyleClass().add("table-cell-amount-negative");
        row.getChildren().add(amount);

        row.setOnMouseClicked(e -> {
            if (!recurringEditMode) return;
            selectRecurringRow(row, rule);
        });

        return row;
    }

    private String formatFrequency(String frequencyType, int interval) {
        if (frequencyType == null) return "";
        return switch (frequencyType.toUpperCase()) {
            case "DAILY" -> Localization.get("management.recurring.frequency.daily")
                    .replace("{n}", String.valueOf(interval));
            case "WEEKLY" -> Localization.get("management.recurring.frequency.weekly")
                    .replace("{n}", String.valueOf(interval));
            case "MONTHLY" -> Localization.get("management.recurring.frequency.monthly")
                    .replace("{n}", String.valueOf(interval));
            case "YEARLY" -> Localization.get("management.recurring.frequency.yearly")
                    .replace("{n}", String.valueOf(interval));
            default -> frequencyType;
        };
    }

    private void selectRecurringRow(HBox row, RecurringRule rule) {
        for (var node : recurringList.getChildren()) {
            if (node instanceof HBox r) r.getStyleClass().setAll("table-row");
        }
        if (selectedRecurringRule == rule) {
            selectedRecurringRule = null;
            return;
        }
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
        if (!recurringEditMode) {
            recurringEditMode = true;
            recurringEditBtn.getStyleClass().setAll("btn-icon-active");
            return;
        }
        if (selectedRecurringRule == null) {
            showRecurringError("management.recurring.error.select_first");
            return;
        }
        editingRecurringRule = selectedRecurringRule;
        recurringModalTitle.setText(Localization.get("management.recurring.modal.title_edit"));
        populateRecurringModal(editingRecurringRule);
        clearRecurringModalError();
        openRecurringModal();
    }

    @FXML
    private void handleRecurringDelete() {
        clearRecurringError();
        if (!recurringEditMode) {
            recurringEditMode = true;
            recurringDeleteBtn.getStyleClass().setAll("btn-icon-danger-active");
            return;
        }
        if (selectedRecurringRule == null) {
            showRecurringError("management.recurring.error.select_first");
            return;
        }
        try {
            ServiceLocator.getRecurringRuleService()
                    .deleteRecurringRule(selectedRecurringRule.getId());
            resetRecurringEditMode();
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
        } catch (NumberFormatException e) {
            showRecurringModalError("recurring.error.invalid_amount"); return;
        }

        int interval;
        try {
            interval = intervalText.isEmpty() ? 1 : Integer.parseInt(intervalText);
            if (interval <= 0) { showRecurringModalError("recurring.error.invalid_interval"); return; }
        } catch (NumberFormatException e) {
            showRecurringModalError("recurring.error.invalid_interval"); return;
        }

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

        // Frequency type string
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
            resetRecurringEditMode();
            closeRecurringModal();
            loadRecurring();
        } catch (Exception e) {
            String msg = e.getMessage();
            showRecurringModalError(msg != null && msg.startsWith("recurring.error.") ? msg : "error.db_error");
        }
    }

    private String getFrequencyType(String localizedFrequency) {
        if (localizedFrequency.equals(Localization.get("management.recurring.modal.frequency_daily")))
            return "DAILY";
        if (localizedFrequency.equals(Localization.get("management.recurring.modal.frequency_weekly")))
            return "WEEKLY";
        if (localizedFrequency.equals(Localization.get("management.recurring.modal.frequency_yearly")))
            return "YEARLY";
        return "MONTHLY";
    }

    @FXML
    private void onRecurringModalClose() {
        closeRecurringModal();
        resetRecurringEditMode();
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
        recurringClassificationCombo.setValue(
                Localization.get("management.recurring.modal.classification_none"));

        recurringTypeCombo.setOnAction(e -> {
            boolean isExpense = Localization.get("management.recurring.modal.type_expense")
                    .equals(recurringTypeCombo.getValue());
            recurringClassificationCombo.setDisable(!isExpense);
            if (!isExpense)
                recurringClassificationCombo.setValue(
                        Localization.get("management.recurring.modal.classification_none"));
        });

        recurringFrequencyCombo.getItems().setAll(
                Localization.get("management.recurring.modal.frequency_daily"),
                Localization.get("management.recurring.modal.frequency_weekly"),
                Localization.get("management.recurring.modal.frequency_monthly"),
                Localization.get("management.recurring.modal.frequency_yearly"));
        recurringFrequencyCombo.setValue(
                Localization.get("management.recurring.modal.frequency_monthly"));

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
            case "DAILY" -> "management.recurring.modal.frequency_daily";
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

    private void resetRecurringEditMode() {
        recurringEditMode = false;
        selectedRecurringRule = null;
        editingRecurringRule = null;
        recurringEditBtn.getStyleClass().setAll("btn-icon-edit");
        recurringDeleteBtn.getStyleClass().setAll("btn-icon-danger");
        fixButtonSize(recurringEditBtn);
        fixButtonSize(recurringDeleteBtn);
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