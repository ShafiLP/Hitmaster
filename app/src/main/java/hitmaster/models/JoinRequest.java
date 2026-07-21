package hitmaster.models;

import java.io.Serializable;

public class JoinRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public final Player player;
    public final String password;

    public JoinRequest(Player player, String password) {
        this.player = player;
        this.password = password != null ? password : "";
    }
}
