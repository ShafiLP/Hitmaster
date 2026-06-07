package hitmaster.services;

import hitmaster.models.Song;
import se.michaelthelin.spotify.SpotifyApi;

public class MusicPlayer {

    private final SpotifyApi spotifyConnection;

    public MusicPlayer() {
        spotifyConnection = Spotify.requestSpotifyConnection();
    }

    public void play(Song song) {
        Spotify.playSpotifyLink(spotifyConnection, song.spotify);
    }

    public void pause() {
        Spotify.pauseCurrentSong(spotifyConnection);
    }

    public void restart(Song song) {
        Spotify.restartSong(spotifyConnection, song.spotify);
    }

    public void seekBackward5sec() {
        Spotify.rewindCurrentSong5sec(spotifyConnection);
    }

    public void seekForward5sec() {
        Spotify.forwardCurrentSong5sec(spotifyConnection);
    }
}
