package sk.stuba.fiit.sporixx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import sk.sporixx.service.ServiceLocator;
import sk.sporixx.ui.SceneManager;
import sk.sporixx.util.Localization;

/*DOCASNY MAIN - TREBA HO NAHRADIT NESKOR*/
public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        loadFonts();

        Localization.load("en");

        ServiceLocator.init();

        ServiceLocator.getRecurringRuleService().processRecurringRules();

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/login.fxml"),
                Localization.getBundle()
        );
        Parent root = loader.load();

        Scene scene = new Scene(root, 1250, 920);
        stage.setTitle("Šporixx");
        stage.setScene(scene);
        SceneManager.setStage(stage);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    private void loadFonts() {
        Font.loadFont(getClass().getResourceAsStream("/assets/fonts/Inter_28pt-SemiBold.ttf"), 12);
        Font.loadFont(getClass().getResourceAsStream("/assets/fonts/Inter_28pt-Regular.ttf"), 12);
        Font.loadFont(getClass().getResourceAsStream("/assets/fonts/Inter_28pt-Bold.ttf"), 12);
    }
}