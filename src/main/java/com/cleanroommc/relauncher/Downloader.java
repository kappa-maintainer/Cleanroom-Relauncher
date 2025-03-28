package com.cleanroommc.relauncher;

import com.github.axet.wget.WGet;
import com.github.axet.wget.info.BrowserInfo;
import com.github.axet.wget.info.DownloadInfo;
import com.github.axet.wget.info.ProxyInfo;
import com.google.common.base.Strings;
import org.apache.commons.codec.digest.DigestUtils;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicBoolean;

public class Downloader {
    private static ProxyInfo proxyInfo = null;
    static {
        BrowserInfo.USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36";
        // Fxxk mojang's 8u51
        if (!Strings.isNullOrEmpty(Config.proxyAddr) && Config.proxyPort > 0) {
            proxyInfo = new ProxyInfo(Config.proxyAddr, Config.proxyPort);
        }
    }
    public static void downloadUntilSucceed(URL url, String sha1, File destination) throws IOException {
        boolean succeed = false;
        Initializer.getSubProgressbar().setValue(0);
        if (!destination.exists()) {
            while (!succeed) {
                downloadWithWget(url, destination);
                if (!Strings.isNullOrEmpty(sha1)) {
                    if (calculateSHA1(destination, sha1)) {
                        succeed = true;
                        Relauncher.LOGGER.info("Downloaded and verified file: {}", destination.getName());
                    }
                } else {
                    succeed = true;
                }
            }
        } else {
            if (!Strings.isNullOrEmpty(sha1)) {
                if (!calculateSHA1(destination, sha1)) {
                    while (!succeed) {
                        downloadWithWget(url, destination);
                        if (calculateSHA1(destination, sha1)) {
                            succeed = true;
                            Relauncher.LOGGER.info("Found and verified cached file: {}", destination.getName());
                        }
                    }
                }
            }
        }

    }

    private static void downloadWithWget(URL url, File destination) throws IOException {
        Initializer.getSubStatusLabel().setText("Downloading " + destination.getName());
        Files.createDirectories(destination.getParentFile().toPath());
        AtomicBoolean stop = new AtomicBoolean(false);
        DownloadInfo info;
        if (proxyInfo == null) {
            info = new DownloadInfo(url);
        } else {
            info = new DownloadInfo(url, proxyInfo);
        }
        Runnable setProgress = () -> {
            int progress;
            if (info.getLength() != null) {
                progress = (int) ((float)info.getDownloaded() / (float)info.getLength() * 100.0F);
            } else {
                progress = 0;
            }
            SwingUtilities.invokeLater(() -> Initializer.getSubProgressbar().setValue(progress));
        };
        info.extract(stop, setProgress);
        if (Config.enableMultipartDownload) {
            try {
                info.enableMultipart();
            } catch (Throwable t) {
                Relauncher.LOGGER.info("File {}'s host not supporting multipart download", destination.getName());
                SwingUtilities.invokeLater(() -> Initializer.getSubProgressbar().setIndeterminate(true));
            }
        }
        WGet w = new WGet(info, destination);

        w.download(stop, setProgress);
        SwingUtilities.invokeLater(() -> Initializer.getSubProgressbar().setIndeterminate(false));
    }

    private static boolean calculateSHA1(File dest, String sha1) {
        Initializer.getSubStatusLabel().setText("Verifying file " + dest.getName());
        try {
            boolean match = DigestUtils.sha1Hex(Files.newInputStream(dest.toPath())).equals(sha1);
            if (!match) {
                Relauncher.LOGGER.warn("SHA1 calculation of file {} failed, re-downloading", dest);
            }
            return match;
        } catch (IOException e) {
            Relauncher.LOGGER.warn("Caught error in SHA1 calculation of file {}, re-downloading", dest, e);
            return false;
        }
    }
}
