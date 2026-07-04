package hitmaster.models;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import hitmaster.design.PastelColor;
import hitmaster.services.Log;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class Player implements Serializable {

    public String username;
    public transient Image img;
    public byte[] imgData;
    public List<Song> songs = new ArrayList<>();
    public int hitmasterPoints = 0;
    public boolean turn = false;
    public Role role = Role.CLIENT;

    public String initial;
    public Color color;

    public enum Role {
        HOST,
        CLIENT
    }

    public Player() { }

    public Player(String username, String imgPath) {
        this.username = username;

        if (imgPath != null) {
            try {
                File file = new File(imgPath);

                if (file.isAbsolute() && file.exists())
                this.img = new Image(file.toURI().toString(), 0, 0, true, true, false);
            } 
            catch (Exception e) {
                this.img = null;
                Log.Warning("Failed loading profile picture of Player \"" + username + "\": ");
            }
        }

        this.imgData = Player.imageToBytes(this.img);
    }

    public StackPane getPlayerImage(double imgSize) {
        StackPane layout = new StackPane();

        if (this.img != null) {
            ImageView profileImage = new ImageView(this.img);
            profileImage.setFitWidth(imgSize);
            profileImage.setFitHeight(imgSize);
            profileImage.setPreserveRatio(true);
            profileImage.setSmooth(true);

            Circle clip = new Circle(imgSize / 2, imgSize / 2, imgSize / 2);
            profileImage.setClip(clip);

            StackPane.setAlignment(profileImage, Pos.CENTER);
            layout.getChildren().add(profileImage);
            
            return layout;
        }

        if (this.initial == null)
            initial = this.username.isBlank() ? "?" : this.username.substring(0, 1).toUpperCase();

        if (this.color == null)
            color = PastelColor.random();
        
        Circle avatarCircle = new Circle(imgSize / 2);
        avatarCircle.setFill(this.color); 

        Label avatarLetterLabel = new Label(initial);
        double fontSize = imgSize * 0.43; 
        avatarLetterLabel.setStyle(String.format("""
            -fx-font-size: %.1fpx;
            -fx-text-fill: black;
            -fx-font-weight: bold;
        """, fontSize));

        StackPane.setAlignment(avatarCircle, Pos.CENTER);
        StackPane.setAlignment(avatarLetterLabel, Pos.CENTER);

        layout.getChildren().addAll(avatarCircle, avatarLetterLabel);
        return layout;
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
