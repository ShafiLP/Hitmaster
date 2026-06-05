package hitmaster;

import hitmaster.views.MainMenu;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) {
        MainMenu menu = new MainMenu(stage);

        Scene scene = new Scene(menu.getView(), 600, 400);

        stage.setTitle("Hitmaster");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
