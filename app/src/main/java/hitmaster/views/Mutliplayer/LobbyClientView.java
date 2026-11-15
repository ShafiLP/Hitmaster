package hitmaster.views.Mutliplayer;

import hitmaster.GameLogic;
import hitmaster.design.UiBuilder;
import hitmaster.models.GameOptions;
import hitmaster.models.Player;
import hitmaster.services.Navigation;
import hitmaster.services.NetworkManager;
import hitmaster.services.SvgParser;
import hitmaster.views.MultiplayerMenuView;
import hitmaster.views.templates.HitmasterDialog;
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

public class LobbyClientView extends HitmasterDialog {

    private Player hostPlayer;
    private final NetworkManager netManager;
    private GameOptions options;

    private final ListView<String> playerListView;

    /**
     * Constructor for "LobbyClientView".
     * Initializes UI.
     * @param lobbyName Name of this lobby.
     */
    public LobbyClientView(String lobbyName, String password, Player ownPlayer, NetworkManager netManager) {
        super("Lobby: " + lobbyName);
        setPadding(new Insets(20));

        this.netManager = netManager;


        // ===== HEADER =====
        VBox headerBox = new VBox(3);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label headerLabel = new Label("Lobby: " + lobbyName);
        headerLabel.getStyleClass().add("header");

        Label descriptionLabel = new Label("Waiting for host to start the game...");
        descriptionLabel.getStyleClass().add("description");


        headerBox.getChildren().addAll(headerLabel, descriptionLabel, UiBuilder.getHorizontalSeparator());
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
        playerListView.getItems().add((hostPlayer != null ? hostPlayer.username : "null") + " (Host)");
        playerListView.getItems().add(ownPlayer.username + " (You)");
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

        controllsBox.getChildren().addAll(backButton, UiBuilder.getHorizontalSpacer());
        bottomBox.getChildren().addAll(UiBuilder.getHorizontalSeparator(), controllsBox);
        setBottom(bottomBox);


        Navigation.getInstance().getStage().addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, event -> {
            stopServer();
        });
    }

    public void setGameOptions(GameOptions options) {
        this.options = options;
    }

    public void startGame() {
        GameLogic gameLogic = new GameLogic(options, false, netManager);
        Navigation.getInstance().show(gameLogic.getView());
        Navigation.getInstance().maximize();
    }

    private void stopServer() {
        netManager.closeConnection();
    }
}
