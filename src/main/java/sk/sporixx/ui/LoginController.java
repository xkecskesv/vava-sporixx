package sk.sporixx.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import sk.sporixx.service.AuthException;
import sk.sporixx.service.ServiceLocator;
import sk.sporixx.service.SessionManager;
import sk.sporixx.util.Localization;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        try {

            ServiceLocator.getAuthService().login(email, password);
            if (SessionManager.getInstance().isAdmin()) {
                SceneManager.switchTo("admin_panel.fxml");
            } else {
                SceneManager.switchTo("dashboard.fxml");
            }

        } catch (AuthException e) {
            //service vrstva hodi exception s klucom
            //kluc prelozime a zobrazime

            String messageKey = e.getMessage();

            if (messageKey != null && isValidKey(messageKey)){

                errorLabel.setText(Localization.get(messageKey));

            } else {
                errorLabel.setText(Localization.get("error.unexpected"));

            }

            errorLabel.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText(Localization.get("error.unexpected"));
            errorLabel.setVisible(true);
        }

    }

    /**
     * Overí, či lokalizačný kľúč existuje v aktuálnom resource bundle.
     *
     * @param key lokalizačný kľúč
     * @return {@code true}, ak kľúč existuje, inak {@code false}
     */
    private boolean isValidKey(String key){
        try{
            Localization.get(key);
            return true;

        }catch(Exception e){
            return false;
        }
    }

    /**
     * Presmeruje používateľa na registračnú obrazovku.
     */
    @FXML
    private void handleNewAccount() {

        try {
            SceneManager.switchTo("register.fxml");
        } catch (Exception e) {
            errorLabel.setText(Localization.get("error.unexpected"));
            errorLabel.setVisible(true);
        }

    }
}