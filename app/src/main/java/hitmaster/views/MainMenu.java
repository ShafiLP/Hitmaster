package hitmaster.views;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

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
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;


public class MainMenu {

    private User user;
    private boolean providerStatus = false;

    // App Info
    private final String VERSION = "0.0.1";
    private final String AUTHOR = "Shafi";

    // UI Elements
    private final BorderPane ROOT = new BorderPane();
    private final Stage STAGE;
    private final Button PROVIDER;
    private final Button PROFILE;

    public MainMenu(Stage stage) {
        user = Database.getCurrentUser();
        PROVIDER = new Button();
        PROFILE = new Button();

        this.STAGE = stage;
        this.buildUI();
    }

    public final void buildUI() {

        // ==============================
        // CENTER (Gamemodes, Quit)
        // ==============================

        // Hitmaster Title
        VBox titleContainer = new VBox(-5);
        titleContainer.setAlignment(Pos.CENTER);

        Label title = new Label("HITMASTER");
        title.getStyleClass().add("title");

        DropShadow ds = new DropShadow();
        ds.setOffsetY(5.0);
        ds.setColor(Color.color(0, 0, 0, 0.5));
        title.setEffect(ds);
        
        titleContainer.getChildren().add(title);
        VBox.setMargin(titleContainer, new Insets(0, 0, 30, 0));

        // Singleplayer Button
        Button singleplayerBtn = new Button("Singleplayer");
        singleplayerBtn.getStyleClass().add("menu-button");
        singleplayerBtn.setMaxWidth(250);
        singleplayerBtn.setPrefWidth(250);
        singleplayerBtn.setOnAction(e -> {
            this.initialiseProviderButton();
            this.checkConnectionStatusBeforeStart();

            if (providerStatus) {
                try {
                    GameOptions options = new GameOptions();
                    options.players = new Player[1];
                    options.players[0] = new Player(Database.getCurrentUser().username, "/setImages/debug.jpg");
                    options.moveTime = 300;
                    options.stealTime = 30;

                    GameLogic game = new GameLogic(options);
                    this.setStage(game.getView(), true);
                }
                catch (Exception ex) {
                    StyleDialog.errorDialog("Error", "Error while starting game:\n" + ex.getMessage());
                }
            }
        });

        // Multiplayer Button
        Button multiplayerBtn = new Button("Multiplayer");
        multiplayerBtn.getStyleClass().add("menu-button");
        multiplayerBtn.setMaxWidth(250);
        multiplayerBtn.setPrefWidth(250);
        multiplayerBtn.setOnAction(e -> {
            this.initialiseProviderButton();
            this.checkConnectionStatusBeforeStart();

            if (providerStatus) {
                try {
                    MultiplayerMenuView multiplayerMenuView = new MultiplayerMenuView(this);
                    multiplayerMenuView.show();
                }
                catch (Exception ex) {
                    StyleDialog.errorDialog("Error", "Error while starting game:\n" + ex.getMessage());
                }
            }
        });

        // Quit Button
        Button quitBtn = new Button("Quit");
        quitBtn.getStyleClass().add("exit-button");
        quitBtn.setMaxWidth(250);
        quitBtn.setPrefWidth(250);
        quitBtn.setOnAction(e -> {
            Log.Info("Closing application");
            System.exit(0);
        });

        VBox centerBox = new VBox(20, title, singleplayerBtn, multiplayerBtn, quitBtn);
        centerBox.setAlignment(Pos.CENTER);

        ROOT.setCenter(centerBox);

        // ==============================
        // TOP (Profile, Provider, Settings)
        // ==============================

        // Top Left
        PROFILE.setText(user.getImage() == null ? "👤 " + user.username : user.username);
        PROFILE.getStyleClass().add("modern-button");
        PROFILE.setPrefWidth(100);

        if (user.getImage() != null) {
            ImageView icon = new ImageView(user.getImage());
            icon.setFitWidth(15);
            icon.setFitHeight(15);

            Circle clip = new javafx.scene.shape.Circle(7.5, 7.5, 7.5);
            icon.setClip(clip);

            PROFILE.setGraphic(icon);
            PROFILE.setContentDisplay(ContentDisplay.LEFT);
        } else {
            PROFILE.setGraphic(null); 
        }

        PROFILE.setOnAction(e -> {
            UserSettingsView userSettings = new UserSettingsView(this);
            userSettings.show();
        });

        this.initialiseProviderButton();
        PROVIDER.setPrefWidth(80);

        // Top Right
        Button settingsBtn = new Button("⚙");
        settingsBtn.getStyleClass().add("modern-button");
        settingsBtn.setPrefWidth(30);
        settingsBtn.setPrefHeight(30);
        settingsBtn.setOnAction(e -> {
            SettingsView settingsView = new SettingsView(this);
            settingsView.show();
        });

        // Add all to Top
        HBox top = new HBox();
        top.setAlignment(Pos.CENTER_LEFT);
        top.setSpacing(8);
        top.setPadding(new Insets(25));
        top.setMaxWidth(Double.MAX_VALUE);

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        top.getChildren().addAll(PROFILE, PROVIDER, topSpacer, settingsBtn);

        ROOT.setTop(top);

        // ==============================
        // BOTTOM (Version, Author, Bug Report)
        // ==============================

        // Version & Author
        Label infoLabel = new Label("v" + VERSION + " | Created by " + AUTHOR);
        infoLabel.getStyleClass().add("description");

        // Links (GitHub & Bug Report)
        HBox bottomRight = new HBox(15);
        bottomRight.setAlignment(Pos.CENTER_RIGHT);

        Button githubBtn = new Button();
        githubBtn.setTooltip(new Tooltip("To GitHub repository"));
        githubBtn.getStyleClass().add("modern-button");
        githubBtn.setMaxHeight(20);
        githubBtn.setPrefHeight(20);

        ImageView icon = new ImageView(new Image(
            getClass().getResourceAsStream("/icons/github.png")
        ));
        icon.setFitWidth(15);
        icon.setFitHeight(15);

        githubBtn.setGraphic(icon);
        githubBtn.setContentDisplay(ContentDisplay.CENTER);

        githubBtn.setOnAction(e -> {
            try {
                Desktop.getDesktop().browse(new URI("https://github.com/ShafiLP/Hitmaster"));
            }
            catch (IOException | URISyntaxException ex) {
                Log.Warning("Failed to open link in browser: " + ex.getMessage());
            }
        });

        Button reportBugBtn = new Button();
        reportBugBtn.setTooltip(new Tooltip("Report Bug"));
        reportBugBtn.getStyleClass().add("modern-button");
        reportBugBtn.setMaxHeight(20);
        reportBugBtn.setPrefHeight(20);

        icon = new ImageView(new Image(
            getClass().getResourceAsStream("/icons/bug.png")
        ));
        icon.setFitWidth(15);
        icon.setFitHeight(15);

        reportBugBtn.setGraphic(icon);
        reportBugBtn.setContentDisplay(ContentDisplay.CENTER);

        reportBugBtn.setOnAction(e -> {
            try {
                Desktop.getDesktop().browse(new URI("https://github.com/ShafiLP/Hitmaster/issues"));
            }
            catch (IOException | URISyntaxException ex) {
                Log.Warning("Failed to open link in browser: " + ex.getMessage());
            }
        });

        // Add all to Bottom
        bottomRight.getChildren().addAll(githubBtn, reportBugBtn);

        Region bottomSpacer = new Region();
        HBox.setHgrow(bottomSpacer, Priority.ALWAYS);

        HBox bottom = new HBox(infoLabel, bottomSpacer, bottomRight);
        bottom.setPadding(new Insets(15, 25, 15, 25));
        bottom.setAlignment(Pos.CENTER);
        bottom.setStyle("-fx-background-color: rgba(0, 0, 0, 0.05);");

        ROOT.setBottom(bottom);
    }

    /**
     * Replaces current Stage with a new Scene.
     * All Menus get closed when this method gets called.
     * @param pane New Scene of the Stage.
     * @param maximized Boolean if Stage should get maximized to full screen or not.
     */
    public void setStage(Pane pane, boolean maximized) {
        Scene scene = new Scene(pane, 800, 600);
        ThemeManager.getInstance().registerScene(scene);
        STAGE.setScene(scene);
        STAGE.setMaximized(maximized);
        STAGE.getIcons().add(new Image(getClass().getResourceAsStream("/cardDesign.png")));
        STAGE.show();
    }

    /**
     * Gets the BorderPane ROOT and returns it.
     * @return BorderPane ROOT.
     */
    public BorderPane getView() {
        return ROOT;
    }

    private boolean checkConnectionStatusBeforeStart() {
        boolean success  = Spotify.checkAccessToken(Spotify.requestSpotifyConnection());
        if (!success) {
            StyleDialog.warningDialog("Connection Failed", "Connection to Spotify API failed.\nCheck internet connection and re-connect to Spotify API.");
            return false;
        }

        success  = Spotify.checkValidDevice(Spotify.requestSpotifyConnection());
        if (!success) {
            StyleDialog.warningDialog("Connection Failed", "Connection to Spotify device failed.\nCheck if a playing device is available. One device of your account should be playing.");
            return false;
        }
        return true;
    }

    /**
     * Initializes the provider button "PROVIDER".
     * Checks if connection to music provider is successful and displays success status.
     * Shows "No Provider" if no music provider is set.
     * Called when starting the application or after connecting a music provider.
     */
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
            if (Spotify.checkValidDevice(Spotify.requestSpotifyConnection())) {
                PROVIDER.setText(" ✓");
                PROVIDER.getStyleClass().removeAll("prov-button-none", "prov-button-warning");
                PROVIDER.getStyleClass().add("prov-button-success");
                providerStatus = true;
            }
            else {
                PROVIDER.setText(" ⚠");
                PROVIDER.getStyleClass().removeAll("prov-button-none", "prov-button-success");
                PROVIDER.getStyleClass().add("prov-button-warning");
                providerStatus = false;
            }
        }
    }

    /**
     * Re-Loads username and user profile picture.
     */
    public void updateProfileButton() {
        // 1) Re-Load user from Database
        user = Database.getCurrentUser();

        // 2) Set new username and profile picture
        PROFILE.setText(user.getImage() == null ? "👤 " + user.username : user.username);
        
        if (user.getImage() != null) {
            ImageView icon = new ImageView(user.getImage());
            icon.setFitWidth(15);
            icon.setFitHeight(15);

            Circle clip = new Circle(7.5, 7.5, 7.5);
            icon.setClip(clip);

            PROFILE.setGraphic(icon);
            PROFILE.setContentDisplay(ContentDisplay.LEFT);
        } else {
            PROFILE.setGraphic(null); 
        }
    }
}