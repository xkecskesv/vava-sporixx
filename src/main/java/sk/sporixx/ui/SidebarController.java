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

    private boolean reportsExpanded = false;

    @FXML
    public void initialize() {
        reportsItem.setOnMouseClicked(event -> toggleReports());
        loadUserInfo();
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
        userAvatar.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/icons/default_profile_picture.png"))
        ));
    }

    private void toggleReports() {
        reportsExpanded = !reportsExpanded;
        reportsSubmenu.setVisible(reportsExpanded);
        reportsSubmenu.setManaged(reportsExpanded);

        String chevronPath = reportsExpanded
                ? "/assets/icons/icon_chevron_down.png"
                : "/assets/icons/icon_chevron_right.png";
        reportsChevron.setImage(new javafx.scene.image.Image(
                getClass().getResourceAsStream(chevronPath)
        ));
    }
}