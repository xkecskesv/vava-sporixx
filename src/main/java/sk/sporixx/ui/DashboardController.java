package sk.sporixx.ui;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import sk.sporixx.dto.AccountsSummaryData;
import sk.sporixx.dto.ActivitiesData;
import sk.sporixx.dto.AnalyticsData;
import sk.sporixx.dto.ChartPeriod;
import sk.sporixx.model.Account;
import sk.sporixx.model.RecurringRule;
import sk.sporixx.model.Transaction;
import sk.sporixx.service.ServiceLocator;
import sk.sporixx.util.Localization;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;

public class DashboardController {

    // FXML - Header
    @FXML private Label dashboardTitle;
    @FXML private Label totalBalanceLabel;
    @FXML private Label totalBalance;

    // FXML - Accounts
    @FXML private HBox accountsContainer;

    // FXML - Analytics
    @FXML private Label analyticsTitle;
    @FXML public ComboBox <String> periodComboBox;
    @FXML private Label analyticsAmount;
    @FXML private Label totalIncomeLabel;
    @FXML private LineChart<String, Number> analyticsChart;

    // FXML - Activities
    @FXML private Label activitiesTitle;
    @FXML private VBox activitiesList;

    private VBox selectedCard = null;
    private ChartPeriod currentChartPeriod = ChartPeriod.TWELVE_MONTHS;

    @FXML
    public void initialize() {
        // Lokalizácia statických textov
        dashboardTitle.setText(Localization.get("dashboard.title"));
        totalBalanceLabel.setText(Localization.get("dashboard.total_balance"));
        analyticsTitle.setText(Localization.get("dashboard.analytics"));
        setupPeriodComboBox();
        totalIncomeLabel.setText(Localization.get("dashboard.analytics.total_income"));
        activitiesTitle.setText(Localization.get("dashboard.activities"));

        // Načítanie dát zo service vrstvy
        AccountsSummaryData accountsData = ServiceLocator.getOverviewService().loadAccountsSummary();
        AnalyticsData analyticsData = ServiceLocator.getOverviewService().loadAnalytics(currentChartPeriod, accountsData.getAccounts().get(0).getId());
        ActivitiesData activitiesData = ServiceLocator.getOverviewService().loadActivities(accountsData.getAccounts().get(0).getId());

        loadTotalBalance(accountsData);
        loadAccounts(accountsData);
        loadAnalyticsChart(analyticsData);
        loadActivities(activitiesData);
    }

    // ============================================================
    //  TOTAL BALANCE
    // ============================================================
    private void loadTotalBalance(AccountsSummaryData data) {
        totalBalance.setText(formatCurrency(data.getTotalBalance()));
    }

    // ============================================================
    //  ACCOUNTS
    // ============================================================
    private void loadAccounts(AccountsSummaryData data) {
        accountsContainer.getChildren().clear();

        for (int i = 0; i < data.getAccounts().size(); i++) {
            VBox card = createAccountCard(data.getAccounts().get(i), i == 0);
            accountsContainer.getChildren().add(card);
        }

        // "+" karta
        VBox addCard = new VBox();
        addCard.getStyleClass().add("account-card-add");
        addCard.setAlignment(Pos.CENTER);
        HBox.setHgrow(addCard, Priority.ALWAYS);
        Label plus = new Label("+");
        plus.getStyleClass().add("account-card-plus");
        addCard.getChildren().add(plus);
        accountsContainer.getChildren().add(addCard);

        // Šípka
        VBox arrow = new VBox();
        arrow.getStyleClass().add("accounts-arrow");
        arrow.setAlignment(Pos.CENTER);
        Label arrowIcon = new Label("›");
        arrowIcon.getStyleClass().add("accounts-arrow-icon");
        arrow.getChildren().add(arrowIcon);
        accountsContainer.getChildren().add(arrow);
    }

    private VBox createAccountCard(Account account, boolean active) {
        VBox card = new VBox(8);
        card.getStyleClass().add(active ? "account-card-active" : "account-card");
        card.setUserData(account);
        HBox.setHgrow(card, Priority.ALWAYS);

        // Header (názov + ikona)
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(getAccountDisplayName(account));
        title.getStyleClass().add(active ? "account-card-title-active" : "account-card-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(title, spacer);

        try {
            ImageView icon = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream(getAccountIconPath(account, active)))));
            icon.setFitWidth(20);
            icon.setFitHeight(20);
            icon.setPreserveRatio(true);
            header.getChildren().add(icon);
        } catch (Exception e) {
            // Ikona sa nenašla
        }

        // Popis
        Label desc = new Label(account.getDescription());
        desc.getStyleClass().add(active ? "account-card-desc-active" : "account-card-desc");

        // Spacer
        Region vspacer = new Region();
        VBox.setVgrow(vspacer, Priority.ALWAYS);

        // Suma
        Label amount = new Label(formatCurrency(account.getCurrentBalance()));
        amount.getStyleClass().add(active ? "account-card-amount-active" : "account-card-amount");

        card.getChildren().addAll(header, desc, vspacer, amount);
        card.setOnMouseClicked(e -> selectCard(card));

        if (active) selectedCard = card;

        return card;
    }

    private String getAccountDisplayName(Account account) {
        if (account.isMainAccount()) return Localization.get("dashboard.account.main");
        if (account.isEmergencyFund()) return Localization.get("dashboard.account.emergency");
        if (account.isSavingAccount()) return Localization.get("dashboard.account.saving");
        return Localization.get("dashboard.account.default");
    }

    private String getAccountIconPath(Account account, boolean active) {
        if (account.isMainAccount()) {
            return active ? "/assets/icons/main_acc_icon.png" : "/assets/icons/main_acc_icon_dark.png";
        }
        if (account.isEmergencyFund()) {
            return active ? "/assets/icons/emergency_fund_icon_light.png" : "/assets/icons/emergency_fund_icon.png";
        }
        if (account.isSavingAccount()) {
            return active ? "/assets/icons/saving_acc_icon_white.png" : "/assets/icons/saving_acc_icon_dark.png";
        }
        return active ? "/assets/icons/main_acc_icon.png" : "/assets/icons/main_acc_icon_dark.png";
    }

    // ============================================================
    //  CARD SELECTION
    // ============================================================
    private void selectCard(VBox card) {
        if (selectedCard == card) return;

        if (selectedCard != null) {
            setCardActive(selectedCard, false);
        }
        setCardActive(card, true);
        selectedCard = card;

        // Preloaduj analytics pre vybraný účet
        Account account = (Account) card.getUserData();
        if (account != null) {
            reloadAnalytics(account);
        }
    }

    private void reloadAnalytics(Account account) {
        AnalyticsData data = ServiceLocator.getOverviewService().loadAnalytics(currentChartPeriod, account.getId());
        loadAnalyticsChart(data);
    }

    private void setCardActive(VBox card, boolean active) {
        card.getStyleClass().setAll(active ? "account-card-active" : "account-card");

        for (var node : card.getChildren()) {
            if (node instanceof HBox hbox) {
                for (var child : hbox.getChildren()) {
                    if (child instanceof Label label) {
                        label.getStyleClass().setAll(active ? "account-card-title-active" : "account-card-title");
                    }
                    if (child instanceof ImageView icon) {
                        Account account = (Account) card.getUserData();
                        if (account != null) {
                            try {
                                icon.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream(getAccountIconPath(account, active)))));
                            } catch (Exception e) {
                                // Ikona sa nenašla
                            }
                        }
                    }
                }
            }
            if (node instanceof Label label) {
                if (label.getText().startsWith("€")) {
                    label.getStyleClass().setAll(active ? "account-card-amount-active" : "account-card-amount");
                } else {
                    label.getStyleClass().setAll(active ? "account-card-desc-active" : "account-card-desc");
                }
            }
        }
    }

    // ============================================================
    //  ANALYTICS CHART
    // ============================================================
    private void loadAnalyticsChart(AnalyticsData data) {
        analyticsChart.getData().clear();

        double totalIncome = data.getChartData().values().stream().mapToDouble(Double::doubleValue).sum();
        analyticsAmount.setText(formatCurrency(totalIncome));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        DateTimeFormatter inputFormatter;
        DateTimeFormatter displayFormatter;

        if (data.getChartPeriod().isGroupByDay()) {
            inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            displayFormatter = DateTimeFormatter.ofPattern("dd MMM");
        } else {
            inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
            displayFormatter = DateTimeFormatter.ofPattern("MMM");
        }

        data.getChartData().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    String displayLabel;
                    try {
                        if (data.getChartPeriod().isGroupByDay()) {
                            displayLabel = LocalDate.parse(entry.getKey(), inputFormatter).format(displayFormatter);
                        } else {
                            displayLabel = LocalDate.parse(entry.getKey() + "-01", DateTimeFormatter.ofPattern("yyyy-MM-dd")).format(displayFormatter);
                        }
                    } catch (Exception e) {
                        displayLabel = entry.getKey();
                    }
                    series.getData().add(new XYChart.Data<>(displayLabel, entry.getValue()));
                });

        analyticsChart.getData().add(series);
    }

    // ============================================================
    //  ACTIVITIES
    // ============================================================
    private void loadActivities(ActivitiesData data) {
        activitiesList.getChildren().clear();

        // Upcoming payments
        if (data.getUpcomingPayments() != null && !data.getUpcomingPayments().isEmpty()) {
            activitiesList.getChildren().add(
                    createGroupTitle(Localization.get("dashboard.activities.upcoming")));
            for (RecurringRule rule : data.getUpcomingPayments()) {
                activitiesList.getChildren().add(createRecurringRow(rule));
            }
        }

        // Recent transactions zoskupené podľa dňa
        if (data.getRecentTransactions() != null && !data.getRecentTransactions().isEmpty()) {
            LocalDate today = LocalDate.now();
            LocalDate yesterday = today.minusDays(1);
            String currentGroup = "";

            for (Transaction tx : data.getRecentTransactions()) {
                LocalDate txDate = tx.getCompleteDate().toLocalDate();
                String groupName;

                if (txDate.equals(today)) {
                    groupName = Localization.get("dashboard.activities.today");
                } else if (txDate.equals(yesterday)) {
                    groupName = Localization.get("dashboard.activities.yesterday");
                } else {
                    groupName = txDate.format(DateTimeFormatter.ofPattern("dd MMM yy"));
                }

                if (!groupName.equals(currentGroup)) {
                    activitiesList.getChildren().add(createGroupTitle(groupName));
                    currentGroup = groupName;
                }

                activitiesList.getChildren().add(createTransactionRow(tx));
            }
        }
    }

    private Label createGroupTitle(String title) {
        Label label = new Label(title);
        label.getStyleClass().add("activities-group-title");
        return label;
    }

    private HBox createTransactionRow(Transaction trans) {
        HBox row = new HBox(10);
        row.getStyleClass().add("activity-row");
        row.setAlignment(Pos.CENTER_LEFT);

        String iconPath = trans.isIncome() ? "/assets/icons/income_icon.png" : "/assets/icons/sent_icon.png";
        try {
            ImageView icon = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream(iconPath))));
            icon.setFitWidth(28);
            icon.setFitHeight(28);
            icon.setPreserveRatio(true);
            row.getChildren().add(icon);
        } catch (Exception e) {
            // Ikona sa nenašla
        }

        VBox info = new VBox(1);
        HBox.setHgrow(info, Priority.ALWAYS);

        String classification = trans.isWant()
                ? Localization.get("dashboard.activities.want")
                : Localization.get("dashboard.activities.need");
        Label name = new Label(trans.getDescription() + " - " + classification);
        name.getStyleClass().add("activity-name");

        Label type = new Label(trans.isIncome()
                ? Localization.get("dashboard.activities.incoming")
                : Localization.get("dashboard.activities.sent"));
        type.getStyleClass().add("activity-type");

        info.getChildren().addAll(name, type);
        row.getChildren().add(info);

        String prefix = trans.isIncome() ? "+ " : "- ";
        Label amount = new Label(prefix + formatCurrency(trans.getAmount()));
        amount.getStyleClass().add(trans.isIncome() ? "activity-amount-positive" : "activity-amount-negative");
        row.getChildren().add(amount);

        return row;
    }

    private HBox createRecurringRow(RecurringRule rule) {
        HBox row = new HBox(10);
        row.getStyleClass().add("activity-row");
        row.setAlignment(Pos.CENTER_LEFT);

        try {
            ImageView icon = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/icons/sent_icon.png"))));
            icon.setFitWidth(28);
            icon.setFitHeight(28);
            icon.setPreserveRatio(true);
            row.getChildren().add(icon);
        } catch (Exception e) {
            // Ikona sa nenašla
        }

        VBox info = new VBox(1);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label name = new Label(rule.getDescription());
        name.getStyleClass().add("activity-name");
        Label type = new Label(Localization.get("dashboard.activities.sent"));
        type.getStyleClass().add("activity-type");
        info.getChildren().addAll(name, type);
        row.getChildren().add(info);

        Label amount = new Label("- " + formatCurrency(rule.getAmount()));
        amount.getStyleClass().add("activity-amount-negative");
        row.getChildren().add(amount);

        return row;
    }

    private void setupPeriodComboBox() {
        periodComboBox.getItems().addAll(
                Localization.get("dashboard.analytics.period.week"),
                Localization.get("dashboard.analytics.period.month"),
                Localization.get("dashboard.analytics.period.six_months"),
                Localization.get("dashboard.analytics.period.twelve_months")
        );
        periodComboBox.setValue(Localization.get("dashboard.analytics.period.twelve_months"));
        periodComboBox.setOnAction(e -> onPeriodChanged());
    }

    private void onPeriodChanged() {
        String selected = periodComboBox.getValue();

        if (selected.equals(Localization.get("dashboard.analytics.period.week"))) {
            currentChartPeriod = ChartPeriod.ONE_WEEK;
        } else if (selected.equals(Localization.get("dashboard.analytics.period.month"))) {
            currentChartPeriod = ChartPeriod.ONE_MONTH;
        } else if (selected.equals(Localization.get("dashboard.analytics.period.six_months"))) {
            currentChartPeriod = ChartPeriod.SIX_MONTHS;
        } else {
            currentChartPeriod = ChartPeriod.TWELVE_MONTHS;
        }

        // Reload analytics pre aktuálny účet a nové obdobie
        if (selectedCard != null) {
            Account account = (Account) selectedCard.getUserData();
            if (account != null) {
                reloadAnalytics(account);
            }
        }
    }

    // ============================================================
    //  HELPER
    // ============================================================
    private String formatCurrency(double value) {
        return String.format("€%,.2f", value);
    }
}