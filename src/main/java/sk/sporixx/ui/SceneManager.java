package sk.sporixx.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Locale;
import java.util.ResourceBundle;

public class SceneManager {

    private static Stage primaryStage;

    public static void setStage(Stage stage) {
        primaryStage = stage;
    }

    public static void switchTo(String fxmlFile) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                SceneManager.class.getResource("/fxml/" + fxmlFile),
                ResourceBundle.getBundle("i18n/messages", new Locale("en"))
        );
        Parent root = loader.load();
        primaryStage.getScene().setRoot(root);
    }
}