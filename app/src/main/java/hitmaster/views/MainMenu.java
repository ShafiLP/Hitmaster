package hitmaster.views;

import hitmaster.design.UI;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainMenu {

    private final BorderPane root = new BorderPane();
    private final Stage stage;

    public MainMenu(Stage stage) {
        this.stage = stage;
        this.buildUI();
    }

    public void buildUI() {

        Label title = new Label("HITMASTER");

        Button settingsBtn = UI.settingsButton();

        Button singleplayerBtn = UI.navButton("Einzelspieler");
        singleplayerBtn.setOnAction(e -> {
            GameView gameView = new GameView();

            Scene scene = new Scene(gameView, 800, 600);
            stage.setScene(scene);
            stage.show();
        });
        Button multiplayerBtn = UI.navButton("Mehrspieler");
        Button quitButton = UI.quitButton();

        VBox centerBox = new VBox(15, title, singleplayerBtn, multiplayerBtn, quitButton);
        centerBox.setAlignment(Pos.CENTER);

        root.setCenter(centerBox);

        // 🔥 oben rechts
        VBox topRight = new VBox(settingsBtn);
        topRight.setAlignment(Pos.TOP_RIGHT);
        topRight.setStyle("-fx-padding: 10;");

        root.setTop(topRight);

        root.setStyle("-fx-background-color: #0f172a;");

        title.setStyle("""
            -fx-text-fill: white;
            -fx-font-size: 20;
        """);
    }

    public BorderPane getView() {
        return root;
    }
}