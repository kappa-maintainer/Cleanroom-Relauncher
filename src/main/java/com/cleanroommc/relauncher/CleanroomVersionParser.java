package com.cleanroommc.relauncher;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;

public class CleanroomVersionParser {
    private static String version = "";
    public static String getVersion() throws IOException {
        if (!CleanroomVersionParser.version.isEmpty()) {
            return CleanroomVersionParser.version;
        }
        Relauncher.LOGGER.info("Parsing latest Cleanroom versions");
        Initializer.getMainStatusLabel().setText("Parsing latest Cleanroom versions");
        File metadata = new File(Relauncher.workingDir, "maven-metadata.xml");
        String version = "0.3.0-alpha";

        Relauncher.LOGGER.info("Downloading metadata");
        Downloader.downloadUntilSucceed(new URL("https://maven.arcseekers.com/releases/com/cleanroommc/cleanroom/maven-metadata.xml"), "", metadata);
        BufferedReader reader = Files.newBufferedReader(metadata.toPath());
        while (reader.ready()) {
            String temp;
            String line = reader.readLine();;
            if (line.contains("<version>")) {
                temp = line.substring(line.indexOf("<version>") + 9, line.indexOf("</version>"));
                if (!temp.contains("build")) {
                    version = temp;
                }
            }
        }
        Relauncher.LOGGER.info("Found version {}", version);
        Initializer.getMainStatusLabel().setText("Found version " + version);
        reader.close();
        CleanroomVersionParser.version = version;
        return version;
    }
}
