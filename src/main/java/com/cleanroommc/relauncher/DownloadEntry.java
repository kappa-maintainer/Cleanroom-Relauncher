package com.cleanroommc.relauncher;

import org.apache.commons.lang3.tuple.Triple;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
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
            libDir = Relauncher.workingDir;
        } else {
            libDir = new File(Config.libraryPath);
        }
        String[] a = triple.getMiddle().split("/");
        String fileName = a[a.length - 1];
        if (Config.respectLibraryStructure) {
            String[] maven = triple.getLeft().split(":");
            fileName = maven[0].replace('.', File.separatorChar) + File.separatorChar + maven[2] + File.separatorChar + fileName;
            if (File.separatorChar == '\\') {
                fileName = fileName.replace("\\", "\\\\"); // I hate Windows
            }
        }
        destination = new File(libDir, fileName);
        try {
            String urlString = triple.getMiddle();
            if (!Config.chineseMode) {
                urlString = urlString.replace("https://repo.maven.apache.org/maven2", "https://maven.aliyun.com/repository/public");
                urlString = urlString.replace("https://repo.cleanroommc.com/releases", "https://nexus.jsdu.cn/repository/cleanroommc-releases");
                urlString = urlString.replace("https://maven.arcseekers.com/releases", "https://nexus.jsdu.cn/repository/arcseekers-releases");
            }
            url = new URL(urlString);
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
    
    public int failed() {
        return attempts.addAndGet(1);
    }
}
