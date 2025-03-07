package com.cleanroommc.relauncher;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;

public class CleanroomVersionParser {
    private static String version = "";
    public static String getVersion() {
        if (!CleanroomVersionParser.version.isEmpty()) {
            return CleanroomVersionParser.version;
        }
        Relauncher.LOGGER.info("Parsing latest Cleanroom versions");
        File metadata = new File(Relauncher.workingDir, "maven-metadata.xml");
        String version = "0.3.0-alpha";
        try{
            Relauncher.LOGGER.info("Downloading metadata");
            Downloader.downloadUntilSucceed(new URL("https://maven.arcseekers.com/releases/com/cleanroommc/cleanroom/maven-metadata.xml"), "", metadata);
            BufferedReader reader = Files.newBufferedReader(metadata.toPath());
            while (reader.ready()) {
                String line = reader.readLine();;
                if (line.contains("latest")) {
                    version = line.substring(line.indexOf("<latest>") + 8, line.indexOf("</latest>"));
                    Relauncher.LOGGER.info("Found version {}", version);
                }
            }
            reader.close();
        }catch (IOException ignored) {}
        CleanroomVersionParser.version = version;
        return version;
    }
}
