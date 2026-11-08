package hitmaster.services;

import hitmaster.views.GameView;
import hitmaster.views.MainMenu;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class Navigation {

    private static Navigation instance;
    
    private final Stage stage;
    private Scene scene;

    private MainMenu mainMenuInstance;

    private Navigation() {
        this.stage = new Stage();
        this.scene = new Scene(new StackPane(), 800, 600);

        stage.getIcons().add(
            new Image(getClass().getResourceAsStream("/cardDesign.png"))
        );

        ThemeManager.getInstance().registerScene(scene);

        stage.setScene(scene);

        Platform.runLater(stage::requestFocus);
    }

    public static synchronized Navigation getInstance() {
        if (instance == null)
            instance = new Navigation();

        return instance;
    }

    public void setMainMenuInstance(MainMenu mainMenu) {
        mainMenuInstance = mainMenu;
    }

    public void toMainMenu() {
        scene.setRoot(mainMenuInstance.getView());
        setTitle("Hitmaster");
    }

    public void show(Parent view) {
        if (stage.getScene() != null) {
            Parent oldRoot = stage.getScene().getRoot();

            if (oldRoot instanceof GameView gameView) {
                gameView.getGameLogic().stopAllTasks();
            }
        }

        scene.setRoot(view);
        if (stage != null && stage.getScene().getRoot() instanceof GameView gameView) {
            stage.setOnCloseRequest(event -> {
                gameView.getGameLogic().stopAllTasks();
            });
        }

        Platform.runLater(stage::requestFocus);
    }

    public void show(Scene scene) {
        this.scene = scene;

        if (stage.getScene() != null) {
            Parent oldRoot = stage.getScene().getRoot();

            if (oldRoot instanceof GameView gameView) {
                gameView.getGameLogic().stopAllTasks();
            }
        }

        stage.setScene(scene);
        if (stage != null && stage.getScene().getRoot() instanceof GameView gameView) {
            stage.setOnCloseRequest(event -> {
                gameView.getGameLogic().stopAllTasks();
            });
        }

        stage.show();
        Platform.runLater(stage::requestFocus);
    }

    public void setTitle(String title) {
        stage.setTitle(title);
    }

    public void maximize() {
        stage.setMaximized(true);
    }

    public void close() {
        stage.close();
    }

    public Stage getStage() {
        return stage;
    }
}
