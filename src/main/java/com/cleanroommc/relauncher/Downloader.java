package com.cleanroommc.relauncher;

import com.google.common.base.Strings;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;

public class Downloader {
    static {
        System.setProperty("javax.net.ssl.checkRevocation", "false");
        System.setProperty("trust_all_cert", "true");
        // Fxxk mojang's 8u51
        if (Config.proxy != null && !Config.proxy.isEmpty()) {
            if (!Config.proxy.startsWith("http") && !Config.proxy.startsWith("socks")) {
                throw new RuntimeException("Invalid proxy config!");
            }
            String[] temp = Config.proxy.split(":");
            String addr = temp[1].substring(2);
            String port = temp[2];
            if (Config.proxy.startsWith("http")) {
                System.setProperty("https.proxyHost", addr);
                System.setProperty("https.proxyPort", port);
            } else {
                System.setProperty("socksProxyHost", addr);
                System.setProperty("socksProxyPort", port);
            }
        }
    }
    public static void downloadUntilSucceed(URL url, String sha1, File destination) {
        boolean succeed = false;
        if (!destination.exists()) {
            while (!succeed) {
                try {
                    FileUtils.copyURLToFile(url, destination);
                    if (!Strings.isNullOrEmpty(sha1)) {
                        String result = DigestUtils.sha1Hex(Files.newInputStream(destination.toPath()));
                        if (result.equals(sha1)) {
                            succeed = true;
                            Relauncher.LOGGER.info("Downloaded and verified file: {}", destination.getName());
                        }
                    } else {
                        succeed = true;
                    }
                } catch (IOException ignored) {
                }
            }
        } else {
            if (!Strings.isNullOrEmpty(sha1)) {
                try {
                    String result = DigestUtils.sha1Hex(Files.newInputStream(destination.toPath()));
                    if (!result.equals(sha1)) {
                        while (!succeed) {
                                FileUtils.copyURLToFile(url, destination);
                                if (!Strings.isNullOrEmpty(sha1)) {
                                    result = DigestUtils.sha1Hex(Files.newInputStream(destination.toPath()));
                                    if (result.equals(sha1)) {
                                        succeed = true;
                                        Relauncher.LOGGER.info("Found and verified cached file: {}", destination.getName());
                                    }
                                } else {
                                    succeed = true;
                                }

                        }
                    }
                } catch (IOException ignored) {}
            }
        }

    }
}
