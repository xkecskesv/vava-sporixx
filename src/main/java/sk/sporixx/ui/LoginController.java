package sk.sporixx.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
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

            // TODO: authService.login(email, password); ked mergneme a adelka prida service
            SceneManager.switchTo("dashboard.fxml");

        }catch(Exception e){
            //service vrstva hodi exception s klucom
            //kluc prelozime a zobrazime

            String messageKey = e.getMessage();

            if (messageKey != null && isValidKey(messageKey)){

                errorLabel.setText(Localization.get(messageKey));

            } else {

                errorLabel.setText(Localization.get("error.unexpected"));

            }

            errorLabel.setVisible(true);
        }

    }

    private boolean isValidKey(String key){
        try{
            Localization.get(key);
            return true;

        }catch(Exception e){
            return false;
        }
    }

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