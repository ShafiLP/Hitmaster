package hitmaster;

import hitmaster.services.Database;
import hitmaster.views.MainMenu;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) {
        //! DEBUG
        Database.initializeDatabase();
        Database.insertCsvIntoDatabase("artists", "artists.csv");
        Database.insertCsvIntoDatabase("songs", "debug.csv");

        MainMenu menu = new MainMenu(stage);

        Scene scene = new Scene(menu.getView(), 600, 400);

        scene.getStylesheets().add(
            getClass().getResource("/styles/app.css").toExternalForm()
        );

        stage.setTitle("Hitmaster");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
