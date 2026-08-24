package hitmaster.design;

import hitmaster.models.Player;
import hitmaster.services.Database;
import hitmaster.services.ThemeManager;
import hitmaster.views.GameView;
import hitmaster.views.MainMenu;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class WinnerPane {
    
    public static void winnerDialog(GameView parent, Player winner) {
        Stage stage = new Stage();
        stage.setTitle("Game End");
        stage.initModality(Modality.APPLICATION_MODAL);

        VBox root = new VBox(25);
        root.setPadding(new Insets(20));
        root.setFillWidth(true);
        root.setAlignment(Pos.CENTER);

        // Winner profile picture
        StackPane avatarContainer = new StackPane();
        avatarContainer.setMinSize(60, 60);
        avatarContainer.setPrefSize(60, 60);
        avatarContainer.setMaxSize(60, 60);

        ImageView avatarView = new ImageView();
        avatarView.setFitWidth(60);
        avatarView.setFitHeight(60);
        avatarView.setPreserveRatio(true);

        Label avatarLetterLabel = new Label();
        avatarLetterLabel.setStyle("""
            "-fx-font-size: 24px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: white;"
        """); 

        Rectangle avatarClip = new Rectangle(60, 60);
        avatarClip.setArcWidth(60);
        avatarClip.setArcHeight(60);
        avatarView.setClip(avatarClip);
        Circle avatarCircle = new Circle(30);

        if (winner.img != null) {
            avatarView.setImage(winner.img);
            avatarContainer.getChildren().add(avatarView);
        }
        else {
            String initial = "?";

            if (winner.username != null && !winner.username.isBlank())
                initial = winner.username.substring(0, 1).toUpperCase();

            avatarLetterLabel.setText(initial);
            avatarCircle.setFill(PastelColor.random());

            avatarContainer.getChildren().addAll(avatarCircle, avatarLetterLabel);
        }

        // Winner message
        Label message = new Label(winner.username + " won the game!");
        message.getStyleClass().add("subheader");

        // Close button
        Button closeButton = new Button("Back to Main Menu");
        closeButton.getStyleClass().add("primary-button");
        closeButton.setOnAction(e -> {
            Stage currentStage = (Stage) parent.getScene().getWindow();
            currentStage.close();

            MainMenu menu = new MainMenu(stage, Database.getInstance().getCurrentUser());

            Scene scene = new Scene(menu.getView(), 600, 450);
            menu.getView().prefWidthProperty().bind(scene.widthProperty());

            ThemeManager.getInstance().registerScene(scene);

            stage.setTitle("Hitmaster");
            stage.setScene(scene);
            stage.show();
        });

        root.getChildren().addAll(avatarContainer, message, closeButton);

        Scene scene = new Scene(root, 400, 200);
        ThemeManager.getInstance().registerScene(scene);
        
        stage.setScene(scene);
        stage.showAndWait();
    }
}
