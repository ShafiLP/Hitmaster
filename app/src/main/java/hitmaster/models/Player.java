package hitmaster.models;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.image.Image;

public class Player {
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
        this.img = new Image(getClass().getResourceAsStream(imgPath));
    }

    public void increaseHitmasterPoints() {
        if (hitmasterPoints < 3)
            hitmasterPoints++;
    }
}
