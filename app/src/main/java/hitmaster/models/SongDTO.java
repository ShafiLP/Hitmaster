package hitmaster.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SongDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    public int id;
    public int year;
    public List<String> artists;
    public List<String> titles;
    public String purpose; 

    public SongDTO(Song song, String purpose) {
        this.id = song.id;
        this.year = song.year;
        this.artists = new ArrayList<>(song.artists);
        this.titles = new ArrayList<>(song.titles);
        this.purpose = purpose;
    }

    public Song toSong() {
        Song s = new Song();
        s.id = this.id;
        s.year = this.year;
        s.artists = this.artists;
        s.titles = this.titles;
        return s;
    }
}