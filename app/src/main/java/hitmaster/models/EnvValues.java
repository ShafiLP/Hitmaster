package hitmaster.models;

import io.github.cdimascio.dotenv.Dotenv;

public class EnvValues {

    private static final Dotenv dotenv = Dotenv.configure()
        .ignoreIfMissing()
        .load();

    public final static String CLIENT_ID = dotenv.get("SPOTIFY_CLIENT_ID", System.getenv("SPOTIFY_CLIENT_ID"));
    public final static String CLIENT_SECRET = dotenv.get("SPOTIFY_CLIENT_SECRET", System.getenv("SPOTIFY_CLIENT_SECRET"));
}
