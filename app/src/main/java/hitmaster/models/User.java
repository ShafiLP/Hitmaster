package hitmaster.models;

import java.io.File;

import hitmaster.services.Log;
import javafx.scene.image.Image;

public class User {
    public int id;
    public String username;
    public String picture;
    public String provider;

    public Image getImage() {
        if (picture == null || picture.isEmpty())
            return null;

        try {
            File file = new File(picture);

            if (file.isAbsolute() && file.exists())
                return new Image(file.toURI().toString(), 0, 0, true, true, false);
        }
        catch (Exception e) {
            Log.Error("An error occured while loading user profile picture: " + e.getMessage());
        }

        return null;
    }
}
