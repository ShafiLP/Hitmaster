package hitmaster.models;

import hitmaster.services.Log;
import javafx.scene.image.Image;

public class User {
    public int id;
    public String username;
    public String picture;
    public String provider;

    public Image getImage() {
        if (picture == null)
            return null;

        try {
            var resourceUrl = getClass().getResource("/" + picture);

            if (resourceUrl != null)
                return new Image(resourceUrl.toExternalForm(), 0, 0, true, true, false);
        }
        catch (Exception e) {
            Log.Error("An error occured while loading user profile picture: " + e.getMessage());
        }

        return null;
    }
}
