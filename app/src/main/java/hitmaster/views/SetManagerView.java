package hitmaster.views;

import java.util.ArrayList;
import java.util.List;

import hitmaster.models.Artist;
import hitmaster.models.Set;
import hitmaster.models.Song;
import hitmaster.services.Database;
import hitmaster.services.ThemeManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class SetManagerView {

    private final Stage STAGE;
    private final VBox SET_LIST_CONTAINER;

    public SetManagerView() {
        STAGE = new Stage();
        STAGE.setTitle("Set Manager");

        // =========================
        // ROOT LAYOUT
        // =========================
        VBox root = new VBox(15);
        root.setPadding(new Insets(15));
        root.setFillWidth(true);

        // =========================
        // HEADER
        // =========================
        Label title = new Label("Set Manager");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        Label description = new Label("Enable or disable active card packages, or create your own.");
        description.setStyle("-fx-text-fill: gray; -fx-font-size: 12px;");

        VBox header = new VBox(5, title, description);
        header.setAlignment(Pos.TOP_LEFT);

        // =========================
        // GLOBAL CONTROLS (Top Actions)
        // =========================
        Button activateAllBtn = new Button("Activate All");
        activateAllBtn.getStyleClass().add("primary-button");
        activateAllBtn.setOnAction(e -> setAllSetsActive(true));

        Button deactivateAllBtn = new Button("Deactivate All");
        deactivateAllBtn.getStyleClass().add("error-button");
        deactivateAllBtn.setOnAction(e -> setAllSetsActive(false));

        Button createSetBtn = new Button("➕ Create New Set");
        createSetBtn.getStyleClass().add("secondary-button");
        createSetBtn.setOnAction(e -> showCreateSetDialog());

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        HBox globalControls = new HBox(10, activateAllBtn, deactivateAllBtn, topSpacer, createSetBtn);
        globalControls.setAlignment(Pos.CENTER_LEFT);

        // =========================
        // SETS CONTENT (Scrollable List)
        // ========================= 
        SET_LIST_CONTAINER = new VBox(10);
        SET_LIST_CONTAINER.setFillWidth(true);

        ScrollPane scrollPane = new ScrollPane(SET_LIST_CONTAINER);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(300);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background-insets: 0; -fx-padding: 0;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        refreshSetList();

        // =========================
        // FOOTER
        // =========================
        Button close = new Button("Close");
        close.getStyleClass().add("primary-button");
        close.setPrefWidth(120);
        close.setOnAction(e -> STAGE.close());

        HBox footer = new HBox(close);
        footer.setAlignment(Pos.BOTTOM_RIGHT);

        root.getChildren().addAll(header, globalControls, scrollPane, footer);

        Scene scene = new Scene(root, 700, 500);
        ThemeManager.getInstance().registerScene(scene);
        STAGE.setScene(scene);
    }

    private void refreshSetList() {
        SET_LIST_CONTAINER.getChildren().clear();
        for (Set set : getAllSets()) {
            SET_LIST_CONTAINER.getChildren().add(createSetRow(set));
        }
    }

    private void setAllSetsActive(boolean active) {
        for (Set set : getAllSets()) {
            set.isActive = active;
        }
        refreshSetList();
    }

    private HBox createSetRow(Set set) {
        // 1) Set Image
        ImageView setImageView = set.getImage();
        if (setImageView != null) {
            setImageView.setFitWidth(50);
            setImageView.setFitHeight(50);
            setImageView.setPreserveRatio(true);
        } else {
            setImageView = new ImageView();
            setImageView.setFitWidth(50);
            setImageView.setFitHeight(50);
        }

        // 2) Text information
        Label setName = new Label(set.name);
        setName.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label songCount = new Label(set.getSongs().size() + " Songs");
        songCount.setStyle("-fx-text-fill: #888888; -fx-font-size: 11px;");

        VBox textContainer = new VBox(2, setName, songCount);
        textContainer.setAlignment(Pos.CENTER_LEFT);

        HBox infoLeft = new HBox(15, setImageView, textContainer);
        infoLeft.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 3) Dynamic activate button
        Button toggleBtn = new Button();
        updateToggleButtonState(toggleBtn, set.isActive);

        toggleBtn.setOnAction(e -> {
            boolean newState = !set.isActive;
            set.isActive = newState;
            updateToggleButtonState(toggleBtn, newState);
            Database.updateSetStatus(set.id, newState);
        });

        HBox row = new HBox(10, infoLeft, spacer, toggleBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 10, 8, 10));
        row.setStyle("-fx-border-color: rgba(255,255,255,0.05); -fx-border-width: 0 0 1 0;");

        return row;
    }

    private void updateToggleButtonState(Button btn, boolean isActive) {
        if (isActive) {
            btn.setText("Deactivate");
            btn.getStyleClass().removeAll("primary-button");
            btn.getStyleClass().add("error-button");
        } else {
            btn.setText("Activate");
            btn.getStyleClass().removeAll("error-button");
            btn.getStyleClass().add("primary-button");
            btn.setStyle("");
        }
        btn.setPrefWidth(120);
    }
    
    private List<Set> getAllSets() {
        return Database.getAllSets();
    }

    private void showCreateSetDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(STAGE);
        dialog.setTitle("Create Custom Set");

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setFillWidth(true);

        // Inputs for set
        TextField setNameField = new TextField();
        setNameField.setPromptText("Set Name...");
        setNameField.getStyleClass().add("modern-textbox");

        TextField setImageField = new TextField();
        setImageField.setPromptText("Image Path / URL...");
        setImageField.getStyleClass().add("modern-textbox");

        // Add song formular
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

        // Table view for addded songs in set
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

        // Action: Add songs to table
        addSongBtn.setOnAction(e -> {
            String artistName = artistField.getText().trim();
            String songTitle = titleField.getText().trim();
            String yearStr = yearField.getText().trim();
            String spotLink = linkField.getText().trim();


            if (!artistName.isEmpty() && !songTitle.isEmpty() && !yearStr.isEmpty()) {
                try {
                    int year = Integer.parseInt(yearStr);
                    
                    Artist artist = new Artist(); 
                    artist.name = artistName;
                    // TODO: Insert artist with id into database
                    Song newSong = new Song(); 
                    newSong.title = songTitle;
                    newSong.artist_id = artist.id;
                    newSong.year = year;
                    newSong.spotify = spotLink;
                    
                    addedSongsList.add(newSong);
                    songTable.getItems().add(newSong);

                    // Clear inputs
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

        // Save / Cancel Buttons
        Button saveSetBtn = new Button("Save Set");
        saveSetBtn.getStyleClass().add("primary-button");
        saveSetBtn.setPrefWidth(120);
        saveSetBtn.setOnAction(e -> {
            String setName = setNameField.getText().trim();
            String imagePath = setImageField.getText().trim();

            if (!setName.isEmpty()) {
                // Generate set instance
                Set newSet = new Set();
                newSet.name = setName;
                newSet.image = imagePath;
                
                // TODO: Save set
                // Database.saveCustomSet(newSet); 
                
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
    }

    public void show() {
        STAGE.initModality(Modality.APPLICATION_MODAL);
        STAGE.showAndWait();
    }
}
