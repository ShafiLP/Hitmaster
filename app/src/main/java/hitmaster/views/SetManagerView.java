package hitmaster.views;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import hitmaster.models.GameSet;
import hitmaster.models.Song;
import hitmaster.services.Database;
import hitmaster.services.ThemeManager;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class SetManagerView {

    private final Stage STAGE;
    private final ScrollPane SCROLL_PANE;
    
    private boolean isGridView = true;
    private String selectedFilter = "ALL";

    public SetManagerView() {
        STAGE = new Stage();
        STAGE.setTitle("Set Manager");
        STAGE.getIcons().add(new Image(getClass().getResourceAsStream("/cardDesign.png")));

        VBox root = new VBox(15);
        root.setPadding(new Insets(15));
        root.setFillWidth(true);

        // =========================
        // HEADER
        // =========================
        Label title = new Label("Set Manager");
        title.getStyleClass().add("header");

        Label description = new Label("Enable or disable active card packages, or create your own custom sets.");
        description.getStyleClass().add("description");

        VBox header = new VBox(5, title, description);
        header.setAlignment(Pos.TOP_LEFT);

        // =========================
        // GLOBAL CONTROLS
        // =========================
        Button activateAllBtn = new Button("Activate All");
        activateAllBtn.getStyleClass().add("primary-button");
        activateAllBtn.setOnAction(e -> setAllSetsActive(true));

        Button deactivateAllBtn = new Button("Deactivate All");
        deactivateAllBtn.getStyleClass().add("tertiary-button");
        deactivateAllBtn.setOnAction(e -> setAllSetsActive(false));

        Button createSetBtn = new Button("➕ Create New Set");
        createSetBtn.getStyleClass().add("secondary-button");
        createSetBtn.setDisable(true);
        createSetBtn.setOnAction(e -> showCreateSetDialog());

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        HBox globalControls = new HBox(10, activateAllBtn, deactivateAllBtn, topSpacer, createSetBtn);
        globalControls.setAlignment(Pos.CENTER_LEFT);

        // =========================
        // FILTER BAR
        // =========================
        Label filterLabel = new Label("Filter");
        filterLabel.getStyleClass().add("subheader");

        HBox filterButtons = new HBox(8);
        filterButtons.setAlignment(Pos.CENTER_LEFT);

        ToggleGroup filterGroup = new ToggleGroup();
        filterButtons.getChildren().addAll(
            createFilterButton("All", "/icons/language/worldwide.png", "ALL", filterGroup, true),
            createFilterButton("International", "/icons/language/worldwide.png", "INT", filterGroup, false),
            createFilterButton("English (English)", "/icons/language/united-kingdom.png", "EN", filterGroup, false),
            createFilterButton("Deutsch (German)", "/icons/language/germany.png", "DE", filterGroup, false),
            createFilterButton("Nederlands (Dutch)", "/icons/language/netherlands.png", "NL", filterGroup, false),
            createFilterButton("Français (French)", "/icons/language/france.png", "FR", filterGroup, false),
            createFilterButton("Español (Spanish)", "/icons/language/spain.png", "ES", filterGroup, false),
            createFilterButton("Português (Portuguese)", "/icons/language/portugal.png", "PT", filterGroup, false),
            createFilterButton("Italiano (Italian)", "/icons/language/italy.png", "IT", filterGroup, false),
            createFilterButton("日本語 (Japanese)", "/icons/language/japan.png", "JP", filterGroup, false)
        );

        // View Toggle
        HBox viewSwitcher = createSegmentedViewSwitcher();

        Region horizontalSpacer = new Region();
        HBox.setHgrow(horizontalSpacer, Priority.ALWAYS);

        HBox filterBar = new HBox(10, filterButtons, horizontalSpacer, viewSwitcher);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        VBox filterContainer = new VBox(5);
        filterContainer.getChildren().addAll(filterLabel, filterBar);

        // =========================
        // SETS CONTENT
        // =========================
        SCROLL_PANE = new ScrollPane();
        SCROLL_PANE.setFitToWidth(true);
        VBox.setVgrow(SCROLL_PANE, Priority.ALWAYS);

        this.refreshSetList();

        // =========================
        // FOOTER
        // =========================
        Button close = new Button("Close");
        close.getStyleClass().add("primary-button");
        close.setCancelButton(true);
        close.setPrefWidth(120);
        close.setOnAction(e -> STAGE.close());

        Label songCount = new Label("Total: " + Database.getInstance().getSongCount() + " songs");
        songCount.getStyleClass().add("description");

        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);

        HBox footer = new HBox(songCount, hSpacer, close);
        footer.setAlignment(Pos.BOTTOM_CENTER);

        root.getChildren().addAll(header, filterContainer, globalControls, SCROLL_PANE, footer);

        Scene scene = new Scene(root, 850, 650);
        ThemeManager.getInstance().registerScene(scene);
        STAGE.setScene(scene);
    }

    /**
     * Creates a button for filtering game sets.
     * Button displays icon, tooltip and uses "modern-button" as style.
     * When clicked, changes selected filter to filterCode and calls method "refreshList()". 
     * @param tooltip Tooltip text displayed when hovering above button.
     * @param iconPath Resource path to button icon (e.g. "/icons/languages/germany.png").
     * @param filterCode Region/Language filter code for button (e.g. "DE" or "ES").
     * @param group ToggleGroup to add the filter button to.
     * @param isSelected Boolean if filter should be applied after creation.
     * @return New filter button as ToggleButton.
     */
    private ToggleButton createFilterButton(String tooltip, String iconPath, String filterCode, ToggleGroup group, boolean isSelected) {
        ToggleButton btn = new ToggleButton();

        btn.setTooltip(new Tooltip(tooltip));

        btn.setGraphic(new ImageView( new Image(getClass().getResourceAsStream(iconPath))) {{
            setFitHeight(25);
            setFitWidth(25);
            setSmooth(true);
        }});

        btn.setToggleGroup(group);
        btn.setSelected(isSelected);

        btn.getStyleClass().add("modern-button");

        btn.setOnAction(e -> {
            if (btn.isSelected()) {
                selectedFilter = filterCode;
                refreshSetList();
            }
        });

        return btn;
    }

    /**
     * Creates buttons to switch game set view between grid and list.
     * When button gets pressed, changes attribute "isGridView" and calls method "refreshSetList()".
     * @return View toggle buttons united as HBox.
     */
    private HBox createSegmentedViewSwitcher() {
        ToggleButton listBtn = new ToggleButton("☰ List");
        listBtn.getStyleClass().add("segmented-button-left");
        
        ToggleButton gridBtn = new ToggleButton("🔲 Grid");
        gridBtn.getStyleClass().add("segmented-button-right");
        gridBtn.setSelected(true); // Default

        ToggleGroup viewGroup = new ToggleGroup();
        listBtn.setToggleGroup(viewGroup);
        gridBtn.setToggleGroup(viewGroup);

        HBox segmentedControl = new HBox(listBtn, gridBtn);
        segmentedControl.getStyleClass().add("segmented-control");

        listBtn.setOnAction(e -> { isGridView = false; refreshSetList(); });
        gridBtn.setOnAction(e -> { isGridView = true; refreshSetList(); });

        return segmentedControl;
    }

    private void refreshSetList() {
        List<GameSet> filteredSets = getFilteredSets();

        if (isGridView) {
            FlowPane grid = new FlowPane();
            double hGap = 15;
            double vGap = 15;
            double minCardWidth = 200;

            grid.setHgap(hGap);
            grid.setVgap(vGap);
            grid.setPadding(new Insets(0));
            grid.setAlignment(Pos.TOP_LEFT);

            Runnable updateCardWidths = () -> {
                double availableWidth = SCROLL_PANE.getViewportBounds().getWidth();
                
                if (availableWidth <= 0) {
                    availableWidth = SCROLL_PANE.getWidth();
                }

                if (availableWidth > 0) {
                    int columns = (int) Math.floor((availableWidth + hGap) / (minCardWidth + hGap));
                    columns = Math.max(1, columns);

                    double calculatedWidth = Math.floor((availableWidth - (hGap * (columns - 1))) / columns);

                    for (Node node : grid.getChildren()) {
                        if (node instanceof VBox card) {
                            card.setMinWidth(calculatedWidth);
                            card.setPrefWidth(calculatedWidth);
                            card.setMaxWidth(calculatedWidth);
                        }
                    }
                }
            };

            for (GameSet set : filteredSets) {
                grid.getChildren().add(createSetCard(set));
            }

            SCROLL_PANE.widthProperty().addListener((obs, oldVal, newVal) -> Platform.runLater(updateCardWidths));
            
            SCROLL_PANE.viewportBoundsProperty().addListener((obs, oldVal, newVal) -> Platform.runLater(updateCardWidths));

            SCROLL_PANE.setContent(grid);

            Platform.runLater(updateCardWidths);
        }
        else {
            VBox list = new VBox(10);
            list.setFillWidth(true);
            list.setPadding(new Insets(0));

            for (int i = 0; i < filteredSets.size(); i++) {
                GameSet set = filteredSets.get(i);
                list.getChildren().add(createSetRow(set, i));
            }

            SCROLL_PANE.setContent(list);
        }
    }

    /**
     * Creates a grid element for a GameSet.
     * Element contains name, image, part of description, on/off toggle, button to view set and song count.
     * @param set GameSet to create grid element for.
     * @return Grid element with GameSet as VBox.
     */
    private VBox createSetCard(GameSet set) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setAlignment(Pos.TOP_CENTER);
        card.getStyleClass().add("set-card-grid");

        ImageView setImageView = setupImageView(set.getImage(), 180, 120);

        Label setName = new Label(set.name);
        setName.getStyleClass().add("header");
        setName.setWrapText(true);
        setName.setAlignment(Pos.CENTER);

        Label descriptionLabel = new Label(set.desc != null ? set.desc : "Error: Couldn't load description for set.");
        descriptionLabel.getStyleClass().add("description");
        descriptionLabel.setWrapText(true);
        descriptionLabel.setPrefHeight(35);
        descriptionLabel.setAlignment(Pos.CENTER);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Label songCountLabel = new Label("Songs: " + Database.getInstance().getSongCountForSet(set.id));
        songCountLabel.getStyleClass().add("description");
        songCountLabel.setPrefHeight(35);
        songCountLabel.setAlignment(Pos.CENTER);

        CheckBox switchToggle = new CheckBox();
        switchToggle.getStyleClass().add("switch-toggle"); 
        switchToggle.setSelected(set.isActive);
        switchToggle.setOnAction(e -> {
            boolean newState = switchToggle.isSelected();
            set.isActive = newState;
            Database.getInstance().updateSetStatus(set.id, newState);
        });

        Button viewSetBtn = new Button();
        viewSetBtn.getStyleClass().add("icon-button");
        viewSetBtn.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/icons/magnifying-glass.png"))) {{ setFitWidth(15); setFitHeight(15); setSmooth(true);}});
        viewSetBtn.setTooltip(new Tooltip("View"));
        viewSetBtn.setOnAction(e -> {
            openSetInfo(set);
        });

        Region hSpacing = new Region();
        HBox.setHgrow(hSpacing, Priority.ALWAYS);
        
        HBox actionRow = new HBox(8, songCountLabel, hSpacing, switchToggle, viewSetBtn);
        actionRow.setAlignment(Pos.CENTER);

        card.getChildren().addAll(setImageView, setName, descriptionLabel, spacer, actionRow);
        return card;
    }

    // List Layout Row Item
    private HBox createSetRow(GameSet set, int index) {
        ImageView setImageView = setupImageView(set.getImage(), 50, 50);

        Label setName = new Label(set.name);
        setName.getStyleClass().add("subheader");

        Label descriptionLabel = new Label(
            set.desc != null
                ? set.desc
                : "Error: Couldn't load description for set."
        );
        descriptionLabel.getStyleClass().add("description");

        descriptionLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(descriptionLabel, Priority.ALWAYS);

        VBox textContainer = new VBox(2, setName, descriptionLabel);
        textContainer.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textContainer, Priority.ALWAYS);

        HBox infoLeft = new HBox(15, setImageView, textContainer);
        infoLeft.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(infoLeft, Priority.ALWAYS);

        Button viewSetBtn = new Button();
        viewSetBtn.getStyleClass().add("icon-button");
        viewSetBtn.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/icons/magnifying-glass.png"))) {{
            setFitWidth(15);
            setFitHeight(15);
            setSmooth(true);
        }});
        viewSetBtn.setTooltip(new Tooltip("View"));
        viewSetBtn.setOnAction(e -> openSetInfo(set));

        // Button darf NICHT schrumpfen
        viewSetBtn.setMinWidth(Region.USE_PREF_SIZE);

        CheckBox switchToggle = new CheckBox();
        switchToggle.getStyleClass().add("switch-toggle");
        switchToggle.setSelected(set.isActive);

        switchToggle.setOnAction(e -> {
            boolean newState = switchToggle.isSelected();
            set.isActive = newState;
            Database.getInstance().updateSetStatus(set.id, newState);
        });

        switchToggle.setMinWidth(Region.USE_PREF_SIZE);

        HBox row = new HBox(
            10,
            infoLeft,
            viewSetBtn,
            switchToggle
        );

        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 15, 10, 15));

        if (index % 2 == 0)
            row.setStyle("-fx-background-color: -fx-subtile-transparent");

        return row;
    }

    private ImageView setupImageView(ImageView setImageView, double width, double height) {
        if (setImageView == null) {
            setImageView = new ImageView();
        }
        setImageView.setFitWidth(width);
        setImageView.setFitHeight(height);
        setImageView.setPreserveRatio(true);
        setImageView.setSmooth(true);
        return setImageView;
    }

    private void setAllSetsActive(boolean active) {
        for (GameSet set : getAllSets()) {
            set.isActive = active;
            Database.getInstance().updateSetStatus(set.id, active);
        }
        this.refreshSetList();
    }

    private List<GameSet> getFilteredSets() {
        // "ALL" selected:
        List<GameSet> allSets = getAllSets();
        if ("ALL".equals(selectedFilter)) { return allSets; }

        // Other:
        return allSets.stream().filter(set -> selectedFilter.equalsIgnoreCase(set.region)).collect(Collectors.toList());
    }

    private List<GameSet> getAllSets() { return Database.getInstance().getAllSets(); }

    /* ============================== */
    /* #region Viewing Sets           */
    /* ============================== */

    private void openSetInfo(GameSet set) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(STAGE);
        dialog.setTitle(set.name);

        // =========================
        // ROOT
        // =========================
        VBox root = new VBox(18);
        root.setPadding(new Insets(20));
        root.setFillWidth(true);

        // =========================
        // HEADER / SET INFO
        // =========================
        ImageView setImage = setupImageView(set.getImage(), 220, 145);

        Label nameLabel = new Label(set.name);
        nameLabel.getStyleClass().add("header");
        nameLabel.setWrapText(true);

        Label descriptionLabel = new Label(
            set.desc != null && !set.desc.isBlank()
                ? set.desc
                : "Error: Description could not be loaded."
        );
        descriptionLabel.getStyleClass().add("description");
        descriptionLabel.setWrapText(true);

        VBox setInfo = new VBox(6, nameLabel, descriptionLabel);
        setInfo.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(setInfo, Priority.ALWAYS);

        HBox header = new HBox(20, setImage, setInfo);
        header.setAlignment(Pos.CENTER_LEFT);

        // =========================
        // SONG HEADER
        // =========================
        Label songsTitle = new Label("Songs");
        songsTitle.getStyleClass().add("subheader");

        Label songCount = new Label(
            Database.getInstance().getSongCountForSet(set.id) + " Songs"
        );
        songCount.getStyleClass().add("description");

        Region titleSpacer = new Region();
        HBox.setHgrow(titleSpacer, Priority.ALWAYS);

        HBox songsHeader = new HBox(10, songsTitle, titleSpacer, songCount);
        songsHeader.setAlignment(Pos.CENTER_LEFT);

        // =========================
        // SEARCH
        // =========================
        TextField searchField = new TextField();
        searchField.setPromptText("Search songs ...");
        searchField.getStyleClass().add("modern-textbox");

        // =========================
        // SONG LIST
        // =========================
        VBox songList = new VBox(6);
        songList.setFillWidth(true);

        ScrollPane songScrollPane = new ScrollPane(songList);
        songScrollPane.setFitToWidth(true);
        songScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        songScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        songScrollPane.setPrefHeight(360);
        VBox.setVgrow(songScrollPane, Priority.ALWAYS);

        List<Song> songs = Database.getInstance().getSongsBySetId(set.id);

        // =========================
        // RENDER SONGS
        // =========================
        Runnable updateSongList = () -> {
            String search = searchField.getText()
                .trim()
                .toLowerCase();

            songList.getChildren().clear();

            List<Song> filteredSongs = songs.stream()
                .filter(song -> {
                    if (search.isEmpty()) {
                        return true;
                    }

                    String artist = song.artists != null
                        ? song.artists.getFirst().toLowerCase()
                        : "";

                    String title = song.titles != null
                        ? song.titles.getFirst().toLowerCase()
                        : "";

                    return artist.contains(search)
                        || title.contains(search);
                })
                .collect(Collectors.toList());

            if (filteredSongs.isEmpty()) {
                Label emptyLabel = new Label(
                    search.isEmpty()
                        ? "Set doesn't contain any songs."
                        : "Found no songs for search term."
                );

                emptyLabel.getStyleClass().add("description");
                emptyLabel.setMaxWidth(Double.MAX_VALUE);
                emptyLabel.setAlignment(Pos.CENTER);

                songList.getChildren().add(emptyLabel);
                return;
            }

            int index = 1;

            for (Song song : filteredSongs) {
                HBox row = createSongInfoRow(song, index++);
                songList.getChildren().add(row);
            }
        };

        searchField.textProperty().addListener(
            (obs, oldValue, newValue) -> updateSongList.run()
        );

        updateSongList.run();

        // =========================
        // CLOSE BUTTON
        // =========================
        Button closeButton = new Button("Close");
        closeButton.getStyleClass().add("primary-button");
        closeButton.setPrefWidth(110);
        closeButton.setOnAction(e -> dialog.close());

        HBox footer = new HBox(closeButton);
        footer.setAlignment(Pos.CENTER_RIGHT);

        // =========================
        // BUILD ROOT
        // =========================
        root.getChildren().addAll(
            header,
            songsHeader,
            searchField,
            songScrollPane,
            footer
        );

        // =========================
        // SCENE
        // =========================
        Scene scene = new Scene(root, 700, 650);
        ThemeManager.getInstance().registerScene(scene);

        dialog.setScene(scene);
        dialog.showAndWait();

        Platform.runLater(STAGE::requestFocus);
    }

    private HBox createSongInfoRow(Song song, int number) {
        Label numberLabel = new Label(String.valueOf(number));
        numberLabel.getStyleClass().add("description");
        numberLabel.setMinWidth(35);
        numberLabel.setAlignment(Pos.CENTER);

        Label titleLabel = new Label(song.titles.getFirst());
        titleLabel.getStyleClass().add("subheader");
        titleLabel.setWrapText(true);

        Label artistLabel = new Label(
            song.artists != null
                ? song.artists.getFirst()
                : "Unknown Artist"
        );
        artistLabel.getStyleClass().add("description");

        VBox songInfo = new VBox(2, titleLabel, artistLabel);
        songInfo.setAlignment(Pos.CENTER_LEFT);

        HBox.setHgrow(songInfo, Priority.ALWAYS);

        /*Label yearLabel = new Label(String.valueOf(song.year));
        yearLabel.getStyleClass().add("description");
        yearLabel.setMinWidth(50);
        yearLabel.setAlignment(Pos.CENTER_RIGHT);*/

        HBox row = new HBox(
            12,
            numberLabel,
            songInfo/*,
            yearLabel*/
        );

        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 12, 10, 12));

        row.setStyle(
            "-fx-background-color: rgba(255,255,255,0.035);" +
            "-fx-background-radius: 8;"
        );

        return row;
    }

    // #endregion

    private void showCreateSetDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(STAGE);
        dialog.setTitle("Create Custom Set");

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setFillWidth(true);

        TextField setNameField = new TextField();
        setNameField.setPromptText("Set Name...");
        setNameField.getStyleClass().add("modern-textbox");

        TextField setImageField = new TextField();
        setImageField.setPromptText("Image Path / URL...");
        setImageField.getStyleClass().add("modern-textbox");

        Label addSongTitle = new Label("Add Songs to Set");
        addSongTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        TextField artistField = new TextField();
        artistField.setPromptText("Artist...");
        artistField.getStyleClass().add("modern-textbox");
        HBox.setHgrow(artistField, Priority.ALWAYS);

        TextField titleField = new TextField();
        titleField.setPromptText("Song Title...");
        titleField.getStyleClass().add("modern-textbox");
        HBox.setHgrow(titleField, Priority.ALWAYS);

        TextField yearField = new TextField();
        yearField.setPromptText("Year...");
        yearField.getStyleClass().add("modern-textbox");
        yearField.setPrefWidth(70);

        TextField linkField = new TextField();
        linkField.setPromptText("Spotify Link...");
        linkField.getStyleClass().add("modern-textbox");
        HBox.setHgrow(linkField, Priority.ALWAYS);

        Button addSongBtn = new Button("Add");
        addSongBtn.getStyleClass().add("secondary-button");

        HBox songInputRow1 = new HBox(8, artistField, titleField, yearField);
        HBox songInputRow2 = new HBox(8, linkField, addSongBtn);

        TableView<Song> songTable = new TableView<>();
        songTable.getStyleClass().add("modern-table");
        songTable.setPrefHeight(180);

        TableColumn<Song, String> artistCol = new TableColumn<>("Artist");
        artistCol.setCellValueFactory(new PropertyValueFactory<>("artist")); 
        artistCol.setPrefWidth(120);

        TableColumn<Song, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        titleCol.setPrefWidth(150);

        TableColumn<Song, Integer> yearCol = new TableColumn<>("Year");
        yearCol.setCellValueFactory(new PropertyValueFactory<>("year"));
        yearCol.setPrefWidth(60);

        TableColumn<Song, String> linkCol = new TableColumn<>("Spotify Link");
        linkCol.setCellValueFactory(new PropertyValueFactory<>("spotifyLink"));
        linkCol.setPrefWidth(180);

        songTable.getColumns().addAll(artistCol, titleCol, yearCol, linkCol);

        List<Song> addedSongsList = new ArrayList<>();

        addSongBtn.setOnAction(e -> {
            String artistName = artistField.getText().trim();
            String songTitle = titleField.getText().trim();
            String yearStr = yearField.getText().trim();
            String spotLink = linkField.getText().trim();

            if (!artistName.isEmpty() && !songTitle.isEmpty() && !yearStr.isEmpty()) {
                try {
                    int year = Integer.parseInt(yearStr);
                    artistField.clear();
                    titleField.clear();
                    yearField.clear();
                    linkField.clear();
                    artistField.requestFocus();
                } catch (NumberFormatException ex) {
                    yearField.setStyle("-fx-border-color: red;");
                }
            }
        });

        yearField.setOnKeyPressed(e -> yearField.setStyle(""));

        Button saveSetBtn = new Button("Save Set");
        saveSetBtn.getStyleClass().add("primary-button");
        saveSetBtn.setPrefWidth(120);
        saveSetBtn.setOnAction(e -> {
            String setName = setNameField.getText().trim();
            String imagePath = setImageField.getText().trim();

            if (!setName.isEmpty()) {
                refreshSetList();
                dialog.close();
            } else {
                setNameField.setStyle("-fx-border-color: red;");
            }
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("error-button");
        cancelBtn.setPrefWidth(100);
        cancelBtn.setOnAction(e -> dialog.close());

        HBox dialogFooter = new HBox(10, cancelBtn, saveSetBtn);
        dialogFooter.setAlignment(Pos.BOTTOM_RIGHT);

        root.getChildren().addAll(
            new Label("Set Details"), setNameField, setImageField, 
            new Region(), addSongTitle, songInputRow1, songInputRow2, songTable, 
            dialogFooter
        );

        Scene scene = new Scene(root, 550, 580);
        ThemeManager.getInstance().registerScene(scene);
        dialog.setScene(scene);
        dialog.showAndWait();

        Platform.runLater(STAGE::requestFocus);
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
