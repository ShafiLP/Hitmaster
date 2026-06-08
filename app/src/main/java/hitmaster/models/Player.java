package hitmaster.models;

public class Player {
    public String username = "";
    public int hitmasterPoints = 0;
    public boolean turn = false;

    public void increaseHitmasterPoints() {
        if (hitmasterPoints < 3)
            hitmasterPoints++;
    }
}
