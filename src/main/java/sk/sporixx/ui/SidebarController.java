package sk.sporixx.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import sk.sporixx.dto.CurrentUser;
import sk.sporixx.service.SessionManager;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Objects;
import javafx.scene.image.Image;

public class SidebarController {

    @FXML private HBox reportsItem;
    @FXML private VBox reportsSubmenu;
    @FXML private ImageView reportsChevron;
    @FXML private ImageView userAvatar;
    @FXML private Label userName;
    @FXML private Label userRole;
    @FXML private HBox overviewItem;
    @FXML private HBox transactionsItem;
    @FXML private HBox budgetingItem;
    @FXML private HBox managementItem;
    @FXML private HBox userBox;
    @FXML private Label incomeLabel;
    @FXML private Label savingsLabel;
    @FXML private Label overviewLabel;
    @FXML private Label reportsLabel;

    private boolean reportsExpanded = false;

    private static String activePage = "dashboard.fxml";

    @FXML
    public void initialize() {
        reportsItem.setOnMouseClicked(event -> toggleReports());

        overviewItem.setOnMouseClicked(e -> navigate("dashboard.fxml"));
        incomeLabel.setOnMouseClicked(e -> navigate("reports_income.fxml"));
        savingsLabel.setOnMouseClicked(e -> navigate("reports_savings.fxml"));
        userBox.setOnMouseClicked(e -> navigate("profile.fxml"));
        userBox.getChildren().forEach(child -> child.setMouseTransparent(true));

        applyActiveState();
        loadUserInfo();
    }

    private void applyActiveState() {
        // Reset všetkých
        setItemActive(overviewItem, overviewLabel, false);
        setItemActive(reportsItem, reportsLabel, false);

        switch (activePage) {
            case "dashboard.fxml" -> setItemActive(overviewItem, overviewLabel, true);
            case "reports_income.fxml", "reports_savings.fxml" -> {
                setItemActive(reportsItem, reportsLabel, true);
                reportsExpanded = true;
                reportsSubmenu.setVisible(true);
                reportsSubmenu.setManaged(true);
                reportsChevron.setImage(new Image(
                        getClass().getResourceAsStream("/assets/icons/icon_chevron_down.png")));
                incomeLabel.setStyle(activePage.equals("reports_income.fxml")
                        ? "-fx-text-fill: #FFFFFF;" : "");
                savingsLabel.setStyle(activePage.equals("reports_savings.fxml")
                        ? "-fx-text-fill: #FFFFFF;" : "");
            }
        }
    }

    private void setItemActive(HBox item, Label label, boolean active) {
        item.getStyleClass().setAll(active ? "sidebar-item-active" : "sidebar-item-row");
        label.getStyleClass().setAll(active ? "sidebar-item-label-active" : "sidebar-item-label");
    }

    private void navigate(String fxml) {
        activePage = fxml;
        try {
            SceneManager.switchTo(fxml);
        } catch (Exception e) {
            System.out.println("Navigate error: " + e.getMessage());
        }
    }

    private void loadUserInfo() {
        CurrentUser user = SessionManager.getInstance().getCurrentUser();
        if (user != null) {
            userName.setText(user.getName() + " " + user.getSurname());

            String photoPath = user.getPhotoPath();
            if (photoPath != null && !photoPath.isBlank()) {
                try {
                    userAvatar.setImage(new Image(new FileInputStream(photoPath)));
                } catch (FileNotFoundException e) {
                    loadFallbackAvatar();
                }
            } else {
                loadFallbackAvatar();
            }
        }
    }

    private void loadFallbackAvatar() {
        userAvatar.setImage(new Image(Objects.requireNonNull(
                getClass().getResourceAsStream("/assets/icons/default_profile_picture.png"))));
    }

    private void toggleReports() {
        reportsExpanded = !reportsExpanded;
        reportsSubmenu.setVisible(reportsExpanded);
        reportsSubmenu.setManaged(reportsExpanded);

        String chevronPath = reportsExpanded
                ? "/assets/icons/icon_chevron_down.png"
                : "/assets/icons/icon_chevron_right.png";
        reportsChevron.setImage(new Image(
                getClass().getResourceAsStream(chevronPath)));
    }
}