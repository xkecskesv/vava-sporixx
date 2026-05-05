package sk.sporixx.ui;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import sk.sporixx.service.ServiceLocator;
import sk.sporixx.service.SettingsService;
import sk.sporixx.util.Localization;

public class SettingsController implements Initializable {

    @FXML private ComboBox<String> languageComboBox;
    @FXML private ComboBox<String> currencyComboBox;

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

        setupGeneralSettings(effectiveResources, settingsService);

        initializing = false;
    }

    private void setupGeneralSettings(ResourceBundle resources, SettingsService settingsService) {
        languageByLabel.clear();
        currencyByLabel.clear();

        languageByLabel.put(resources.getString("settings.general.language.english"), "en");
        languageByLabel.put(resources.getString("settings.general.language.slovak"), "sk");
        languageByLabel.put(resources.getString("settings.general.language.czech"), "cs");
        languageByLabel.put(resources.getString("settings.general.language.polish"), "pl");
        languageByLabel.put(resources.getString("settings.general.language.german"), "de");

        currencyByLabel.put(resources.getString("settings.general.currency.eur"), "EUR");
        currencyByLabel.put(resources.getString("settings.general.currency.usd"), "USD");
        currencyByLabel.put(resources.getString("settings.general.currency.czk"), "CZK");
        currencyByLabel.put(resources.getString("settings.general.currency.gbp"), "GBP");
        currencyByLabel.put(resources.getString("settings.general.currency.pln"), "PLN");

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

}

