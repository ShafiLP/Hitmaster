package hitmaster.views;

import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import hitmaster.GameLogic;
import hitmaster.models.GameOptions;
import hitmaster.models.Player;
import hitmaster.services.Database;
import hitmaster.services.ThemeManager;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
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
        // SETTINGS CONTENT
        // =========================
        VBox content = new VBox(15);
        content.setFillWidth(true);

        // 1) Configure Sets
        Button manageSetsBtn = new Button("Manage Sets");
        manageSetsBtn.getStyleClass().add("modern-button");
        manageSetsBtn.setPrefWidth(180);
        manageSetsBtn.setOnAction(e -> {
            SetManagerView setManagerView = new SetManagerView();
            setManagerView.show(STAGE);
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
        turnTimeInput.setTextFormatter(createNumberFormatter());
        turnTimeInput.setPromptText("Turn Time...");
        turnTimeInput.getStyleClass().add("modern-textbox");
        turnTimeInput.setPrefWidth(180);
        HBox turnTimeControl = createSecondsInput(turnTimeInput, 300);
        
        HBox turnTimeRow = createSettingRow("Turn Time", "Set time each player gets to log in their guess.", turnTimeControl);

        // 4) Steal time input
        TextField stealTimeInput = new TextField();
        stealTimeInput.setTextFormatter(createNumberFormatter());
        stealTimeInput.setPromptText("Steal Time...");
        stealTimeInput.getStyleClass().add("modern-textbox");
        stealTimeInput.setPrefWidth(180);
        HBox stealTimeControl = createSecondsInput(stealTimeInput, 30);
        
        HBox stealTimeRow = createSettingRow("Steal Time", "Time players get after guesses to steal a card.", stealTimeControl);

        content.getChildren().addAll(setsRow, namesRow, turnTimeRow, stealTimeRow);

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

        Button startGame = new Button("Start Game");
        startGame.getStyleClass().add("primary-button");
        startGame.setPrefWidth(140);
        startGame.setOnAction(e -> {
            GameOptions options = new GameOptions();
            options.players = new Player[2];
            options.players[0] = new Player(p1Name.getText(), Database.getCurrentUser().picture);
            options.players[0].role = Player.Role.HOST;
            options.players[1] = new Player(p2Name.getText(), null);
            options.players[1].role = Player.Role.HOST;
            options.moveTime = Integer.parseInt(turnTimeInput.getText());
            options.stealTime = Integer.parseInt(stealTimeInput.getText());

            STAGE.close();

            GameLogic game = new GameLogic(options, true, null);
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

        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                startGame.fire();
                event.consume(); 
            }
        });

        STAGE.setScene(scene);

        Platform.runLater(STAGE::requestFocus);
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

    private HBox createSecondsInput(TextField field, int defaultValue) {
        field.getStyleClass().add("modern-textbox");
        field.setPrefWidth(80);
        field.setText(String.valueOf(defaultValue));

        Label unit = new Label("s");
        unit.getStyleClass().add("description");

        HBox box = new HBox(5, field, unit);
        box.setAlignment(Pos.CENTER_LEFT);

        return box;
    }

    private TextFormatter<String> createNumberFormatter() {

        Pattern pattern = Pattern.compile("\\d{0,3}"); // max 3 digits

        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();

            if (pattern.matcher(newText).matches()) {
                return change;
            }
            return null;
        };

        return new TextFormatter<>(filter);
    }

    public void show(Stage parent) {
        STAGE.setOnShowing(e -> {
            Platform.runLater(() -> {
                double ownerX = parent.getX();
                double ownerY = parent.getY();
                double ownerWidth = parent.getWidth();
                double ownerHeight = parent.getHeight();

                double newWidth = STAGE.getWidth();
                double newHeight = STAGE.getHeight();

                double centerX = ownerX + (ownerWidth / 2.0) - (newWidth / 2.0);
                double centerY = ownerY + (ownerHeight / 2.0) - (newHeight / 2.0);

                STAGE.setX(centerX);
                STAGE.setY(centerY);
            });
        });

        STAGE.initModality(Modality.APPLICATION_MODAL);
        STAGE.showAndWait();
    }
}