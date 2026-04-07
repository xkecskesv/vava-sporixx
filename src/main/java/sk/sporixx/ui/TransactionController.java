package sk.sporixx.ui;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import sk.sporixx.model.Account;
import sk.sporixx.model.Transaction;
import sk.sporixx.service.SessionManager;
import sk.sporixx.util.Localization;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class TransactionController {

    // Header
    @FXML private ImageView userAvatar;

    // Tabuľka
    @FXML private Button newTransactionBtn;
    @FXML private Button editBtn;
    @FXML private Button deleteBtn;
    @FXML private TextField searchField;
    @FXML private HBox accountFilterContainer;
    @FXML private VBox transactionsList;

    // Modal
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

    // Stav
    private List<Transaction> allTransactions = new ArrayList<>();
    private List<Transaction> filteredTransactions = new ArrayList<>();
    private Transaction selectedTransaction = null;
    private String activeAccountFilter = "ALL";
    private List<Account> userAccounts = new ArrayList<>();

    // Kategórie — TODO: nahradiť service volaním
    private static final List<String> CATEGORIES = List.of(
            "Food", "Transport", "Clothing", "Entertainment",
            "Health", "Utilities", "Rent", "Other"
    );

    @FXML
    public void initialize() {
        userAccounts = SessionManager.getInstance().getAccounts();

        loadUserAvatar();
        loadMockTransactions();
        setupAccountFilters();
        setupSearch();
        renderTransactions(allTransactions);
        setupDatePicker();
    }

    // ============================================================
    //  AVATAR
    // ============================================================
    private void loadUserAvatar() {
        try {
            String photoPath = SessionManager.getInstance().getCurrentUser().getPhotoPath();
            if (photoPath != null && !photoPath.isBlank()) {
                userAvatar.setImage(new Image(photoPath));
            } else {
                userAvatar.setImage(new Image(Objects.requireNonNull(
                        getClass().getResourceAsStream("/assets/icons/default_profile_picture.png"))));
            }
        } catch (Exception e) {
            // fallback
        }
    }

    // ============================================================
    //  MOCK DÁTA — TODO: nahradiť TransactionService
    // ============================================================
    private void loadMockTransactions() {
        // Prázdne — service nie je hotový
        allTransactions = new ArrayList<>();
        filteredTransactions = new ArrayList<>(allTransactions);
    }

    // ============================================================
    //  ACCOUNT FILTERS
    // ============================================================
    private void setupAccountFilters() {
        accountFilterContainer.getChildren().clear();

        Button allBtn = createFilterButton("ALL",
                Localization.get("transactions.filter.all"));
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
        String query = searchField.getText().trim().toLowerCase();

        filteredTransactions = allTransactions.stream()
                .filter(t -> {
                    if (!activeAccountFilter.equals("ALL")) {
                        if (t.getAccountId() != Integer.parseInt(activeAccountFilter)) return false;
                    }
                    if (!query.isEmpty()) {
                        return t.getDescription().toLowerCase().contains(query);
                    }
                    return true;
                })
                .collect(Collectors.toList());

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

        row.setOnMouseClicked(e -> {
            selectTransaction(tx, row);
        });

        // Name
        Label name = new Label(tx.getDescription());
        name.getStyleClass().add("table-cell");
        name.setPrefWidth(220);

        // Category — TODO: category name z ID
        Label category = new Label(String.valueOf(tx.getCategoryId()));
        category.getStyleClass().add("table-cell");
        category.setPrefWidth(140);

        // Need/Want
        String nw = tx.isWant() ? Localization.get("dashboard.activities.want")
                : tx.isNeed() ? Localization.get("dashboard.activities.need") : "-";
        Label needWant = new Label(nw);
        needWant.getStyleClass().add("table-cell");
        needWant.setPrefWidth(100);

        // Type
        String type = tx.isIncome()
                ? Localization.get("dashboard.activities.incoming")
                : Localization.get("dashboard.activities.sent");
        Label typeLabel = new Label(type);
        typeLabel.getStyleClass().add("table-cell");
        typeLabel.setPrefWidth(100);

        // Date
        Label date = new Label(tx.getCompleteDate()
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
        date.getStyleClass().add("table-cell");
        date.setPrefWidth(120);

        // Amount
        String prefix = tx.isIncome() ? "+ " : "- ";
        Label amount = new Label(prefix + formatCurrency(tx.getAmount()));
        amount.getStyleClass().add(tx.isIncome()
                ? "table-cell-amount-positive" : "table-cell-amount-negative");
        amount.setPrefWidth(100);

        row.getChildren().addAll(name, category, needWant, typeLabel, date, amount);
        return row;
    }

    private void selectTransaction(Transaction tx, HBox row) {
        // Deselect všetky
        transactionsList.getChildren().forEach(n -> n.getStyleClass().setAll("table-row"));
        row.getStyleClass().setAll("table-row-selected");
        selectedTransaction = tx;
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
        // TODO: TransactionService.deleteTransaction
        allTransactions.remove(selectedTransaction);
        selectedTransaction = null;
        applyFilters();
    }

    private void openModal(boolean isEdit) {
        modalTitle.setText(isEdit
                ? Localization.get("transactions.modal.edit")
                : Localization.get("transactions.modal.new"));

        // Labels
        fromAccountLabel.setText(Localization.get("transactions.modal.from_account"));
        toAccountLabel.setText(Localization.get("transactions.modal.to_account"));
        typeLabel.setText(Localization.get("transactions.modal.type"));
        nameLabel.setText(Localization.get("transactions.modal.name"));
        categoryLabel.setText(Localization.get("transactions.modal.category"));
        needWantLabel.setText(Localization.get("transactions.modal.need_want"));
        dateLabel.setText(Localization.get("transactions.modal.date"));
        amountLabel.setText(Localization.get("transactions.modal.amount"));

        // From Account — len Main Account
        fromAccountCombo.getItems().setAll(
                userAccounts.stream()
                        .filter(Account::isMainAccount)
                        .map(Account::getDescription)
                        .collect(Collectors.toList()));
        if (!fromAccountCombo.getItems().isEmpty())
            fromAccountCombo.setValue(fromAccountCombo.getItems().get(0));

        // To Account — všetky okrem main
        toAccountCombo.getItems().setAll(
                userAccounts.stream()
                        .filter(a -> !a.isMainAccount())
                        .map(Account::getDescription)
                        .collect(Collectors.toList()));
        if (!toAccountCombo.getItems().isEmpty())
            toAccountCombo.setValue(toAccountCombo.getItems().get(0));

        // Type
        typeCombo.getItems().setAll(
                Localization.get("transactions.type.income"),
                Localization.get("transactions.type.expense"),
                Localization.get("transactions.type.saving"),
                Localization.get("transactions.type.investment")
        );
        typeCombo.setValue(Localization.get("transactions.type.expense"));
        typeCombo.setOnAction(e -> onTypeChanged());

        // Categories
        categoryCombo.getItems().setAll(CATEGORIES);
        if (!categoryCombo.getItems().isEmpty())
            categoryCombo.setValue(categoryCombo.getItems().get(0));

        // Need/Want
        needWantCombo.getItems().setAll(
                Localization.get("dashboard.activities.need"),
                Localization.get("dashboard.activities.want")
        );
        needWantCombo.setValue(Localization.get("dashboard.activities.need"));

        // Reset
        betweenAccountsCheck.setSelected(false);
        toAccountSection.setVisible(false);
        toAccountSection.setManaged(false);
        categorySection.setVisible(false);
        categorySection.setManaged(false);
        nameField.clear();
        amountField.clear();
        amountPreview.setText("");
        datePicker.setValue(LocalDate.now());

        // Ak edit — naplň polia
        if (isEdit && selectedTransaction != null) {
            nameField.setText(selectedTransaction.getDescription());
            amountField.setText(String.valueOf(selectedTransaction.getAmount()));
            datePicker.setValue(selectedTransaction.getCompleteDate().toLocalDate());
            if (selectedTransaction.getTargetAccountId() != null) {
                betweenAccountsCheck.setSelected(true);
                toAccountSection.setVisible(true);
                toAccountSection.setManaged(true);
            }
        }

        // Amount preview listener
        amountField.textProperty().addListener((obs, old, newVal) -> {
            try {
                double val = Double.parseDouble(newVal.replace(",", "."));
                boolean isIncome = typeCombo.getValue()
                        .equals(Localization.get("transactions.type.income"));
                amountPreview.setText((isIncome ? "+ " : "") + formatCurrency(val));
            } catch (NumberFormatException e) {
                amountPreview.setText("");
            }
        });

        // Overlay zatvorenie
        final boolean[] pressedOnOverlay = {false};
        modalOverlay.setOnMousePressed(e -> pressedOnOverlay[0] = e.getTarget() == modalOverlay);
        modalOverlay.setOnMouseReleased(e -> {
            if (pressedOnOverlay[0] && e.getTarget() == modalOverlay) closeModal();
        });

        modalOverlay.setVisible(true);
        modalOverlay.setManaged(true);
    }

    @FXML
    private void onBetweenAccountsChanged() {
        boolean checked = betweenAccountsCheck.isSelected();
        toAccountSection.setVisible(checked);
        toAccountSection.setManaged(checked);

        if (checked) {
            typeCombo.getItems().setAll(Localization.get("transactions.type.saving"));
            typeCombo.setValue(Localization.get("transactions.type.saving"));
            categorySection.setVisible(false);
            categorySection.setManaged(false);
        } else {
            typeCombo.getItems().setAll(
                    Localization.get("transactions.type.income"),
                    Localization.get("transactions.type.expense"),
                    Localization.get("transactions.type.saving"),
                    Localization.get("transactions.type.investment")
            );
            typeCombo.setValue(Localization.get("transactions.type.expense"));
            onTypeChanged();
        }
    }

    private void onTypeChanged() {
        String type = typeCombo.getValue();
        boolean isExpense = type.equals(Localization.get("transactions.type.expense"));
        categorySection.setVisible(isExpense);
        categorySection.setManaged(isExpense);
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
        String name = nameField.getText().trim();
        String amountText = amountField.getText().trim();

        if (name.isEmpty() || amountText.isEmpty() || datePicker.getValue() == null) return;

        double amount;
        try {
            amount = Double.parseDouble(amountText.replace(",", "."));
        } catch (NumberFormatException e) {
            return;
        }

        String typeVal = typeCombo.getValue();
        int typeId = typeVal.equals(Localization.get("transactions.type.income"))
                ? Transaction.TYPE_INCOME : Transaction.TYPE_EXPENSE;

        Integer classificationId = null;
        if (typeId == Transaction.TYPE_EXPENSE) {
            classificationId = needWantCombo.getValue()
                    .equals(Localization.get("dashboard.activities.want"))
                    ? Transaction.CLASSIFICATION_WANT
                    : Transaction.CLASSIFICATION_NEED;
        }

        // Nájdi from account
        Account fromAccount = userAccounts.stream()
                .filter(a -> a.getDescription().equals(fromAccountCombo.getValue()))
                .findFirst().orElse(null);
        if (fromAccount == null) return;

        Integer targetAccountId = null;
        if (betweenAccountsCheck.isSelected()) {
            Account toAccount = userAccounts.stream()
                    .filter(a -> a.getDescription().equals(toAccountCombo.getValue()))
                    .findFirst().orElse(null);
            if (toAccount != null) targetAccountId = toAccount.getId();
        }

        Transaction tx = Transaction.builder()
                .accountId(fromAccount.getId())
                .targetAccountId(targetAccountId)
                .transactionTypeId(typeId)
                .transactionStatusId(Transaction.STATUS_COMPLETED)
                .spendingClassificationId(classificationId)
                .categoryId(1) // TODO: category lookup
                .amount(amount)
                .currencyCode(fromAccount.getDefaultCurrencyCode())
                .description(name)
                .completeDate(datePicker.getValue().atStartOfDay())
                .createdAt(LocalDateTime.now())
                .build();

        // TODO: TransactionService.addTransaction(tx)
        if (selectedTransaction != null) {
            allTransactions.remove(selectedTransaction);
        }
        allTransactions.add(tx);

        applyFilters();
        closeModal();
    }

    private void closeModal() {
        modalOverlay.setVisible(false);
        modalOverlay.setManaged(false);
    }

    private void setupDatePicker() {
        datePicker.setConverter(new javafx.util.StringConverter<LocalDate>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

            @Override
            public String toString(LocalDate date) {
                return date != null ? date.format(formatter) : "";
            }

            @Override
            public LocalDate fromString(String string) {
                return (string != null && !string.isEmpty())
                        ? LocalDate.parse(string, formatter) : null;
            }
        });
    }

    private String formatCurrency(double value) {
        return String.format("€%,.2f", value);
    }
}