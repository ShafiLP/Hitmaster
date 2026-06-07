package hitmaster.views;

import hitmaster.design.UI;
import hitmaster.services.Database;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainMenu {

    private final BorderPane root = new BorderPane();
    private final Stage stage;

    public MainMenu(Stage stage) {
        this.stage = stage;
        this.buildUI();
    }

    public final void buildUI() {
        Label title = new Label("HITMASTER");

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

        // ==============================
        // TOP (Profile, Provider, Settings)
        // ==============================

        // Top Left
        Button profile = new Button("👤 " + Database.getCurrentUser().username);
        profile.getStyleClass().add("nav-button");

        Button provider = new Button("🎵 | ✔️"); // TODO: Get from database and show "{icon}: {status}"
        provider.getStyleClass().add("nav-button");
        provider.setOnAction(e -> {
            ProviderSettingsView providerSettingsView = new ProviderSettingsView();
            providerSettingsView.show();
        });

        VBox topLeft = new VBox(profile, provider);
        topLeft.setAlignment(Pos.TOP_LEFT);
        topLeft.setSpacing(8);
        topLeft.setStyle("-fx-padding: 10;");

        root.setTop(topLeft);


        // Top right
        Button settings = new Button("⚙️");
        settings.getStyleClass().add("nav-button");

        VBox topRight = new VBox(settings);
        topRight.setAlignment(Pos.TOP_RIGHT);
        topRight.setStyle("-fx-padding: 10;");


        // Add all to top
        HBox top = new HBox();
        top.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        top.getChildren().addAll(topLeft, spacer, topRight);
        top.setPadding(new Insets(10));

        root.setTop(top);



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