package hitmaster.models;

import java.util.List;

import hitmaster.services.Database;

public class Song {
    public int id;
    public List<String> titles;
    public List<String> artists;
    public int year;
    public String spotify;

    public List<Set> getActiveSongSets() {
        return Database.getActiveSetsBySongId(id);
    }
}
