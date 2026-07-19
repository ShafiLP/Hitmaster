package hitmaster.models;

public class MultiplayerLobby {
    
    public String name;
    public String ip;
    public String appVersion;

    public int playerCount;
    public int playerMax;

    public MultiplayerLobby() {}

    public MultiplayerLobby(String lobbyName, String ipAdress, String appVersion, int playerCount, int playerMax) {
        this.name = lobbyName;
        this.ip = ipAdress;
        this.appVersion = appVersion;

        this.playerCount = playerCount;
        this.playerMax = playerMax;
    }
}
