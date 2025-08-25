package com.cleanroommc.relauncher;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class MMCPackDownloader {
    public static void downloadAndExtract () throws IOException {
        File mmcDir = new File(Relauncher.workingDir, "mmcpack");
        File libraries = new File(mmcDir, "libraries");
        File universal = new File(libraries, "cleanroom-0.3.13-alpha-universal.jar");
        mmcDir.mkdir();
        File pack = new File(mmcDir, "mmcpack.zip");
        if (!pack.exists()) {
            if (!Config.useLocalPack) {
                String version = CleanroomVersionParser.getVersion();
                universal = new File(libraries, "cleanroom-" + version + "-universal.jar");
                Relauncher.LOGGER.info("Downloading MMC pack with version {}", version);
                List<DownloadEntry> list = new ArrayList<>(1);
                list.add(new DownloadEntry(new URL("https://github.com/CleanroomMC/Cleanroom/releases/download/" + version + "/Cleanroom-MMC-instance-" + version + ".zip"), pack, ""));
                Downloader.downloadAll(list);
            } else {
                if (Relauncher.workingDir.listFiles() != null) {
                    Optional<File> packfile = Arrays.stream(Relauncher.workingDir.listFiles()).filter(file -> file.getName().startsWith("Cleanroom-MMC-instance-") && file.getName().endsWith(".zip")).findFirst();
                    if (packfile.isPresent()) {
                        Relauncher.LOGGER.info("Found local pack {}", packfile.get());
                        Files.copy(packfile.get().toPath(), pack.toPath());
                    } else {
                        Initializer.getMainStatusLabel().setText("Configured to use local mmc pack but can't find matched target");
                        Initializer.getConfirmButton().setEnabled(true);
                        return;
                    }
                } else {
                    Initializer.getMainStatusLabel().setText("Configured to use local mmc pack but directory is empty");
                    Initializer.getConfirmButton().setEnabled(true);
                    return;
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
                if (entry.getName().endsWith(".jar")) {
                    Relauncher.LOGGER.info("Universal jar: {}", entry.getName());
                    String name = entry.getName();
                    if (name.startsWith("libraries")) {
                        name = name.substring(9);
                    }
                    universal = new File(libraries, name);
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

    }
}
