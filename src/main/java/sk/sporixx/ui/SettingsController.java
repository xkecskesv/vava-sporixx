package sk.sporixx.ui;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ToggleButton;

public class SettingsController implements Initializable {

    @FXML private ToggleButton upcomingToggle;
    @FXML private ToggleButton budgetToggle;
    @FXML private ToggleButton remindersToggle;
    @FXML private ToggleButton goalsToggle;
    @FXML private ToggleButton achievementsToggle;

    private String onText = "On";
    private String offText = "Off";

    /**
     * Initializes localized toggle labels and wires all settings toggles
     * to update their displayed text when state changes.
     *
     * @param location unused location of the FXML file
     * @param resources resource bundle used for localized ON/OFF text
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (resources != null) {
            onText = resources.getString("settings.value.on");
            offText = resources.getString("settings.value.off");
        }

        List<ToggleButton> toggles = List.of(
                upcomingToggle,
                budgetToggle,
                remindersToggle,
                goalsToggle,
                achievementsToggle
        );

        for (ToggleButton toggle : toggles) {
            refreshToggleText(toggle);
            toggle.selectedProperty().addListener((obs, oldValue, newValue) -> refreshToggleText(toggle));
        }
    }

    /**
     * Updates a toggle button label according to its selected state.
     *
     * @param toggle target toggle button
     */
    private void refreshToggleText(ToggleButton toggle) {
        toggle.setText(toggle.isSelected() ? onText : offText);
    }
}

