package hitmaster.services;

import java.awt.HeadlessException;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import hitmaster.design.StyleDialog;

public class UpdateService {
    
    private static final String CURRENT_VERSION = UpdateService.loadVersion();
    private static final String REPO_OWNER = "ShafiLP";
    private static final String REPO_NAME = "Hitmaster";
    private static final String EXE_NAME = "HitMaster.exe";

    /**
     * Fetches latest release from GitHub releases of project.
     * If a newer version is available, asks user if new update should be installed.
     * When user accepts update dialog, calls downloadAndInstallUpdate() method.
     * If user declines updating, returns back to main code.
     */
    public static void checkForUpdates() {
        Log.Info("Current Version: " + CURRENT_VERSION);
        Log.Info("Checking for Updates...");
        String apiUrl = String.format("https://api.github.com/repos/%s/%s/releases", REPO_OWNER, REPO_NAME);

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Accept", "application/vnd.github.v3+json")
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            Log.Info("Received Response: " + response.statusCode());

            if (response.statusCode() == 200) {
                String json = response.body();

                String latestVersion = UpdateService.parseJsonValue(json, "tag_name");
                String downloadUrl = UpdateService.parseJsonValue(json, "browser_download_url");

                if (UpdateService.isNewerVersion(CURRENT_VERSION, latestVersion)) {
                    boolean dialogResult = StyleDialog.questionDialog("Update available", "A new version for HitMaster is available!\nDo you want to install it?", "Install");

                    if (dialogResult) {
                        UpdateService.downloadAndInstallUpdate(downloadUrl);
                    }
                }
            }
        }
        catch (HeadlessException | IOException | InterruptedException e) {
            Log.Error("An error occured while checking for updates: " + e.getMessage());
        }
    }

    /**
     * Compares two versions and returns boolean, if latest version is newer than current version.
     * @param current Current version of application.
     * @param latest Latest version of application.
     * @return Result if latest version is newer than current.
     */
    private static boolean isNewerVersion(String current, String latest) {
        if (current == null || latest == null || current.isEmpty() || latest.isEmpty()) {
            return false;
        }

        if (current.equals("@version@") || current.equals("unknown")) {
            Log.Info("Development build detected: Skipping update check.");
            return false;
        }

        String cClean = current.toLowerCase().replaceAll("^v", "");
        String lClean = latest.toLowerCase().replaceAll("^v", "");

        String[] cParts = cClean.split("-", 2);
        String[] lParts = lClean.split("-", 2);

        String[] cNums = cParts[0].split("\\.");
        String[] lNums = lParts[0].split("\\.");
        
        int maxLength = Math.max(cNums.length, lNums.length);
        for (int i = 0; i < maxLength; i++) {
            int cNum = i < cNums.length ? Integer.parseInt(cNums[i]) : 0;
            int lNum = i < lNums.length ? Integer.parseInt(lNums[i]) : 0;

            if (lNum > cNum) return true;
            if (cNum > lNum) return false;
        }

        boolean cHasSuffix = cParts.length > 1;
        boolean lHasSuffix = lParts.length > 1;

        if (cHasSuffix && !lHasSuffix) {
            return true; 
        }
        if (!cHasSuffix && lHasSuffix) {
            return true;
        }
        if (cHasSuffix && lHasSuffix) {
            return lParts[1].compareTo(cParts[1]) > 0;
        }

        return false;
    }

    /**
     * Downloads the version of app from given URL, deletes the current installation and installs the new downloaded version.
     * Script runs in CMD because current build gets deleted.
     * @param downloadUrl URL to app version to download and install.
     */
    private static void downloadAndInstallUpdate(String downloadUrl) {
        try {
            Path tempExe = Paths.get(System.getProperty("java.io.tmpdir"), "update_temp.exe");
            
            try (BufferedInputStream in = new BufferedInputStream(new URI(downloadUrl).toURL().openStream());
                FileOutputStream fileOutputStream = new FileOutputStream(tempExe.toFile())) {
                byte[] dataBuffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                    fileOutputStream.write(dataBuffer, 0, bytesRead);
                }
            }

            Path currentExePath = Paths.get(System.getProperty("user.dir"), EXE_NAME);

            Path batchFile = Paths.get(System.getProperty("java.io.tmpdir"), "updater.bat");
            String batchContent = String.format("""
                @echo off
                timeout /t 2 /nobreak > nul
                copy /y "%s" "%s"
                start "" "%s"
                del "%%~f0"
                """,
                tempExe.toAbsolutePath(),
                currentExePath.toAbsolutePath(),
                currentExePath.toAbsolutePath()
            );
            Files.writeString(batchFile, batchContent);

            Runtime.getRuntime().exec("cmd /c start \"\" \"" + batchFile.toAbsolutePath() + "\"");
            System.exit(0);
        }
        catch (Exception e) {
            Log.Error("An error occured while installing new update: " + e.getMessage());
        }
    }

    /**
     * Looks for a key value in a json format String and returns it.
     * @param json Json text as String.
     * @param key Key to look for in Json.
     * @return Value for key in Json as String.
     */
    private static String parseJsonValue(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\":\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    /**
     * Loads current version of app from project.proerties file.
     * @return Current app version as String.
     */
    private static String loadVersion() {
        Properties properties = new Properties();

        try (InputStream input = UpdateService.class.getClassLoader().getResourceAsStream("project.properties")) {
            if (input == null)
                return "unknown";

            properties.load(input);
            return properties.getProperty("version", "unknown");
        }
        catch (IOException e) {
            return "unknown";
        }
    }
}
