package sk.sporixx.ui;

import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import sk.sporixx.dto.*;
import sk.sporixx.model.RecurringRule;
import sk.sporixx.model.Transaction;
import sk.sporixx.service.ServiceLocator;
import sk.sporixx.util.Localization;

import java.util.Map;
import java.util.TreeSet;

public class ReportsIncomeController {

    @FXML private ComboBox<String> periodComboBox;
    @FXML private Button exportButton;
    @FXML private Label totalIncomeAmount;
    @FXML private LineChart<String, Number> incomeExpenseChart;
    @FXML private NumberAxis lineYAxis;
    @FXML private PieChart categoryPieChart;
    @FXML private BarChart<String, Number> recurringBarChart;
    @FXML private BarChart<String, Number> wantNeedChart;
    @FXML private Label needPercentLabel;
    @FXML private Label wantPercentLabel;
    @FXML private Label recurringTotalLabel;

    private ChartPeriod currentPeriod = ChartPeriod.TWELVE_MONTHS;

    @FXML
    public void initialize() {
        setupPeriodComboBox();
        loadAll();
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
            currentPeriod = ChartPeriod.ONE_WEEK;
        } else if (selected.equals(Localization.get("dashboard.analytics.period.month"))) {
            currentPeriod = ChartPeriod.ONE_MONTH;
        } else if (selected.equals(Localization.get("dashboard.analytics.period.six_months"))) {
            currentPeriod = ChartPeriod.SIX_MONTHS;
        } else {
            currentPeriod = ChartPeriod.TWELVE_MONTHS;
        }
        loadAll();
    }

    private void loadAll() {
        loadIncomeExpenseChart();
        loadCategoryChart();
        loadRecurringChart();
        loadWantNeedChart();
    }

    // ============================================================
    //  INCOME VS EXPENSES
    // ============================================================
    private void loadIncomeExpenseChart() {
        IncomeExpenseData data = ServiceLocator.getReportsService()
                .loadIncomeExpenseData(currentPeriod);

        totalIncomeAmount.setText("+ " + formatCurrency(data.getTotalIncome()));

        incomeExpenseChart.setAnimated(false);
        incomeExpenseChart.getData().clear();

        // Zozbieraj všetky unikátne kľúče zoradené
        TreeSet<String> allKeys = new TreeSet<>();
        allKeys.addAll(data.getMonthlyIncome().keySet());
        allKeys.addAll(data.getMonthlyExpense().keySet());

        // Nastav kategórie explicitne pred pridaním dát
        CategoryAxis xAxis = (CategoryAxis) incomeExpenseChart.getXAxis();
        xAxis.setAutoRanging(false);
        xAxis.getCategories().setAll(allKeys);

        XYChart.Series<String, Number> incomeSeries = new XYChart.Series<>();
        incomeSeries.setName(Localization.get("reports.income.legend.income"));

        XYChart.Series<String, Number> expenseSeries = new XYChart.Series<>();
        expenseSeries.setName(Localization.get("reports.income.legend.expenses"));

        allKeys.forEach(key -> {
            incomeSeries.getData().add(new XYChart.Data<>(key,
                    data.getMonthlyIncome().getOrDefault(key, 0.0)));
            expenseSeries.getData().add(new XYChart.Data<>(key,
                    data.getMonthlyExpense().getOrDefault(key, 0.0)));
        });

        incomeExpenseChart.getData().addAll(incomeSeries, expenseSeries);

        double max = Math.max(
                data.getMonthlyIncome().values().stream().mapToDouble(Double::doubleValue).max().orElse(100),
                data.getMonthlyExpense().values().stream().mapToDouble(Double::doubleValue).max().orElse(100)
        );
        lineYAxis.setAutoRanging(false);
        lineYAxis.setLowerBound(0);
        lineYAxis.setUpperBound(max * 1.1);
        lineYAxis.setTickUnit(max / 5.0);
    }

    // ============================================================
    //  CATEGORY PIE CHART
    // ============================================================
    private void loadCategoryChart() {
        CategoryExpenseData data = ServiceLocator.getReportsService()
                .loadCategoryExpenseData(currentPeriod);

        categoryPieChart.getData().clear();

        data.getExpenseByCategory().forEach((category, amount) ->
                categoryPieChart.getData().add(
                        new PieChart.Data(category + " " + formatCurrency(amount), amount)));
    }

    // ============================================================
    //  RECURRING BAR CHART
    // ============================================================
    private void loadRecurringChart() {
        RecurringExpenseData data = ServiceLocator.getReportsService()
                .loadRecurringExpenseData();

        recurringBarChart.setAnimated(false);
        recurringBarChart.getData().clear();
        recurringBarChart.setCategoryGap(30);
        recurringBarChart.setBarGap(2);

        XYChart.Series<String, Number> series = new XYChart.Series<>();

        for (RecurringRule rule : data.getItems()) {
            XYChart.Data<String, Number> dataPoint =
                    new XYChart.Data<>(rule.getDescription(), rule.getAmount());

            boolean isWant = rule.getSpendingClassificationId() == Transaction.CLASSIFICATION_WANT;

            dataPoint.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    newNode.setStyle("-fx-bar-fill: " + (isWant ? "#EF4444" : "#3A7DF6") + ";");
                }
            });

            series.getData().add(dataPoint);
        }

        recurringBarChart.getData().add(series);
        recurringBarChart.setLegendVisible(false);

        javafx.application.Platform.runLater(() -> {
            recurringBarChart.layout();
            addBarLabels(recurringBarChart);
        });

        recurringTotalLabel.setText(formatCurrency(data.getTotal()));
    }

    private void addBarLabels(BarChart<String, Number> chart) {
        for (XYChart.Series<String, Number> series : chart.getData()) {
            for (XYChart.Data<String, Number> item : series.getData()) {
                javafx.application.Platform.runLater(() -> {
                    if (item.getNode() != null) {
                        double value = item.getYValue().doubleValue();
                        if (value <= 0) return;

                        String labelText = String.valueOf((int) value);
                        Label label = new Label(labelText);
                        label.getStyleClass().add("bar-value-label");

                        javafx.scene.layout.StackPane bar =
                                (javafx.scene.layout.StackPane) item.getNode();
                        bar.getChildren().add(label);
                        bar.setAlignment(label, javafx.geometry.Pos.TOP_CENTER);
                        label.setTranslateY(-18);
                    }
                });
            }
        }
    }

    // ============================================================
    //  WANT VS NEED BAR CHART
    // ============================================================
    private void loadWantNeedChart() {
        WantNeedData data = ServiceLocator.getReportsService()
                .loadWantNeedData(currentPeriod);

        wantNeedChart.setAnimated(false);
        wantNeedChart.getData().clear();
        wantNeedChart.setLegendVisible(false);

        XYChart.Series<String, Number> series = new XYChart.Series<>();

        XYChart.Data<String, Number> needData = new XYChart.Data<>(
                Localization.get("reports.income.need"), data.getTotalNeed());
        XYChart.Data<String, Number> wantData = new XYChart.Data<>(
                Localization.get("reports.income.want"), data.getTotalWant());

        needData.nodeProperty().addListener((obs, oldNode, newNode) -> {
            if (newNode != null) newNode.setStyle("-fx-bar-fill: #3A7DF6;");
        });
        wantData.nodeProperty().addListener((obs, oldNode, newNode) -> {
            if (newNode != null) newNode.setStyle("-fx-bar-fill: #EF4444;");
        });

        series.getData().addAll(needData, wantData);
        wantNeedChart.getData().add(series);

        needPercentLabel.setText(String.format("%.1f%%", data.getNeedPercentage()));
        wantPercentLabel.setText(String.format("%.1f%%", data.getWantPercentage()));
    }

    // ============================================================
    //  EXPORT
    // ============================================================
    @FXML
    private void handleExport() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle(Localization.get("reports.export"));
        fileChooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("XML súbory", "*.xml"));
        fileChooser.setInitialFileName("income_expense_report.xml");

        java.io.File file = fileChooser.showSaveDialog(exportButton.getScene().getWindow());
        if (file == null) return;

        try {
            ServiceLocator.getExportService()
                    .exportIncomeExpenseToXml(currentPeriod, file.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ============================================================
    //  HELPER
    // ============================================================
    private String formatCurrency(double value) {
        return String.format("€%,.2f", value);
    }
}