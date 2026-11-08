package hitmaster.views.Mutliplayer;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import hitmaster.GameLogic;
import hitmaster.design.StyleDialog;
import hitmaster.design.UiBuilder;
import hitmaster.models.GameOptionsDTO;
import hitmaster.models.JoinRequest;
import hitmaster.models.MultiplayerLobby;
import hitmaster.models.Player;
import hitmaster.models.User;
import hitmaster.services.Database;
import hitmaster.services.Log;
import hitmaster.services.Navigation;
import hitmaster.services.NetworkManager;
import hitmaster.services.SvgParser;
import hitmaster.services.ThemeManager;
import hitmaster.services.UpdateService;
import hitmaster.views.GameSettingsView;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class LobbyView extends VBox {

    private final Player PLAYER;
    private final String APP_VERSION;

    private NetworkManager discoveryNetManager;
    private NetworkManager activeNetManager;
    private final Set<String> foundLobbiesTracker = new HashSet<>(); // Prevents duplicates

    private LobbyClientView clientView;

    /**
     * Constructor for ConnectToLobbyView.
     * Initializes UI and starts looking for available lobbies.
     */
    public LobbyView() {
        super(25);
        setFillWidth(true);

        APP_VERSION = LobbyView.loadAppVersion();

        User user = Database.getInstance().getCurrentUser();
        PLAYER = new Player(user.username, user.picture);


        // ===== Lobbies =====
        VBox content = new VBox(15);
        content.setFillWidth(true);

        Label statusLabel = new Label("Searching for lobbies...");

        Button createLobbyButton = new Button("New Lobby");

        createLobbyButton.setGraphic(SvgParser.streamToSvgIcon(
            getClass().getResourceAsStream("/icons/plus.svg")
        ));

        createLobbyButton.setOnAction(e -> {
            GameSettingsView settingsView = new GameSettingsView(true);
            settingsView.show();

            stopDiscovery();
        });

        ListView<MultiplayerLobby> lobbyListView = new ListView<>();
        lobbyListView.setPrefHeight(120);
        lobbyListView.getStyleClass().add("modern-listview");

        lobbyListView.setCellFactory(lv -> new ListCell<>() {
            private final HBox layout = new HBox();
            private final Label nameLabel = new Label();
            private final Label lockLabel = new Label("🔒");
            private final Label countLabel = new Label("1/2 👤");

            {
                layout.setAlignment(Pos.CENTER_LEFT);
                countLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 13px;");
                lockLabel.setStyle("-fx-font-size: 13px;");
                
                HBox.setHgrow(nameLabel, Priority.ALWAYS);
                nameLabel.setMaxWidth(Double.MAX_VALUE);
                
                layout.getChildren().addAll(nameLabel, lockLabel, countLabel);
            }

            @Override
            protected void updateItem(MultiplayerLobby item, boolean empty) {
                super.updateItem(item, empty);
                
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                }
                else {
                    nameLabel.setText(item.name);
                    nameLabel.textFillProperty().bind(this.textFillProperty()); 

                    lockLabel.setVisible(item.hasPassword);
                    lockLabel.setManaged(item.hasPassword);

                    setGraphic(layout);
                }
            }
        });

        HBox statusCreateRow = new HBox(5);
        statusCreateRow.setAlignment(Pos.BOTTOM_CENTER);
        statusCreateRow.getChildren().addAll(statusLabel, UiBuilder.getHorizontalSpacer(), createLobbyButton);

        content.getChildren().addAll(statusCreateRow, lobbyListView);


        // ===== Manual Input =====
        TextField ipInput = new TextField();
        ipInput.setPrefWidth(180);
        ipInput.setPromptText("Enter IP manually (e.g. 192.168.1.1)");

        Button connectToGame = new Button("Connect");
        connectToGame.getStyleClass().add("primary-button");
        connectToGame.setPrefWidth(140);
        connectToGame.setOnAction(e -> {
            String ip = ipInput.getText().trim();

            if (ip.isEmpty()) {
                ipInput.setStyle("-fx-border-color: red;");
                return;
            }

            MultiplayerLobby selectedLobby = lobbyListView.getSelectionModel().getSelectedItem();

            if (selectedLobby != null && selectedLobby.hasPassword) {
                String password = showPasswordInputDialog(selectedLobby.name);

                if (password == null) 
                    return;

                connectToHost(ip, selectedLobby.name, password, connectToGame, lobbyListView, ipInput, statusLabel);
            }
            else {
                connectToHost(ip, selectedLobby.name, "", connectToGame, lobbyListView, ipInput, statusLabel);
            }
        });

        HBox connectRow = new HBox(7);
        HBox.setHgrow(ipInput, Priority.ALWAYS);
        connectRow.getChildren().addAll(ipInput, connectToGame);

        content.getChildren().addAll(connectRow);

        lobbyListView.setOnMouseClicked(click -> {
            if (click.getClickCount() == 2) {
                MultiplayerLobby selected = lobbyListView.getSelectionModel().getSelectedItem();
                
                if (selected != null) {
                    // Check if app version of host and client are equal
                    if (!selected.appVersion.equals(APP_VERSION)) {
                        StyleDialog.errorDialog("Version Error", "Couldn't connect to host:\nApp versions are not equal.\nHost Version: " + selected.appVersion + "\nYour Version: " + APP_VERSION);
                        return;
                    }

                    ipInput.setText(selected.ip);
                    connectToGame.fire();
                }
            }
        });


        // ===== SHORTCUTS =====
        addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                connectToGame.fire();
                event.consume(); 
            }
        });

        Navigation.getInstance().getStage().addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, event -> {
            stopDiscovery();
        });

        getChildren().add(content);
        startDiscovery(lobbyListView, statusLabel);
    }

    private void connectToHost(String ip, String lobbyName, String password, Button connectBtn, ListView<MultiplayerLobby> lobbyListView, TextField ipInput, Label statusLabel) {
        stopDiscovery();

        connectBtn.setDisable(true);
        connectBtn.setText("Connecting...");
        lobbyListView.setDisable(true);
        ipInput.setEditable(false);
        ipInput.setStyle("");
        statusLabel.setText("Connecting to " + lobbyName + "...");

        new Thread(() -> {
            try {
                activeNetManager = new NetworkManager();

                activeNetManager.startAsClient(ip, 5050, receivedObj -> {
                    if (receivedObj instanceof GameOptionsDTO hostOptions) {
                        switch (hostOptions.purpose) {
                            case "START_GAME":
                                Platform.runLater(() -> {
                                    clientView.close();

                                    for (Player player : hostOptions.players) {
                                        if (player != null) {
                                            player.decodeImage();
                                        }
                                    }
                                    
                                    GameLogic gameLogic = new GameLogic(hostOptions.getGameOptions(), false, activeNetManager);
                                    Navigation.getInstance().show(gameLogic.getView());
                                    Navigation.getInstance().maximize();
                                    
                                    Log.Success("GameOptions received! Starting Game...");
                                });
                            break;

                            case "OPTIONS_UPDATE":
                                // 1) Check if clientView is initialized
                                if (this.clientView == null) {
                                    Log.Error("Received \"" + hostOptions.purpose + "\" with clientView being null");
                                    return;
                                }

                                // 2) Update game options on UI
                                clientView.updateGameOptions(hostOptions.getGameOptions());
                            break;
                        }
                        
                    }
                    else if ("JOIN_SUCCESS".equals(receivedObj)) {
                        Log.Success("Successfully joined lobby!");
                        Platform.runLater(() -> statusLabel.setText("Joined! Waiting for Host to start..."));

                        // Show lobby to client
                        Platform.runLater(() -> {
                            // TODO: clientView = new LobbyClientView(lobbyName, password, PLAYER, activeNetManager);
                            //clientView.show(STAGE);
                        });
                    }
                    else if ("REJECTED_PASSWORD".equals(receivedObj)) {
                        Log.Error("Rejected connection: Wrong password!");
                        Platform.runLater(() -> {
                            StyleDialog.errorDialog("Access Denied", "Incorrect password entered for this lobby.");
                            this.resetUiAfterConnectionError(connectBtn, lobbyListView, ipInput, statusLabel);
                        });
                    }
                });

                activeNetManager.sendObject(new JoinRequest(PLAYER, password));
            }
            catch (IOException ex) {
                Log.Error("Couldn't connect to host: " + ex.getMessage());
                
                Platform.runLater(() -> {
                    StyleDialog.errorDialog("Connection Error", "Failed to connect to host at " + ip);
                    this.resetUiAfterConnectionError(connectBtn, lobbyListView, ipInput, statusLabel);
                });
            }
        }).start();
    }

    private void resetUiAfterConnectionError(Button connectBtn, ListView<MultiplayerLobby> lobbyListView, TextField ipInput, Label statusLabel) {
        connectBtn.setDisable(false);
        connectBtn.setText("Connect");

        lobbyListView.setDisable(false);
        ipInput.setStyle("-fx-border-color: red;");
        ipInput.setEditable(true);

        startDiscovery(lobbyListView, statusLabel);
    }

    private String showPasswordInputDialog(String lobbyName) {
        Stage stage = new Stage();
        stage.setTitle("Protected Lobby");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(Navigation.getInstance().getStage());

        Label headerLabel = new Label("Password Required");
        headerLabel.getStyleClass().add("header");

        Label descriptionLabel = new Label("The lobby \"" + lobbyName + "\" is protected with a password.");
        descriptionLabel.getStyleClass().add("description");
        descriptionLabel.setWrapText(true);

        GridPane inputGrid = new GridPane();
        inputGrid.setHgap(15);
        inputGrid.setVgap(12);
        inputGrid.setPadding(new Insets(10, 0, 10, 0));

        Label passwordPrompt = new Label("Password: ");
        passwordPrompt.getStyleClass().add("subheader");

        PasswordField passwordInput = new PasswordField();
        passwordInput.setPromptText("Enter Password");
        passwordInput.setPrefWidth(200);

        inputGrid.add(passwordPrompt, 0, 0);
        inputGrid.add(passwordInput, 1, 0);

        final String[] result = new String[1];

        Button joinButton = new Button("Join");
        joinButton.getStyleClass().add("primary-button");
        joinButton.setOnAction(e -> {
            result[0] = passwordInput.getText().trim();
            stage.close();
        });

        Button cancelButton = new Button("Cancel");
        cancelButton.getStyleClass().add("error-button");
        cancelButton.setOnAction(e -> {
            result[0] = null;
            stage.close();
        });

        HBox buttonBox = new HBox(15, cancelButton, joinButton);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(15, headerLabel, descriptionLabel, inputGrid, buttonBox);
        root.setPadding(new Insets(20));

        Scene scene = new Scene(root);
        ThemeManager.getInstance().registerScene(scene);

        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                joinButton.fire();
                event.consume();
            }
        });

        stage.setScene(scene);
        stage.setResizable(false);
        stage.sizeToScene();
        stage.showAndWait();

        return result[0];
    }

    /**
     * Starts searching for open lobbies.
     * When lobby gets found, adds it to list of available lobbies.
     * @param lobbyListView ListView of available lobbies to put new found lobbies at.
     * @param statusLabel Status label to put current status on.
     */
    private void startDiscovery(ListView<MultiplayerLobby> lobbyListView, Label statusLabel) {
        if (discoveryNetManager == null) {
            foundLobbiesTracker.clear();
            lobbyListView.getItems().clear();
            
            discoveryNetManager = new NetworkManager();
            discoveryNetManager.startLobbyDiscovery((lobby) -> {
                if (!foundLobbiesTracker.contains(lobby.ip)) {
                    foundLobbiesTracker.add(lobby.ip);
                    Platform.runLater(() -> {
                        lobbyListView.getItems().add(lobby);
                        statusLabel.setText("Lobbies found in your network (Double-click to join):");
                    });
                }
            });
        }
    }

    /**
     * Stops searching for open lobbies.
     * Called when connecting to a lobby or when closing view.
     */
    public void stopDiscovery() {
        if (discoveryNetManager != null) {
            discoveryNetManager.stopLobbyDiscovery();
            discoveryNetManager = null;
            Log.Info("Stopped LAN Discovery thread.");
        }

        if (clientView != null) {
            clientView.close();
        }

        if (activeNetManager != null) {
            activeNetManager.closeConnection();
        }
    }

    /**
     * Loads current version of app from project.proerties file.
     * @return Current app version as String.
     */
    private static String loadAppVersion() {
        Properties properties = new Properties();

        try (InputStream input = UpdateService.class.getClassLoader().getResourceAsStream("project.properties")) {
            if (input == null)
                return "unknown";

            properties.load(input);
            return properties.getProperty("version", "unknown");
        }
        catch (IOException e) {
            return "unknown";
        }
    }
}
