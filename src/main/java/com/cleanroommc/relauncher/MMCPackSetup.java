package com.cleanroommc.relauncher;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class MMCPackSetup {
    public static String cp;
    public static void downloadAndParse() {
        String version = VersionParser.getVersion();
        File mmcDir = new File(Relauncher.workingDir, "mmcpack");
        File universal = new File(new File(mmcDir, "libraries"), "cleanroom-" + version + "-universal.jar");
        File patches = new File(mmcDir, "patches");
        File vanillaJson = new File(patches, "net.minecraft.json");
        File moddedJson = new File(patches, "net.minecraftforge.json");
        File lwjglJson = new File(patches, "org.lwjgl3.json");
        mmcDir.mkdir();
        File pack = new File(mmcDir, "mmcpack.zip");
        try {
            Downloader.downloadUntilSucceed(new URL("https://github.com/CleanroomMC/Cleanroom/releases/download/" + version + "/Cleanroom-MMC-instance-" + version + ".zip"), "", pack);
            try (ZipFile zipFile = new ZipFile(pack)) {
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (entry.getName().equals(".packignore") || entry.getName().equals("instance.cfg")) {
                        continue;
                    }
                    File entryDestination = new File(mmcDir,  entry.getName());
                    if (entry.isDirectory()) {
                        entryDestination.mkdirs();
                    } else {
                        entryDestination.getParentFile().mkdirs();
                        try (InputStream in = zipFile.getInputStream(entry);
                             OutputStream out = Files.newOutputStream(entryDestination.toPath())) {
                            IOUtils.copy(in, out);
                        }
                    }
                }
            }
            File universalTarget = new File(Relauncher.workingDir, universal.getName());
            Files.move(universal.toPath(), universalTarget.toPath());
            cp = universalTarget.getAbsolutePath();

            JsonObject vanilla = new JsonParser().parse(IOUtils.toString(Files.newBufferedReader(vanillaJson.toPath()))).getAsJsonObject();
            JsonObject modded = new JsonParser().parse(IOUtils.toString(Files.newBufferedReader(moddedJson.toPath()))).getAsJsonObject();
            JsonObject lwjgl = new JsonParser().parse(IOUtils.toString(Files.newBufferedReader(lwjglJson.toPath()))).getAsJsonObject();
            Map<String, String> result = new TreeMap<>();
            parseAndAddLibraries(vanilla, result);
            handleVanillaRules(result);
            parseAndAddLibraries(modded, result);
            parseAndAddLibraries(lwjgl, result);
            handleLwjglRules(result);

            StringBuilder builder = new StringBuilder();
            for (Map.Entry<String, String> entry : result.entrySet()){
                String[] a = entry.getKey().split("/");
                String fileName = a[a.length - 1];
                File libFile = new File(Relauncher.workingDir, fileName);
                Downloader.downloadUntilSucceed(new URL(entry.getKey()), entry.getValue(), libFile);
                builder.append(libFile.getAbsolutePath()).append(";");
            }

            cp += builder.deleteCharAt(builder.length() - 1).toString();


        } catch (IOException e) {
            Relauncher.LOGGER.error(e);
        }
    }

    private static void parseAndAddLibraries(JsonObject o, Map<String, String> in) {
        o.getAsJsonArray("libraries").forEach(jsonElement -> {
            JsonObject artifact = jsonElement.getAsJsonObject().get("downloads").getAsJsonObject().get("artifact").getAsJsonObject();
            in.put(artifact.get("url").getAsString(), artifact.get("sha1").getAsString());
        });
    }

    private static void handleVanillaRules(Map<String, String> in) {
        for (Map.Entry<String, String> entry: in.entrySet()) {
            String[] a = entry.getKey().split("/");
            if (a[a.length - 1].startsWith("java-objc-bridge")) {
                in.remove(entry.getKey());
            }
        }
        if (SystemUtils.IS_OS_WINDOWS) {
            in.put("https://libraries.minecraft.net/com/mojang/text2speech/1.10.3/text2speech-1.10.3-natives-windows.jar", "84a4b856389cc4f485275b1f63497a95a857a443");
        } else if (SystemUtils.IS_OS_LINUX) {
            if (System.getProperty("os.arch").equals("x86_64")) {
                in.put("https://libraries.minecraft.net/com/mojang/text2speech/1.10.3/text2speech-1.10.3-natives-linux.jar", "ab7896aec3b3dd272b06194357f2d98f832c0cfc");
            }
        }
    }

    private static void handleLwjglRules(Map<String, String> in) {
        for (Map.Entry<String, String> entry: in.entrySet()) {
            String[] a  = entry.getKey().split("/");
            String fileName = a[a.length - 1];
            String suffix = fileName.substring(fileName.indexOf("natives") + 8).replace(".jar", "");
            String os, arch;
            if (suffix.contains("-")) {
                os = suffix.split("-")[0];
                arch = suffix.split("-")[1];
            } else {
                os = suffix;
                arch = "";
            }
            if (SystemUtils.IS_OS_LINUX) {
                if (!os.equals("linux")) {
                    in.remove(entry.getKey());
                } else {
                    if (System.getProperty("os.arch").equals("x86_64")) {
                        if (!arch.isEmpty()) {
                            in.remove(entry.getKey());
                        }
                    }
                }
            } else if (SystemUtils.IS_OS_MAC) {
                if (!os.equals("macos")) {
                    in.remove(entry.getKey());
                } else {
                    if (System.getProperty("os.arch").equals("x86_64")) {
                        if (!arch.isEmpty()) {
                            in.remove(entry.getKey());
                        }
                    }
                }
            } else if (SystemUtils.IS_OS_WINDOWS) {
                if (!os.equals("windows")) {
                    in.remove(entry.getKey());
                } else {
                    if (System.getProperty("os.arch").equals("x86_64")) {
                        if (!arch.isEmpty()) {
                            in.remove(entry.getKey());
                        }
                    }
                }
            }
        }
    }
}
