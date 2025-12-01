package com.cleanroommc.relauncher;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.util.internal.StringUtil;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class MMCPackDownloader {
    public static void downloadAndExtract () throws IOException {
        File mmcDir = new File(Relauncher.workingDir, "mmcpack");
        File libraries = new File(mmcDir, "libraries");
        File universalJar = new File(libraries, "cleanroom-0.3.27-alpha-universal.jar");
        mmcDir.mkdir();
        File pack = new File(mmcDir, "mmcpack.zip");
        if (!pack.exists()) {
            if (!Config.useLocalPack) {
                String version = CleanroomVersionParser.getVersion();
                universalJar = new File(libraries, "cleanroom-" + version + "-universal.jar");
                Relauncher.LOGGER.info("Downloading MMC pack with version {}", version);
                List<DownloadEntry> list = new ArrayList<>(1);
                list.add(new DownloadEntry(new URL("https://repo.cleanroommc.com/releases/com/cleanroommc/cleanroom/" + version + "/cleanroom-" + version + ".zip"), pack, ""));
                Downloader.downloadAll(list);
            } else {
                if (Relauncher.workingDir.listFiles() != null) {
                    Optional<File> packfile = Arrays.stream(Relauncher.workingDir.listFiles()).filter(file -> file.getName().startsWith("Cleanroom-MMC-instance-") && file.getName().endsWith(".zip")).findFirst();
                    if (packfile.isPresent()) {
                        Relauncher.LOGGER.info("Found local pack {}", packfile.get());
                        Files.copy(packfile.get().toPath(), pack.toPath());
                    } else {
                        extractBundledZip(pack);
                    }
                } else {
                    extractBundledZip(pack);
                }
            }
        }
        Relauncher.LOGGER.info("Extracting MMC pack");
        try (ZipFile zipFile = new ZipFile(pack)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().equals(".packignore") || entry.getName().equals("instance.cfg")) {
                    continue;
                }
                if (entry.getName().endsWith("net.minecraftforge.json")) {
                    JsonObject modJson = new JsonParser().parse(IOUtils.toString(zipFile.getInputStream(entry), StandardCharsets.UTF_8)).getAsJsonObject();
                    modJson.getAsJsonArray("libraries").forEach(jsonElement -> {
                        String libraryName = jsonElement.getAsJsonObject().get("name").getAsString();
                        if (libraryName.startsWith("com.cleanroommc:cleanroom")) {
                            String version = libraryName.split(":")[2].replace("-universal", "");
                            CleanroomVersionParser.setVersion(version);
                            Relauncher.LOGGER.info("Extracting Cleanroom version {}", version);
                        }
                    });
                    universalJar = new File(libraries, "cleanroom-" + CleanroomVersionParser.getVersion() + "-universal.jar");
                }
                File entryDestination = new File(mmcDir, entry.getName());
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
        if (universalJar.exists()) {
            File universalTarget = new File(Relauncher.workingDir, universalJar.getName());
            Files.copy(universalJar.toPath(), universalTarget.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

    }
    
    private static void extractBundledZip(File pack) {
        Relauncher.LOGGER.info("Attempt to unzip");
        try {
            FileOutputStream out = new FileOutputStream(pack);
            InputStream in = Relauncher.class.getResourceAsStream("/mmcpack.zip");
            IOUtils.copy(in, out);
            out.close();
            in.close();
        } catch (IOException e) {
            Relauncher.LOGGER.error("Failed to unzip bundled mmc pack");
            Relauncher.LOGGER.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
