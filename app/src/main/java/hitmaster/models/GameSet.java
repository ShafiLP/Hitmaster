package hitmaster.models;

import java.util.List;

import hitmaster.services.Database;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class GameSet {
    public int id;
    public String name;
    public String img;
    public String icon;
    public String csv;
    public boolean isActive;

    public GameSet() {}

    public GameSet(String name, String image, String icon, String csv, boolean isActive) {
        this.name = name;
        this.img = image;
        this.icon = icon;
        this.csv = csv;
        this.isActive = isActive;
    }

    public ImageView getImage() {
        return new ImageView(
            new Image(getClass().getResourceAsStream("/setImages/" + img),
            500,
            500,
            true,
            true)
        );
    }

    public ImageView getIcon() {
        return new ImageView(
            new Image(getClass().getResourceAsStream("/icons/set-icons/" + icon),
            18,
            18,
            true,
            true)
        );
    }

    public List<Song> getSongs() {
        return Database.getInstance().getSongsBySetId(id);
    }
}
