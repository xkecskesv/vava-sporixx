package sk.sporixx.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class DashboardController {

    @FXML private VBox cardMain;
    @FXML private VBox cardEmergency;

    @FXML
    public void initialize() {
        cardMain.setOnMouseClicked(e -> selectCard(cardMain));
        cardEmergency.setOnMouseClicked(e -> selectCard(cardEmergency));
    }

    private void selectCard(VBox selected) {
        // Zoznam všetkých kariet
        VBox[] cards = { cardMain, cardEmergency };

        for (VBox card : cards) {
            if (card == selected) {
                card.getStyleClass().clear();
                card.getStyleClass().add("account-card-active");

                // Prepni labely na aktívne štýly
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