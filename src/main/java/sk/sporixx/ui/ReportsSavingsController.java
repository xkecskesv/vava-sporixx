package sk.sporixx.ui;

import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import sk.sporixx.dto.SavingAccountReportData;
import sk.sporixx.service.ServiceLocator;
import sk.sporixx.util.Localization;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class ReportsSavingsController {

    @FXML private Button prevButton;
    @FXML private Button nextButton;
    @FXML private Button exportButton;
    @FXML private Button importButton;
    @FXML private VBox emptyState;
    @FXML private HBox cardsRow;
    @FXML private HBox progressRow;

    private List<SavingAccountReportData> accounts;
    private int scrollOffset = 0;
    private static final int MAX_VISIBLE = 3;

    @FXML
    public void initialize() {
        accounts = ServiceLocator.getReportsService().loadSavingAccountsData();

        if (accounts == null || accounts.isEmpty()) {
            emptyState.setVisible(true);
            emptyState.setManaged(true);
            prevButton.setDisable(true);
            nextButton.setDisable(true);
            return;
        }

        renderCards();
    }

    @FXML
    private void handlePrev() {
        if (scrollOffset > 0) {
            scrollOffset--;
            renderCards();
        }
    }

    @FXML
    private void handleNext() {
        if (scrollOffset + MAX_VISIBLE < accounts.size()) {
            scrollOffset++;
            renderCards();
        }
    }

    private void renderCards() {
        cardsRow.getChildren().clear();
        progressRow.getChildren().clear();

        int from = scrollOffset;
        int to = Math.min(scrollOffset + MAX_VISIBLE, accounts.size());

        for (int i = from; i < to; i++) {
            SavingAccountReportData data = accounts.get(i);
            cardsRow.getChildren().add(createPieCard(data));
            progressRow.getChildren().add(createProgressCard(data));
        }

        prevButton.setDisable(scrollOffset == 0);
        nextButton.setDisable(scrollOffset + MAX_VISIBLE >= accounts.size());
    }

    private VBox createPieCard(SavingAccountReportData data) {
        VBox card = new VBox(12);
        card.getStyleClass().add("dashboard-card");
        HBox.setHgrow(card, Priority.ALWAYS);

        Label nameLabel = new Label(data.getAccountName());
        nameLabel.getStyleClass().add("dashboard-card-title");

        PieChart pieChart = new PieChart();
        pieChart.setAnimated(false);
        pieChart.setLegendVisible(true);
        pieChart.setLegendSide(javafx.geometry.Side.BOTTOM);
        pieChart.setLabelsVisible(true);
        pieChart.getStyleClass().add("reports-pie-chart");
        pieChart.setPrefHeight(300);
        VBox.setVgrow(pieChart, Priority.ALWAYS);

        PieChart.Data savedUpData = new PieChart.Data(
                Localization.get("reports.savings.saved_up") + " " +
                        formatCurrency(data.getSavedUp()), data.getSavedUp());
        PieChart.Data needToSaveData = new PieChart.Data(
                Localization.get("reports.savings.need_to_save") + " " +
                        formatCurrency(data.getNeedToSave()), data.getNeedToSave());

        pieChart.getData().addAll(savedUpData, needToSaveData);

        javafx.application.Platform.runLater(() -> {
            if (savedUpData.getNode() != null)
                savedUpData.getNode().setStyle("-fx-pie-color: #3A7DF6;");
            if (needToSaveData.getNode() != null)
                needToSaveData.getNode().setStyle("-fx-pie-color: #1A3A8F;");
        });

        HBox totalRow = new HBox(8);
        totalRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label totalLabel = new Label(Localization.get("reports.savings.total_to_save"));
        totalLabel.getStyleClass().add("analytics-subtitle");
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label totalAmount = new Label(formatCurrency(data.getTargetAmount()));
        totalAmount.getStyleClass().add("recurring-total");
        totalRow.getChildren().addAll(totalLabel, spacer, totalAmount);

        card.getChildren().addAll(nameLabel, pieChart, totalRow);
        return card;
    }

    private VBox createProgressCard(SavingAccountReportData data) {
        VBox card = new VBox(8);
        card.getStyleClass().add("dashboard-card");
        HBox.setHgrow(card, Priority.ALWAYS);

        Label title = new Label(Localization.get("reports.savings.expectation_vs_reality"));
        title.getStyleClass().add("dashboard-card-title");

        NumberAxis yAxis = new NumberAxis();
        yAxis.getStyleClass().add("analytics-axis");
        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(0);
        yAxis.setUpperBound(data.getTargetAmount() * 1.1);
        yAxis.setTickUnit(data.getTargetAmount() / 5.0);

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.getStyleClass().add("analytics-axis");

        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        chart.setCreateSymbols(false);
        chart.setVerticalGridLinesVisible(false);
        chart.getStyleClass().add("analytics-chart");
        VBox.setVgrow(chart, Priority.ALWAYS);
        chart.setPrefHeight(250);

        XYChart.Series<String, Number> expectedSeries = new XYChart.Series<>();
        XYChart.Series<String, Number> actualSeries = new XYChart.Series<>();

        DateTimeFormatter inputFormatter = getInputFormatter(data.getProgressGrouping());
        DateTimeFormatter displayFormatter = getDisplayFormatter(data.getProgressGrouping());

        data.getExpectedProgress().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> expectedSeries.getData().add(
                        new XYChart.Data<>(formatLabel(e.getKey(), displayFormatter),
                                e.getValue())));

        data.getActualProgress().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> actualSeries.getData().add(
                        new XYChart.Data<>(formatLabel(e.getKey(), displayFormatter),
                                e.getValue())));

        chart.getData().addAll(expectedSeries, actualSeries);

        javafx.application.Platform.runLater(() -> {
            if (!chart.getData().isEmpty() && chart.getData().get(0).getNode() != null) {
                chart.getData().get(0).getNode()
                        .lookup(".chart-series-line")
                        .setStyle("-fx-stroke: #3A7DF6; -fx-stroke-width: 2px;");
            }
            if (chart.getData().size() > 1 && chart.getData().get(1).getNode() != null) {
                chart.getData().get(1).getNode()
                        .lookup(".chart-series-line")
                        .setStyle("-fx-stroke: #1A3A8F; -fx-stroke-width: 2px;");
            }
        });

        // Legenda
        HBox legend = new HBox(16);
        legend.setAlignment(javafx.geometry.Pos.CENTER);

        HBox expectedLegend = new HBox(6);
        expectedLegend.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        javafx.scene.layout.Region expectedDot = new javafx.scene.layout.Region();
        expectedDot.setPrefSize(12, 3);
        expectedDot.setStyle("-fx-background-color: #3A7DF6; -fx-background-radius: 2px;");
        Label expectedLabel = new Label(Localization.get("reports.savings.expected"));
        expectedLabel.getStyleClass().add("analytics-subtitle");
        expectedLegend.getChildren().addAll(expectedDot, expectedLabel);

        HBox actualLegend = new HBox(6);
        actualLegend.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        javafx.scene.layout.Region actualDot = new javafx.scene.layout.Region();
        actualDot.setPrefSize(12, 3);
        actualDot.setStyle("-fx-background-color: #1A3A8F; -fx-background-radius: 2px;");
        Label actualLabel = new Label(Localization.get("reports.savings.actual"));
        actualLabel.getStyleClass().add("analytics-subtitle");
        actualLegend.getChildren().addAll(actualDot, actualLabel);

        legend.getChildren().addAll(expectedLegend, actualLegend);

        card.getChildren().addAll(title, chart, legend);
        return card;
    }

    private DateTimeFormatter getInputFormatter(String grouping) {
        return switch (grouping) {
            case "DAY" -> DateTimeFormatter.ofPattern("yyyy-MM-dd");
            case "MONTH" -> DateTimeFormatter.ofPattern("yyyy-MM");
            default -> DateTimeFormatter.ofPattern("yyyy");
        };
    }

    private DateTimeFormatter getDisplayFormatter(String grouping) {
        return switch (grouping) {
            case "DAY" -> DateTimeFormatter.ofPattern("dd MMM");
            case "MONTH" -> DateTimeFormatter.ofPattern("MMM yy");
            default -> DateTimeFormatter.ofPattern("yyyy");
        };
    }

    private String formatLabel(String key, DateTimeFormatter display) {
        try {
            return switch (key.length()) {
                case 4 -> LocalDate.parse(key + "-01-01",
                        DateTimeFormatter.ofPattern("yyyy-MM-dd")).format(display);
                case 7 -> LocalDate.parse(key + "-01",
                        DateTimeFormatter.ofPattern("yyyy-MM-dd")).format(display);
                default -> LocalDate.parse(key,
                        DateTimeFormatter.ofPattern("yyyy-MM-dd")).format(display);
            };
        } catch (Exception e) {
            return key;
        }
    }

    @FXML
    private void handleExport() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle(Localization.get("reports.export"));
        fileChooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("XML súbory", "*.xml"));
        fileChooser.setInitialFileName("saving_accounts_report.xml");

        java.io.File file = fileChooser.showSaveDialog(exportButton.getScene().getWindow());
        if (file == null) return;

        try {
            ServiceLocator.getExportService()
                    .exportSavingAccountsToXml(file.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleImport() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle(Localization.get("reports.import"));
        fileChooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("XML súbory", "*.xml"));

        java.io.File file = fileChooser.showOpenDialog(importButton.getScene().getWindow());
        if (file == null) return;

        try {
            ServiceLocator.getImportService()
                    .importSavingAccountsFromXml(file.getAbsolutePath());
            accounts = ServiceLocator.getReportsService().loadSavingAccountsData();
            renderCards();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String formatCurrency(double value) {
        return String.format("€%,.2f", value);
    }
}