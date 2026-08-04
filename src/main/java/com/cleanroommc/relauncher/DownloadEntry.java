package com.cleanroommc.relauncher;

import net.minecraft.launchwrapper.Launch;
import org.apache.commons.lang3.tuple.Triple;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class DownloadEntry {
    private final URL url;
    private final File destination;
    private final String sha1;
    private final AtomicInteger attempts = new AtomicInteger(0);
    public DownloadEntry(URL url, File destination, String sha1) {
        this.url = url;
        this.destination = destination;
        this.sha1 = sha1;
    }
    
    public DownloadEntry(Triple<String, String, String> triple) {
        File libDir;
        if (Config.libraryPath.isEmpty()) {
            libDir = new File(Launch.minecraftHome.getParentFile(), "libraries");
        } else {
            libDir = new File(Config.libraryPath);
        }
        String[] urlParts = triple.getMiddle().split("/");
        String fileName = urlParts[urlParts.length - 1];
        if (Config.respectLibraryStructure) {
            String[] maven = triple.getLeft().split(":");
            if (maven.length < 3) {
                throw new IllegalArgumentException("Invalid Maven coordinate: " + triple.getLeft());
            }
            fileName = maven[0].replace('.', File.separatorChar)
                    + File.separatorChar + maven[1]
                    + File.separatorChar + maven[2]
                    + File.separatorChar + fileName;
        }
        destination = new File(libDir, fileName);
        try {
            url = new URL(triple.getMiddle());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        sha1 = triple.getRight();
    }

    public URL getUrl() {
        return url;
    }

    public File getDestination() {
        return destination;
    }

    public String getSha1() {
        return sha1;
    }

    /** Returns the normalized target path used by the batch scheduler for de-duplication and locking. */
    String getNormalizedDestinationPath() {
        try {
            return destination.getCanonicalFile().toPath().normalize().toString();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to normalize download destination " + destination, e);
        }
    }

    /** Returns the primary URL declared by the download metadata. */
    String getRequestUrl() {
        return url.toExternalForm();
    }

    /** Returns the primary URL and its automatic mirror candidate, without duplicates. */
    List<String> getCandidateUrls() {
        return candidateUrls(getRequestUrl());
    }

    static List<String> candidateUrls(String primaryUrl) {
        List<String> result = new ArrayList<>(2);
        result.add(primaryUrl);
        String mirror = applyMirrorUrl(primaryUrl);
        if (!mirror.equals(primaryUrl)) {
            result.add(mirror);
        }
        return result;
    }

    /** Records one failed attempt for callers that still expose the legacy retry counter. */
    public int failed() {
        return attempts.addAndGet(1);
    }

    /** Returns the legacy per-entry failed-attempt count. The downloader keeps its own batch-local count. */
    public int getAttempts() {
        return attempts.get();
    }

    public static String applyMirrorUrl(String url) {
        url = url.replace("https://repo.maven.apache.org/maven2", "https://maven.aliyun.com/repository/public");
        url = url.replace("https://repo.cleanroommc.com/releases", "https://nexus.jsdu.cn/repository");
        url = url.replace("https://maven.arcseekers.com/releases", "https://nexus.jsdu.cn/repository");
        return url;
    }
}
