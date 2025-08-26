package com.cleanroommc.relauncher;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.TlsSocketStrategy;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.ssl.SSLContextBuilder;

import javax.net.ssl.SSLContext;
import javax.swing.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Downloader {
    private static final PoolingHttpClientConnectionManager connManager;
    private static final HttpClientBuilder builder;
    private static final PriorityQueue<DownloadEntry> queue = new PriorityQueue<>();

    static {

        try {
            SSLContext sslContext = new SSLContextBuilder()
                    .loadTrustMaterial(null, (x509CertChain, authType) -> true)
                    .build();
            TlsSocketStrategy tlsStrategy = new DefaultClientTlsStrategy(sslContext);
            connManager = PoolingHttpClientConnectionManagerBuilder
                    .create()
                    .setTlsSocketStrategy(tlsStrategy)
                    .build();
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            throw new RuntimeException(e);
        }

        connManager.setMaxTotal(Config.maxDownloadSession);

        if (!Config.proxyAddr.isEmpty() && Config.proxyPort != 0) {
            builder = HttpClients
                    .custom()
                    .setConnectionManager(connManager)
                    .setConnectionManagerShared(true)
                    .setProxy(new HttpHost(Config.proxyAddr, Config.proxyPort));
        } else {
            builder = HttpClients
                    .custom()
                    .setConnectionManager(connManager)
                    .setConnectionManagerShared(true);
        }

    }

    public static void downloadAll(List<DownloadEntry> list) {
        Initializer.getMainProgressbar().setMaximum(list.size());
        try (CloseableHttpClient client = builder.build()) {
            while (!list.isEmpty() || !queue.isEmpty()) {
                while (!queue.isEmpty()) {
                    list.add(queue.poll());
                }
                ExecutorService pool = Executors.newFixedThreadPool(Config.maxDownloadSession);
                AtomicInteger i = new AtomicInteger();
                while (!list.isEmpty()) {
                    DownloadEntry entry = list.remove(0);
                    try {
                        HttpGet httpGet = new HttpGet(entry.getUrl().toURI());
                        pool.submit(new DownloadThread(client, httpGet, i.getAndIncrement(), entry));
                    } catch (URISyntaxException e) {
                        Relauncher.LOGGER.error(e);
                        // no-op
                    }
                }
                pool.shutdown();
                pool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
            }
        } catch (IOException | InterruptedException e) {
            Relauncher.LOGGER.error(e);
        }
    }


    private static boolean calculateSHA1(File dest, String sha1) {
        if (sha1.isEmpty()) {
            return true;
        }
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

    private static class DownloadThread extends Thread {
        private final CloseableHttpClient httpClient;
        private final HttpGet httpget;
        private final DownloadEntry entry;

        public DownloadThread(CloseableHttpClient httpClient, HttpGet httpget, int id, DownloadEntry entry) {
            this.httpClient = httpClient;
            this.httpget = httpget;
            setName("Download thread#" + id);
            this.entry = entry;
        }

        @Override
        public void run() {
            Relauncher.LOGGER.info("Checking {}...", entry.getDestination());
            if (entry.getDestination().exists()) {
                if (calculateSHA1(entry.getDestination(), entry.getSha1())) {
                    Relauncher.LOGGER.info("File {} already exist, skipping", entry.getDestination());
                    Initializer.addProgress();
                    return;
                }
            } else {
                try {
                    Files.createDirectories(entry.getDestination().getParentFile().toPath());
                } catch (IOException e) {
                    Relauncher.LOGGER.error("Create file failed.");
                    Relauncher.LOGGER.error(e.getMessage());
                }
            }
            try {
                //Executing the request
                ClassicHttpResponse httpresponse = httpClient.execute(httpget, response -> {
                    try (FileOutputStream outputStream = new FileOutputStream(entry.getDestination())) {
                        IOUtils.copy(response.getEntity().getContent(), outputStream);
                    }
                    httpClient.close();
                    return (CloseableHttpResponse) response;
                });
                if (!entry.getSha1().isEmpty()) {
                    if (!calculateSHA1(entry.getDestination(), entry.getSha1())) {
                        Relauncher.LOGGER.info("Downloaded file {} has invalid checksum, re-downloading...", entry.getDestination());
                        if (entry.failed() >= Config.maxRetry) {
                            Relauncher.LOGGER.error("Download {} reached max attempts", entry.getDestination());
                            throw new RuntimeException("Max retry reached");
                        } else {
                            queue.add(entry);
                        }
                    } else {
                        Initializer.addProgress();
                    }
                }

            } catch (Exception e) {
                Relauncher.LOGGER.error(e);
            }
        }
    }
}
