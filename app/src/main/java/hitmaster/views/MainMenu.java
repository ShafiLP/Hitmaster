package hitmaster.views;

import hitmaster.GameLogic;
import hitmaster.design.StyleDialog;
import hitmaster.models.GameOptions;
import hitmaster.models.Player;
import hitmaster.models.User;
import hitmaster.services.Database;
import hitmaster.services.Log;
import hitmaster.services.Spotify;
import hitmaster.services.ThemeManager;
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
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainMenu {

    private final BorderPane ROOT = new BorderPane();
    private final Stage STAGE;

    private User user;
    private boolean providerStatus = false;

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

        Label title = new Label("HITMASTER");
        title.getStyleClass().add("title");

        Button singleplayerBtn = new Button("Singleplayer");
        singleplayerBtn.getStyleClass().add("menu-button");
        singleplayerBtn.setOnAction(e -> {
            if (providerStatus) {
                GameOptions options = new GameOptions();
                options.players = new Player[1];
                options.players[0] = new Player(Database.getCurrentUser().username, "/setImages/debug.jpg");

                GameLogic game = new GameLogic(options);
                this.setStage(game.getView(), true);
            }
            else {
                StyleDialog.warningDialog("Warning", "Please connect to a music service before starting the game.");
            }
        });

        Button multiplayerBtn = new Button("Multiplayer");
        multiplayerBtn.getStyleClass().add("menu-button");
        multiplayerBtn.setOnAction(e -> {
            if (providerStatus) {
                MultiplayerMenuView multiplayerMenuView = new MultiplayerMenuView(this);
                multiplayerMenuView.show();
            }
            else {
                StyleDialog.warningDialog("Warning", "Please connect to a music service before starting the game.");
            }
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
        profile.getStyleClass().add("modern-button");

        initialiseProviderButton();

        VBox topLeft = new VBox(profile, PROVIDER);
        topLeft.setAlignment(Pos.TOP_LEFT);
        topLeft.setSpacing(8);
        topLeft.setStyle("-fx-padding: 10;");

        ROOT.setTop(topLeft);


        // Top right
        Button settings = new Button("⚙");
        settings.getStyleClass().add("modern-button");
        settings.setOnAction(e -> {
            SettingsView settingsView = new SettingsView(this);
            settingsView.show();
        });

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

    public void setStage(Pane pane, boolean maximized) {
        Scene scene = new Scene(pane, 800, 600);
        ThemeManager.getInstance().registerScene(scene);
        STAGE.setScene(scene);
        STAGE.setMaximized(maximized);
        STAGE.getIcons().add(new Image(getClass().getResourceAsStream("/cardDesign.png")));
        STAGE.show();
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
        providerStatus = false;

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
                providerStatus = true;
            }
            else {
                PROVIDER.setText(" ⚠");
                PROVIDER.getStyleClass().add("prov-button-warning");
                providerStatus = false;
            }
        }
    }
}