package hitmaster;

import hitmaster.services.Database;
import hitmaster.services.ThemeManager;
import hitmaster.services.UpdateService;
import hitmaster.views.MainMenu;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    
    @Override
    public void start(Stage stage) {
        // 1) Check For Update
        UpdateService.checkForUpdates();

        //! DEBUG
        Database.initializeDatabase();

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
