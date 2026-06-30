package hitmaster.services;

import hitmaster.models.Song;
import se.michaelthelin.spotify.SpotifyApi;

public class MusicPlayer {

    private final SpotifyApi spotifyConnection;
    private boolean isPlaying = false;

    public MusicPlayer() {
        spotifyConnection = Spotify.requestSpotifyConnection();
    }

    public boolean validateConnection() {
        if (spotifyConnection == null) Log.Info("No connection");
        return spotifyConnection != null;
    }

    public void playPause(Song song) {
        isPlaying = Spotify.isSongPlaying(spotifyConnection);

        if (isPlaying) {
            pause();
        }
        else {
            play(song);
        }
    }

    public void play(Song song) {
        Spotify.playSpotifyLink(spotifyConnection, song.spotify);
        isPlaying = true;
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

    public String[] getAvailableDevices() {
        return Spotify.getAllDevices(spotifyConnection);
    }

    public String getCurrentDevice() {
        return Spotify.getCurrentDevice(spotifyConnection);
    }

    public void setCurrentDevice(String device) {
        Spotify.setCurrentDevice(spotifyConnection, device);
    }

    public int getVolume() {
        return Spotify.getVolume(spotifyConnection);
    }

    public void setVolume(int volume) {
        Spotify.setVolume(spotifyConnection, volume);
    }
}
