package sk.sporixx.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import sk.sporixx.dto.CurrentUser;
import sk.sporixx.service.SessionManager;
import sk.sporixx.util.AvatarUtil;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Objects;

public class SidebarController {

    @FXML private HBox overviewItem;
    @FXML private HBox reportsItem;
    @FXML private HBox transactionsItem;
    @FXML private HBox budgetingItem;
    @FXML private HBox managementItem;
    @FXML private HBox familyManagementItem; // zachované pre kompatibilitu

    @FXML private VBox reportsSubmenu;
    @FXML private ImageView reportsChevron;

    @FXML private VBox managementSubmenu;
    @FXML private ImageView managementChevron;
    @FXML private Label personalManagementLabel;
    @FXML private Label familyManagementLabel;

    @FXML private HBox userBox;

    @FXML private Label overviewLabel;
    @FXML private Label reportsLabel;
    @FXML private Label transactionsLabel;
    @FXML private Label budgetingLabel;
    @FXML private Label managementLabel;
    @FXML private Label incomeLabel;
    @FXML private Label savingsLabel;

    @FXML private ImageView userAvatar;
    @FXML private Label userName;
    @FXML private Label userRole;

    private boolean reportsExpanded = false;
    private boolean managementExpanded = false;
    private boolean isFamilyManager = false;

    private static String activePage = "dashboard.fxml";

    @FXML
    public void initialize() {
        CurrentUser currentUser = SessionManager.getInstance().getCurrentUser();
        isFamilyManager = currentUser.checkisParent();

        setupNavigation();
        setupManagementDropdown();
        applyActiveState();
        loadUserInfo();
    }

    private void setupNavigation() {
        overviewItem.setOnMouseClicked(e -> navigate("dashboard.fxml"));
        reportsItem.setOnMouseClicked(e -> toggleReports());
        incomeLabel.setOnMouseClicked(e -> navigate("reports_income.fxml"));
        savingsLabel.setOnMouseClicked(e -> navigate("reports_savings.fxml"));
        transactionsItem.setOnMouseClicked(e -> navigate("transactions.fxml"));
        budgetingItem.setOnMouseClicked(e -> navigate("budgeting.fxml"));
        userBox.setOnMouseClicked(e -> navigate("profile.fxml"));
        userBox.getChildren().forEach(child -> child.setMouseTransparent(true));
    }

    private void setupManagementDropdown() {
        if (isFamilyManager) {
            // FAMILY_MANAGER — management item otvára dropdown
            managementChevron.setVisible(true);
            managementChevron.setManaged(true);

            managementItem.setOnMouseClicked(e -> toggleManagement());
            personalManagementLabel.setOnMouseClicked(e -> navigate("management.fxml"));
            familyManagementLabel.setOnMouseClicked(e -> navigate("family_management.fxml"));
        } else {
            // Normálny user — priama navigácia
            managementItem.setOnMouseClicked(e -> navigate("management.fxml"));
        }
    }

    private void toggleManagement() {
        managementExpanded = !managementExpanded;
        managementSubmenu.setVisible(managementExpanded);
        managementSubmenu.setManaged(managementExpanded);

        String chevronPath = managementExpanded
                ? "/assets/icons/icon_chevron_down.png"
                : "/assets/icons/icon_chevron_right.png";
        managementChevron.setImage(new Image(getClass().getResourceAsStream(chevronPath)));
    }

    private void applyActiveState() {
        // Reset všetkých
        setItemActive(overviewItem, overviewLabel, false);
        setItemActive(reportsItem, reportsLabel, false);
        setItemActive(transactionsItem, transactionsLabel, false);
        setItemActive(budgetingItem, budgetingLabel, false);
        setItemActive(managementItem, managementLabel, false);
        incomeLabel.setStyle("");
        savingsLabel.setStyle("");
        personalManagementLabel.setStyle("");
        familyManagementLabel.setStyle("");

        switch (activePage) {
            case "dashboard.fxml" -> setItemActive(overviewItem, overviewLabel, true);

            case "transactions.fxml" -> setItemActive(transactionsItem, transactionsLabel, true);

            case "budgeting.fxml" -> setItemActive(budgetingItem, budgetingLabel, true);

            case "management.fxml" -> {
                setItemActive(managementItem, managementLabel, true);
                if (isFamilyManager) {
                    managementExpanded = true;
                    managementSubmenu.setVisible(true);
                    managementSubmenu.setManaged(true);
                    managementChevron.setImage(new Image(
                            getClass().getResourceAsStream("/assets/icons/icon_chevron_down.png")));
                    personalManagementLabel.setStyle("-fx-text-fill: #FFFFFF;");
                }
            }

            case "family_management.fxml" -> {
                setItemActive(managementItem, managementLabel, true);
                managementExpanded = true;
                managementSubmenu.setVisible(true);
                managementSubmenu.setManaged(true);
                managementChevron.setImage(new Image(
                        getClass().getResourceAsStream("/assets/icons/icon_chevron_down.png")));
                familyManagementLabel.setStyle("-fx-text-fill: #FFFFFF;");
            }

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

    private void toggleReports() {
        reportsExpanded = !reportsExpanded;
        reportsSubmenu.setVisible(reportsExpanded);
        reportsSubmenu.setManaged(reportsExpanded);

        String chevronPath = reportsExpanded
                ? "/assets/icons/icon_chevron_down.png"
                : "/assets/icons/icon_chevron_right.png";
        reportsChevron.setImage(new Image(getClass().getResourceAsStream(chevronPath)));
    }

    private void loadUserInfo() {
        CurrentUser user = SessionManager.getInstance().getCurrentUser();
        if (user != null) {
            userName.setText(user.getName() + " " + user.getSurname());
            userRole.setText(user.getRole() != null ? user.getRole().name() : "");
        }
        AvatarUtil.apply(userAvatar, user != null ? user.getPhotoPath() : null, 32, getClass());
    }

    private void loadFallbackAvatar() {
        userAvatar.setImage(new Image(Objects.requireNonNull(
                getClass().getResourceAsStream("/assets/icons/default_profile_picture.png"))));
    }
}