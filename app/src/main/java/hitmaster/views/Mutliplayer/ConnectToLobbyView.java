package hitmaster.views.Mutliplayer;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import hitmaster.GameLogic;
import hitmaster.models.GameOptions;
import hitmaster.models.Player;
import hitmaster.models.User;
import hitmaster.services.Database;
import hitmaster.services.Log;
import hitmaster.services.NetworkManager;
import hitmaster.services.ThemeManager;
import hitmaster.views.MainMenu;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ConnectToLobbyView {
    
    private final MainMenu PARENT;
    private final Stage STAGE;
    private final Stage PREV_STAGE;

    private final Player PLAYER;

    private NetworkManager discoveryNetManager;
    private final Set<String> foundLobbiesTracker = new HashSet<>(); // Prevents duplicates

    /**
     * Constructor for ConnectToLobbyView.
     * Initializes UI and starts looking for available lobbies.
     * @param parent MainMenu that started this view (Used to set stage later).
     * @param prevStage Previous stage (Used to show view on top of it).
     */
    public ConnectToLobbyView(MainMenu parent, Stage prevStage) {
        this.PARENT = parent;
        this.PREV_STAGE = prevStage;

        STAGE = new Stage();
        STAGE.setTitle("Connect To Host");

        User user = Database.getCurrentUser();
        PLAYER = new Player(user.username, user.picture);

        // =========================
        // ROOT LAYOUT
        // =========================
        VBox root = new VBox(25);
        root.setPadding(new Insets(20));
        root.setFillWidth(true);

        // =========================
        // HEADER
        // =========================
        Label title = new Label("Connect To Host");
        title.getStyleClass().add("header");

        Label description = new Label("Select a lobby from list or enter host's IP-adress to connect to game.");
        description.getStyleClass().add("header-description");

        VBox header = new VBox(5, title, description);
        header.setAlignment(Pos.TOP_LEFT);

        // =========================
        // CONTENT
        // =========================
        VBox content = new VBox(15);
        content.setFillWidth(true);

        Label statusLabel = new Label("Searching for lobbies...");

        ListView<String> lobbyListView = new ListView<>();
        lobbyListView.setPrefHeight(120);
        lobbyListView.getStyleClass().add("modern-listview");

        lobbyListView.setCellFactory(lv -> new ListCell<>() {
            private final HBox layout = new HBox();
            private final Label nameLabel = new Label();
            private final Label countLabel = new Label("1/2 👤");

            {
                layout.setAlignment(Pos.CENTER_LEFT);
                countLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 13px;");
                
                HBox.setHgrow(nameLabel, Priority.ALWAYS);
                nameLabel.setMaxWidth(Double.MAX_VALUE);
                
                layout.getChildren().addAll(nameLabel, countLabel);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                }
                else {
                    nameLabel.setText(item);
                    nameLabel.textFillProperty().bind(this.textFillProperty()); 
                    setGraphic(layout);
                }
            }
        });
        
        TextField ipInput = new TextField();
        ipInput.getStyleClass().add("modern-textbox");
        ipInput.setPrefWidth(180);
        ipInput.setPromptText("Enter IP manually (e.g. 192.168.1.1)");

        content.getChildren().addAll(statusLabel, lobbyListView, ipInput);

        // =========================
        // FOOTER (Cancel & Start)
        // =========================
        Button cancel = new Button("Cancel");
        cancel.getStyleClass().add("modern-button");
        cancel.setCancelButton(true);
        cancel.setPrefWidth(120);
        cancel.setOnAction(e -> {
            STAGE.close();
        });

        Button connectToGame = new Button("Connect");
        connectToGame.getStyleClass().add("primary-button");
        connectToGame.setPrefWidth(140);
        connectToGame.setOnAction(e -> {
            String ip = ipInput.getText().trim();

            if (ip.isEmpty()) {
                ipInput.setStyle("-fx-border-color: red;");
                return;
            }

            this.stopDiscovery();

            connectToGame.setDisable(true);
            connectToGame.setText("Connecting...");
            lobbyListView.setDisable(true);
            ipInput.setEditable(false);

            new Thread(() -> {
                try {
                    int port = 5050; // Same port as host!
                    NetworkManager netManager = new NetworkManager();
                    
                    // Start connecting
                    netManager.startAsClient(ip, port, receivedObj -> {
                        if (receivedObj instanceof GameOptions hostOptions) {
                            Platform.runLater(() -> {
                                STAGE.close(); 

                                for (Player player : hostOptions.players) {
                                    player.decodeImage();
                                }
                                
                                GameLogic gameLogic = new GameLogic(hostOptions, false, netManager);
                                PARENT.setStage(gameLogic.getView(), true);
                                
                                Log.Success("GameOptions received! Starting Game...");
                            });
                        }
                        Log.Info("Received by host: " + receivedObj);
                    });

                    Log.Info("Sending player");
                    netManager.sendObject(PLAYER);
                    Log.Info("Sent player");
                }
                catch (IOException ex) {
                    Log.Error("Couldn't connect to host: " + ex.getMessage());
                    
                    Platform.runLater(() -> {
                        connectToGame.setDisable(false);
                        connectToGame.setText("Connect");

                        lobbyListView.setDisable(false);
                        this.startDiscovery(lobbyListView, statusLabel);

                        ipInput.setStyle("-fx-border-color: red;");
                        ipInput.setEditable(true);
                    });
                }
            }).start();
        });

        lobbyListView.setOnMouseClicked(click -> {
            if (click.getClickCount() == 2) {
                String selected = lobbyListView.getSelectionModel().getSelectedItem();
                
                if (selected != null && selected.contains("(") && selected.contains(")")) {
                    String ip = selected.substring(selected.indexOf("(") + 1, selected.indexOf(")"));
                    ipInput.setText(ip);
                    connectToGame.fire();
                }
            }
        });

        HBox footer = new HBox(10, cancel, connectToGame);
        footer.setAlignment(Pos.BOTTOM_RIGHT);
        footer.setPadding(new Insets(10, 0, 0, 0));

        // =========================
        // ROOT ASSEMBLY & SCENE
        // =========================
        root.getChildren().addAll(header, content, footer);

        Scene scene = new Scene(root, 520, 360);
        ThemeManager.getInstance().registerScene(scene);

        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                connectToGame.fire();
                event.consume(); 
            }
        });

        STAGE.setScene(scene);
        STAGE.setOnCloseRequest(e -> this.stopDiscovery());

        Platform.runLater(STAGE::requestFocus);
        this.startDiscovery(lobbyListView, statusLabel);
    }

    /**
     * Starts searching for open lobbies.
     * When lobby gets found, adds it to list of available lobbies.
     * @param lobbyListView ListView of available lobbies to put new found lobbies at.
     * @param statusLabel Status label to put current status on.
     */
    private void startDiscovery(ListView<String> lobbyListView, Label statusLabel) {
        if (discoveryNetManager == null) {
            foundLobbiesTracker.clear();
            lobbyListView.getItems().clear();
            
            discoveryNetManager = new NetworkManager();
            discoveryNetManager.startLobbyDiscovery((lobbyName, ipAddress, port) -> {
                String entry = lobbyName;
                
                if (!foundLobbiesTracker.contains(entry)) {
                    foundLobbiesTracker.add(entry);
                    Platform.runLater(() -> {
                        lobbyListView.getItems().add(entry);
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
    private void stopDiscovery() {
        if (discoveryNetManager != null) {
            discoveryNetManager.stopLobbyDiscovery();
            discoveryNetManager = null;
            Log.Info("Stopped LAN Discovery thread.");
        }
    }

    /**
     * Shows already initialized UI and waits.
     * Shows UI right above previous stage.
     */
    public void show() {
        // 1) Place view on top of previous stage
        STAGE.setOnShowing(e -> {
            Platform.runLater(() -> {
                double ownerX = PREV_STAGE.getX();
                double ownerY = PREV_STAGE.getY();
                double ownerWidth = PREV_STAGE.getWidth();
                double ownerHeight = PREV_STAGE.getHeight();

                double newWidth = STAGE.getWidth();
                double newHeight = STAGE.getHeight();

                double centerX = ownerX + (ownerWidth / 2.0) - (newWidth / 2.0);
                double centerY = ownerY + (ownerHeight / 2.0) - (newHeight / 2.0);

                STAGE.setX(centerX);
                STAGE.setY(centerY);

                if (PREV_STAGE != null)
                    PREV_STAGE.close();
            });
        });
        
        // 2) Show view and wait
        STAGE.initModality(Modality.APPLICATION_MODAL);
        STAGE.showAndWait();
    }
}
