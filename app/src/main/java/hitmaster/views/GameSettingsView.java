package hitmaster.views;

import java.util.List;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import hitmaster.GameLogic;
import hitmaster.design.StyleDialog;
import hitmaster.design.ToggleSwitch;
import hitmaster.design.UiBuilder;
import hitmaster.models.GameMode;
import hitmaster.models.GameOptions;
import hitmaster.models.GameSet;
import hitmaster.models.Player;
import hitmaster.services.Database;
import hitmaster.services.Navigation;
import hitmaster.services.SvgParser;
import hitmaster.services.ThemeManager;
import hitmaster.views.templates.HitmasterDialog;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class GameSettingsView extends HitmasterDialog {

    private boolean multiplayerSettings = false;

    // ----- Inputs -----
    private final TextField cardsAmountTextField;
    private final TextField chipsAmountTextField;
    private final TextField chipsStartTextField;
    private final TextField turnTimeTextField;
    private final TextField stealTimeTextField;
    private final TextField bufferTimeTextField;

    private final ToggleGroup gameModeButtonsToggleGroup;
    private final ToggleSwitch enableChipsToggleSwitch;
    private final ToggleSwitch chipsSkipActionToggleSwitch;
    private final ToggleSwitch chipsStealActionToggleSwitch;
    private final ToggleSwitch chipsClaimActionToggleSwitch;
    
    public GameSettingsView() {
        this(false);
    }

    public GameSettingsView(boolean multiplayer) {
        super("Game Setup");
        this.multiplayerSettings = multiplayer;

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

        Label gameModeLabel = new Label("Game Mode");
        gameModeLabel.getStyleClass().add("subheader");

        HBox gameModeButtonsBox = new HBox(5);
        gameModeButtonsBox.setAlignment(Pos.CENTER_LEFT);

        gameModeButtonsToggleGroup = new ToggleGroup();

        for (GameMode gameMode : GameMode.values()) {
            if (gameMode == GameMode.TEAM) continue;

            ToggleButton button = new ToggleButton(gameMode.getName());

            button.setToggleGroup(gameModeButtonsToggleGroup);
            button.setUserData(gameMode);

            gameModeButtonsBox.getChildren().add(button);

            if (gameMode == GameMode.CLASSIC) button.setSelected(true);
        }

        gameModeBox.getChildren().addAll(gameModeLabel, gameModeButtonsBox);
        gameOptionsBox.getChildren().add(gameModeBox);


        // Turn Time --------------------
        turnTimeTextField = new TextField();
        turnTimeTextField.setTextFormatter(createNumberFormatter());

        HBox turnTimeBox = createSettingRow(
            "Turn Time",
            "Duration each players gets to submit their move.",
            createTimeInputRow(turnTimeTextField, "s", 300)
        );
        gameOptionsBox.getChildren().add(turnTimeBox);


        // Steal Time --------------------
        stealTimeTextField = new TextField();
        stealTimeTextField.setTextFormatter(createNumberFormatter());

        HBox stealTimeBox = createSettingRow(
            "Steal Time",
            "Duration a player gets to steal their opponents card, when performing 'steal' action.",
            createTimeInputRow(stealTimeTextField, "s", 30)
        );
        if (multiplayerSettings) gameOptionsBox.getChildren().add(stealTimeBox);


        // Buffer Time --------------------
        bufferTimeTextField = new TextField();
        bufferTimeTextField.setTextFormatter(createNumberFormatter());

        HBox bufferTimeBox = createSettingRow(
            "Buffer Time",
            "Time waited after confirming guess until card gets revealed.",
            createTimeInputRow(bufferTimeTextField, "s", 5)
        );
        if (multiplayerSettings) gameOptionsBox.getChildren().add(bufferTimeBox);


        // Amount of Cards --------------------
        cardsAmountTextField = new TextField("10");
        cardsAmountTextField.setTextFormatter(createNumberFormatter());

        HBox cardsBox = createSettingRow(
            "Cards",
            "Amount of cards that are required for a player to win the game.",
            cardsAmountTextField
        );
        gameOptionsBox.getChildren().add(cardsBox);


        // Chips --------------------
        VBox chipsBox = new VBox(5);

        // Enable
        enableChipsToggleSwitch = new ToggleSwitch();
        enableChipsToggleSwitch.setSelected(true);

        HBox chipsEnableBox = createSettingRow(
            "Chips",
            "Chips can be used to perform different actions.",
            enableChipsToggleSwitch
        );

        // Actions
        chipsSkipActionToggleSwitch = new ToggleSwitch();
        chipsSkipActionToggleSwitch.setSelected(true);
        chipsStealActionToggleSwitch = new ToggleSwitch();
        chipsStealActionToggleSwitch.setSelected(true);
        chipsClaimActionToggleSwitch = new ToggleSwitch();
        chipsClaimActionToggleSwitch.setSelected(true);

        chipsStealActionToggleSwitch.setOnAction(e -> {
            boolean enabled = chipsStealActionToggleSwitch.isSelected();
            stealTimeTextField.setDisable(!enabled);
            bufferTimeTextField.setDisable(!enabled);
        });

        VBox chipsActionsToggleSwitchesBox = new VBox(5);
        chipsActionsToggleSwitchesBox.getChildren().add(createActionToggleRow("Skip", chipsSkipActionToggleSwitch));
        if (multiplayerSettings) chipsActionsToggleSwitchesBox.getChildren().add(createActionToggleRow("Steal", chipsStealActionToggleSwitch));
        chipsActionsToggleSwitchesBox.getChildren().add(createActionToggleRow("Claim", chipsClaimActionToggleSwitch));


        Label actionsTitle = new Label("Actions");
        actionsTitle.getStyleClass().add("subheader");

        Label actionsDescription = new Label("Actions player can perform with their chips.");
        actionsDescription.getStyleClass().add("description");

        VBox chipsActionsBox = new VBox(
            5,
            actionsTitle,
            actionsDescription,
            chipsActionsToggleSwitchesBox
        );
        chipsActionsBox.setAlignment(Pos.CENTER_LEFT);

        // Amount
        chipsAmountTextField = new TextField("5");
        chipsAmountTextField.setTextFormatter(createNumberFormatter());

        HBox chipsAmountBox = createSettingRow(
            "Amount",
            "Amount of chips players can keep to perform different actions.",
            chipsAmountTextField
        );

        // Start Chips
        chipsStartTextField = new TextField("2");
        chipsStartTextField.setTextFormatter(createNumberFormatter());

        HBox chipsStartBox = createSettingRow(
            "Start Chips",
            "Amound of chips players starts with.",
            chipsStartTextField
        );

        // Deactivate settings when disabled
        enableChipsToggleSwitch.setOnAction(e -> {
            boolean enabled = enableChipsToggleSwitch.isSelected();

            chipsAmountTextField.setDisable(!enabled);
            chipsStartTextField.setDisable(!enabled);

            chipsSkipActionToggleSwitch.setDisable(!enabled);
            chipsClaimActionToggleSwitch.setDisable(!enabled);
            chipsStealActionToggleSwitch.setDisable(!enabled);

            if (multiplayerSettings && chipsStealActionToggleSwitch.isSelected()) {
                stealTimeTextField.setDisable(!enabled);
                bufferTimeTextField.setDisable(!enabled);
            }
        });

        chipsBox.getChildren().addAll(UiBuilder.getHorizontalSeparator(), chipsEnableBox, chipsAmountBox, chipsStartBox, chipsActionsBox);
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

        ScrollPane activeSetsScrollPane = new ScrollPane(activeSetsBox);

        activeSetsScrollPane.setFitToHeight(false);
        activeSetsScrollPane.setFitToWidth(false);

        activeSetsScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        activeSetsScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        activeSetsScrollPane.setPannable(false);

        activeSetsScrollPane.setMinViewportHeight(210);
        activeSetsScrollPane.setPrefViewportHeight(210);
        activeSetsScrollPane.setPrefHeight(220);

        activeSetsBox.setAlignment(Pos.CENTER_LEFT);
        activeSetsBox.setPrefHeight(210);

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

        setManagerBox.getChildren().addAll(activeSetsLabel, activeSetsScrollPane, manageSetsButton);
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

        Button startButton = new Button(multiplayerSettings ? "Local Game" : "Start Game");
        startButton.getStyleClass().add("primary-button");
        startButton.setPrefWidth(140);

        startButton.setOnAction(e -> {
            // ----- Local Multiplayer -----
            if (multiplayerSettings) {
                showCreateLocalLobbyDialog();
            }
            // ----- Singleplayer -----
            else {
                GameOptions options = buildGameOptions();
                GameLogic game = new GameLogic(options, true, null);
                Navigation.getInstance().show(game.getView());
                Navigation.getInstance().maximize();
            }
        });

        Button createLobbyButton = new Button("Host Lobby");
        createLobbyButton.getStyleClass().add("primary-button");

        createLobbyButton.setOnAction(e -> {
            // ----- LAN Multiplayer -----
            showCreateLanLobbyDialog();
        });

        Button backButton = new Button();
        backButton.setTooltip(new Tooltip("Go Back"));
        backButton.setGraphic(SvgParser.streamToColoredSvgIcon(
            getClass().getResourceAsStream("/icons/arrow-left.svg"),
            "-primary-brush"
        ));

        backButton.setOnAction(e -> {
            if (multiplayerSettings) {
                MultiplayerMenuView menuView = new MultiplayerMenuView();
                menuView.show();
            }
            else {
                Navigation.getInstance().toMainMenu();
            }
        });

        controllsBox.getChildren().addAll(backButton, UiBuilder.getHorizontalSpacer(), startButton);
        if (multiplayerSettings) controllsBox.getChildren().add(createLobbyButton);
        bottomBox.getChildren().addAll(UiBuilder.getHorizontalSeparator(), controllsBox);
        setBottom(bottomBox);


        // ===== SHORTCUTS =====
        addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (!multiplayerSettings) {
                    startButton.fire();
                } else {
                    createLobbyButton.fire();
                }

                event.consume(); 
            }
        });
    }

    private GameOptions buildGameOptions() {
        GameOptions options = new GameOptions();
            
        options.players = new Player[multiplayerSettings ? 2 : 1];
        options.players[0] = new Player(
            Database.getInstance().getCurrentUser().username,
            Database.getInstance().getCurrentUser().picture
        );
        options.players[0].role = Player.Role.HOST;

        options.gameMode = (GameMode) gameModeButtonsToggleGroup.getSelectedToggle().getUserData();
        options.cardAmount = Integer.parseInt(cardsAmountTextField.getText());

        options.chipsEnabled = enableChipsToggleSwitch.isSelected() && (chipsSkipActionToggleSwitch.isSelected() || chipsClaimActionToggleSwitch.isSelected());
        options.chipAmount = Integer.parseInt(chipsAmountTextField.getText());
        options.startChips = Integer.parseInt(chipsStartTextField.getText());
        options.skipEnabled = chipsSkipActionToggleSwitch.isSelected();
        options.stealEnabled = multiplayerSettings ? chipsStealActionToggleSwitch.isSelected() : false;
        options.claimEnabled = chipsClaimActionToggleSwitch.isSelected();

        options.turnTime = Integer.parseInt(turnTimeTextField.getText());
        options.stealTime = Integer.parseInt(stealTimeTextField.getText());
        options.bufferTime = Integer.parseInt(bufferTimeTextField.getText());

        return options;

    }

    private void showCreateLocalLobbyDialog() {
        Stage stage = new Stage();
        stage.setTitle("Create Lobby");
        stage.initModality(Modality.APPLICATION_MODAL);

        // ===== Header & Description =====
        Label headerLabel = new Label("Start local game");
        headerLabel.getStyleClass().add("header");

        Label descriptionLabel = new Label("Enter usernames before starting the game.");
        descriptionLabel.getStyleClass().add("description");
        descriptionLabel.setWrapText(true);

        // ===== Inputs =====
        GridPane inputGrid = new GridPane();
        inputGrid.setHgap(15);
        inputGrid.setVgap(12);
        inputGrid.setPadding(new Insets(10, 0, 10, 0));

        // Player 1
        Label player1Prompt = new Label("Player 1: ");
        player1Prompt.getStyleClass().add("subheader");

        TextField player1NameInput = new TextField(Database.getInstance().getCurrentUser().username);
        player1NameInput.setPromptText("Name");
        player1NameInput.setPrefWidth(200);

        // Player 2
        Label player2Prompt = new Label("Player 2: ");
        player2Prompt.getStyleClass().add("subheader");

        TextField player2NameInput = new TextField();
        player2NameInput.setPromptText("Name");
        player2NameInput.setPrefWidth(200);

        // Add Elements to Grid
        inputGrid.add(player1Prompt, 0, 0);
        inputGrid.add(player1NameInput, 1, 0);
        inputGrid.add(player2Prompt, 0, 1);
        inputGrid.add(player2NameInput, 1, 1);

        // ===== Buttons =====
        Button startLocalGameButton = new Button("Start Game");
        startLocalGameButton.getStyleClass().add("primary-button");
        startLocalGameButton.setOnAction(e -> {
            if (!player1NameInput.getText().isEmpty() && !player1NameInput.getText().isBlank() && !player2NameInput.getText().isEmpty() && !player2NameInput.getText().isBlank()) {
                GameOptions options = buildGameOptions();

                options.players[0] = new Player(player1NameInput.getText(), Database.getInstance().getCurrentUser().picture);
                options.players[0].role = Player.Role.HOST;
                options.players[1] = new Player(player2NameInput.getText(), null);
                options.players[1].role = Player.Role.HOST;

                GameLogic game = new GameLogic(options, true, null);
                Navigation.getInstance().show(game.getView());
                Navigation.getInstance().maximize();

                stage.close();
            }
            else {
                StyleDialog.errorDialog("Enter player names", "Please enter valid names for all players before starting the game!");
            }
        });

        Button cancelButton = new Button("Cancel");
        cancelButton.getStyleClass().add("error-button");
        cancelButton.setOnAction(e -> stage.close());

        HBox buttonBox = new HBox(15);
        buttonBox.getChildren().addAll(cancelButton, startLocalGameButton);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        // ===== Build Layout =====
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.getChildren().addAll(headerLabel, descriptionLabel, inputGrid, buttonBox);

        Scene scene = new Scene(root);
        ThemeManager.getInstance().registerScene(scene);

        // Place centered to parent 
        stage.setOnShowing(e -> {
            Stage parent = Navigation.getInstance().getStage();
        
            Platform.runLater(() -> {
                double ownerX = parent.getX();
                double ownerY = parent.getY();
                double ownerWidth = parent.getWidth();
                double ownerHeight = parent.getHeight();

                double newWidth = parent.getWidth();
                double newHeight = parent.getHeight();

                double centerX = ownerX + (ownerWidth / 2.0) - (newWidth / 2.0);
                double centerY = ownerY + (ownerHeight / 2.0) - (newHeight / 2.0);

                stage.setX(centerX);
                stage.setY(centerY);
            });
        });

        stage.setScene(scene);
        stage.setResizable(false);
        stage.sizeToScene();
        stage.showAndWait();
    }

    /**
     * Opens dialog for creating a new LAN multiplayer lobby.
     * User can make input for lobby name and password.
     */
    private void showCreateLanLobbyDialog() {
        Stage stage = new Stage();
        stage.setTitle("Create Lobby");
        stage.initModality(Modality.APPLICATION_MODAL);

        // ===== Header & Description =====
        Label headerLabel = new Label("Host new lobby");
        headerLabel.getStyleClass().add("header");

        Label descriptionLabel = new Label("Set up your lobby details below.\nYou can optionally set a password.");
        descriptionLabel.getStyleClass().add("description");
        descriptionLabel.setWrapText(true);

        // ===== Inputs =====
        GridPane inputGrid = new GridPane();
        inputGrid.setHgap(15);
        inputGrid.setVgap(12);
        inputGrid.setPadding(new Insets(10, 0, 10, 0));

        // Lobby Name
        Label lobbyNamePrompt = new Label("Lobby Name: ");
        lobbyNamePrompt.getStyleClass().add("subheader");

        TextField lobbyNameInput = new TextField(Database.getInstance().getCurrentUser().username + "'s Lobby");
        lobbyNameInput.setPromptText("Lobby Name");
        lobbyNameInput.setPrefWidth(200);

        // Password
        Label passwordPrompt = new Label("Password: ");
        passwordPrompt.getStyleClass().add("subheader");

        TextField passwordInput = new TextField();
        passwordInput.setPromptText("Password");
        passwordInput.setPrefWidth(200);

        // Add Elements to Grid
        inputGrid.add(lobbyNamePrompt, 0, 0);
        inputGrid.add(lobbyNameInput, 1, 0);
        inputGrid.add(passwordPrompt, 0, 1);
        inputGrid.add(passwordInput, 1, 1);

        // ===== Buttons =====
        Button hostLobbyButton = new Button("Host");
        hostLobbyButton.getStyleClass().add("primary-button");
        hostLobbyButton.setOnAction(e -> {
            if (!lobbyNameInput.getText().isEmpty() || !lobbyNameInput.getText().isBlank()) {
                GameOptions options = buildGameOptions();

                LobbyHostView hostLobbyView = new LobbyHostView(options, lobbyNameInput.getText(), passwordInput.getText(), 5050);
                hostLobbyView.show();

                stage.close();
            }
            else {
                StyleDialog.errorDialog("Enter Lobby Name", "Please enter a valid name for your lobby before hosting!");
            }
        });

        Button cancelButton = new Button("Cancel");
        cancelButton.getStyleClass().add("error-button");
        cancelButton.setOnAction(e -> stage.close());

        HBox buttonBox = new HBox(15);
        buttonBox.getChildren().addAll(cancelButton, hostLobbyButton);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        // ===== Build Layout =====
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.getChildren().addAll(headerLabel, descriptionLabel, inputGrid, buttonBox);

        Scene scene = new Scene(root);
        ThemeManager.getInstance().registerScene(scene);

        // Place centered to parent 
        stage.setOnShowing(e -> {
            Stage parent = Navigation.getInstance().getStage();
        
            Platform.runLater(() -> {
                double ownerX = parent.getX();
                double ownerY = parent.getY();
                double ownerWidth = parent.getWidth();
                double ownerHeight = parent.getHeight();

                double newWidth = parent.getWidth();
                double newHeight = parent.getHeight();

                double centerX = ownerX + (ownerWidth / 2.0) - (newWidth / 2.0);
                double centerY = ownerY + (ownerHeight / 2.0) - (newHeight / 2.0);

                stage.setX(centerX);
                stage.setY(centerY);
            });
        });

        stage.setScene(scene);
        stage.setResizable(false);
        stage.sizeToScene();
        stage.showAndWait();
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

    private HBox createActionToggleRow(String title, ToggleSwitch toggle) {
        Label label = new Label(title);

        label.setPrefWidth(55);
        label.setMinWidth(55);

        HBox row = new HBox(8, label, toggle);
        row.setAlignment(Pos.CENTER_LEFT);

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

        card.setMinHeight(210);
        card.setPrefHeight(210);
        card.setMaxHeight(210);
        
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