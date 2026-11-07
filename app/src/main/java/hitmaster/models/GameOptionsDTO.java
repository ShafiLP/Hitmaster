package hitmaster.models;

import java.io.Serializable;
import java.util.List;

public class GameOptionsDTO implements Serializable {

    public Player[] players;
    public int turnTime;
    public int stealTime;

    public List<GameSet> activeSets;

    public String purpose;

    public GameOptionsDTO(GameOptions options, String purpose) {
        this.players = options.players;
        this.turnTime = options.turnTime;
        this.stealTime = options.stealTime;

        this.purpose = purpose;
    }

    public GameOptionsDTO(GameOptions options, List<GameSet> activeSets, String purpose) {
        this.players = options.players;
        this.turnTime = options.turnTime;
        this.stealTime = options.stealTime;
        this.activeSets = activeSets;

        this.purpose = purpose;
    }

    public GameOptions getGameOptions() {
        GameOptions options = new GameOptions();

        options.players = this.players;
        options.turnTime = this.turnTime;
        options.stealTime = this.stealTime;

        return options;
    }
}
