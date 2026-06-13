package hitmaster;

import hitmaster.services.Database;
import hitmaster.services.ThemeManager;
import hitmaster.views.MainMenu;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class Main extends Application {
    
    @Override
    public void start(Stage stage) {
        //! DEBUG
        Database.initializeDatabase();
        Database.insertCsvIntoSongs("debug.csv", 1);
        Database.insertCsvIntoSongs("hitster-de.csv", 3);

        MainMenu menu = new MainMenu(stage);

        Scene scene = new Scene(menu.getView(), 600, 400);

        ThemeManager.getInstance().registerScene(scene);

        stage.setTitle("Hitmaster");
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/cardDesign.png")));
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
