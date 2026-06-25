package com.cleanroommc.relauncher;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class CleanroomVersionParser {
    public static final String BUNDLED_VERSION = "0.5.14-alpha";
    private static String version = "";

    public static String getVersion() throws IOException {
        if (!CleanroomVersionParser.version.isEmpty()) {
            return CleanroomVersionParser.version;
        }
        Relauncher.LOGGER.info("Parsing latest Cleanroom versions");
        Initializer.getMainStatusLabel().setText(Messages.get("status.parsing_versions"));
        File metadata = new File(Relauncher.workingDir, "maven-metadata.xml");
        String version = BUNDLED_VERSION;

        Relauncher.LOGGER.info("Downloading metadata");
        List<DownloadEntry> list = new ArrayList<>(1);
        list.add(new DownloadEntry(new URL("https://repo.cleanroommc.com/releases/com/cleanroommc/cleanroom/maven-metadata.xml"), metadata, ""));
        try {
            Downloader.downloadAll(list);
        } catch (Exception e) {
            Relauncher.LOGGER.error(e);
            throw new RuntimeException(e);
        }
        BufferedReader reader = Files.newBufferedReader(metadata.toPath());
        while (reader.ready()) {
            String temp;
            String line = reader.readLine();
            if (line.contains("<version>")) {
                temp = line.substring(line.indexOf("<version>") + 9, line.indexOf("</version>"));
                if (!temp.contains("build")) {
                    version = temp;
                }
            }
        }
        Relauncher.LOGGER.info("Found version {}", version);
        Initializer.getMainStatusLabel().setText(Messages.get("status.found_version", version));
        reader.close();
        CleanroomVersionParser.version = version;
        return version;
    }
    
    public static void setVersion(String version) {
        CleanroomVersionParser.version = version;
    }

    public static String readCachedVersion() {
        File mmcPackJson = new File(Relauncher.workingDir, "mmcpack/mmc-pack.json");
        if (!mmcPackJson.exists()) {
            return null;
        }
        return readVersionFromMMCPackJson(mmcPackJson);
    }

    public static String readVersionFromLocalMMCPack(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        File file = new File(path);
        if (!file.exists() || !file.isFile()) {
            return null;
        }
        try (ZipFile zip = new ZipFile(file)) {
            ZipEntry entry = zip.getEntry("mmc-pack.json");
            if (entry == null) {
                return null;
            }
            try (InputStream in = zip.getInputStream(entry)) {
                JsonObject root = new JsonParser().parse(IOUtils.toString(in, StandardCharsets.UTF_8)).getAsJsonObject();
                return readVersionFromJsonObject(root);
            }
        } catch (IOException e) {
            Relauncher.LOGGER.warn("Failed to read version from local MMC pack {}", path, e);
            return null;
        }
    }

    private static String readVersionFromMMCPackJson(File jsonFile) {
        try (FileInputStream fis = new FileInputStream(jsonFile)) {
            JsonObject root = new JsonParser().parse(IOUtils.toString(fis, StandardCharsets.UTF_8)).getAsJsonObject();
            return readVersionFromJsonObject(root);
        } catch (Exception e) {
            Relauncher.LOGGER.error("Failed to read version from mmc-pack.json", e);
            return null;
        }
    }

    private static String readVersionFromJsonObject(JsonObject root) {
        JsonArray components = root.getAsJsonArray("components");
        for (JsonElement element : components) {
            JsonObject comp = element.getAsJsonObject();
            if ("net.minecraftforge".equals(comp.get("uid").getAsString())) {
                return comp.get("version").getAsString();
            }
        }
        return null;
    }
}
