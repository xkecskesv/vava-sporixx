package sk.sporixx.ui;

import javafx.fxml.FXML;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class SidebarController {

    @FXML private HBox reportsItem;
    @FXML private VBox reportsSubmenu;
    @FXML private ImageView reportsChevron;

    private boolean reportsExpanded = false;

    @FXML
    public void initialize() {
        reportsItem.setOnMouseClicked(event -> toggleReports());
    }

    private void toggleReports() {
        reportsExpanded = !reportsExpanded;
        reportsSubmenu.setVisible(reportsExpanded);
        reportsSubmenu.setManaged(reportsExpanded);

        // Zmena ikony chevronu
        String chevronPath = reportsExpanded
                ? "/assets/icons/icon_chevron_down.png"
                : "/assets/icons/icon_chevron_right.png";
        reportsChevron.setImage(new javafx.scene.image.Image(
                getClass().getResourceAsStream(chevronPath)
        ));
    }

}
