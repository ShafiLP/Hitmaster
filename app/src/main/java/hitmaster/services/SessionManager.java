package hitmaster.services;

import java.util.UUID;

public class SessionManager {

    private String sessionId;

    private static SessionManager instance;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }
    
    public String getSessionId() {
        if (sessionId == null || sessionId.isEmpty() || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }
        
        return sessionId;
    }
}
