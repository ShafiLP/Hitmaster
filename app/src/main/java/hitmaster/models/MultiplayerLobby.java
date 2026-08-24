package hitmaster.models;

public class MultiplayerLobby {
    
    public String name;
    public String appVersion;
    public String ip;

    public int playerCount;
    public int playerMax;

    public boolean hasPassword = false;

    public MultiplayerLobby() {}

    public MultiplayerLobby(String lobbyName, String ipAdress, String appVersion, int playerCount, int playerMax) {
        this.name = lobbyName;
        this.appVersion = appVersion;
        this.ip = ipAdress;

        this.playerCount = playerCount;
        this.playerMax = playerMax;
    }
}
