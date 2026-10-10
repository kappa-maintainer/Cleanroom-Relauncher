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
    public static final String BUNDLED_VERSION = "0.6.2-alpha";
    private static String version = "";
    private static CleanroomApi.Release release = null;

    public static String getVersion() throws IOException {
        if (!CleanroomVersionParser.version.isEmpty()) {
            return CleanroomVersionParser.version;
        }
        Relauncher.LOGGER.info("Querying the latest Cleanroom version");
        Initializer.setStatusText(Messages.get("status.parsing_versions"));

        CleanroomApi.Release fetched = CleanroomApi.fetch();
        String resolved = fetched.getVersion();
        Relauncher.LOGGER.info("Found version {}", resolved);
        Initializer.setStatusText(Messages.get("status.found_version", resolved));
        CleanroomVersionParser.release = fetched;
        CleanroomVersionParser.version = resolved;
        return resolved;
    }

    /**
     * MMC pack zip URL for the given version. Prefers the URL reported by the download API and
     * falls back to the predictable maven layout for versions resolved without an API response.
     */
    public static String getPackUrl(String version) {
        CleanroomApi.Release fetched = CleanroomVersionParser.release;
        if (fetched != null && fetched.getMavenMmc() != null) {
            return fetched.getMavenMmc();
        }
        return "https://repo.cleanroommc.com/releases/com/cleanroommc/cleanroom/" + version + "/cleanroom-" + version + ".zip";
    }

    public static void setVersion(String version) {
        CleanroomVersionParser.version = version;
    }

    public static void resetVersion() {
        CleanroomVersionParser.version = "";
        CleanroomVersionParser.release = null;
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
        } catch (IOException | RuntimeException e) {
            Relauncher.LOGGER.warn("Failed to read version from local MMC pack {}", path, e);
            return null;
        }
    }

    public static String readVersionFromMMCPackJson(File jsonFile) {
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
