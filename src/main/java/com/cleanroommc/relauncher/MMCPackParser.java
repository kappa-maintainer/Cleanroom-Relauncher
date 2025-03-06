package com.cleanroommc.relauncher;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class MMCPackParser {
    public static String cp;
    public static void parseMMCPack() {
        String version = CleanroomVersionParser.getVersion();
        File mmcDir = new File(Relauncher.workingDir, "mmcpack");
        File universal = new File(new File(mmcDir, "libraries"), "cleanroom-" + version + "-universal.jar");
        File patches = new File(mmcDir, "patches");
        File vanillaJson = new File(patches, "net.minecraft.json");
        File moddedJson = new File(patches, "net.minecraftforge.json");
        File lwjglJson = new File(patches, "org.lwjgl3.json");
        mmcDir.mkdir();
        try {
            File universalTarget = new File(Relauncher.workingDir, universal.getName());
            Files.copy(universal.toPath(), universalTarget.toPath(), StandardCopyOption.REPLACE_EXISTING);
            cp = universalTarget.getAbsolutePath() + ":";

            JsonObject vanilla = new JsonParser().parse(IOUtils.toString(Files.newBufferedReader(vanillaJson.toPath()))).getAsJsonObject();
            JsonObject modded = new JsonParser().parse(IOUtils.toString(Files.newBufferedReader(moddedJson.toPath()))).getAsJsonObject();
            JsonObject lwjgl = new JsonParser().parse(IOUtils.toString(Files.newBufferedReader(lwjglJson.toPath()))).getAsJsonObject();
            List<Pair<String, String>> result = new ArrayList<>(parseAndAddVanillaLibraries(vanilla));
            result.addAll(parseAndAddModdedLibraries(modded));
            result.addAll(parseAndAddLwjglLibraries(lwjgl));

            StringBuilder builder = new StringBuilder();
            for (Pair<String, String> entry : result){
                String[] a = entry.getKey().split("/");
                String fileName = a[a.length - 1];
                File libFile = new File(Relauncher.workingDir, fileName);
                Relauncher.LOGGER.info("Downloading : {}", libFile.getName());
                Downloader.downloadUntilSucceed(new URL(entry.getLeft()), entry.getRight(), libFile);
                builder.append(libFile.getAbsolutePath()).append(":");
            }

            cp += builder.deleteCharAt(builder.length() - 1).toString();


        } catch (IOException e) {
            Relauncher.LOGGER.error(e);
        }
    }

    private static List<Pair<String, String>> parseAndAddModdedLibraries(JsonObject o) {
        List<Pair<String, String>> result = new ArrayList<>();
        o.getAsJsonArray("libraries").forEach(jsonElement -> {
            JsonObject libraries = jsonElement.getAsJsonObject();
            if (libraries != null) {
                JsonElement downloads = libraries.getAsJsonObject().get("downloads");
                if (downloads != null) {
                    JsonObject artifact = downloads.getAsJsonObject().get("artifact").getAsJsonObject();
                    result.add(Pair.of(artifact.get("url").getAsString(), artifact.get("sha1").getAsString()));
                }
            }
        });
        return result;
    }

    private static List<Pair<String, String>> parseAndAddVanillaLibraries(JsonObject o) {
        List<Pair<String, String>> result = new ArrayList<>();
        o.getAsJsonArray("libraries").forEach(jsonElement -> {
            JsonObject libraries = jsonElement.getAsJsonObject();
            if (libraries != null) {
                JsonElement downloads = libraries.getAsJsonObject().get("downloads");
                JsonElement name = libraries.getAsJsonObject().get("name");
                if (downloads != null) {
                    if (name != null && !name.getAsString().startsWith("ca.weblite")) {
                        JsonObject artifact = downloads.getAsJsonObject().get("artifact").getAsJsonObject();
                        result.add(Pair.of(artifact.get("url").getAsString(), artifact.get("sha1").getAsString()));
                        if (name.getAsString().startsWith("com.mojang:text2speech")) {
                            JsonElement classifiers = downloads.getAsJsonObject().get("classifiers");
                            if (classifiers != null) {
                                if ((SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_FREE_BSD) && System.getProperty("os.arch").equals("x86_64")) {
                                    JsonElement linux = classifiers.getAsJsonObject().get("natives-linux");
                                    result.add(Pair.of(linux.getAsJsonObject().get("url").getAsString(), linux.getAsJsonObject().get("sha1").getAsString()));
                                    return;
                                }
                                if (SystemUtils.IS_OS_WINDOWS) {
                                    JsonElement linux = classifiers.getAsJsonObject().get("natives-windows");
                                    result.add(Pair.of(linux.getAsJsonObject().get("url").getAsString(), linux.getAsJsonObject().get("sha1").getAsString()));
                                }
                            }
                        }
                    }
                }
            }
        });
        return result;
    }

    private static List<Pair<String, String>> parseAndAddLwjglLibraries(JsonObject o) {
        List<Pair<String, String>> result = new ArrayList<>();
        String suffix = getLwjglSuffix();
        o.getAsJsonArray("libraries").forEach(jsonElement -> {
            JsonObject libraries = jsonElement.getAsJsonObject();
            if (libraries != null) {
                JsonElement downloads = libraries.getAsJsonObject().get("downloads");
                JsonElement name = libraries.getAsJsonObject().get("name");
                if (downloads != null) {
                    if (name.getAsString().split(":").length == 4 && !name.getAsString().endsWith(suffix)) return;
                    JsonObject artifact = downloads.getAsJsonObject().get("artifact").getAsJsonObject();
                    result.add(Pair.of(artifact.get("url").getAsString(), artifact.get("sha1").getAsString()));
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
            if (System.getProperty("os.arch").contains("64")) {
                arch = "x86";
            }
        } else if (SystemUtils.IS_OS_LINUX) {
            os = "linux";
        } else if (SystemUtils.IS_OS_MAC) {
            os = "macos";
        }
        if (System.getProperty("os.arch").toLowerCase().contains("arm")) {
            arch = "arm";
            if (System.getProperty("os.arch").contains("64")) {
                arch += "64";
            } else {
                arch += "32";
            }
        }
        String suffix = "natives-" + os;
        if (!arch.isEmpty()) {
            suffix += "-" + arch;
        }
        return suffix;
    }

}
