package hitmaster.services;

public class Log {
    private static final String GREEN  = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String RED = "\u001B[31m";
    private static final String RESET  = "\u001B[0m";

    public static void Info(String output) {
        System.out.println("[INFO]: " + output + RESET);
    }
    public static void Success(String output) {
        System.out.println(GREEN + "[SUCCESS]: " + output + RESET);
    }
    public static void Warning(String output) {
        System.out.println(YELLOW + "[WARNING]: " + output + RESET);
    }
    public static void Error(String output) {
        System.out.println(RED + "[ERROR]: " + output + RESET);
    }
}
