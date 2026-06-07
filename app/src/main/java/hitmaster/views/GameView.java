package hitmaster.views;

import java.util.List;
import java.util.Random;

import hitmaster.design.SongCard;
import hitmaster.models.Song;
import hitmaster.services.Database;
import hitmaster.services.MusicPlayer;
import javafx.scene.layout.Pane;

public class GameView extends Pane {
    public GameView() {
        List<Song> songs = Database.getAllSongs();
        Random random = new Random();

        Song randomSong = songs.get(
            random.nextInt(songs.size())
        );

        SongCard card = new SongCard(randomSong);
        MusicPlayer.play(card.song);

        getChildren().add(card);

        card.setLayoutX(100);
        card.setLayoutY(100);
    }
}
