package sk.sporixx.ui;

import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import sk.sporixx.dto.CategoryExpenseData;
import sk.sporixx.dto.IncomeExpenseData;
import sk.sporixx.dto.RecurringExpenseData;
import sk.sporixx.dto.WantNeedData;
import sk.sporixx.model.RecurringRule;
import sk.sporixx.model.Transaction;
import sk.sporixx.service.ServiceLocator;
import sk.sporixx.util.Localization;

import java.util.Map;

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

    private int currentMonths = 12;

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
            currentMonths = 0; // špeciálny prípad — 1 týždeň
        } else if (selected.equals(Localization.get("dashboard.analytics.period.month"))) {
            currentMonths = 1;
        } else if (selected.equals(Localization.get("dashboard.analytics.period.six_months"))) {
            currentMonths = 6;
        } else {
            currentMonths = 12;
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
                .loadIncomeExpenseData(currentMonths == 0 ? 1 : currentMonths);

        System.out.println("Income entries: " + data.getMonthlyIncome().size());
        System.out.println("Expense entries: " + data.getMonthlyExpense().size());
        System.out.println("Expenses: " + data.getMonthlyExpense());

        totalIncomeAmount.setText("+ " + formatCurrency(data.getTotalIncome()));

        incomeExpenseChart.setAnimated(false);
        incomeExpenseChart.getData().clear();

        XYChart.Series<String, Number> incomeSeries = new XYChart.Series<>();
        incomeSeries.setName(Localization.get("reports.income.legend.income"));

        XYChart.Series<String, Number> expenseSeries = new XYChart.Series<>();
        expenseSeries.setName(Localization.get("reports.income.legend.expenses"));

        data.getMonthlyIncome().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> incomeSeries.getData()
                        .add(new XYChart.Data<>(e.getKey(), e.getValue())));

        data.getMonthlyExpense().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> expenseSeries.getData()
                        .add(new XYChart.Data<>(e.getKey(), e.getValue())));

        incomeExpenseChart.getData().addAll(incomeSeries, expenseSeries);

        // Manuálne Y bounds
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
                .loadCategoryExpenseData(currentMonths == 0 ? 1 : currentMonths);

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

        XYChart.Series<String, Number> wantSeries = new XYChart.Series<>();
        wantSeries.setName(Localization.get("reports.income.legend.want"));

        XYChart.Series<String, Number> needSeries = new XYChart.Series<>();
        needSeries.setName(Localization.get("reports.income.legend.need"));

        for (RecurringRule rule : data.getItems()) {
            if (rule.getSpendingClassificationId() == Transaction.CLASSIFICATION_WANT) {
                wantSeries.getData().add(
                        new XYChart.Data<>(rule.getDescription(), rule.getAmount()));
            } else {
                needSeries.getData().add(
                        new XYChart.Data<>(rule.getDescription(), rule.getAmount()));
            }
        }

        recurringBarChart.getData().addAll(wantSeries, needSeries);
    }

    // ============================================================
    //  WANT VS NEED BAR CHART
    // ============================================================
    private void loadWantNeedChart() {
        WantNeedData data = ServiceLocator.getReportsService()
                .loadWantNeedData(currentMonths == 0 ? 1 : currentMonths);

        wantNeedChart.setAnimated(false);
        wantNeedChart.getData().clear();

        XYChart.Series<String, Number> needSeries = new XYChart.Series<>();
        needSeries.setName(Localization.get("reports.income.need"));
        needSeries.getData().add(new XYChart.Data<>(
                Localization.get("reports.income.need"), data.getTotalNeed()));

        XYChart.Series<String, Number> wantSeries = new XYChart.Series<>();
        wantSeries.setName(Localization.get("reports.income.want"));
        wantSeries.getData().add(new XYChart.Data<>(
                Localization.get("reports.income.want"), data.getTotalWant()));

        wantNeedChart.getData().addAll(needSeries, wantSeries);

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
                    .exportIncomeExpenseToXml(currentMonths == 0 ? 1 : currentMonths, file.getAbsolutePath());
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