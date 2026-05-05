package sk.sporixx.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import sk.sporixx.service.ServiceLocator;
import sk.sporixx.util.Localization;


public class RegisterController {

    @FXML public PasswordField repeatPasswordField;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    @FXML
    private void handleRegister() {
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String repeatPassword = repeatPasswordField.getText();


        try{

           ServiceLocator.getAuthService().register(firstName, lastName, email, password, repeatPassword);
           SceneManager.switchTo("dashboard.fxml");

        }catch(Exception e) {

            String messageKey = e.getMessage();

            if(messageKey != null && isValidKey(messageKey)) {
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
    private void handleHasAccount() {
        try {
            SceneManager.switchTo("login.fxml");
        } catch (Exception e) {
            errorLabel.setText(Localization.get("error.unexpected"));
            errorLabel.setVisible(true);
        }
    }
}