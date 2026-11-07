package hitmaster.models;

import java.io.Serializable;

public class GameOptions implements Serializable {
    public Player[] players;

    public GameMode gameMode;
    public int cardCount;
    public int chipCount;

    public int turnTime;
    public int stealTime;
}
