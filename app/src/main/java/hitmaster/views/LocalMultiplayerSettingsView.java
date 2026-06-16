package hitmaster.views;

import hitmaster.GameLogic;
import hitmaster.models.GameOptions;
import hitmaster.models.Player;
import hitmaster.services.Database;
import hitmaster.services.ThemeManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class LocalMultiplayerSettingsView {

    private final MainMenu PARENT;
    private final Stage STAGE;

    public LocalMultiplayerSettingsView(MainMenu parent) {
        this.PARENT = parent;

        STAGE = new Stage();
        STAGE.setTitle("Match Setup");

        // =========================
        // ROOT LAYOUT
        // =========================
        VBox root = new VBox(25);
        root.setPadding(new Insets(20));
        root.setFillWidth(true);

        // =========================
        // HEADER
        // =========================
        Label title = new Label("Game Settings");
        title.getStyleClass().add("header");

        Label description = new Label("Configure the rules before launching the multiplayer match.");
        description.getStyleClass().add("header-description");

        VBox header = new VBox(5, title, description);
        header.setAlignment(Pos.TOP_LEFT);

        // =========================
        // SETTINGS CONTENT (Platzhalter-Formular)
        // =========================
        VBox content = new VBox(15);
        content.setFillWidth(true);

        // 1) Configure Sets
        Button manageSetsBtn = new Button("Manage Sets");
        manageSetsBtn.getStyleClass().add("modern-button");
        manageSetsBtn.setPrefWidth(180);
        manageSetsBtn.setOnAction(e -> {
            SetManagerView setManagerView = new SetManagerView();
            setManagerView.show();
        });
        
        HBox setsRow = createSettingRow("Song Sets", "Import, export or edit your custom song packages.", manageSetsBtn);

        // 2) Player names
        VBox playerNames = new VBox();
        TextField p1Name = new TextField();
        p1Name.getStyleClass().add("modern-textbox");
        p1Name.setPrefWidth(180);
        p1Name.setPromptText("Player 1...");
        p1Name.setText(Database.getCurrentUser().username);
        playerNames.getChildren().add(p1Name);
        TextField p2Name = new TextField();
        p2Name.getStyleClass().add("modern-textbox");
        p2Name.setPrefWidth(180);
        p2Name.setPromptText("Player 2...");
        playerNames.getChildren().add(p2Name);

        HBox namesRow = createSettingRow("Player Names", "Enter player usernames.", playerNames);

        // 3) Turn time
        TextField turnTimeInput = new TextField();
        turnTimeInput.setPromptText("Turn Time...");
        turnTimeInput.getStyleClass().add("modern-textbox");
        turnTimeInput.setPrefWidth(180);
        
        HBox turnTimeRow = createSettingRow("Turn Time", "Set time each player gets to log in their guess.", turnTimeInput);

        // 4) Steal time input
        TextField stealTimeInput = new TextField();
        stealTimeInput.setPromptText("Steal Time...");
        stealTimeInput.getStyleClass().add("modern-textbox");
        stealTimeInput.setPrefWidth(180);
        
        HBox stealTimeRow = createSettingRow("Steal Time", "Time players get after guesses to steal a card.", stealTimeInput);

        content.getChildren().addAll(setsRow, namesRow, turnTimeRow, stealTimeRow);

        // =========================
        // FOOTER (Abbrechen & Starten)
        // =========================
        Button cancel = new Button("Cancel");
        cancel.getStyleClass().add("modern-button");
        cancel.setPrefWidth(120);
        cancel.setOnAction(e -> {
            STAGE.close();
            new MultiplayerMenuView(PARENT).show();
        });

        Button startGame = new Button("Start Game");
        startGame.getStyleClass().add("primary-button");
        startGame.setPrefWidth(140);
        startGame.setOnAction(e -> {
            GameOptions options = new GameOptions();
            options.players = new Player[2];
            options.players[0] = new Player(p1Name.getText(), "/" + Database.getCurrentUser().picture);
            options.players[1] = new Player(p2Name.getText(), "/" + Database.getCurrentUser().picture);
            options.moveTime = Integer.parseInt(turnTimeInput.getText());
            options.stealTime = Integer.parseInt(stealTimeInput.getText());

            STAGE.close();

            GameLogic game = new GameLogic(options);
            PARENT.setStage(game.getView(), true);
        });

        HBox footer = new HBox(10, cancel, startGame);
        footer.setAlignment(Pos.BOTTOM_RIGHT);
        footer.setPadding(new Insets(10, 0, 0, 0));

        // =========================
        // ROOT ASSEMBLY & SCENE
        // =========================
        root.getChildren().addAll(header, content, footer);

        Scene scene = new Scene(root, 520, 450);
        ThemeManager.getInstance().registerScene(scene);
        STAGE.setScene(scene);
    }

    private HBox createSettingRow(String titleText, String descText, javafx.scene.Node control) {
        Label rowTitle = new Label(titleText);
        rowTitle.getStyleClass().add("subheader");

        Label rowDesc = new Label(descText);
        rowDesc.getStyleClass().add("description");

        VBox textContainer = new VBox(2, rowTitle, rowDesc);
        textContainer.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(10, textContainer, spacer, control);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(5, 0, 5, 0));
        row.setStyle("-fx-border-color: rgba(255,255,255,0.05); -fx-border-width: 0 0 1 0;");

        return row;
    }

    public void show() {
        STAGE.initModality(Modality.APPLICATION_MODAL);
        STAGE.showAndWait();
    }
}