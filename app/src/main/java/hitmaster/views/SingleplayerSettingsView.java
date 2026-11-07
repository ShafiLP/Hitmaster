package hitmaster.views;

import java.util.List;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import hitmaster.GameLogic;
import hitmaster.design.UiBuilder;
import hitmaster.models.GameOptions;
import hitmaster.models.GameSet;
import hitmaster.models.Player;
import hitmaster.services.Database;
import hitmaster.services.Navigation;
import hitmaster.services.SvgParser;
import hitmaster.views.templates.HitmasterDialog;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class SingleplayerSettingsView extends HitmasterDialog {

    public SingleplayerSettingsView() {
        super("Game Setup");

        // ===== HEADER =====
        VBox headerBox = new VBox(3);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label headerLabel = new Label("Game Setup");
        headerLabel.getStyleClass().add("header");

        Label descriptionLabel = new Label("Personalize your game for the best experience!");
        descriptionLabel.getStyleClass().add("description");

        headerBox.getChildren().addAll(headerLabel, descriptionLabel, UiBuilder.getHorizontalSeparator());
        setTop(headerBox);


        // ===== GAME OPTIONS =====
        VBox gameOptionsBox = new VBox(10);

        
        // Game Mode --------------------
        VBox gameModeBox = new VBox(5);

        ToggleGroup gameModeButtonsToggleGroup = new ToggleGroup();

        Label gameModeLabel = new Label("Game Mode");
        gameModeLabel.getStyleClass().add("subheader");

        HBox gameModeButtonsBox = new HBox(5);
        gameModeButtonsBox.setAlignment(Pos.CENTER_LEFT);

        // TODO: Replace placeholder
        gameModeButtonsBox.getChildren().addAll(
            new ToggleButton("Classic") {{ setToggleGroup(gameModeButtonsToggleGroup); setSelected(true); }},
            new ToggleButton("Advanced") {{ setToggleGroup(gameModeButtonsToggleGroup); }},
            new ToggleButton("Expert") {{ setToggleGroup(gameModeButtonsToggleGroup); }}
        );

        gameModeBox.getChildren().addAll(gameModeLabel, gameModeButtonsBox);
        gameOptionsBox.getChildren().add(gameModeBox);


        // Turn Time --------------------
        TextField turnTimeTextField = new TextField();
        turnTimeTextField.setTextFormatter(createNumberFormatter());

        HBox turnTimeBox = createSettingRow(
            "Turn Time",
            "Duration each players gets to submit their move.",
            createTimeInputRow(turnTimeTextField, "s", 300)
        );
        gameOptionsBox.getChildren().add(turnTimeBox);


        // Amount of Cards --------------------
        TextField cardsTextField = new TextField();
        cardsTextField.setTextFormatter(createNumberFormatter());

        HBox cardsBox = createSettingRow(
            "Cards",
            "Amount of cards that are needed for a player to win the game.",
            cardsTextField
        );
        gameOptionsBox.getChildren().add(cardsBox);


        // Amount of Chips --------------------
        TextField chipsTextField = new TextField();
        chipsTextField.setTextFormatter(createNumberFormatter());

        HBox chipsBox = createSettingRow(
            "Chips",
            "Amount of chips players can keep to perform different actions.",
            chipsTextField
        );
        gameOptionsBox.getChildren().add(chipsBox);


        // Set Manager --------------------
        VBox setManagerBox = new VBox(10);

        Label activeSetsLabel = new Label("Active Sets");
        activeSetsLabel.getStyleClass().add("subheader");

        List<GameSet> activeSets = Database.getInstance().getActiveSets();

        HBox activeSetsBox = new HBox(5);
        activeSetsBox.setAlignment(Pos.CENTER_LEFT);

        for (GameSet gameSet : activeSets) {
            activeSetsBox.getChildren().add(createSetCard(gameSet));
        }

        ScrollPane activeSetsCrollPane = new ScrollPane(activeSetsBox);
        activeSetsCrollPane.setFitToHeight(true);
        activeSetsCrollPane.setFitToWidth(false);
        activeSetsCrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        activeSetsCrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        activeSetsCrollPane.setPannable(true);

        Button manageSetsButton = new Button("Manage Sets");
        manageSetsButton.setGraphic(SvgParser.streamToColoredSvgIcon(
            getClass().getResourceAsStream("/icons/settings.svg"),
            "-primary-brush"
        ));

        manageSetsButton.setOnAction(e -> {
            SetManagerView setManagerView = new SetManagerView();
            setManagerView.setOnCloseRequest(f -> {
                List<GameSet> activeSetsReload = Database.getInstance().getActiveSets();

                activeSetsBox.getChildren().clear();
                for (GameSet gameSet : activeSetsReload) {
                    activeSetsBox.getChildren().add(createSetCard(gameSet));
                }
            });
            setManagerView.show(Navigation.getInstance().getStage());
        });

        setManagerBox.getChildren().addAll(activeSetsLabel, activeSetsCrollPane, manageSetsButton);
        gameOptionsBox.getChildren().addAll(UiBuilder.getHorizontalSeparator(), setManagerBox);
        

        // Scroll Pane --------------------
        ScrollPane centerScrollPane = new ScrollPane(gameOptionsBox);

        centerScrollPane.setFitToWidth(true);
        centerScrollPane.setFitToHeight(false);

        centerScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        centerScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        setCenter(centerScrollPane);


        // ===== BOTTOM CONTROLS =====
        VBox bottomBox = new VBox(5);
        HBox controllsBox = new HBox(10);

        Button startButton = new Button("Start Game");
        startButton.getStyleClass().add("primary-button");
        startButton.setPrefWidth(140);

        startButton.setOnAction(e -> {
            GameOptions options = new GameOptions();
            
            options.players = new Player[1];
            options.players[0] = new Player(
                Database.getInstance().getCurrentUser().username,
                Database.getInstance().getCurrentUser().picture
            );
            options.players[0].role = Player.Role.HOST;

            options.cardCount = Integer.parseInt(cardsTextField.getText());
            options.chipCount = Integer.parseInt(chipsTextField.getText());

            options.turnTime = Integer.parseInt(turnTimeTextField.getText());
            options.stealTime = 60;

            GameLogic game = new GameLogic(options, true, null);
            Navigation.getInstance().show(game.getView());
            Navigation.getInstance().maximize();
        });

        Button backButton = new Button();
        backButton.setTooltip(new Tooltip("Go Back"));
        backButton.setGraphic(SvgParser.streamToColoredSvgIcon(
            getClass().getResourceAsStream("/icons/arrow-left.svg"),
            "-primary-brush"
        ));

        backButton.setOnAction(e -> {
            Navigation.getInstance().toMainMenu();
        });

        controllsBox.getChildren().addAll(backButton, UiBuilder.getHorizontalSpacer(), startButton);
        bottomBox.getChildren().addAll(UiBuilder.getHorizontalSeparator(), controllsBox);
        setBottom(bottomBox);


        // ===== SHORTCUTS =====
        addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                startButton.fire();
                event.consume(); 
            }
        });
    }

    private HBox createSettingRow(String titleText, String descText, Node control) {
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

    private HBox createTimeInputRow(TextField field, String unit, int defaultValue) {
        field.setPrefWidth(80);
        field.setText(String.valueOf(defaultValue));

        Label unitLabel = new Label(unit);
        unitLabel.getStyleClass().add("description");

        HBox box = new HBox(5, field, unitLabel);
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

    private VBox createSetCard(GameSet set) {
        VBox card = new VBox(10);
        card.setPrefWidth(180);
        card.setMaxWidth(180);
        card.setMinWidth(180);
        
        card.getStyleClass().add("set-card-grid");
        card.setPadding(new Insets(15));
        card.setAlignment(Pos.TOP_CENTER);

        ImageView setImageView = setupImageView(set.getImage(), 180, 90);

        Label setName = new Label(set.name);
        setName.getStyleClass().add("header");
        setName.setWrapText(true);
        setName.setAlignment(Pos.CENTER_RIGHT);
        setName.setPrefHeight(40);
        setName.setMaxHeight(40);

        Label songCountLabel = new Label("Songs: " + Database.getInstance().getSongCountForSet(set.id));
        songCountLabel.getStyleClass().add("description");
        songCountLabel.setMinHeight(25);
        songCountLabel.setPrefHeight(25);
        songCountLabel.setMaxHeight(25);
        songCountLabel.setAlignment(Pos.CENTER);

        card.getChildren().addAll(setImageView, setName, songCountLabel);
        return card;
    }

    private ImageView setupImageView(ImageView setImageView, double width, double height) {
        if (setImageView == null) 
            setImageView = new ImageView();

        setImageView.setFitWidth(width);
        setImageView.setFitHeight(height);
        setImageView.setPreserveRatio(true);
        setImageView.setSmooth(true);

        return setImageView;
    }
}