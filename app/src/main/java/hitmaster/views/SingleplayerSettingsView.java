package hitmaster.views;

import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

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
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class SingleplayerSettingsView {

    private final MainMenu PARENT;
    private final Stage STAGE;

    public SingleplayerSettingsView(MainMenu parent) {
        this.PARENT = parent;

        STAGE = new Stage();
        STAGE.setTitle("Game Setup");

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

        Label description = new Label("Configure the rules before launching the singleplayer game.");
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
            setManagerView.show();
        });
        
        HBox setsRow = createSettingRow("Song Sets", "Import, export or edit your custom song packages.", manageSetsBtn);

        // 2) Turn time
        TextField turnTimeInput = new TextField();
        turnTimeInput.setTextFormatter(createNumberFormatter());
        turnTimeInput.setPromptText("Turn Time...");
        turnTimeInput.getStyleClass().add("modern-textbox");
        turnTimeInput.setPrefWidth(180);
        HBox turnTimeControl = createSecondsInput(turnTimeInput, 300);
        
        HBox turnTimeRow = createSettingRow("Turn Time", "Set time each player gets to log in their guess.", turnTimeControl);

        content.getChildren().addAll(setsRow, turnTimeRow);

        // =========================
        // FOOTER (Cancel & Start)
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
            options.players = new Player[1];
            options.players[0] = new Player(Database.getCurrentUser().username, Database.getCurrentUser().picture);
            options.players[0].role = Player.Role.HOST;
            options.moveTime = Integer.parseInt(turnTimeInput.getText());
            options.stealTime = 60;

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

        Scene scene = new Scene(root, 520, 300);
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

    public void show() {
        STAGE.initModality(Modality.APPLICATION_MODAL);
        STAGE.showAndWait();
    }
}