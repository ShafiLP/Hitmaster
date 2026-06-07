package hitmaster.services;

import hitmaster.models.Song;

public class MusicPlayer {
    public static void play(Song song) {

        /*Media media = new Media();
        MediaPlayer player = new MediaPlayer(media);

        player.play();*/

        Spotify.playSpotifyLink(song.spotify);
    }
}
