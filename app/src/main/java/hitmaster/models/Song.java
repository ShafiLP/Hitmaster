package hitmaster.models;

import hitmaster.services.Database;

public class Song {
    public int id;
    public String title;
    public String alias;
    public int artist_id;
    public int year;
    public String spotify;
    public int set_id;

    public Artist getArtist() {
        return Database.getArtistById(artist_id);
    }
}
