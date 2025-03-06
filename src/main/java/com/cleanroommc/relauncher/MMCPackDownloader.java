package com.cleanroommc.relauncher;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class MMCPackDownloader {
    public static void downloadAndExtract () {
        String version = CleanroomVersionParser.getVersion();
        File mmcDir = new File(Relauncher.workingDir, "mmcpack");
        File universal = new File(new File(mmcDir, "libraries"), "cleanroom-" + version + "-universal.jar");
        mmcDir.mkdir();
        File pack = new File(mmcDir, "mmcpack.zip");
        try {
            if (!pack.exists()) {
                if (!Config.useLocalPack) {
                    Downloader.downloadUntilSucceed(new URL("https://github.com/CleanroomMC/Cleanroom/releases/download/" + version + "/Cleanroom-MMC-instance-" + version + ".zip"), "", pack);
                } else {
                    if (Relauncher.workingDir.listFiles() != null) {
                        Optional<File> packfile = Arrays.stream(Relauncher.workingDir.listFiles()).filter(file -> file.getName().startsWith("Cleanroom-MMC-instance-") && file.getName().endsWith(".zip")).findFirst();
                        if (packfile.isPresent()) {
                            Files.copy(packfile.get().toPath(), pack.toPath());
                        } else {
                            throw new RuntimeException("Configured to use local mmc pack but can't find matched target");
                        }
                    } else {
                        throw new RuntimeException("Configured to use local mmc pack but directory empty");
                    }
                }
            }
            try (ZipFile zipFile = new ZipFile(pack)) {
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (entry.getName().equals(".packignore") || entry.getName().equals("instance.cfg")) {
                        continue;
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
            File universalTarget = new File(Relauncher.workingDir, universal.getName());
            Files.copy(universal.toPath(), universalTarget.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            Relauncher.LOGGER.error("Get MMC pack failed: ", e);
        }
    }
}
