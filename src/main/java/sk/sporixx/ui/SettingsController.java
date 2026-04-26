package sk.sporixx.ui;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ToggleButton;
import sk.sporixx.service.ServiceLocator;
import sk.sporixx.service.SettingsService;
import sk.sporixx.util.Localization;

public class SettingsController implements Initializable {

    @FXML private ComboBox<String> languageComboBox;
    @FXML private ComboBox<String> currencyComboBox;

    @FXML private ToggleButton upcomingToggle;
    @FXML private ToggleButton budgetToggle;
    @FXML private ToggleButton remindersToggle;
    @FXML private ToggleButton goalsToggle;
    @FXML private ToggleButton achievementsToggle;

    private String onText = "On";
    private String offText = "Off";
    private boolean initializing = true;

    private final Map<String, String> languageByLabel = new LinkedHashMap<>();
    private final Map<String, String> currencyByLabel = new LinkedHashMap<>();

    /**
     * Initializes localized toggle labels and wires all settings toggles
     * to update their displayed text when state changes.
     *
     * @param location unused location of the FXML file
     * @param resources resource bundle used for localized ON/OFF text
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        SettingsService settingsService = ServiceLocator.getSettingsService();
        ResourceBundle effectiveResources = resolveResources(resources);

        onText = effectiveResources.getString("settings.value.on");
        offText = effectiveResources.getString("settings.value.off");

        setupGeneralSettings(effectiveResources, settingsService);

        List<ToggleButton> toggles = List.of(
                upcomingToggle,
                budgetToggle,
                remindersToggle,
                goalsToggle,
                achievementsToggle
        );

        upcomingToggle.setSelected(settingsService.isUpcomingPaymentsEnabled());
        budgetToggle.setSelected(settingsService.isBudgetLimitAlertsEnabled());
        remindersToggle.setSelected(settingsService.isSavingRemindersEnabled());
        goalsToggle.setSelected(settingsService.isSavingGoalsUpdatesEnabled());
        achievementsToggle.setSelected(settingsService.isAchievementsEnabled());

        for (ToggleButton toggle : toggles) {
            refreshToggleText(toggle);
            toggle.selectedProperty().addListener((obs, oldValue, newValue) -> refreshToggleText(toggle));
        }

        upcomingToggle.selectedProperty().addListener((obs, oldValue, newValue) ->
                settingsService.setUpcomingPaymentsEnabled(newValue));
        budgetToggle.selectedProperty().addListener((obs, oldValue, newValue) ->
                settingsService.setBudgetLimitAlertsEnabled(newValue));
        remindersToggle.selectedProperty().addListener((obs, oldValue, newValue) ->
                settingsService.setSavingRemindersEnabled(newValue));
        goalsToggle.selectedProperty().addListener((obs, oldValue, newValue) ->
                settingsService.setSavingGoalsUpdatesEnabled(newValue));
        achievementsToggle.selectedProperty().addListener((obs, oldValue, newValue) ->
                settingsService.setAchievementsEnabled(newValue));

        initializing = false;
    }

    private void setupGeneralSettings(ResourceBundle resources, SettingsService settingsService) {
        languageByLabel.clear();
        currencyByLabel.clear();

        languageByLabel.put(resources.getString("settings.general.language.english"), "en");
        languageByLabel.put(resources.getString("settings.general.language.slovak"), "sk");
        languageByLabel.put(resources.getString("settings.general.language.czech"), "cs");

        currencyByLabel.put(resources.getString("settings.general.currency.eur"), "EUR");
        currencyByLabel.put(resources.getString("settings.general.currency.usd"), "USD");
        currencyByLabel.put(resources.getString("settings.general.currency.czk"), "CZK");

        languageComboBox.getItems().setAll(languageByLabel.keySet());
        currencyComboBox.getItems().setAll(currencyByLabel.keySet());

        languageComboBox.setValue(resolveLabel(languageByLabel, settingsService.getLanguageCode()));
        currencyComboBox.setValue(resolveLabel(currencyByLabel, settingsService.getCurrencyCode()));

        languageComboBox.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (initializing || newValue == null) {
                return;
            }

            String selectedLanguage = languageByLabel.get(newValue);
            if (selectedLanguage == null || selectedLanguage.equals(settingsService.getLanguageCode())) {
                return;
            }

            settingsService.setLanguageCode(selectedLanguage);
            Localization.setLanguage(selectedLanguage);

            reloadCurrentSceneOrThrow("language switch");
        });

        currencyComboBox.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (initializing || newValue == null) {
                return;
            }

            String selectedCurrency = currencyByLabel.get(newValue);
            if (selectedCurrency == null || selectedCurrency.equals(settingsService.getCurrencyCode())) {
                return;
            }

            settingsService.setCurrencyCode(selectedCurrency);
            reloadCurrentSceneOrThrow("currency switch");
        });
    }

    private void reloadCurrentSceneOrThrow(String action) {
        try {
            SceneManager.reloadCurrentScene();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to reload scene after " + action, e);
        }
    }

    private ResourceBundle resolveResources(ResourceBundle resources) {
        if (resources != null) {
            return resources;
        }

        ResourceBundle fallbackBundle = Localization.getBundle();
        if (fallbackBundle != null) {
            return fallbackBundle;
        }

        throw new IllegalStateException("Resource bundle is not available for SettingsController");
    }

    private String resolveLabel(Map<String, String> mapping, String code) {
        return mapping.entrySet().stream()
                .filter(entry -> entry.getValue().equals(code))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(mapping.keySet().iterator().next());
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

