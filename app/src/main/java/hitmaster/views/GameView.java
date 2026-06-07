package hitmaster.views;

import java.util.List;
import java.util.Random;

import hitmaster.design.CardStripPane;
import hitmaster.design.SongCard;
import hitmaster.models.Song;
import hitmaster.services.Database;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;

public class GameView extends Pane {
    public GameView() {
        this.getStylesheets().add(
            getClass().getResource("/styles/app.css").toExternalForm()
        );

        CardStripPane strip = new CardStripPane();
        strip.setPrefHeight(200);
        strip.prefWidthProperty().bind(this.widthProperty());
        strip.layoutYProperty().bind(this.heightProperty().subtract(strip.prefHeightProperty()));
        strip.setLayoutX(0);
        this.getChildren().add(strip);

        List<Song> songs = Database.getAllSongs();
        Random random = new Random();

        Song randomSong = songs.get(
            random.nextInt(songs.size())
        );

        SongCard card = new SongCard(randomSong);

        strip.registerExternalCard(card);

        //! DEBUG Random songs in strip
        for (int i = 0; i < 3; i++) {
            SongCard tempCard = new SongCard(songs.get(i));
            tempCard.showFront();
            strip.addCard(tempCard);
        }

        Button flip = new Button("Flip");
        flip.getStyleClass().add("primary-button");
        flip.setOnAction(e -> {
            card.showFront();
        });

        this.getChildren().addAll(card, flip);

        card.setLayoutX(100);
        card.setLayoutY(100);
    }
}
