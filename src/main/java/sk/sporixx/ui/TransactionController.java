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
import sk.sporixx.util.Localization;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    private List<Transaction> allTransactions = new ArrayList<>();
    private List<Transaction> filteredTransactions = new ArrayList<>();
    private Transaction selectedTransaction = null;
    private String activeAccountFilter = "ALL";
    private List<Account> userAccounts = new ArrayList<>();
    private List<Category> categories = new ArrayList<>();

    @FXML
    public void initialize() {
        userAccounts = SessionManager.getInstance().getAccounts();
        categories = ServiceLocator.getCategoryService().getCategories();

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
            SearchCriteria.SearchCriteriaBuilder builder = SearchCriteria.builder()
                    .searchText(searchField.getText().trim());

            if (!activeAccountFilter.equals("ALL")) {
                int accountId = Integer.parseInt(activeAccountFilter);
                filteredTransactions = ServiceLocator.getTransactionService()
                        .searchTransactions(builder.build(), accountId);
            } else {
                filteredTransactions = ServiceLocator.getTransactionService()
                        .searchTransactions(builder.build());
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

        // Name
        Label name = new Label(tx.getDescription());
        name.getStyleClass().add("table-cell");
        name.setPrefWidth(220);

        // Category — null-safe lookup
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
        transactionsList.getChildren().forEach(n ->
                n.getStyleClass().setAll("table-row"));
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
                        .map(Account::getDescription)
                        .collect(Collectors.toList()));
        if (!fromAccountCombo.getItems().isEmpty())
            fromAccountCombo.setValue(fromAccountCombo.getItems().get(0));

        fromAccountCombo.setOnAction(e -> updateToAccountOptions());
        updateToAccountOptions();

        // To Account combo
        toAccountCombo.getItems().setAll(
                userAccounts.stream()
                        .filter(a -> !a.isMainAccount())
                        .map(Account::getDescription)
                        .collect(Collectors.toList()));
        if (!toAccountCombo.getItems().isEmpty())
            toAccountCombo.setValue(toAccountCombo.getItems().get(0));

        // Type combo
        typeCombo.getItems().setAll(
                Localization.get("transactions.type.income"),
                Localization.get("transactions.type.expense"),
                Localization.get("transactions.type.investment")
        );
        typeCombo.setValue(Localization.get("transactions.type.expense"));
        typeCombo.setOnAction(e -> onTypeChanged());

        // Categories combo
        categoryCombo.getItems().setAll(
                categories.stream().map(Category::getName).collect(Collectors.toList()));
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

            // Kategória — null-safe
            if (selectedTransaction.getCategoryId() != null) {
                String catName = categories.stream()
                        .filter(c -> c.getId() == selectedTransaction.getCategoryId())
                        .map(Category::getName)
                        .findFirst()
                        .orElse(null);
                if (catName != null) categoryCombo.setValue(catName);
            }

            // Need/Want
            if (selectedTransaction.isWant()) {
                needWantCombo.setValue(Localization.get("dashboard.activities.want"));
            } else {
                needWantCombo.setValue(Localization.get("dashboard.activities.need"));
            }

            // Zachovaj typ pre submitTransaction()
            if (selectedTransaction.isIncome()) {
                typeCombo.setValue(Localization.get("transactions.type.income"));
            } else {
                typeCombo.setValue(Localization.get("transactions.type.expense"));
            }

            // Category vždy viditeľná, need/want len pre expense
            categorySection.setVisible(true);
            categorySection.setManaged(true);
            boolean isExpense = !selectedTransaction.isIncome();
            needWantCombo.setVisible(isExpense);
            needWantCombo.setManaged(isExpense);
            needWantLabel.setVisible(isExpense);
            needWantLabel.setManaged(isExpense);

        } else {
            // Nová transakcia — reset viditeľnosti
            betweenAccountsCheck.setVisible(true);
            betweenAccountsCheck.setManaged(true);
            typeSection.setVisible(true);
            typeSection.setManaged(true);
            onTypeChanged();
        }

        // Amount preview listener
        amountField.textProperty().addListener((obs, old, newVal) -> {
            try {
                double val = Double.parseDouble(newVal.replace(",", "."));
                boolean isIncome = typeCombo.getValue()
                        .equals(Localization.get("transactions.type.income"));
                amountPreview.setText((isIncome ? "+ " : "- ") + formatCurrency(val));
            } catch (NumberFormatException e) {
                amountPreview.setText("");
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
                .filter(a -> a.getDescription().equals(fromVal))
                .findFirst().orElse(null);

        if (fromAccount == null) return;

        if (fromAccount.isMainAccount()) {
            toAccountCombo.getItems().setAll(
                    userAccounts.stream()
                            .filter(a -> !a.isMainAccount())
                            .map(Account::getDescription)
                            .collect(Collectors.toList()));
        } else {
            toAccountCombo.getItems().setAll(
                    userAccounts.stream()
                            .filter(Account::isMainAccount)
                            .map(Account::getDescription)
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
                    Localization.get("transactions.type.expense"),
                    Localization.get("transactions.type.investment")
            );
            typeCombo.setValue(Localization.get("transactions.type.expense"));
            onTypeChanged();
        }
    }

    private void onTypeChanged() {
        categorySection.setVisible(true);
        categorySection.setManaged(true);

        // Need/Want len pre expense
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
        String name = nameField.getText().trim();
        String amountText = amountField.getText().trim();

        if (name.isEmpty() || amountText.isEmpty() || datePicker.getValue() == null) return;

        double amount;
        try {
            amount = Double.parseDouble(amountText.replace(",", "."));
        } catch (NumberFormatException e) {
            return;
        }

        // From account
        Account fromAccount;
        if (betweenAccountsCheck.isSelected()) {
            fromAccount = userAccounts.stream()
                    .filter(a -> a.getDescription().equals(fromAccountCombo.getValue()))
                    .findFirst().orElse(null);
        } else {
            fromAccount = userAccounts.stream()
                    .filter(Account::isMainAccount)
                    .findFirst().orElse(null);
        }
        if (fromAccount == null) return;

        // Type
        String typeVal = typeCombo.getValue();
        int typeId = typeVal.equals(Localization.get("transactions.type.income"))
                ? Transaction.TYPE_INCOME : Transaction.TYPE_EXPENSE;

        // Classification — len pre expense
        Integer classificationId = null;
        if (typeId == Transaction.TYPE_EXPENSE && !betweenAccountsCheck.isSelected()) {
            classificationId = needWantCombo.getValue()
                    .equals(Localization.get("dashboard.activities.want"))
                    ? Transaction.CLASSIFICATION_WANT
                    : Transaction.CLASSIFICATION_NEED;
        }

        // Category — null-safe
        Integer categoryId = null;
        if (categoryCombo.getValue() != null) {
            categoryId = categories.stream()
                    .filter(c -> c.getName().equals(categoryCombo.getValue()))
                    .map(Category::getId)
                    .findFirst()
                    .orElse(1);
        }

        // Target account pre transfer
        Integer targetAccountId = null;
        if (betweenAccountsCheck.isSelected()) {
            Account toAccount = userAccounts.stream()
                    .filter(a -> a.getDescription().equals(toAccountCombo.getValue()))
                    .findFirst().orElse(null);
            if (toAccount != null) targetAccountId = toAccount.getId();
        }

        try {
            if (selectedTransaction != null) {
                Transaction updated = Transaction.builder()
                        .id(selectedTransaction.getId())
                        .accountId(fromAccount.getId())
                        .targetAccountId(targetAccountId)
                        .transactionTypeId(typeId)
                        .transactionStatusId(Transaction.STATUS_COMPLETED)
                        .spendingClassificationId(classificationId)
                        .categoryId(categoryId)
                        .amount(amount)
                        .currencyCode(fromAccount.getDefaultCurrencyCode())
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
                        categoryId,
                        classificationId,
                        name,
                        amount,
                        fromAccount.getDefaultCurrencyCode(),
                        datePicker.getValue()
                );
            }

            loadTransactions();
            closeModal();

        } catch (Exception e) {
            e.printStackTrace();
        }
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