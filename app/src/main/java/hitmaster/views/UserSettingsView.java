package hitmaster.views;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import hitmaster.design.StyleDialog;
import hitmaster.models.User;
import hitmaster.services.Database;
import hitmaster.services.Log;
import hitmaster.services.ThemeManager;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class UserSettingsView {

    private final MainMenu PARENT;
    private final Stage STAGE;
    private final User user;
    
    private ImageView avatarPreview;
    private String selectedImagePath;

    public UserSettingsView(MainMenu parent) {
        this.PARENT = parent;
        this.user = Database.getCurrentUser();
        this.selectedImagePath = null; 

        STAGE = new Stage();
        STAGE.setTitle("User Profile Settings");

        // =========================
        // ROOT LAYOUT
        // =========================
        VBox root = new VBox(25);
        root.setPadding(new Insets(20));
        root.setFillWidth(true);

        // =========================
        // HEADER
        // =========================
        Label title = new Label("Profile Settings");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        Label description = new Label("Update your username and profile picture.");
        description.setStyle("-fx-text-fill: gray; -fx-font-size: 12px;");

        VBox header = new VBox(5, title, description);
        header.setAlignment(Pos.TOP_LEFT);

        // =========================
        // CONTENT (Formular)
        // =========================
        VBox content = new VBox(15);
        content.setFillWidth(true);

        // 1) Username Row
        TextField usernameField = new TextField(user.username);
        usernameField.getStyleClass().add("modern-textfield");
        usernameField.setPrefWidth(180);
        usernameField.setMaxWidth(180);

        HBox usernameRow = createSettingRow("Username", "Change your public profile display name.", usernameField);

        // 2) Profile Picture Row
        avatarPreview = new ImageView();
        avatarPreview.setFitWidth(45);
        avatarPreview.setFitHeight(45);
        avatarPreview.setPreserveRatio(true);
        
        Circle clip = new Circle(22.5, 22.5, 22.5);
        avatarPreview.setClip(clip);

        this.loadUserAvatar();

        Button uploadBtn = new Button("Upload Image");
        uploadBtn.getStyleClass().add("modern-button");
        uploadBtn.setPrefWidth(120);
        uploadBtn.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Profile Picture");
            fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
            );
            
            File selectedFile = fileChooser.showOpenDialog(STAGE);
            if (selectedFile != null) {
                try {
                    String fileUrl = selectedFile.toURI().toURL().toExternalForm();
                    avatarPreview.setImage(new Image(fileUrl));
                    selectedImagePath = selectedFile.getAbsolutePath(); 
                } catch (MalformedURLException ex) {
                    StyleDialog.warningDialog("Error", "Could not load the selected image.");
                }
            }
        });

        HBox avatarControlBox = new HBox(15, avatarPreview, uploadBtn);
        avatarControlBox.setAlignment(Pos.CENTER_RIGHT);

        HBox avatarRow = createSettingRow("Profile Picture", "Upload a new avatar for your profile.", avatarControlBox);

        content.getChildren().addAll(usernameRow, avatarRow);

        // =========================
        // FOOTER (Cancel & Save Buttons)
        // =========================
        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("modern-button");
        cancelBtn.setPrefWidth(100);
        cancelBtn.setOnAction(e -> STAGE.close());

        Button saveBtn = new Button("Save");
        saveBtn.getStyleClass().add("primary-button");
        saveBtn.setPrefWidth(100);
        saveBtn.setOnAction((ActionEvent e) -> {
            String newUsername = usernameField.getText().trim();
            
            if (newUsername.isEmpty()) {
                StyleDialog.warningDialog("Validation Error", "Username cannot be empty.");
                return;
            }

            user.username = newUsername;

            if (selectedImagePath != null) {
                try {
                    File sourceFile = new File(selectedImagePath);

                    String fileName = sourceFile.getName();
                    String extension = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();

                    File resourcesDir = new File("src/main/resources");
                    if (!resourcesDir.exists())
                        resourcesDir.mkdirs();

                    File destFile = new File(resourcesDir, "userImage" + extension);

                    Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                    user.picture = "userImage" + extension;
                }
                catch (IOException ex) {
                    Log.Warning("Failed to save profile image: " + ex.getMessage());
                    StyleDialog.warningDialog("Error", "Could not save the profile image to resources.");
                    return;
                }
            }

            Database.updateUser(user);

            PARENT.initialiseProviderButton(); 
            PARENT.updateProfileButton();
            
            STAGE.close();
        });

        HBox footer = new HBox(10, cancelBtn, saveBtn);
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

    /**
     * Loads current user avatar from Database.
     * If no user avatar is found, load debug image.
     */
    private void loadUserAvatar() {
        try {
            Image image = user.getImage();

            if (image != null) {
                avatarPreview.setImage(image);
                return;
            }
            
            // Fallback
            avatarPreview.setImage(new Image(getClass().getResourceAsStream("/setImages/debug.jpg")));
        }
        catch (Exception e) {
            avatarPreview.setImage(null); 
        }
    }

    private HBox createSettingRow(String titleText, String descText, javafx.scene.Node control) {
        Label rowTitle = new Label(titleText);
        rowTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label rowDesc = new Label(descText);
        rowDesc.setStyle("-fx-text-fill: #888888; -fx-font-size: 11px;");

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

    public void focus() {
        if (STAGE.isShowing()) {
            STAGE.toFront();
        } else {
            STAGE.show();
        }
    }
}