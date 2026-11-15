package hitmaster.models;

public enum GameMode {
    CLASSIC("Classic"),
    ADVANCED("Advanced"),
    EXPERT("Expert"),
    TEAM("Team");

    private final String name;

    GameMode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
