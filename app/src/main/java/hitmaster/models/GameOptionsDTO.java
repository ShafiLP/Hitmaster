package hitmaster.models;

import java.io.Serializable;

public class GameOptionsDTO implements Serializable {

    public Player[] players;

    public GameMode gameMode;
    public int cardAmount;

    public boolean chipsEnabled;
    public int chipAmount;
    public int startChips;
    public boolean skipEnabled;
    public boolean stealEnabled;
    public boolean claimEnabled;

    public int turnTime;
    public int stealTime;
    public int bufferTime;

    public String purpose;

    public GameOptionsDTO(GameOptions options, String purpose) {
        this.players = options.players;

        this.gameMode = options.gameMode;
        this.cardAmount = options.cardAmount;

        this.chipsEnabled = options.chipsEnabled;
        this.chipAmount = options.chipAmount;
        this.startChips = options.startChips;
        this.skipEnabled = options.skipEnabled;
        this.stealEnabled = options.stealEnabled;
        this.claimEnabled = options.claimEnabled;

        this.turnTime = options.turnTime;
        this.stealTime = options.stealTime;
        this.bufferTime = options.bufferTime;

        this.purpose = purpose;
    }

    public GameOptions getGameOptions() {
        GameOptions options = new GameOptions();

        options.players = this.players;

        options.gameMode = this.gameMode;
        options.cardAmount = this.cardAmount;

        options.chipsEnabled = this.chipsEnabled;
        options.chipAmount = this.chipAmount;
        options.startChips = this.startChips;
        options.skipEnabled = this.skipEnabled;
        options.stealEnabled = this.stealEnabled;
        options.claimEnabled = this.claimEnabled;

        options.turnTime = this.turnTime;
        options.stealTime = this.stealTime;
        options.bufferTime = this.bufferTime;

        return options;
    }
}
