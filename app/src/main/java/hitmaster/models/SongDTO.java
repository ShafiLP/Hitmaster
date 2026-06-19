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

    public List<SongDTO> songList;

    public SongDTO(Song song, String purpose) {
        this.id = song.id;
        this.year = song.year;

        if (song.artists != null) {
            this.artists = new ArrayList<>(song.artists);
        }
        else {
            this.artists = new ArrayList<>();
        }

        if (song.titles != null) {
            this.titles = new ArrayList<>(song.titles);
        }
        else {
            this.titles = new ArrayList<>();
        }

        this.purpose = purpose;
    }

    public SongDTO(List<Song> songs, String purpose) {
        this.purpose = purpose;
        this.songList = new ArrayList<>();
        for (Song s : songs) {
            this.songList.add(new SongDTO(s, purpose));
        }
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