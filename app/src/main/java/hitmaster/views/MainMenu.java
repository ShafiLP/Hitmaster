package hitmaster.views;

import hitmaster.GameLogic;
import hitmaster.models.User;
import hitmaster.services.Database;
import hitmaster.services.Log;
import hitmaster.services.Spotify;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainMenu {

    private final BorderPane ROOT = new BorderPane();
    private final Stage STAGE;

    private User user;
    private final Button PROVIDER;

    public MainMenu(Stage stage) {
        user = Database.getCurrentUser();
        PROVIDER = new Button();

        this.STAGE = stage;
        this.buildUI();
    }

    public final void buildUI() {
        // ==============================
        // CENTER (Gamemodes, Quit)
        // ==============================

        ROOT.getStyleClass().add("app-background");

        Label title = new Label("HITMASTER");
        title.getStyleClass().add("title");

        Button singleplayerBtn = new Button("Singleplayer");
        singleplayerBtn.getStyleClass().add("menu-button");
        singleplayerBtn.setOnAction(e -> {
            GameLogic game = new GameLogic();

            Scene scene = new Scene(game.getView(), 800, 600);
            STAGE.setScene(scene);
            STAGE.show();
        });

        Button multiplayerBtn = new Button("Multiplayer");
        multiplayerBtn.getStyleClass().add("menu-button");
        multiplayerBtn.setOnAction(e -> {
            // TODO
            GameLogic game = new GameLogic();

            Scene scene = new Scene(game.getView(), 800, 600);
            STAGE.setScene(scene);
            STAGE.show();
        });

        Button exitBtn = new Button("Exit");
        exitBtn.getStyleClass().add("exit-button");
        exitBtn.setOnAction(e -> {
            Log.Info("Closing application");
            System.exit(0);
        });

        VBox centerBox = new VBox(15, title, singleplayerBtn, multiplayerBtn, exitBtn);
        centerBox.setAlignment(Pos.CENTER);

        ROOT.setCenter(centerBox);

        // ==============================
        // TOP (Profile, Provider, Settings)
        // ==============================

        // Top Left
        Button profile = new Button("👤 " + user.username);
        profile.getStyleClass().add("nav-button");

        initialiseProviderButton();

        VBox topLeft = new VBox(profile, PROVIDER);
        topLeft.setAlignment(Pos.TOP_LEFT);
        topLeft.setSpacing(8);
        topLeft.setStyle("-fx-padding: 10;");

        ROOT.setTop(topLeft);


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

        ROOT.setTop(top);
    }

    public BorderPane getView() {
        return ROOT;
    }

    public void initialiseProviderButton() {
        // Re-load user
        user = Database.getCurrentUser();

        PROVIDER.setText("No provider");
        PROVIDER.getStyleClass().add("prov-button-none");
        PROVIDER.setOnAction(e -> {
            ProviderSettingsView providerSettingsView = new ProviderSettingsView(this);
            providerSettingsView.show();
        });

        // SPOTIFY
        if (user.provider != null && user.provider.equals("spotify")) {
            ImageView icon = new ImageView(new Image(
                getClass().getResourceAsStream("/icons/spotify.png")
            ));

            icon.setFitWidth(15);
            icon.setFitHeight(15);

            PROVIDER.setGraphic(icon);
            PROVIDER.setContentDisplay(ContentDisplay.LEFT);

            // Check connection
            if (Spotify.requestSpotifyConnection() != null) {
                PROVIDER.setText(" ✓");
                PROVIDER.getStyleClass().add("prov-button-success");
            }
            else {
                PROVIDER.setText(" ⚠");
                PROVIDER.getStyleClass().add("prov-button-warning");
            }
        }
    }
}