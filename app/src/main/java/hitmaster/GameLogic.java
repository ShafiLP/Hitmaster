package hitmaster;

import java.util.Collections;
import java.util.List;

import hitmaster.design.SongCard;
import hitmaster.models.Song;
import hitmaster.models.User;
import hitmaster.services.Database;
import hitmaster.views.GameView;

public class GameLogic {

    private final GameView VIEW;
    private final User[] PLAYERS;

    private List<Song> songs;
    private Song currentSong;

    public GameLogic() {
        PLAYERS = new User[1];
        PLAYERS[0] = Database.getCurrentUser(); // TODO: Custom user count

        // 1) Read songs from DB and shuffle them
        songs = loadSongsFromDB();
        Collections.shuffle(songs);
        this.VIEW = new GameView(this, songs.getFirst());

        // 2) Add one song to user's card strip for starting setup
        addFirstToCardStrip();

        // 3) Add first card to stack
        addFirstToCardStack();
    }

    public List<Song> loadSongsFromDB() {
        return Database.getAllSongs();
    }

    private void addFirstToCardStrip() {
        VIEW.addToCardStrip(songs.getFirst());
        songs.removeFirst();
    }

    public void addFirstToCardStack() {
        VIEW.addToCardStack(songs.getFirst());
        currentSong = songs.getFirst();
        songs.removeFirst();
    }

    public boolean checkSongInformation(String artist, String title) {
        // TODO: Alias + add points
        return (currentSong.getArtist().name.equals(artist) && currentSong.title.equals(title));
    }

    public boolean checkSongOrder(List<SongCard> songCards) {
        // 1) Search for currentSong
        int idx = -1;
        for (int i = 0; i < songCards.size(); i++) {
            if (songCards.get(i).song.id == currentSong.id) {
                idx = i;
                break;
            }
        }
        if (idx == -1)
            return false;

        // 2) Check position
        if (songCards.get(idx).equals(songCards.getFirst()))
            return (songCards.get(idx + 1).song.year >= songCards.get(idx).song.year);

        if (songCards.get(idx).equals(songCards.getLast()))
            return (songCards.get(idx - 1).song.year <= songCards.get(idx).song.year);

        return (songCards.get(idx - 1).song.year <= songCards.get(idx).song.year && songCards.get(idx + 1).song.year >= songCards.get(idx).song.year);
    }

    public GameView getView() {
        return VIEW;
    }
}
