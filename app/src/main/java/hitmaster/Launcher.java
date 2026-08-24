package hitmaster;

public class Launcher {
    public static void main(String[] args) {
        try {
            /*String userHome = System.getProperty("user.home");
            File logFile = new File(userHome + "/Desktop/hitmaster_error.txt");
            
            PrintStream ps = new PrintStream(logFile);
            System.setOut(ps);
            System.setErr(ps);*/
            
            Main.main(args);
            
        }
        catch (Throwable t) {
            t.printStackTrace();
        }
    }
}