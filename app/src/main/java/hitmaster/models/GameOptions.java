package hitmaster.models;

import java.io.Serializable;

public class GameOptions implements Serializable {
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
}
