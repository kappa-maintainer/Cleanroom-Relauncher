package com.cleanroommc.relauncher;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.List;

public class MMCPackParser {
    private static String cp;
    private static final String osArch = System.getProperty("os.arch");
    public static void parseMMCPack() throws IOException {
        String version = CleanroomVersionParser.getVersion();
        File mmcDir = new File(Relauncher.workingDir, "mmcpack");
        File universal = new File(new File(mmcDir, "libraries"), "cleanroom-" + version + "-universal.jar");
        File patches = new File(mmcDir, "patches");
        File vanillaJson = new File(patches, "net.minecraft.json");
        File moddedJson = new File(patches, "net.minecraftforge.json");
        File lwjglJson = new File(patches, "org.lwjgl3.json");
        mmcDir.mkdir();
        File universalTarget = new File(Relauncher.workingDir, universal.getName());
        Files.copy(universal.toPath(), universalTarget.toPath(), StandardCopyOption.REPLACE_EXISTING);
        cp = universalTarget.getAbsolutePath() + File.pathSeparator;

        JsonObject vanilla = new JsonParser().parse(IOUtils.toString(Files.newBufferedReader(vanillaJson.toPath()))).getAsJsonObject();
        JsonObject modded = new JsonParser().parse(IOUtils.toString(Files.newBufferedReader(moddedJson.toPath()))).getAsJsonObject();
        JsonObject lwjgl = new JsonParser().parse(IOUtils.toString(Files.newBufferedReader(lwjglJson.toPath()))).getAsJsonObject();
        List<Triple<String, String, String>> result = new ArrayList<>(parseAndAddVanillaLibraries(vanilla));
        result.addAll(parseAndAddModdedLibraries(modded));
        result.addAll(parseAndAddLwjglLibraries(lwjgl));

        StringBuilder builder = new StringBuilder();

        Initializer.getMainProgressbar().setMaximum(result.size());
        Initializer.getMainProgressbar().setMinimum(0);

        File libDir;
        if (Config.libraryPath.isEmpty()) {
            libDir = Relauncher.workingDir;
        } else {
            libDir = new File(Config.libraryPath);
        }
        for (int i = 0; i < result.size(); i++){
            Triple<String, String, String> triple = result.get(i);
            String[] a = triple.getMiddle().split("/");
            String fileName = a[a.length - 1];
            if (Config.respectLibraryStructure) {
                String[] maven = triple.getLeft().split(":");
                fileName = maven[0].replace('.', File.separatorChar) + File.separatorChar + maven[2] + File.separatorChar + fileName;
                if (File.separatorChar == '\\') {
                    fileName = fileName.replace("\\", "\\\\"); // I hate Windows
                }
            }
            File libFile = new File(libDir, fileName);
            Relauncher.LOGGER.info("Grabbing : {}", triple.getLeft());
            Initializer.getMainProgressbar().setValue(i);
            Initializer.getMainStatusLabel().setText("Grabbing " + i + "/" + result.size() + ": " + triple.getLeft().split(":")[1]);
            Downloader.downloadUntilSucceed(new URL(triple.getMiddle()), triple.getRight(), libFile);
            builder.append(libFile.getAbsolutePath()).append(File.pathSeparator);
        }

        cp += builder.toString();

    }

    private static List<Triple<String, String, String>> parseAndAddModdedLibraries(JsonObject o) {
        List<Triple<String, String, String>> result = new ArrayList<>();
        o.getAsJsonArray("libraries").forEach(jsonElement -> {
            JsonObject libraries = jsonElement.getAsJsonObject();
            if (libraries != null) {
                JsonElement downloads = libraries.getAsJsonObject().get("downloads");
                JsonElement name = libraries.getAsJsonObject().get("name");
                if (downloads != null && name != null) {
                    JsonObject artifact = downloads.getAsJsonObject().get("artifact").getAsJsonObject();
                    result.add(Triple.of(name.getAsString(), artifact.get("url").getAsString(), artifact.get("sha1").getAsString()));
                }
            }
        });
        return result;
    }

    private static List<Triple<String, String, String>> parseAndAddVanillaLibraries(JsonObject o) {
        List<Triple<String, String, String>> result = new ArrayList<>();
        o.getAsJsonArray("libraries").forEach(jsonElement -> {
            JsonObject libraries = jsonElement.getAsJsonObject();
            if (libraries != null) {
                JsonElement downloads = libraries.getAsJsonObject().get("downloads");
                JsonElement name = libraries.getAsJsonObject().get("name");
                if (downloads != null) {
                    if (name != null && !name.getAsString().startsWith("ca.weblite")) {
                        String mavenName = name.getAsString();
                        JsonObject artifact = downloads.getAsJsonObject().get("artifact").getAsJsonObject();
                        result.add(Triple.of(mavenName, artifact.get("url").getAsString(), artifact.get("sha1").getAsString()));
                        if (mavenName.startsWith("com.mojang:text2speech")) {
                            JsonElement classifiers = downloads.getAsJsonObject().get("classifiers");
                            if (classifiers != null) {
                                if ((SystemUtils.IS_OS_LINUX) && osArch.contains("64") && !osArch.toLowerCase().contains("arm") && !osArch.toLowerCase().contains("aarch")) {
                                    JsonElement linux = classifiers.getAsJsonObject().get("natives-linux");
                                    result.add(Triple.of(mavenName, linux.getAsJsonObject().get("url").getAsString(), linux.getAsJsonObject().get("sha1").getAsString()));
                                    return;
                                }
                                if (SystemUtils.IS_OS_WINDOWS) {
                                    JsonElement linux = classifiers.getAsJsonObject().get("natives-windows");
                                    result.add(Triple.of(mavenName, linux.getAsJsonObject().get("url").getAsString(), linux.getAsJsonObject().get("sha1").getAsString()));
                                }
                            }
                        }
                    }
                }
            }
        });
        return result;
    }

    private static List<Triple<String, String, String>> parseAndAddLwjglLibraries(JsonObject o) {
        List<Triple<String, String, String>> result = new ArrayList<>();
        String suffix = getLwjglSuffix();
        o.getAsJsonArray("libraries").forEach(jsonElement -> {
            JsonObject libraries = jsonElement.getAsJsonObject();
            if (libraries != null) {
                JsonElement downloads = libraries.getAsJsonObject().get("downloads");
                JsonElement name = libraries.getAsJsonObject().get("name");
                if (downloads != null && name != null) {
                    String mavenName = name.getAsString();
                    if (mavenName.split(":").length == 4 && !mavenName.endsWith(suffix)) return;
                    JsonObject artifact = downloads.getAsJsonObject().get("artifact").getAsJsonObject();
                    result.add(Triple.of(mavenName, artifact.get("url").getAsString(), artifact.get("sha1").getAsString()));
                }
            }
        });
        return result;
    }

    private static String getLwjglSuffix() {
        String os = "";
        String arch = "";
        if (SystemUtils.IS_OS_WINDOWS) {
            os = "windows";
            if (!osArch.contains("64")) {
                arch = "x86";
            }
        } else if (SystemUtils.IS_OS_LINUX) {
            os = "linux";
        } else if (SystemUtils.IS_OS_MAC) {
            os = "macos";
        }
        if (osArch.toLowerCase().contains("arm") || osArch.contains("aarch")) {
            arch = "arm";
            if (osArch.contains("64")) {
                arch += "64";
            } else {
                arch += "32";
            }
        }
        String suffix = "natives-" + os;
        if (!arch.isEmpty()) {
            suffix += "-" + arch;
        }
        Relauncher.LOGGER.info("LWJGL suffix: {}, os.arch: {}", suffix, osArch);
        return suffix;
    }

    public static String getClassPath() {
        return cp;
    }

}
