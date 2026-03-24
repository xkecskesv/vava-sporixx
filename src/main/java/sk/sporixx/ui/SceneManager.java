package sk.sporixx.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

public class SceneManager {

    private static Stage primaryStage;
    private static String currentLanguage = "sk";

    public static void setStage(Stage stage) {
        primaryStage = stage;
    }

    public static void setLanguage(String language) {
        currentLanguage = language;
    }

    public static void switchTo(String fxmlFile) throws Exception {
        ResourceBundle bundle = new PropertyResourceBundle(
                new InputStreamReader(
                        SceneManager.class.getResourceAsStream("/i18n/messages_" + currentLanguage + ".properties"),
                        StandardCharsets.UTF_8
                )
        );

        FXMLLoader loader = new FXMLLoader(
                SceneManager.class.getResource("/fxml/" + fxmlFile),
                bundle
        );
        Parent root = loader.load();
        primaryStage.getScene().setRoot(root);
    }
}