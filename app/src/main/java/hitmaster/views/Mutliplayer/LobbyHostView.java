package hitmaster.views.Mutliplayer;

import java.net.InetAddress;
import java.net.UnknownHostException;

import hitmaster.GameLogic;
import hitmaster.design.ShowHideLabel;
import hitmaster.design.UiBuilder;
import hitmaster.models.GameOptions;
import hitmaster.models.GameOptionsDTO;
import hitmaster.models.JoinRequest;
import hitmaster.models.Player;
import hitmaster.services.Log;
import hitmaster.services.Navigation;
import hitmaster.services.NetworkManager;
import hitmaster.services.SvgParser;
import hitmaster.views.MultiplayerMenuView;
import hitmaster.views.templates.HitmasterDialog;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.WindowEvent;

public class LobbyHostView extends HitmasterDialog {

    private NetworkManager netManager;
    private GameOptions options;

    private final ListView<String> playerListView;

    /**
     * Constructor for "LobbyHostView".
     * Initializes UI and starts lobby broadcasting.
     * @param lobbyName Name of this lobby.
     * @param tcpPort Port to start the lobby broadcasting at.
     */
    public LobbyHostView(GameOptions options, String lobbyName, String password, int tcpPort) {
        super("Lobby: " + lobbyName);
        setPadding(new Insets(20));

        this.options = options;


        // ===== HEADER =====
        VBox headerBox = new VBox(3);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label headerLabel = new Label("Lobby: " + lobbyName);
        headerLabel.getStyleClass().add("header");

        Label descriptionLabel = new Label("Waiting for players to join...");
        descriptionLabel.getStyleClass().add("description");

        HBox ipBox = new HBox(
            8,
            new Label("Your IP:"),
            new ShowHideLabel(getIpAddress()) {{
                addToExceptionChars('.');
            }}
        );

        headerBox.getChildren().addAll(new HBox(headerLabel, UiBuilder.getHorizontalSpacer(), ipBox), descriptionLabel, UiBuilder.getHorizontalSeparator());
        setTop(headerBox);


        // ===== LOBBY VIEW =====
        HBox content = new HBox(20);
        content.setFillHeight(true);
        VBox.setVgrow(content, Priority.ALWAYS);

        VBox lobbyViewBox = new VBox(8);
        HBox.setHgrow(lobbyViewBox, Priority.ALWAYS);

        Label playersTitle = new Label("Players in Lobby:");
        playersTitle.getStyleClass().add("subheader");
        
        playerListView = new ListView<>();
        playerListView.getStyleClass().add("modern-listview");
        playerListView.setPrefHeight(180);
        playerListView.getItems().add(options.players[0].username + " (Host) (You)");
        lobbyViewBox.getChildren().addAll(playersTitle, playerListView);

        setCenter(lobbyViewBox);


        // ===== BOTTOM CONTROLS =====
        VBox bottomBox = new VBox(5);
        HBox controllsBox = new HBox(10);

        Button backButton = new Button();
        backButton.setTooltip(new Tooltip("Go Back"));
        backButton.setGraphic(SvgParser.streamToColoredSvgIcon(
            getClass().getResourceAsStream("/icons/arrow-left.svg"),
            "-primary-brush"
        ));

        backButton.setOnAction(e -> {
            MultiplayerMenuView menuView = new MultiplayerMenuView();
            Navigation.getInstance().show(menuView);
            stopServer();
        });

        Button startGameButton = new Button("Start Game");
        startGameButton.getStyleClass().add("primary-button");
        startGameButton.setPrefWidth(140);
        startGameButton.setDisable(true);
        startGameButton.setOnAction(e -> {
            stopServerBroadcastOnly(); 
            
            netManager.sendObject(new GameOptionsDTO(this.options, "START_GAME")); 
            
            GameLogic gameLogic = new GameLogic(this.options, true, netManager);
            Navigation.getInstance().show(gameLogic.getView());
            Navigation.getInstance().maximize();
        });

        controllsBox.getChildren().addAll(backButton, UiBuilder.getHorizontalSpacer(), startGameButton);
        bottomBox.getChildren().addAll(UiBuilder.getHorizontalSeparator(), controllsBox);
        setBottom(bottomBox);


        Navigation.getInstance().getStage().addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, event -> {
            stopServer();
        });

        startNetworking(tcpPort, lobbyName, password, startGameButton);
    }

    /**
     * Starts lobby as host and waits for a player to join.
     * When player object received, displays player in player list and unlocks play button.
     * @param port Port to wait for player object.
     * @param lobbyName Name of lobby (used for lobby broadcast).
     * @param password Passowrd to join lobby. Blank if none.
     * @param startGameBtn Reference to "Start Game" button so it can be enabled when player connects.
     */
    private void startNetworking(int port, String lobbyName, String password, Button startGameBtn) {
        netManager = new NetworkManager();
        
        new Thread(() -> {
            netManager.startAsHost(port, password, receivedObj -> {
                if (receivedObj instanceof JoinRequest request) {
                    Player clientPlayer = request.player;
                    clientPlayer.decodeImage();
                    this.options.players[1] = clientPlayer;

                    Log.Info("Player joined host lobby: " + clientPlayer.username);

                    Platform.runLater(() -> {
                        playerListView.getItems().add(clientPlayer.username);
                        startGameBtn.setDisable(false);
                    });

                    netManager.sendObject(new GameOptionsDTO(this.options, "OPTIONS_UPDATE"));
                }
            });
        }).start();

        netManager.startLobbyBroadcast(lobbyName, !password.isEmpty());
    }

    /**
     * Gets the IP address from local device and returns it as a String.
     * @return IP address of local device; "Unknown" if an error occurs.
     */
    private String getIpAddress() {
        String tempIp = "Unknown";

        try {
            tempIp = InetAddress.getLocalHost().getHostAddress();
        }
        catch (UnknownHostException e) {
            Log.Error("Could not determine local IP");
        }

        return tempIp;
    }

    /**
     * Stops broadcasting lobby to other players without closing connection.
     * Called when lobby is full.
     */
    private void stopServerBroadcastOnly() {
        if (netManager != null) {
            netManager.stopLobbyBroadcast();
        }
    }

    /**
     * Stops server connection fully.
     * Closes lobby and stops server broadcasting.
     * Used when game gets cancelled instead of started.
     */
    private void stopServer() {
        stopServerBroadcastOnly();
        netManager.closeConnection();
    }
}
