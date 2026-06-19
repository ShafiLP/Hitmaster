package hitmaster.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import hitmaster.services.Log;
import javafx.scene.image.Image;

public class Player implements Serializable {
    public String username;
    public Image img;
    public List<Song> songs = new ArrayList<>();
    public int hitmasterPoints = 0;
    public boolean turn = false;
    public Role role = Role.CLIENT;

    public enum Role {
        HOST,
        CLIENT
    }

    public Player() { }

    public Player(String username, String imgPath) {
        this.username = username;

        if (imgPath != null) {
            try {
                this.img = new Image(getClass().getResourceAsStream(imgPath));
            } 
            catch (Exception e) {
                this.img = null;
                Log.Warning("Failed loading profile picture of Player \"" + username + "\": ");
            }
        }
    }

    public void increaseHitmasterPoints() {
        if (hitmasterPoints < 3)
            hitmasterPoints++;
    }
}
