package com.cleanroommc.relauncher;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraftforge.fml.common.versioning.ComparableVersion;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class MMCPackDownloader {
    public static void downloadAndExtract () throws IOException {
        File mmcDir = new File(Relauncher.workingDir, "mmcpack");
        mmcDir.mkdir();
        File pack = new File(mmcDir, "mmcpack.zip");
        if (!Config.useLocalPack) {
            String version = CleanroomVersionParser.getVersion();
            boolean needDownload = true;
            if (pack.exists()) {
                String cachedVersion = CleanroomVersionParser.readCachedVersion();
                if (cachedVersion != null && !isVersionNewer(version, cachedVersion)) {
                    Relauncher.LOGGER.info("Cached mmc pack version {} is up-to-date, skipping download", cachedVersion);
                    needDownload = false;
                }
            }
            if (needDownload) {
                Relauncher.LOGGER.info("Downloading MMC pack with version {}", version);
                Initializer.setStatusText(Messages.get("status.downloading_mmc_pack", version));
                String zipUrl = "https://repo.cleanroommc.com/releases/com/cleanroommc/cleanroom/" + version + "/cleanroom-" + version + ".zip";
                if (Config.useMirror) {
                    zipUrl = DownloadEntry.applyMirrorUrl(zipUrl);
                }
                String sha1 = fetchSha1(zipUrl + ".sha1");
                List<DownloadEntry> list = new ArrayList<>(1);
                list.add(new DownloadEntry(new URL(zipUrl), pack, sha1));
                try {
                    Downloader.downloadAll(list);
                } catch (RuntimeException e) {
                    Relauncher.LOGGER.error("MMC pack download failed: {}", e.getMessage());
                    throw new IOException(Messages.get("status.mmc_download_failed", Config.maxRetry), e);
                }
                if (!pack.exists() || pack.length() == 0) {
                    throw new IOException(Messages.get("status.mmc_download_failed", Config.maxRetry));
                }
            }
        } else {
            if (!Config.localPackPath.isEmpty()) {
                File localPack = new File(Config.localPackPath);
                if (localPack.exists() && localPack.isFile()) {
                    Relauncher.LOGGER.info("Using local pack {}", localPack.getAbsolutePath());
                    Files.copy(localPack.toPath(), pack.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } else {
                    Relauncher.LOGGER.warn("Local pack not found at {}, falling back to bundled", Config.localPackPath);
                    extractBundledZip(pack);
                }
            } else if (pack.exists()) {
                Relauncher.LOGGER.info("Using cached mmc pack");
            } else {
                extractBundledZip(pack);
            }
        }

        File universalJar = extractPackContent(mmcDir, pack);
        File extractedLibraries = new File(mmcDir, "libraries");
        File extractedPatches = new File(mmcDir, "patches");
        if (!extractedLibraries.exists() || !extractedPatches.exists()) {
            Relauncher.LOGGER.warn("MMC pack extraction produced incomplete result");
            if (Config.useLocalPack && !Config.localPackPath.isEmpty()) {
                throw new IOException("Local MMC pack does not contain required directories");
            }
            Relauncher.LOGGER.info("Cleaning up and retrying with bundled pack");
            pack.delete();
            try {
                FileUtils.deleteDirectory(mmcDir);
            } catch (IOException ignored) {
            }
            mmcDir.mkdir();
            extractBundledZip(pack);
            universalJar = extractPackContent(mmcDir, pack);
            if (!extractedLibraries.exists() || !extractedPatches.exists()) {
                throw new IOException("MMC pack extraction failed even with bundled pack");
            }
        }

        if (universalJar.exists()) {
            File universalTarget = new File(Relauncher.workingDir, universalJar.getName());
            Files.copy(universalJar.toPath(), universalTarget.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

    }

    private static File extractPackContent(File mmcDir, File pack) throws IOException {
        File libraries = new File(mmcDir, "libraries");
        File universalJar = new File(libraries, "cleanroom-" + CleanroomVersionParser.BUNDLED_VERSION + "-universal.jar");
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
        } catch (IOException e) {
            Relauncher.LOGGER.error("Failed to extract MMC pack, removing corrupted file");
            pack.delete();
            throw e;
        }
        return universalJar;
    }
    
    private static boolean isVersionNewer(String latest, String cached) {
        return new ComparableVersion(latest).compareTo(new ComparableVersion(cached)) > 0;
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

    private static String fetchSha1(String sha1Url) {
        try {
            String content = Downloader.downloadString(sha1Url);
            if (!content.isEmpty()) {
                Relauncher.LOGGER.info("Fetched SHA1 hash for MMC pack: {}", content);
                return content;
            }
        } catch (IOException e) {
            Relauncher.LOGGER.warn("Failed to fetch SHA1 hash from {}, proceeding without integrity check", sha1Url);
        }
        return "";
    }
}
