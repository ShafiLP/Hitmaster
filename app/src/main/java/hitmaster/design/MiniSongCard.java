package hitmaster.design;

import java.util.Random;

import hitmaster.models.Song;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

public class MiniSongCard extends StackPane {

    private final Color COLOR;

    public MiniSongCard(Song song, double size, Image img, Paint fill, String letter) {
        this.COLOR = PastelColor.random();

        this.setPrefSize(size, size);

        Rectangle clip = new Rectangle(size, size);
        clip.setArcWidth(16);
        clip.setArcHeight(16);
        this.setClip(clip);

        BorderPane layout = new BorderPane();
        layout.setPadding(new Insets(2));

        if (song == null || song.artists == null || song.artists.isEmpty()) {
            layout.setStyle(
                "-fx-background-color: #111111;" +
                "-fx-background-radius: 8;" +
                "-fx-border-radius: 8;" +
                "-fx-border-color: #333333;" +
                "-fx-border-width: 1.5px;"
            );

            StackPane miniAvatarContainer = new StackPane();
            double miniAvatarSize = size * 0.55;
            miniAvatarContainer.setMinSize(miniAvatarSize, miniAvatarSize);
            miniAvatarContainer.setPrefSize(miniAvatarSize, miniAvatarSize);
            miniAvatarContainer.setMaxSize(miniAvatarSize, miniAvatarSize);

            if (img != null) {
                ImageView miniAvatarView = new ImageView(img);
                miniAvatarView.setFitWidth(miniAvatarSize);
                miniAvatarView.setFitHeight(miniAvatarSize);
                miniAvatarView.setPreserveRatio(true);

                Rectangle miniClip = new Rectangle(miniAvatarSize, miniAvatarSize);
                miniClip.setArcWidth(miniAvatarSize);
                miniClip.setArcHeight(miniAvatarSize);
                miniAvatarView.setClip(miniClip);

                miniAvatarContainer.getChildren().add(miniAvatarView);
            }
            else {
                Circle miniCircle = new Circle(miniAvatarSize / 2.0);
                miniCircle.setFill(fill);

                Label miniLetterLabel = new Label(letter);
                miniLetterLabel.setStyle("-fx-font-size: " + (miniAvatarSize * 0.45) + "px; -fx-font-weight: bold; -fx-text-fill: white;");

                miniAvatarContainer.getChildren().addAll(miniCircle, miniLetterLabel);
            }

            layout.setCenter(miniAvatarContainer);
        }
        else {
            layout.setStyle(String.format(
                "-fx-background-color: rgb(%d,%d,%d);" +
                "-fx-background-radius: 8;" +
                "-fx-border-radius: 8;" +
                "-fx-border-color: black;" +
                "-fx-border-width: 1px;",
                (int)(COLOR.getRed() * 255),
                (int)(COLOR.getGreen() * 255),
                (int)(COLOR.getBlue() * 255)
            ));

            Label artistLabel = new Label(song.artists.getFirst());
            artistLabel.setStyle("-fx-font-size: " + (size * 0.09) + "px; -fx-text-fill: black;");
            artistLabel.setWrapText(false);
            StackPane top = new StackPane(artistLabel);

            Label yearLabel = new Label(String.valueOf(song.year));
            yearLabel.setStyle("-fx-font-size: " + (size * 0.25) + "px; -fx-font-weight: bold; -fx-text-fill: black;");
            
            Label titleLabel = new Label(song.titles.getFirst());
            titleLabel.setStyle("-fx-font-size: " + (size * 0.09) + "px; -fx-text-fill: black;");
            titleLabel.setWrapText(false);
            StackPane bottom = new StackPane(titleLabel);

            layout.setTop(top);
            layout.setCenter(yearLabel);
            layout.setBottom(bottom);
        }

        this.getChildren().add(layout);
    }

    private static class PastelColor {
        public static Color random() {
            Random rand = new Random();
            double r = (rand.nextInt(50) + 150) / 255.0;
            double g = (rand.nextInt(50) + 150) / 255.0;
            double b = (rand.nextInt(50) + 150) / 255.0;
            return Color.color(r, g, b);
        }
    }
}
