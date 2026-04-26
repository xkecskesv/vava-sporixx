package sk.sporixx.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import sk.sporixx.dto.BudgetData;
import sk.sporixx.dto.BudgetWarning;
import sk.sporixx.dto.SavingAccountReportData;
import sk.sporixx.service.ServiceLocator;
import sk.sporixx.util.Localization;
import java.util.ArrayList;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class BudgetingController {

    // Budget Setup
    @FXML private TextField monthlyIncomeField;
    @FXML private TextField foodField;
    @FXML private TextField rentField;
    @FXML private TextField transportField;
    @FXML private TextField utilitiesField;
    @FXML private TextField otherField;
    @FXML private Label setupErrorLabel;

    // Allocation karta
    @FXML private VBox allocationCard;
    @FXML private Label allocationTitle;
    @FXML private Button allocationEditBtn;
    @FXML private Button allocationCancelBtn;
    @FXML private Button allocationConfirmBtn;

    @FXML private TextField essentialAmountField;
    @FXML private TextField essentialPercentField;
    @FXML private TextField emergencyAmountField;
    @FXML private TextField emergencyPercentField;
    @FXML private TextField savingsAmountField;
    @FXML private TextField savingsPercentField;
    @FXML private TextField toInvestAmountField;
    @FXML private TextField toInvestPercentField;
    @FXML private TextField funMoneyAmountField;
    @FXML private TextField funMoneyPercentField;
    @FXML private Label allocationErrorLabel;

    // Pie chart
    @FXML private StackPane pieChartContainer;
    @FXML private VBox pieLegend;

    // Emergency Fund
    @FXML private Label emergencyBalanceLabel;
    @FXML private Label emergencyMinLabel;
    @FXML private Label emergencyOptLabel;
    @FXML private StackPane emergencyChartContainer;

    // Saving accounts
    @FXML private Button savingPrevBtn;
    @FXML private Button savingNextBtn;
    @FXML private HBox savingCardsRow;

    private BudgetData budgetData;
    private List<SavingAccountReportData> savingAccounts;
    private int savingOffset = 0;
    private static final int MAX_SAVING_VISIBLE = 1;

    private double snapEssential, snapEmergency, snapSavings, snapToInvest;
    private boolean syncingAmount = false;
    private boolean syncingPercent = false;

    private static final String[][] PIE_LEGEND = {
            {"#1A3A8F", "budgeting.allocation.savings"},
            {"#3A7DF6", "budgeting.allocation.to_invest"},
            {"#F59E0B", "budgeting.allocation.essential"},
            {"#EF4444", "budgeting.allocation.emergency"},
            {"#8B5CF6", "budgeting.allocation.fun_money"}
    };

    @FXML
    public void initialize() {
        setupNavButton(savingPrevBtn, "/assets/icons/icon_arrow_left.png");
        setupNavButton(savingNextBtn, "/assets/icons/icon_arrow_right.png");
        loadData();
        setupBudgetSetupListeners();
    }

    // ============================================================
    //  NAČÍTANIE DÁT
    // ============================================================
    private void loadData() {
        try {
            budgetData = ServiceLocator.getBudgetService().loadBudgetData();
        } catch (Exception e) {
            budgetData = null;
            e.printStackTrace();
        }
        try {
            savingAccounts = ServiceLocator.getReportsService().loadSavingAccountsData();
        } catch (Exception e) {
            savingAccounts = List.of();
        }
        populateBudgetSetup();
        populateAllocation();
        populateEmergencyFund();
        renderSavingCards();
    }

    // ============================================================
    //  BUDGET SETUP
    // ============================================================
    private void populateBudgetSetup() {
        if (budgetData == null) return;
        monthlyIncomeField.setText(formatRaw(budgetData.getMonthlyIncome()));
        foodField.setText(formatRaw(budgetData.getFood()));
        rentField.setText(formatRaw(budgetData.getRent()));
        transportField.setText(formatRaw(budgetData.getTransport()));
        utilitiesField.setText(formatRaw(budgetData.getUtilities()));
        otherField.setText(formatRaw(budgetData.getOther()));
    }

    private void setupBudgetSetupListeners() {
        for (TextField field : List.of(monthlyIncomeField, foodField, rentField,
                transportField, utilitiesField, otherField)) {
            field.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                if (wasFocused && !isNowFocused) {
                    saveBudgetSetup();
                }
            });
        }
    }

    private void saveBudgetSetup() {
        try {
            double monthlyIncome = parseDouble(monthlyIncomeField.getText());
            double food = parseDouble(foodField.getText());
            double rent = parseDouble(rentField.getText());
            double transport = parseDouble(transportField.getText());
            double utilities = parseDouble(utilitiesField.getText());
            double other = parseDouble(otherField.getText());

            BudgetWarning warning = ServiceLocator.getBudgetService()
                    .saveBudgetSetup(monthlyIncome, food, rent, transport, utilities, other);

            setupErrorLabel.setVisible(false);
            setupErrorLabel.setManaged(false);

            budgetData = ServiceLocator.getBudgetService().loadBudgetData();
            populateAllocation();
            populateEmergencyFund();

            if (warning != BudgetWarning.NONE) {
                showSetupError(Localization.get("budget.warning." + warning.name().toLowerCase()));
            }

        } catch (NumberFormatException e) {
            // Neúplný input — ignoruj
        } catch (Exception e) {
            showSetupError(Localization.get(e.getMessage()));
        }
    }

    private void showSetupError(String msg) {
        setupErrorLabel.setText(msg);
        setupErrorLabel.setVisible(true);
        setupErrorLabel.setManaged(true);
    }

    // ============================================================
    //  ALLOCATION — populate
    // ============================================================
    private void populateAllocation() {
        if (budgetData == null) return;
        double income = budgetData.getMonthlyIncome();

        setAllocationRow(essentialAmountField, essentialPercentField,
                budgetData.getEssentialExpenses(), budgetData.getEssentialExpensesPercent());
        setAllocationRow(emergencyAmountField, emergencyPercentField,
                budgetData.getEmergencyFund(), budgetData.getEmergencyFundPercent());
        setAllocationRow(savingsAmountField, savingsPercentField,
                budgetData.getSavings(), budgetData.getSavingsPercent());
        setAllocationRow(toInvestAmountField, toInvestPercentField,
                budgetData.getToInvest(), budgetData.getToInvestPercent());
        setAllocationRow(funMoneyAmountField, funMoneyPercentField,
                budgetData.getFunMoney(), budgetData.getFunMoneyPercent());

        renderPieChart(income);
    }

    private void setAllocationRow(TextField amountField, TextField percentField,
                                  double amount, double percent) {
        amountField.setText(formatRaw(amount));
        percentField.setText(formatPercent(percent));
    }

    // ============================================================
    //  ALLOCATION EDIT MODE
    // ============================================================
    @FXML
    private void handleAllocationEdit() {
        snapEssential = parseDoubleSafe(essentialAmountField.getText());
        snapEmergency = parseDoubleSafe(emergencyAmountField.getText());
        snapSavings = parseDoubleSafe(savingsAmountField.getText());
        snapToInvest = parseDoubleSafe(toInvestAmountField.getText());

        setEditMode(true);
        setupAllocationEditListeners();
    }

    @FXML
    private void handleAllocationCancel() {
        if (budgetData != null) {
            double income = budgetData.getMonthlyIncome();
            setAllocationRow(essentialAmountField, essentialPercentField,
                    snapEssential, toPercent(snapEssential, income));
            setAllocationRow(emergencyAmountField, emergencyPercentField,
                    snapEmergency, toPercent(snapEmergency, income));
            setAllocationRow(savingsAmountField, savingsPercentField,
                    snapSavings, toPercent(snapSavings, income));
            setAllocationRow(toInvestAmountField, toInvestPercentField,
                    snapToInvest, toPercent(snapToInvest, income));
            double funMoney = income - snapEssential - snapEmergency - snapSavings - snapToInvest;
            setAllocationRow(funMoneyAmountField, funMoneyPercentField,
                    funMoney, toPercent(funMoney, income));
        }
        allocationErrorLabel.setVisible(false);
        allocationErrorLabel.setManaged(false);
        setEditMode(false);
    }

    @FXML
    private void handleAllocationConfirm() {
        try {
            double essential = parseDouble(essentialAmountField.getText());
            double emergency = parseDouble(emergencyAmountField.getText());
            double savings = parseDouble(savingsAmountField.getText());
            double toInvest = parseDouble(toInvestAmountField.getText());

            BudgetWarning warning = ServiceLocator.getBudgetService()
                    .saveCustomAllocation(essential, emergency, savings, toInvest);

            allocationErrorLabel.setVisible(false);
            allocationErrorLabel.setManaged(false);

            budgetData = ServiceLocator.getBudgetService().loadBudgetData();
            populateAllocation();
            setEditMode(false);

            if (warning != BudgetWarning.NONE) {
                allocationErrorLabel.setText(
                        Localization.get("budget.warning." + warning.name().toLowerCase()));
                allocationErrorLabel.setVisible(true);
                allocationErrorLabel.setManaged(true);
            }

        } catch (NumberFormatException e) {
            allocationErrorLabel.setText(Localization.get("budget.error.invalid_number"));
            allocationErrorLabel.setVisible(true);
            allocationErrorLabel.setManaged(true);
        } catch (Exception e) {
            allocationErrorLabel.setText(Localization.get(e.getMessage()));
            allocationErrorLabel.setVisible(true);
            allocationErrorLabel.setManaged(true);
        }
    }

    private void setEditMode(boolean edit) {
        allocationTitle.setText(edit
                ? Localization.get("budgeting.allocation.title.custom")
                : Localization.get("budgeting.allocation.title"));

        allocationEditBtn.setVisible(!edit);
        allocationEditBtn.setManaged(!edit);
        allocationCancelBtn.setVisible(edit);
        allocationCancelBtn.setManaged(edit);
        allocationConfirmBtn.setVisible(edit);
        allocationConfirmBtn.setManaged(edit);

        essentialAmountField.setEditable(false);
        essentialPercentField.setEditable(false);

        emergencyAmountField.setEditable(edit);
        emergencyPercentField.setEditable(edit);
        savingsAmountField.setEditable(edit);
        savingsPercentField.setEditable(edit);
        toInvestAmountField.setEditable(edit);
        toInvestPercentField.setEditable(edit);

        funMoneyAmountField.setEditable(false);
        funMoneyPercentField.setEditable(false);
    }

    private void setupAllocationEditListeners() {
        double income = budgetData != null ? budgetData.getMonthlyIncome() : 0;

        setupAmountPercentSync(emergencyAmountField, emergencyPercentField, income);
        setupAmountPercentSync(savingsAmountField, savingsPercentField, income);
        setupAmountPercentSync(toInvestAmountField, toInvestPercentField, income);

        setupPercentAmountSync(emergencyPercentField, emergencyAmountField, income);
        setupPercentAmountSync(savingsPercentField, savingsAmountField, income);
        setupPercentAmountSync(toInvestPercentField, toInvestAmountField, income);
    }

    private void setupAmountPercentSync(TextField amountField, TextField percentField,
                                        double income) {
        amountField.textProperty().addListener((obs, old, newVal) -> {
            if (syncingAmount) return;
            syncingPercent = true;
            try {
                double amount = parseDoubleSafe(newVal);
                percentField.setText(formatPercent(toPercent(amount, income)));
                recalcFunMoney(income);
            } finally {
                syncingPercent = false;
            }
        });
    }

    private void setupPercentAmountSync(TextField percentField, TextField amountField,
                                        double income) {
        percentField.textProperty().addListener((obs, old, newVal) -> {
            if (syncingPercent) return;
            syncingAmount = true;
            try {
                double percent = parseDoubleSafe(newVal.replace("%", ""));
                double amount = (percent / 100.0) * income;
                amountField.setText(formatRaw(amount));
                recalcFunMoney(income);
            } finally {
                syncingAmount = false;
            }
        });
    }

    private void recalcFunMoney(double income) {
        double essential = parseDoubleSafe(essentialAmountField.getText());
        double emergency = parseDoubleSafe(emergencyAmountField.getText());
        double savings = parseDoubleSafe(savingsAmountField.getText());
        double toInvest = parseDoubleSafe(toInvestAmountField.getText());
        double funMoney = Math.max(0, income - essential - emergency - savings - toInvest);
        funMoneyAmountField.setText(formatRaw(funMoney));
        funMoneyPercentField.setText(formatPercent(toPercent(funMoney, income)));
    }

    // ============================================================
    //  PIE CHART
    // ============================================================
    private void renderPieChart(double income) {
        pieChartContainer.getChildren().clear();
        pieLegend.getChildren().clear();
        if (budgetData == null || income <= 0) return;

        PieChart pieChart = new PieChart();
        pieChart.setAnimated(false);
        pieChart.setLegendVisible(false);
        pieChart.setLabelsVisible(false);
        pieChart.getStyleClass().add("budgeting-pie-chart");
        pieChart.setPrefSize(180, 180);
        pieChart.setMaxSize(180, 180);

        pieChart.getData().addAll(
                new PieChart.Data("", budgetData.getSavings()),
                new PieChart.Data("", budgetData.getToInvest()),
                new PieChart.Data("", budgetData.getEssentialExpenses()),
                new PieChart.Data("", budgetData.getEmergencyFund()),
                new PieChart.Data("", budgetData.getFunMoney())
        );

        pieChartContainer.getChildren().add(pieChart);

        for (String[] entry : PIE_LEGEND) {
            HBox row = new HBox(8);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            Region dot = new Region();
            dot.setPrefSize(10, 10);
            dot.setStyle("-fx-background-color: " + entry[0] + "; -fx-background-radius: 3px;");
            Label lbl = new Label(Localization.get(entry[1]));
            lbl.getStyleClass().add("analytics-subtitle");
            row.getChildren().addAll(dot, lbl);
            pieLegend.getChildren().add(row);
        }
    }

    // ============================================================
    //  EMERGENCY FUND
    // ============================================================
    private void populateEmergencyFund() {
        if (budgetData == null) return;
        emergencyBalanceLabel.setText(formatCurrency(budgetData.getEmergencyFundCurrent()));
        emergencyMinLabel.setText(Localization.get("budgeting.emergency.minimal")
                + ": " + formatCurrency(budgetData.getMinimalEmergencyFund()));
        emergencyOptLabel.setText(Localization.get("budgeting.emergency.optimal")
                + ": " + formatCurrency(budgetData.getOptimalEmergencyFund()));
        renderEmergencyChart(budgetData.getEmergencyFundHistory());
    }

    private void renderEmergencyChart(Map<String, Double> history) {
        emergencyChartContainer.getChildren().clear();
        if (history == null || history.isEmpty()) return;

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.getStyleClass().add("analytics-axis");

        NumberAxis yAxis = new NumberAxis();
        yAxis.getStyleClass().add("analytics-axis");
        yAxis.setAutoRanging(false);

        List<Double> values = new ArrayList<>(history.values());
        double minVal = values.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double maxVal = values.stream().mapToDouble(Double::doubleValue).max().orElse(1000);
        double padding = (maxVal - minVal) * 0.15;
        double lowerBound = Math.max(0, minVal - padding);
        double upperBound = maxVal + padding;
        double tickUnit = (upperBound - lowerBound) / 5.0;

        yAxis.setLowerBound(lowerBound);
        yAxis.setUpperBound(upperBound);
        yAxis.setTickUnit(tickUnit);
        yAxis.setTickLabelFormatter(new NumberAxis.DefaultFormatter(yAxis) {
            @Override
            public String toString(Number value) {
                double v = value.doubleValue();
                if (v >= 1000) return String.format("%.0fk", v / 1000);
                return String.format("%.0f", v);
            }
        });

        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        chart.setCreateSymbols(false);
        chart.setVerticalGridLinesVisible(false);
        chart.getStyleClass().add("analytics-chart");
        chart.prefWidthProperty().bind(emergencyChartContainer.widthProperty());
        chart.prefHeightProperty().bind(emergencyChartContainer.heightProperty());
        StackPane.setAlignment(chart, javafx.geometry.Pos.CENTER);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        history.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> series.getData().add(
                        new XYChart.Data<>(formatMonthLabel(e.getKey()), e.getValue())));

        chart.getData().add(series);

        Platform.runLater(() -> {
            var seriesNode = chart.getData().get(0).getNode();
            if (seriesNode != null) {
                var line = seriesNode.lookup(".chart-series-line");
                if (line != null)
                    line.setStyle("-fx-stroke: #3A7DF6; -fx-stroke-width: 2px;");
            }
        });

        emergencyChartContainer.getChildren().add(chart);
        emergencyChartContainer.layout();
    }

    private String formatMonthLabel(String key) {
        try {
            if (key.length() == 7) {
                java.time.YearMonth ym = java.time.YearMonth.parse(key);
                return ym.format(java.time.format.DateTimeFormatter.ofPattern("MMM"));
            }
        } catch (Exception ignored) {}
        return key;
    }

    // ============================================================
    //  SAVING ACCOUNTS
    // ============================================================
    @FXML
    private void handleSavingPrev() {
        if (savingOffset > 0) {
            savingOffset--;
            renderSavingCards();
        }
    }

    @FXML
    private void handleSavingNext() {
        if (savingOffset + MAX_SAVING_VISIBLE < savingAccounts.size()) {
            savingOffset++;
            renderSavingCards();
        }
    }

    private void renderSavingCards() {
        savingCardsRow.getChildren().clear();

        if (savingAccounts == null || savingAccounts.isEmpty()) {
            Label empty = new Label(Localization.get("budgeting.saving_accounts.empty"));
            empty.getStyleClass().add("analytics-subtitle");
            savingCardsRow.getChildren().add(empty);
            savingPrevBtn.setDisable(true);
            savingNextBtn.setDisable(true);
            return;
        }

        int from = savingOffset;
        int to = Math.min(savingOffset + MAX_SAVING_VISIBLE, savingAccounts.size());
        for (int i = from; i < to; i++) {
            savingCardsRow.getChildren().add(createSavingPieCard(savingAccounts.get(i)));
        }

        savingPrevBtn.setDisable(savingOffset == 0);
        savingNextBtn.setDisable(savingOffset + MAX_SAVING_VISIBLE >= savingAccounts.size());
    }

    private VBox createSavingPieCard(SavingAccountReportData data) {
        VBox card = new VBox(10);
        card.getStyleClass().add("dashboard-card");
        HBox.setHgrow(card, Priority.ALWAYS);

        Label nameLabel = new Label(data.getAccountName());
        nameLabel.getStyleClass().add("dashboard-card-title");

        PieChart pieChart = new PieChart();
        pieChart.setAnimated(false);
        pieChart.setLegendVisible(false);
        pieChart.setLabelsVisible(true);
        pieChart.getStyleClass().add("reports-pie-chart");
        pieChart.setPrefHeight(200);

        PieChart.Data savedUpData = new PieChart.Data(
                formatCurrency(data.getSavedUp()), data.getSavedUp());
        PieChart.Data needToSaveData = new PieChart.Data(
                formatCurrency(data.getNeedToSave()), data.getNeedToSave());
        pieChart.getData().addAll(savedUpData, needToSaveData);

        Platform.runLater(() -> {
            if (savedUpData.getNode() != null)
                savedUpData.getNode().setStyle("-fx-pie-color: #3A7DF6;");
            if (needToSaveData.getNode() != null)
                needToSaveData.getNode().setStyle("-fx-pie-color: #1A3A8F;");
        });

        VBox legend = new VBox(4);
        legend.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        HBox savedRow = new HBox(8);
        savedRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Region savedDot = new Region();
        savedDot.setPrefSize(10, 10);
        savedDot.setStyle("-fx-background-color: #3A7DF6; -fx-background-radius: 3px;");
        Label savedLbl = new Label(Localization.get("reports.savings.saved_up"));
        savedLbl.getStyleClass().add("analytics-subtitle");
        savedRow.getChildren().addAll(savedDot, savedLbl);

        HBox needRow = new HBox(8);
        needRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Region needDot = new Region();
        needDot.setPrefSize(10, 10);
        needDot.setStyle("-fx-background-color: #1A3A8F; -fx-background-radius: 3px;");
        Label needLbl = new Label(Localization.get("reports.savings.need_to_save"));
        needLbl.getStyleClass().add("analytics-subtitle");
        needRow.getChildren().addAll(needDot, needLbl);

        legend.getChildren().addAll(savedRow, needRow);
        card.getChildren().addAll(nameLabel, pieChart, legend);
        return card;
    }

    // ============================================================
    //  HELPERS
    // ============================================================
    private void setupNavButton(Button button, String iconPath) {
        try {
            ImageView icon = new ImageView(new Image(
                    Objects.requireNonNull(getClass().getResourceAsStream(iconPath))));
            icon.setFitWidth(16);
            icon.setFitHeight(16);
            icon.setPreserveRatio(true);
            button.setText("");
            button.setGraphic(icon);
        } catch (Exception ignored) {}
    }

    private double parseDouble(String text) {
        if (text == null || text.isBlank()) throw new NumberFormatException("empty");
        return Double.parseDouble(text.trim().replace(",", ".").replace("€", ""));
    }

    private double parseDoubleSafe(String text) {
        try {
            return parseDouble(text);
        } catch (Exception e) {
            return 0.0;
        }
    }

    private double toPercent(double amount, double income) {
        if (income <= 0) return 0;
        return Math.round((amount / income) * 100.0 * 10.0) / 10.0;
    }

    private String formatRaw(double value) {
        return String.format("%.2f", value);
    }

    private String formatPercent(double percent) {
        return String.format("%.1f%%", percent);
    }

    private String formatCurrency(double value) {
        return String.format("€%,.2f", value);
    }
}