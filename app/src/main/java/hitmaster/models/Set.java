package hitmaster.models;

import java.util.List;

import hitmaster.services.Database;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class Set {
    public int id;
    public String name;
    public String image;
    public String csv;
    public boolean isActive;

    public Set() {}

    public Set(String name, String image, String csv, boolean isActive) {
        this.name = name;
        this.image = image;
        this.csv = csv;
        this.isActive = isActive;
    }

    public ImageView getImage() {
        return new ImageView(
            new Image(getClass().getResourceAsStream("/setImages/" + image))
        );
    }

    public List<Song> getSongs() {
        return Database.getSongsBySetId(id);
    }

    public void insertIntoDatabase() {
        Database.insertCsvIntoDatabase("songs", csv);
    }
}
