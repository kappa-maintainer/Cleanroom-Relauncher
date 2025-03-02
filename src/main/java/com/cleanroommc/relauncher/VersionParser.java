package com.cleanroommc.relauncher;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;

public class VersionParser {
    public static String getVersion() {
        File metadata = new File(Relauncher.workingDir, "maven-metadata.xml");
        String version = "0.3.0-alpha";
        try{
            Downloader.downloadUntilSucceed(new URL("https://maven.outlands.top/releases/com/cleanroommc/cleanroom/maven-metadata.xml"), "", metadata);


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
        return version;
    }
}
