package hitmaster.models;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import hitmaster.services.Log;
import javafx.scene.image.Image;

public class Player implements Serializable {

    public String username;
    public transient Image img;
    public byte[] imgData;
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
                this.img = new Image(getClass().getResourceAsStream("/" + imgPath));
            } 
            catch (Exception e) {
                this.img = null;
                Log.Warning("Failed loading profile picture of Player \"" + username + "\": ");
            }
        }

        this.imgData = Player.imageToBytes(this.img);
    }

    public static byte[] imageToBytes(Image image) {
        if (image == null)
            return null;

        BufferedImage bImage = javafx.embed.swing.SwingFXUtils.fromFXImage(image, null);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(bImage, "png", out);
            return out.toByteArray();
        }
        catch (IOException e) {
            Log.Error("Couldn't convert Image into bytes: " + e.getMessage());
            return null;
        }
    }

    public static Image bytesToImage(byte[] data) {
        if (data == null)
            return null;

        return new Image(new ByteArrayInputStream(data));
    }

    public void decodeImage() {
        this.img = Player.bytesToImage(this.imgData);
    }

    public void increaseHitmasterPoints() {
        if (hitmasterPoints < 3)
            hitmasterPoints++;
    }
}
