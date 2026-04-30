package sk.sporixx.ui;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import sk.sporixx.dto.SearchCriteria;
import sk.sporixx.model.Account;
import sk.sporixx.model.Category;
import sk.sporixx.model.Transaction;
import sk.sporixx.service.ServiceLocator;
import sk.sporixx.service.SessionManager;
import sk.sporixx.util.CurrencyFormatUtil;
import sk.sporixx.util.Localization;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class TransactionController {

    @FXML private ImageView userAvatar;
    @FXML private Button newTransactionBtn;
    @FXML private Button editBtn;
    @FXML private Button deleteBtn;
    @FXML private TextField searchField;
    @FXML private HBox accountFilterContainer;
    @FXML private VBox transactionsList;
    @FXML private StackPane modalOverlay;
    @FXML private Label modalTitle;
    @FXML private CheckBox betweenAccountsCheck;
    @FXML private VBox toAccountSection;
    @FXML private Label fromAccountLabel;
    @FXML private ComboBox<String> fromAccountCombo;
    @FXML private Label toAccountLabel;
    @FXML private ComboBox<String> toAccountCombo;
    @FXML private Label typeLabel;
    @FXML private ComboBox<String> typeCombo;
    @FXML private Label nameLabel;
    @FXML private TextField nameField;
    @FXML private HBox categorySection;
    @FXML private Label categoryLabel;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private Label needWantLabel;
    @FXML private ComboBox<String> needWantCombo;
    @FXML private Label dateLabel;
    @FXML private DatePicker datePicker;
    @FXML private Label amountLabel;
    @FXML private TextField amountField;
    @FXML private Label amountPreview;
    @FXML private VBox fromAccountSection;
    @FXML private VBox typeSection;
    @FXML private Label modalErrorLabel;

    private List<Transaction> allTransactions = new ArrayList<>();
    private List<Transaction> filteredTransactions = new ArrayList<>();
    private Transaction selectedTransaction = null;
    private String activeAccountFilter = "ALL";
    private List<Account> userAccounts = new ArrayList<>();
    private List<Category> categories = new ArrayList<>();
    private List<Category> selectableCategories = new ArrayList<>();

    @FXML
    public void initialize() {
        userAccounts = SessionManager.getInstance().getAccounts();
        categories = ServiceLocator.getCategoryService().getCategories();
        selectableCategories = ServiceLocator.getCategoryService().getSelectableCategories();

        loadTransactions();
        setupAccountFilters();
        setupSearch();
        setupDatePicker();
    }

    // ============================================================
    //  NAČÍTANIE TRANSAKCIÍ
    // ============================================================
    private void loadTransactions() {
        try {
            allTransactions = ServiceLocator.getTransactionService().getAllTransactions();
        } catch (Exception e) {
            allTransactions = new ArrayList<>();
            e.printStackTrace();
        }
        filteredTransactions = new ArrayList<>(allTransactions);
        renderTransactions(filteredTransactions);
    }

    // ============================================================
    //  ACCOUNT FILTERS
    // ============================================================
    private void setupAccountFilters() {
        accountFilterContainer.getChildren().clear();

        Button allBtn = createFilterButton("ALL", Localization.get("transactions.filter.all"));
        accountFilterContainer.getChildren().add(allBtn);

        for (Account account : userAccounts) {
            if (account.isMainAccount()) {
                Button btn = createFilterButton(
                        String.valueOf(account.getId()),
                        Localization.get("dashboard.account.main"));
                accountFilterContainer.getChildren().add(btn);
            }
        }

        setFilterActive("ALL");
    }

    private Button createFilterButton(String id, String label) {
        Button btn = new Button(label);
        btn.setUserData(id);
        btn.getStyleClass().add("filter-btn");
        btn.setOnAction(e -> {
            activeAccountFilter = id;
            setFilterActive(id);
            applyFilters();
        });
        return btn;
    }

    private void setFilterActive(String id) {
        for (var node : accountFilterContainer.getChildren()) {
            if (node instanceof Button btn) {
                btn.getStyleClass().setAll(
                        btn.getUserData().equals(id) ? "filter-btn-active" : "filter-btn");
            }
        }
    }

    // ============================================================
    //  SEARCH
    // ============================================================
    private void setupSearch() {
        searchField.textProperty().addListener((obs, old, newVal) -> applyFilters());
    }

    private void applyFilters() {
        try {
            String rawInput = searchField.getText().trim();

            if (!activeAccountFilter.equals("ALL")) {
                int accountId = Integer.parseInt(activeAccountFilter);
                filteredTransactions = ServiceLocator.getTransactionService()
                        .searchTransactions(rawInput, accountId);
            } else {
                filteredTransactions = ServiceLocator.getTransactionService()
                        .searchTransactions(rawInput);
            }
        } catch (Exception e) {
            filteredTransactions = new ArrayList<>();
        }
        renderTransactions(filteredTransactions);
    }

    // ============================================================
    //  RENDER TRANSAKCIÍ
    // ============================================================
    private void renderTransactions(List<Transaction> transactions) {
        transactionsList.getChildren().clear();

        if (transactions.isEmpty()) {
            Label empty = new Label(Localization.get("transactions.empty"));
            empty.getStyleClass().add("analytics-subtitle");
            empty.setPadding(new javafx.geometry.Insets(24));
            transactionsList.getChildren().add(empty);
            return;
        }

        for (Transaction tx : transactions) {
            transactionsList.getChildren().add(createTransactionRow(tx));
        }
    }

    private HBox createTransactionRow(Transaction tx) {
        HBox row = new HBox(0);
        row.getStyleClass().add("table-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setOnMouseClicked(e -> selectTransaction(tx, row));

        Label name = new Label(tx.getDescription());
        name.getStyleClass().add("table-cell");
        HBox.setHgrow(name, Priority.ALWAYS);
        name.setMaxWidth(Double.MAX_VALUE);

        String categoryName = "-";
        if (tx.getCategoryId() != null) {
            if (tx.getCategoryId() == Transaction.CATEGORY_SAVING ||
                    tx.getCategoryId() == Transaction.CATEGORY_SAVING_EXPENSE) {
                categoryName = Localization.get("transactions.category.savings");
            } else {
                categoryName = categories.stream()
                        .filter(c -> c.getId() == tx.getCategoryId())
                        .map(Category::getName)
                        .findFirst()
                        .orElse("-");
            }
        }
        Label category = new Label(categoryName);
        category.getStyleClass().add("table-cell");
        HBox.setHgrow(category, Priority.SOMETIMES);
        category.setMaxWidth(Double.MAX_VALUE);

        String nw = tx.isWant() ? Localization.get("dashboard.activities.want")
                : tx.isNeed() ? Localization.get("dashboard.activities.need") : "-";
        Label needWant = new Label(nw);
        needWant.getStyleClass().add("table-cell");
        needWant.setMinWidth(80);
        needWant.setPrefWidth(100);

        String type = tx.isIncome()
                ? Localization.get("dashboard.activities.incoming")
                : Localization.get("dashboard.activities.sent");
        Label typeLabel = new Label(type);
        typeLabel.getStyleClass().add("table-cell");
        typeLabel.setMinWidth(80);
        typeLabel.setPrefWidth(100);

        Label date = new Label(tx.getCompleteDate()
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
        date.getStyleClass().add("table-cell");
        date.setMinWidth(100);
        date.setPrefWidth(120);

        String prefix = tx.isIncome() ? "+ " : "- ";
        Label amount = new Label(prefix + formatCurrency(tx.getAmount()));
        amount.getStyleClass().add(tx.isIncome()
                ? "table-cell-amount-positive" : "table-cell-amount-negative");
        amount.setMinWidth(80);
        amount.setPrefWidth(100);

        row.getChildren().addAll(name, category, needWant, typeLabel, date, amount);
        return row;
    }

    private void selectTransaction(Transaction tx, HBox row) {
        transactionsList.getChildren().forEach(n ->
                n.getStyleClass().setAll("table-row"));
        row.getStyleClass().setAll("table-row-selected");
        selectedTransaction = tx;
    }

    // ============================================================
    //  AMOUNT PREVIEW
    //  Volaná pri zmene sumy aj pri zmene typu — fix Bug 3
    // ============================================================
    private void updateAmountPreview() {
        try {
            double val = Double.parseDouble(amountField.getText().replace(",", "."));
            boolean isIncome = typeCombo.getValue() != null &&
                    typeCombo.getValue().equals(Localization.get("transactions.type.income"));
            amountPreview.setText((isIncome ? "+ " : "- ") + formatCurrency(val));
        } catch (NumberFormatException e) {
            amountPreview.setText("");
        }
    }

    // ============================================================
    //  MODAL
    // ============================================================
    @FXML
    private void handleNewTransaction() {
        selectedTransaction = null;
        openModal(false);
    }

    @FXML
    private void handleEdit() {
        if (selectedTransaction == null) return;
        openModal(true);
    }

    @FXML
    private void handleDelete() {
        if (selectedTransaction == null) return;
        try {
            ServiceLocator.getTransactionService()
                    .deleteTransactions(List.of(selectedTransaction.getId()));
            selectedTransaction = null;
            loadTransactions();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openModal(boolean isEdit) {
        clearModalError();
        modalTitle.setText(isEdit
                ? Localization.get("transactions.modal.edit")
                : Localization.get("transactions.modal.new"));

        fromAccountLabel.setText(Localization.get("transactions.modal.from_account"));
        toAccountLabel.setText(Localization.get("transactions.modal.to_account"));
        typeLabel.setText(Localization.get("transactions.modal.type"));
        nameLabel.setText(Localization.get("transactions.modal.name"));
        categoryLabel.setText(Localization.get("transactions.modal.category"));
        needWantLabel.setText(Localization.get("transactions.modal.need_want"));
        dateLabel.setText(Localization.get("transactions.modal.date"));
        amountLabel.setText(Localization.get("transactions.modal.amount"));

        // Reset viditeľnosti
        fromAccountSection.setVisible(false);
        fromAccountSection.setManaged(false);
        toAccountSection.setVisible(false);
        toAccountSection.setManaged(false);

        // From Account combo
        fromAccountCombo.getItems().setAll(
                userAccounts.stream()
                        .map(this::accountDisplayName)
                        .collect(Collectors.toList()));
        if (!fromAccountCombo.getItems().isEmpty())
            fromAccountCombo.setValue(fromAccountCombo.getItems().get(0));

        fromAccountCombo.setOnAction(e -> updateToAccountOptions());
        updateToAccountOptions();

        // To Account combo
        toAccountCombo.getItems().setAll(
                userAccounts.stream()
                        .filter(a -> !a.isMainAccount())
                        .map(this::accountDisplayName)
                        .collect(Collectors.toList()));
        if (!toAccountCombo.getItems().isEmpty())
            toAccountCombo.setValue(toAccountCombo.getItems().get(0));

        // Type combo
        typeCombo.getItems().setAll(
                Localization.get("transactions.type.income"),
                Localization.get("transactions.type.expense")
        );
        typeCombo.setValue(Localization.get("transactions.type.expense"));

        // Fix Bug 3 — zmena typu refreshne aj amount preview
        typeCombo.setOnAction(e -> {
            onTypeChanged();
            updateAmountPreview();
        });

        // Categories combo
        categoryCombo.getItems().setAll(
                selectableCategories.stream().map(Category::getName).collect(Collectors.toList()));
        if (!categoryCombo.getItems().isEmpty())
            categoryCombo.setValue(categoryCombo.getItems().get(0));

        // Need/Want combo
        needWantCombo.getItems().setAll(
                Localization.get("dashboard.activities.need"),
                Localization.get("dashboard.activities.want")
        );
        needWantCombo.setValue(Localization.get("dashboard.activities.need"));

        // Reset polí
        betweenAccountsCheck.setSelected(false);
        nameField.clear();
        amountField.clear();
        amountPreview.setText("");
        datePicker.setValue(LocalDate.now());

        if (isEdit && selectedTransaction != null) {
            // Skryť polia ktoré sa pri edit nemenia
            betweenAccountsCheck.setVisible(false);
            betweenAccountsCheck.setManaged(false);
            typeSection.setVisible(false);
            typeSection.setManaged(false);
            fromAccountSection.setVisible(false);
            fromAccountSection.setManaged(false);
            toAccountSection.setVisible(false);
            toAccountSection.setManaged(false);

            // Naplň editovateľné polia
            nameField.setText(selectedTransaction.getDescription());
            amountField.setText(String.valueOf(selectedTransaction.getAmount()));
            datePicker.setValue(selectedTransaction.getCompleteDate().toLocalDate());

            // Zachovaj typ pre submitTransaction()
            if (selectedTransaction.isIncome()) {
                typeCombo.setValue(Localization.get("transactions.type.income"));
            } else {
                typeCombo.setValue(Localization.get("transactions.type.expense"));
            }

            // Fix: zisti či je to transfer transakcia
            boolean isTransfer = selectedTransaction.getCategoryId() != null &&
                    (selectedTransaction.getCategoryId() == Transaction.CATEGORY_SAVING ||
                            selectedTransaction.getCategoryId() == Transaction.CATEGORY_SAVING_EXPENSE ||
                            selectedTransaction.getCategoryId() == Transaction.CATEGORY_TRANSFER);

            if (isTransfer) {
                // Transfer — skry kategóriu aj need/want
                categorySection.setVisible(false);
                categorySection.setManaged(false);
                needWantCombo.setVisible(false);
                needWantCombo.setManaged(false);
                needWantLabel.setVisible(false);
                needWantLabel.setManaged(false);
            } else {
                // Štandardná transakcia — nastav kategóriu a need/want
                categorySection.setVisible(true);
                categorySection.setManaged(true);

                if (selectedTransaction.getCategoryId() != null) {
                    String catName = categories.stream()
                            .filter(c -> c.getId() == selectedTransaction.getCategoryId())
                            .map(Category::getName)
                            .findFirst()
                            .orElse(null);
                    if (catName != null) categoryCombo.setValue(catName);
                }

                boolean isExpense = !selectedTransaction.isIncome();
                needWantCombo.setVisible(isExpense);
                needWantCombo.setManaged(isExpense);
                needWantLabel.setVisible(isExpense);
                needWantLabel.setManaged(isExpense);

                if (selectedTransaction.isWant()) {
                    needWantCombo.setValue(Localization.get("dashboard.activities.want"));
                } else {
                    needWantCombo.setValue(Localization.get("dashboard.activities.need"));
                }
            }

        } else {
            // Nová transakcia — reset viditeľnosti
            betweenAccountsCheck.setVisible(true);
            betweenAccountsCheck.setManaged(true);
            typeSection.setVisible(true);
            typeSection.setManaged(true);
            onTypeChanged();
        }

        // Amount preview listener — používa updateAmountPreview() pre konzistenciu
        amountField.textProperty().addListener((obs, old, newVal) -> updateAmountPreview());

        // DatePicker fix pre ručné zadávanie
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        datePicker.getEditor().focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                String text = datePicker.getEditor().getText();
                if (text != null && !text.isEmpty()) {
                    try {
                        datePicker.setValue(LocalDate.parse(text, fmt));
                    } catch (Exception ignored) {}
                }
            }
        });

        final boolean[] pressedOnOverlay = {false};
        modalOverlay.setOnMousePressed(e -> pressedOnOverlay[0] = e.getTarget() == modalOverlay);
        modalOverlay.setOnMouseReleased(e -> {
            if (pressedOnOverlay[0] && e.getTarget() == modalOverlay) closeModal();
        });

        modalOverlay.setVisible(true);
        modalOverlay.setManaged(true);
    }

    private void updateToAccountOptions() {
        String fromVal = fromAccountCombo.getValue();
        if (fromVal == null) return;

        Account fromAccount = userAccounts.stream()
                .filter(a -> accountDisplayName(a).equals(fromVal))
                .findFirst().orElse(null);

        if (fromAccount == null) return;

        if (fromAccount.isMainAccount()) {
            toAccountCombo.getItems().setAll(
                    userAccounts.stream()
                            .filter(a -> !a.isMainAccount())
                            .map(this::accountDisplayName)
                            .collect(Collectors.toList()));
        } else {
            toAccountCombo.getItems().setAll(
                    userAccounts.stream()
                            .filter(Account::isMainAccount)
                            .map(this::accountDisplayName)
                            .collect(Collectors.toList()));
        }

        if (!toAccountCombo.getItems().isEmpty())
            toAccountCombo.setValue(toAccountCombo.getItems().get(0));
    }

    @FXML
    private void onBetweenAccountsChanged() {
        boolean checked = betweenAccountsCheck.isSelected();
        fromAccountSection.setVisible(checked);
        fromAccountSection.setManaged(checked);
        toAccountSection.setVisible(checked);
        toAccountSection.setManaged(checked);
        typeSection.setVisible(!checked);
        typeSection.setManaged(!checked);

        if (checked) {
            categorySection.setVisible(false);
            categorySection.setManaged(false);
        } else {
            typeCombo.getItems().setAll(
                    Localization.get("transactions.type.income"),
                    Localization.get("transactions.type.expense")
            );
            typeCombo.setValue(Localization.get("transactions.type.expense"));
            onTypeChanged();
        }
        updateAmountPreview();
    }

    private void onTypeChanged() {
        categorySection.setVisible(true);
        categorySection.setManaged(true);

        String type = typeCombo.getValue();
        boolean isExpense = type != null &&
                type.equals(Localization.get("transactions.type.expense"));
        needWantCombo.setVisible(isExpense);
        needWantCombo.setManaged(isExpense);
        needWantLabel.setVisible(isExpense);
        needWantLabel.setManaged(isExpense);
    }

    @FXML
    private void onModalConfirm() {
        submitTransaction();
    }

    @FXML
    private void onModalClose() {
        closeModal();
    }

    private void submitTransaction() {
        clearModalError();

        String name = nameField.getText().trim();
        String amountText = amountField.getText().trim();

        if (name.isEmpty()) { showModalError("transaction.error.description_required"); return; }
        if (amountText.isEmpty()) { showModalError("transaction.error.invalid_amount"); return; }
        if (datePicker.getValue() == null) { showModalError("transaction.error.date_required"); return; }

        double amount;
        try {
            amount = Double.parseDouble(amountText.replace(",", "."));
            if (amount <= 0) { showModalError("transaction.error.invalid_amount"); return; }
        } catch (NumberFormatException e) {
            showModalError("transaction.error.invalid_amount");
            return;
        }

        Account fromAccount;
        if (betweenAccountsCheck.isSelected()) {
            fromAccount = userAccounts.stream()
                    .filter(a -> accountDisplayName(a).equals(fromAccountCombo.getValue()))
                    .findFirst().orElse(null);
        } else {
            fromAccount = userAccounts.stream()
                    .filter(Account::isMainAccount)
                    .findFirst().orElse(null);
        }
        if (fromAccount == null) return;

        String typeVal = typeCombo.getValue();
        int typeId = typeVal != null && typeVal.equals(Localization.get("transactions.type.income"))
                ? Transaction.TYPE_INCOME : Transaction.TYPE_EXPENSE;

        Integer classificationId = null;
        if (typeId == Transaction.TYPE_EXPENSE && !betweenAccountsCheck.isSelected()) {
            classificationId = needWantCombo.getValue() != null &&
                    needWantCombo.getValue().equals(Localization.get("dashboard.activities.want"))
                    ? Transaction.CLASSIFICATION_WANT
                    : Transaction.CLASSIFICATION_NEED;
        }

        Integer categoryId = null;
        if (categoryCombo.getValue() != null) {
            categoryId = categories.stream()
                    .filter(c -> c.getName().equals(categoryCombo.getValue()))
                    .map(Category::getId)
                    .findFirst()
                    .orElse(1);
        }

        Integer targetAccountId = null;
        if (betweenAccountsCheck.isSelected()) {
            Account toAccount = userAccounts.stream()
                    .filter(a -> accountDisplayName(a).equals(toAccountCombo.getValue()))
                    .findFirst().orElse(null);
            if (toAccount != null) targetAccountId = toAccount.getId();
        }

        try {
            String currencyCode = ServiceLocator.getSettingsService().getCurrencyCode();
            if (selectedTransaction != null) {
                Transaction updated = Transaction.builder()
                        .id(selectedTransaction.getId())
                        .accountId(fromAccount.getId())
                        .targetAccountId(selectedTransaction.getTargetAccountId())
                        .transactionTypeId(typeId)
                        .transactionStatusId(Transaction.STATUS_COMPLETED)
                        .spendingClassificationId(classificationId)
                        .categoryId(categoryId != null ? categoryId : selectedTransaction.getCategoryId())
                        .amount(amount)
                        .currencyCode(currencyCode)
                        .description(name)
                        .completeDate(datePicker.getValue().atStartOfDay())
                        .createdAt(selectedTransaction.getCreatedAt())
                        .build();
                ServiceLocator.getTransactionService().updateTransaction(updated);
            } else {
                ServiceLocator.getTransactionService().addTransaction(
                        fromAccount.getId(),
                        typeId,
                        targetAccountId,
                        categoryId != null ? categoryId : 1,
                        classificationId,
                        name,
                        amount,
                        currencyCode,
                        datePicker.getValue()
                );
            }

            loadTransactions();
            closeModal();

        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.startsWith("transaction.error.")) {
                showModalError(msg);
            } else {
                showModalError("error.db_error");
            }
        }
    }

    private void closeModal() {
        modalOverlay.setVisible(false);
        modalOverlay.setManaged(false);
    }

    private void setupDatePicker() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        datePicker.setConverter(new javafx.util.StringConverter<LocalDate>() {
            @Override
            public String toString(LocalDate date) {
                return date != null ? date.format(fmt) : "";
            }
            @Override
            public LocalDate fromString(String string) {
                if (string == null || string.isEmpty()) return null;
                try {
                    return LocalDate.parse(string, fmt);
                } catch (Exception e) {
                    return null;
                }
            }
        });
    }

    private void showModalError(String key) {
        modalErrorLabel.setText(Localization.get(key));
        modalErrorLabel.setVisible(true);
        modalErrorLabel.setManaged(true);
    }

    private void clearModalError() {
        modalErrorLabel.setVisible(false);
        modalErrorLabel.setManaged(false);
    }

    private String formatCurrency(double value) {
        return CurrencyFormatUtil.format(value);
    }

    private String accountDisplayName(Account account) {
        return switch (account.getAccountTypeId()) {
            case Account.MAIN_ACCOUNT   -> Localization.get("dashboard.account.main");
            case Account.EMERGENCY_FUND -> Localization.get("dashboard.account.emergency");
            default -> account.getDescription() != null && !account.getDescription().isBlank()
                    ? account.getDescription()
                    : Localization.get("dashboard.account.default");
        };
    }
}