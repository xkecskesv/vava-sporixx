package sk.stuba.fiit.sporixx;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import java.util.Locale;
import java.util.ResourceBundle;

/*DOCASNY MAIN - TREBA HO NAHRADIT*/
public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        loadFonts();

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/login_basic.fxml"),
                ResourceBundle.getBundle("i18n/messages", new Locale("sk"))
        );
        Parent root = loader.load();

        Scene scene = new Scene(root, 1250, 920);
        stage.setTitle("Šporixx");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    private void loadFonts(){
        Font font_semi = Font.loadFont(getClass().getResourceAsStream("/assets/fonts/Inter_28pt-SemiBold.ttf"), 12);
        Font font_reg = Font.loadFont(getClass().getResourceAsStream("/assets/fonts/Inter_28pt-Regular.ttf"), 12);
        Font font_bold = Font.loadFont(getClass().getResourceAsStream("/assets/fonts/Inter_28pt-Bold.ttf"), 12);
    }
}