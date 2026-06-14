package hitmaster.models;

import javafx.scene.image.Image;

public class User {
    public int id;
    public String username;
    public String picture;
    public String provider;

    public Image getImage() {
        try {
            return new Image(getClass().getResourceAsStream("/" + picture));
        }
        catch (Exception e) {
            return null;
        }
    }
}
