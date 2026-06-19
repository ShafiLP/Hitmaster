package hitmaster;

import hitmaster.services.Database;
import hitmaster.services.ThemeManager;
import hitmaster.views.MainMenu;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    
    @Override
    public void start(Stage stage) {
        //! DEBUG
        Database.initializeDatabase();
        Database.insertJsonIntoSongs("songs.json");

        Database.addSongsToSetFromCsv("hitster-de.csv", 1);

        MainMenu menu = new MainMenu(stage);

        Scene scene = new Scene(menu.getView(), 600, 450);
        menu.getView().prefWidthProperty().bind(scene.widthProperty());

        ThemeManager.getInstance().registerScene(scene);

        stage.setTitle("Hitmaster");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
