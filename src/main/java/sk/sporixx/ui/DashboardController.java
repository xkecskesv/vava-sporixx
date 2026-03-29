package sk.sporixx.ui;

import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class DashboardController {

    @FXML private VBox cardMain;
    @FXML private VBox cardEmergency;
    @FXML private LineChart<String, Number> analyticsChart;

    @FXML
    public void initialize() {
        cardMain.setOnMouseClicked(e -> selectCard(cardMain));
        cardEmergency.setOnMouseClicked(e -> selectCard(cardEmergency));
        loadAnalyticsChart();
    }

    private void loadAnalyticsChart() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.getData().add(new XYChart.Data<>("Jan", 3200));
        series.getData().add(new XYChart.Data<>("Feb", 4100));
        series.getData().add(new XYChart.Data<>("Mar", 3800));
        series.getData().add(new XYChart.Data<>("Apr", 5200));
        series.getData().add(new XYChart.Data<>("May", 6800));
        series.getData().add(new XYChart.Data<>("Jun", 7200));
        series.getData().add(new XYChart.Data<>("Jul", 9800));
        series.getData().add(new XYChart.Data<>("Aug", 9500));
        series.getData().add(new XYChart.Data<>("Sep", 10200));
        series.getData().add(new XYChart.Data<>("Oct", 8800));
        series.getData().add(new XYChart.Data<>("Nov", 7500));
        series.getData().add(new XYChart.Data<>("Dec", 8900));
        analyticsChart.getData().add(series);
    }

    private void selectCard(VBox selected) {
        VBox[] cards = { cardMain, cardEmergency };

        for (VBox card : cards) {
            if (card == selected) {
                card.getStyleClass().clear();
                card.getStyleClass().add("account-card-active");

                for (var node : card.getChildren()) {
                    if (node instanceof Label label) {
                        String text = label.getText();
                        if (text.startsWith("€")) {
                            label.getStyleClass().setAll("account-card-amount-active");
                        }
                    }
                    if (node instanceof HBox hbox) {
                        for (var child : hbox.getChildren()) {
                            if (child instanceof Label label) {
                                label.getStyleClass().setAll("account-card-title-active");
                            }
                        }
                    } //TODO: Sem pridat aj zmenu ikonu na negatívnu farbu! Požiadať mareka o dodanie ikon oboch farieb aby som mohol prepinat farby accountov
                    if (node instanceof Label label && !label.getText().startsWith("€")) {
                        if (!label.getText().contains(",")) {
                            label.getStyleClass().setAll("account-card-desc-active");
                        }
                    }
                }
            } else {
                card.getStyleClass().clear();
                card.getStyleClass().add("account-card");

                for (var node : card.getChildren()) {
                    if (node instanceof Label label) {
                        String text = label.getText();
                        if (text.startsWith("€")) {
                            label.getStyleClass().setAll("account-card-amount");
                        }
                    }
                    if (node instanceof HBox hbox) {
                        for (var child : hbox.getChildren()) {
                            if (child instanceof Label label) {
                                label.getStyleClass().setAll("account-card-title");
                            }
                        }
                    }
                    if (node instanceof Label label && !label.getText().startsWith("€")) {
                        if (!label.getText().contains(",")) {
                            label.getStyleClass().setAll("account-card-desc");
                        }
                    }
                }
            }
        }
    }
}