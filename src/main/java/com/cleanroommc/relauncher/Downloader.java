package com.cleanroommc.relauncher;

import nl.altindag.ssl.SSLFactory;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.TlsSocketStrategy;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Shared HTTP download engine used by the MMC pack and library download flows.
 * All JDK APIs used here are available on Java 8; newer Java APIs are not needed
 * because Apache HttpClient supplies the connection pooling and request layer.
 */
public class Downloader {
    private static final long RANGE_THRESHOLD = 4L * 1024L * 1024L;
    private static final long RANGE_CHUNK_SIZE = 512L * 1024L;
    private static final int NON_RANGE_SOURCE_PROBE_BYTES = 8 * 1024;
    private static final long DEFAULT_BACKOFF_MILLIS = 200L;
    private static final long MAX_BACKOFF_MILLIS = 5_000L;
    private static final long SOURCE_PROBE_RESPONSE_TIMEOUT_MILLIS = 5_000L;
    private static final long EXECUTOR_SHUTDOWN_GRACE_MILLIS = 1_000L;
    private static final long EXECUTOR_SHUTDOWN_FORCE_MILLIS = 2_000L;
    private static final RequestConfig SOURCE_PROBE_CONFIG = RequestConfig.custom()
            .setConnectionRequestTimeout(Timeout.ofSeconds(10))
            .setConnectTimeout(Timeout.ofSeconds(10))
            .setResponseTimeout(Timeout.ofMilliseconds(SOURCE_PROBE_RESPONSE_TIMEOUT_MILLIS))
            .setHardCancellationEnabled(true)
            .build();
    private static final RequestConfig DOWNLOAD_REQUEST_CONFIG = RequestConfig.custom()
            .setConnectionRequestTimeout(Timeout.ofSeconds(15))
            .setConnectTimeout(Timeout.ofSeconds(15))
            .setResponseTimeout(Timeout.ofSeconds(60))
            .setHardCancellationEnabled(true)
            .build();
    private static final ConcurrentMap<String, TargetLock> TARGET_LOCKS = new ConcurrentHashMap<>();
    private static final Object ENGINE_LOCK = new Object();
    private static volatile HttpEngine httpEngine;

    /** Retained for compatibility; downloads are exposed through static methods. */
    public Downloader() {
    }

    public static void downloadAll(List<DownloadEntry> list) {
        if (list == null) {
            throw new IllegalArgumentException("Download list cannot be null");
        }
        final List<DownloadEntry> entries = deduplicate(list);
        final long batchStarted = System.nanoTime();
        Initializer.getMainProgressbar().setMaximum(entries.size());
        Initializer.setCurrentFile(null);
        if (entries.isEmpty()) {
            Relauncher.LOGGER.info("Download batch contains no entries");
            return;
        }

        final HttpEngine engine = acquireHttpEngine();
        final int concurrency = engine.getConcurrency();
        Relauncher.LOGGER.info("Download batch started: {} unique files, {} HTTP slots", entries.size(), concurrency);
        final ExecutorService filePool = Executors.newFixedThreadPool(concurrency);
        final ExecutorService requestPool = Executors.newFixedThreadPool(concurrency,
                new NamedDaemonThreadFactory("Relauncher HTTP Request"));
        final ExecutorCompletionService<Void> completions = new ExecutorCompletionService<>(filePool);
        int submitted = 0;
        int completed = 0;
        Throwable firstFailure = null;
        try {
            while (submitted < entries.size() && submitted - completed < concurrency) {
                submitEntry(entries.get(submitted++), completions, requestPool, engine);
            }
            while (completed < submitted) {
                try {
                    completions.take().get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    firstFailure = firstFailure == null ? e : firstFailure;
                    break;
                } catch (ExecutionException e) {
                    if (firstFailure == null) {
                        firstFailure = unwrap(e);
                    }
                    // Stop submitting new files after the first unrecoverable error.
                }
                completed++;
                if (firstFailure == null) {
                    while (submitted < entries.size() && submitted - completed < concurrency) {
                        submitEntry(entries.get(submitted++), completions, requestPool, engine);
                    }
                }
            }
        } finally {
            Relauncher.LOGGER.info("Download batch file tasks finished after {} ms; shutting down executors",
                    elapsedMillis(batchStarted));
            filePool.shutdown();
            requestPool.shutdown();
            try {
                awaitExecutor(filePool, "file");
            } finally {
                try {
                    shutdownRequestExecutor(requestPool, engine);
                } finally {
                    releaseHttpEngine(engine);
                }
            }
        }
        if (firstFailure != null) {
            if (firstFailure instanceof RuntimeException) {
                throw (RuntimeException) firstFailure;
            }
            throw new RuntimeException(firstFailure);
        }
        Relauncher.LOGGER.info("Download batch completed in {} ms", elapsedMillis(batchStarted));
    }

    private static void submitEntry(DownloadEntry entry,
                                    ExecutorCompletionService<Void> completions,
                                    ExecutorService requestPool,
                                    HttpEngine engine) {
        completions.submit(() -> {
            downloadOne(entry, requestPool, engine);
            return null;
        });
    }

    private static Throwable unwrap(ExecutionException exception) {
        Throwable cause = exception.getCause();
        return cause == null ? exception : cause;
    }

    private static void awaitExecutor(ExecutorService executor, String name) {
        long started = System.nanoTime();
        boolean interrupted = false;
        try {
            if (awaitTermination(executor, EXECUTOR_SHUTDOWN_GRACE_MILLIS)) {
                Relauncher.LOGGER.info("{} download executor terminated in {} ms", name, elapsedMillis(started));
                return;
            }
            Relauncher.LOGGER.warn("{} download executor did not terminate after {} ms; interrupting remaining tasks",
                    name, elapsedMillis(started));
            executor.shutdownNow();
            if (awaitTermination(executor, EXECUTOR_SHUTDOWN_FORCE_MILLIS)) {
                Relauncher.LOGGER.info("{} download executor terminated after forced cancellation in {} ms",
                        name, elapsedMillis(started));
                return;
            }
            throw new IllegalStateException(name + " download executor did not terminate after HTTP cancellation");
        } catch (InterruptedException e) {
            interrupted = true;
            executor.shutdownNow();
            throw new IllegalStateException("Interrupted while shutting down " + name + " download executor", e);
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static void shutdownRequestExecutor(ExecutorService requestPool, HttpEngine engine) {
        shutdownRequestExecutor(requestPool, engine, true);
    }

    private static void shutdownRequestExecutor(ExecutorService requestPool,
                                                HttpEngine engine,
                                                boolean canRetireEngine) {
        long started = System.nanoTime();
        boolean interrupted = false;
        try {
            if (awaitTermination(requestPool, EXECUTOR_SHUTDOWN_GRACE_MILLIS)) {
                Relauncher.LOGGER.info("request download executor terminated in {} ms", elapsedMillis(started));
                return;
            }
            Relauncher.LOGGER.warn("request download executor did not terminate after {} ms; closing active HTTP connections",
                    elapsedMillis(started));
            if (canRetireEngine) {
                engine.closeConnections();
                discardHttpEngine(engine);
            } else {
                Relauncher.LOGGER.warn("Keeping shared HTTP client alive while cancelling source probe tasks");
            }
            requestPool.shutdownNow();
            if (awaitTermination(requestPool, EXECUTOR_SHUTDOWN_FORCE_MILLIS)) {
                Relauncher.LOGGER.info("request download executor terminated after closing active connections in {} ms",
                        elapsedMillis(started));
                return;
            }
            Relauncher.LOGGER.error("request download executor still has {} active task(s) after {} ms; continuing shutdown",
                    engine.getActiveRequestCount(), elapsedMillis(started));
        } catch (InterruptedException e) {
            interrupted = true;
            if (canRetireEngine) {
                engine.closeConnections();
                discardHttpEngine(engine);
            }
            requestPool.shutdownNow();
            Relauncher.LOGGER.warn("Interrupted while shutting down request download executor", e);
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static boolean awaitTermination(ExecutorService executor, long timeoutMillis) throws InterruptedException {
        return executor.awaitTermination(timeoutMillis, TimeUnit.MILLISECONDS);
    }

    private static long elapsedMillis(long started) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
    }

    private static List<DownloadEntry> deduplicate(List<DownloadEntry> entries) {
        Map<String, DownloadEntry> unique = new LinkedHashMap<>();
        for (DownloadEntry entry : entries) {
            if (entry == null) {
                throw new IllegalArgumentException("Download list contains null entry");
            }
            String target = entry.getNormalizedDestinationPath();
            DownloadEntry previous = unique.get(target);
            if (previous == null) {
                unique.put(target, entry);
            } else if (!sameSource(previous, entry)
                    || !normaliseSha1(previous.getSha1()).equals(normaliseSha1(entry.getSha1()))) {
                throw new IllegalArgumentException("Conflicting download entries for target " + target);
            } else {
                Relauncher.LOGGER.info("Skipping duplicate download target {}", target);
            }
        }
        return new ArrayList<>(unique.values());
    }

    private static String normaliseSha1(String sha1) {
        return sha1 == null ? "" : sha1.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean sameSource(DownloadEntry first, DownloadEntry second) {
        List<String> firstCandidates = first.getCandidateUrls();
        List<String> secondCandidates = second.getCandidateUrls();
        for (String firstUrl : firstCandidates) {
            if (secondCandidates.contains(firstUrl)) {
                return true;
            }
        }
        return false;
    }

    private static void downloadOne(DownloadEntry entry, ExecutorService requestPool, HttpEngine engine) throws Exception {
        File destination = entry.getDestination();
        String targetKey = entry.getNormalizedDestinationPath();
        TargetLock lock = acquireTargetLock(targetKey);
        try {
        synchronized (lock) {
            if (destination.getParentFile() == null) {
                throw new IOException("Download destination has no parent directory: " + destination);
            }
            Files.createDirectories(destination.getParentFile().toPath());
            File lockFile = new File(targetKey + ".download.lock");
            try (FileChannel lockChannel = FileChannel.open(lockFile.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                 FileLock ignored = lockChannel.lock()) {
                if (entry.getSha1() == null || entry.getSha1().trim().isEmpty()) {
                    Relauncher.LOGGER.warn("No SHA1 supplied for {}, downloaded content cannot be validated", destination);
                }
                if (isCached(destination, entry.getSha1())) {
                    Relauncher.LOGGER.info("File {} already exists, skipping", destination.getName());
                    Initializer.addProgress(destination.getName());
                    return;
                }
                Initializer.setCurrentFile(destination.getName());
                File temporary = new File(destination.getParentFile(), destination.getName() + ".part-" + UUID.randomUUID().toString());
                try {
                    long selectionStarted = System.nanoTime();
                    RaceResult winner = selectWinner(entry.getCandidateUrls(), requestPool, engine);
                    Relauncher.LOGGER.info("Selected {} for {} in {} ms",
                            winner.url, destination.getName(), elapsedMillis(selectionStarted));
                    transferWithRetry(entry, winner.url, winner.probe, temporary, requestPool, engine);
                    publishAtomically(temporary.toPath(), destination.toPath());
                    Initializer.addProgress(destination.getName());
                } finally {
                    Files.deleteIfExists(temporary.toPath());
                }
            }
        }
        } finally {
            releaseTargetLock(targetKey, lock);
        }
    }

    private static TargetLock acquireTargetLock(final String targetKey) {
        return TARGET_LOCKS.compute(targetKey, (key, existing) -> {
            TargetLock lock = existing == null ? new TargetLock() : existing;
            lock.references++;
            return lock;
        });
    }

    private static void releaseTargetLock(final String targetKey, final TargetLock lock) {
        TARGET_LOCKS.compute(targetKey, (key, existing) -> {
            if (existing != lock) {
                Relauncher.LOGGER.error("Download target lock changed unexpectedly for {}", targetKey);
                return existing;
            }
            lock.references--;
            if (lock.references < 0) {
                throw new IllegalStateException("Download target lock reference count became negative for " + targetKey);
            }
            return lock.references == 0 ? null : lock;
        });
    }

    private static boolean isCached(File destination, String sha1) {
        if (!destination.isFile()) {
            return false;
        }
        if (sha1 == null || sha1.trim().isEmpty()) {
            return true;
        }
        return calculateSHA1(destination, sha1);
    }

    private static boolean calculateSHA1(File dest, String sha1) {
        try (InputStream input = Files.newInputStream(dest.toPath())) {
            boolean match = DigestUtils.sha1Hex(input).equalsIgnoreCase(normaliseSha1(sha1));
            if (!match) {
                Relauncher.LOGGER.warn("SHA1 calculation of file {} failed, re-downloading", dest);
            }
            return match;
        } catch (IOException e) {
            Relauncher.LOGGER.warn("Caught error in SHA1 calculation of file {}, re-downloading", dest.getName(), e);
            return false;
        }
    }

    private static RaceResult selectWinner(List<String> candidates,
                                            ExecutorService requestPool,
                                            HttpEngine engine) throws Exception {
        return selectWinner(candidates, requestPool, engine, Math.max(1, Config.maxRetry));
    }

    private static RaceResult selectWinner(List<String> candidates,
                                            ExecutorService requestPool,
                                            HttpEngine engine,
                                            int maxAttempts) throws Exception {
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("No download URL candidates supplied");
        }
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("Source selection attempts must be positive");
        }
        Throwable lastFailure = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return raceCandidates(candidates, requestPool, engine);
            } catch (Exception e) {
                lastFailure = e;
                if (!isRetryableSourceFailure(e) || attempt >= maxAttempts) {
                    throw e;
                }
                Relauncher.LOGGER.warn("All download sources failed on source-selection attempt {}", attempt, e);
                sleepBackoff(attempt, e instanceof HttpStatusException
                        ? ((HttpStatusException) e).retryAfterMillis : null);
            }
        }
        throw new IOException("All download sources failed", lastFailure);
    }

    private static RaceResult raceCandidates(List<String> candidates,
                                             ExecutorService requestPool,
                                             HttpEngine engine) throws Exception {
        ExecutorCompletionService<RaceResult> completions = new ExecutorCompletionService<>(requestPool);
        List<RaceCandidate> races = new ArrayList<>(candidates.size());
        Throwable firstFailure = null;
        for (final String candidate : candidates) {
            final HttpGet request = createProbeRequest(candidate);
            RaceCandidate race = new RaceCandidate(request);
            race.future = completions.submit(() -> new RaceResult(candidate, probe(candidate, request, engine)));
            races.add(race);
        }
        try {
            int remaining = races.size();
            while (remaining > 0) {
                Future<RaceResult> future = completions.take();
                remaining--;
                try {
                    RaceResult winner = future.get();
                    Relauncher.LOGGER.info("Selected download source {}", winner.url);
                    return winner;
                } catch (ExecutionException e) {
                    Throwable failure = unwrap(e);
                    if (firstFailure == null) {
                        firstFailure = failure;
                    }
                }
            }
        } finally {
            cancelRaceCandidates(races);
        }
        if (firstFailure instanceof Exception) {
            throw (Exception) firstFailure;
        }
        throw new IOException("All download sources failed", firstFailure);
    }

    private static boolean isRetryableSourceFailure(Exception failure) {
        return !(failure instanceof HttpStatusException && !((HttpStatusException) failure).retryable);
    }

    private static void transferWithRetry(DownloadEntry entry, String url, Probe initialProbe, File temporary,
                                          ExecutorService requestPool, HttpEngine engine) throws Exception {
        int maxAttempts = Math.max(1, Config.maxRetry);
        Throwable lastFailure = null;
        boolean rangeEnabled = initialProbe != null && initialProbe.totalLength > RANGE_THRESHOLD;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                Files.deleteIfExists(temporary.toPath());
                if (rangeEnabled) {
                    try {
                        downloadSegmented(entry, url, temporary, initialProbe, requestPool, engine);
                    } catch (RangeFallbackException e) {
                        rangeEnabled = false;
                        Files.deleteIfExists(temporary.toPath());
                        engine.discardIdleConnectionsAfterRangeCancellation();
                        Relauncher.LOGGER.warn("Range download became inconsistent for {}; falling back to one full request: {}",
                                entry.getDestination().getName(), e.getMessage());
                        waitForRequest(requestPool, (Callable<Void>) () -> {
                            downloadSingle(url, temporary, engine);
                            return null;
                        });
                    }
                } else {
                    waitForRequest(requestPool, (Callable<Void>) () -> {
                        downloadSingle(url, temporary, engine);
                        return null;
                    });
                }
                if (entry.getSha1() != null && !entry.getSha1().trim().isEmpty()
                        && !calculateSHA1(temporary, entry.getSha1())) {
                    throw new IOException("Checksum mismatch for " + entry.getDestination());
                }
                return;
            } catch (Exception e) {
                lastFailure = e;
                if (e instanceof SegmentDownloadException
                        || (e instanceof HttpStatusException && !((HttpStatusException) e).retryable)) {
                    throw e;
                }
                if (attempt >= maxAttempts) {
                    Relauncher.LOGGER.error("Download {} reached max attempts", entry.getDestination().getName(), e);
                    throw e;
                }
                Relauncher.LOGGER.warn("Retrying download {} after attempt {}", entry.getDestination().getName(), attempt, e);
                Initializer.setStatusText(Messages.get("status.retrying_download", entry.getDestination().getName(), attempt, maxAttempts));
                sleepBackoff(attempt, e instanceof HttpStatusException
                        ? ((HttpStatusException) e).retryAfterMillis : null);
            }
        }
        throw new IOException("Download failed", lastFailure);
    }

    private static <T> T waitForRequest(ExecutorService requestPool, Callable<T> request) throws Exception {
        Future<T> future = requestPool.submit(request);
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            future.cancel(true);
            throw e;
        } catch (ExecutionException e) {
            Throwable cause = unwrap(e);
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new RuntimeException("HTTP request failed", cause);
        }
    }

    private static Probe probe(String url, HttpGet request, HttpEngine engine) throws IOException {
        return engine.execute(request, new ResponseProcessor<Probe>() {
            private CloseMode closeMode = CloseMode.GRACEFUL;

            @Override
            public Probe process(CloseableHttpResponse response) throws IOException {
                int code = response.getCode();
                if (code == 200) {
                    closeMode = CloseMode.IMMEDIATE;
                    sampleNonRangeProbePayload(request, response.getEntity(), url);
                    return null;
                }
                if (isProbeFallbackStatus(code)) {
                    return null;
                }
                if (code != 206) {
                    throw HttpStatusException.from(response, url, code);
                }
                Header contentRange = response.getFirstHeader("Content-Range");
                long total = parseContentRange(contentRange == null ? null : contentRange.getValue());
                Header etag = response.getFirstHeader("ETag");
                Header lastModified = response.getFirstHeader("Last-Modified");
                String validator = validatorValue(etag, lastModified);
                if (total <= 0 || validator == null || validator.trim().isEmpty()) {
                    return null;
                }
                consume(response.getEntity());
                return new Probe(total, validator);
            }

            @Override
            public CloseMode responseCloseMode() {
                return closeMode;
            }
        });
    }

    /**
     * Confirms that an endpoint can transfer real data without allowing a Range probe to drain a
     * complete non-range artifact before the source race has selected a winner.
     */
    private static void sampleNonRangeProbePayload(HttpGet request, HttpEntity entity, String url) throws IOException {
        if (entity == null) {
            throw new IOException("Empty non-range source probe response for " + url);
        }
        int sampled = 0;
        byte[] buffer = new byte[Math.min(8 * 1024, NON_RANGE_SOURCE_PROBE_BYTES)];
        InputStream input = entity.getContent();
        try {
            while (sampled < NON_RANGE_SOURCE_PROBE_BYTES) {
                int read = input.read(buffer, 0, Math.min(buffer.length, NON_RANGE_SOURCE_PROBE_BYTES - sampled));
                if (read < 0) {
                    break;
                }
                sampled += read;
            }
        } finally {
            // Abort before HttpEngine performs its immediate close; never close the entity stream first.
            request.cancel();
        }
        if (sampled == 0) {
            throw new IOException("Non-range source probe returned no payload bytes for " + url);
        }
        Relauncher.LOGGER.debug("Sampled {} byte(s) from non-range source probe {}; cancelled remaining payload",
                sampled, url);
    }

    private static boolean isProbeFallbackStatus(int code) {
        return code == 405 || code == 416 || code == 501;
    }

    private static HttpGet createProbeRequest(String url) {
        HttpGet request = new HttpGet(url);
        request.setConfig(SOURCE_PROBE_CONFIG);
        request.setHeader("Range", "bytes=0-0");
        request.setHeader("Accept-Encoding", "identity");
        return request;
    }

    private static void cancelRaceCandidates(List<RaceCandidate> races) {
        int cancelled = 0;
        for (RaceCandidate race : races) {
            if (!race.future.isDone()) {
                race.request.cancel();
                race.future.cancel(true);
                cancelled++;
            }
        }
        if (cancelled > 0) {
            Relauncher.LOGGER.info("Cancelled {} losing source probe request(s) without waiting for their full response", cancelled);
        }
    }

    private static long parseContentRange(String value) {
        if (value == null) {
            return -1;
        }
        String trimmed = value.trim();
        if (!trimmed.startsWith("bytes 0-0/")) {
            return -1;
        }
        try {
            return Long.parseLong(trimmed.substring("bytes 0-0/".length()));
        } catch (NumberFormatException e) {
            Relauncher.LOGGER.warn("Ignoring invalid Range probe Content-Range header: {}", value);
            return -1;
        }
    }

    private static void downloadSingle(String url, File temporary, HttpEngine engine) throws IOException {
        HttpGet request = new HttpGet(url);
        request.setConfig(DOWNLOAD_REQUEST_CONFIG);
        request.setHeader("Accept-Encoding", "identity");
        engine.execute(request, (ResponseProcessor<Void>) response -> {
            int code = response.getCode();
            if (code != 200) {
                throw HttpStatusException.from(response, url, code);
            }
            HttpEntity entity = response.getEntity();
            if (entity == null) {
                throw new IOException("Empty response entity for " + url);
            }
            try (InputStream input = entity.getContent(); OutputStream output = Files.newOutputStream(temporary.toPath())) {
                IOUtils.copy(input, output);
            }
            return null;
        });
    }

    private static void downloadSegmented(DownloadEntry entry, String url, File temporary, Probe probe,
                                          ExecutorService requestPool, HttpEngine engine) throws Exception {
        long total = probe.totalLength;
        long segmentCount = (total + RANGE_CHUNK_SIZE - 1L) / RANGE_CHUNK_SIZE;
        if (segmentCount > Integer.MAX_VALUE) {
            throw new IOException("Download requires too many range segments: " + segmentCount);
        }
        int count = (int) segmentCount;
        File segmentDirectory = new File(temporary.getPath() + ".segments");
        Files.createDirectories(segmentDirectory.toPath());
        List<File> segments = new ArrayList<>(count);
        List<SegmentTransfer> transfers = new ArrayList<>(count);
        try {
            ExecutorCompletionService<Void> completions = new ExecutorCompletionService<>(requestPool);
            int nextSegment = 0;
            int inFlight = 0;
            int maxInFlight = Math.max(1, engine.getConcurrency());
            while (nextSegment < count && inFlight < maxInFlight) {
                transfers.add(submitSegment(url, probe, segmentDirectory, segments, nextSegment++, completions, total, engine));
                inFlight++;
            }
            while (inFlight > 0) {
                Future<Void> future = completions.take();
                try {
                    future.get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw e;
                } catch (ExecutionException e) {
                    Throwable cause = unwrap(e);
                    if (cause instanceof Exception) {
                        throw (Exception) cause;
                    }
                    if (cause instanceof Error) {
                        throw (Error) cause;
                    }
                    throw new RuntimeException(cause);
                }
                inFlight--;
                if (nextSegment < count) {
                    transfers.add(submitSegment(url, probe, segmentDirectory, segments, nextSegment++, completions, total, engine));
                    inFlight++;
                }
            }
            try (OutputStream output = Files.newOutputStream(temporary.toPath())) {
                for (File segment : segments) {
                    try (InputStream input = Files.newInputStream(segment.toPath())) {
                        IOUtils.copy(input, output, 8192);
                    }
                }
            }
            if (temporary.length() != total) {
                throw new IOException("Segmented download length mismatch for " + entry.getDestination());
            }
        } finally {
            cancelAndAwait(transfers);
            for (File segment : segments) {
                Files.deleteIfExists(segment.toPath());
            }
            Files.deleteIfExists(segmentDirectory.toPath());
        }
    }

    private static SegmentTransfer submitSegment(String url, Probe probe, File segmentDirectory,
                                                 List<File> segments, int index,
                                                 ExecutorCompletionService<Void> completions,
                                                 long total, HttpEngine engine) {
        long start = index * RANGE_CHUNK_SIZE;
        long end = Math.min(total - 1L, start + RANGE_CHUNK_SIZE - 1L);
        File segment = new File(segmentDirectory, String.format("%08d.part", index));
        segments.add(segment);
        final long segmentStart = start;
        final long segmentEnd = end;
        final File segmentFile = segment;
        final SegmentTransfer transfer = new SegmentTransfer();
        Future<Void> future = completions.submit(() -> {
            if (!transfer.start()) {
                return null;
            }
            try {
                downloadSegmentWithRetry(url, segmentFile, segmentStart, segmentEnd,
                        total, probe.validator, engine, transfer);
                return null;
            } finally {
                transfer.finish();
            }
        });
        transfer.setFuture(future);
        return transfer;
    }

    private static void cancelAndAwait(List<SegmentTransfer> transfers) {
        boolean interrupted = Thread.interrupted();
        for (SegmentTransfer transfer : transfers) {
            transfer.cancel();
        }
        for (SegmentTransfer transfer : transfers) {
            for (;;) {
                try {
                    transfer.awaitStopped();
                    break;
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private static void downloadSegmentWithRetry(String url, File destination, long start, long end,
                                                 long total, String validator, HttpEngine engine,
                                                 SegmentTransfer transfer) throws IOException {
        int maxAttempts = Math.max(1, Config.maxRetry);
        IOException last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                HttpGet request = new HttpGet(url);
                request.setConfig(DOWNLOAD_REQUEST_CONFIG);
                request.setHeader("Range", "bytes=" + start + "-" + end);
                request.setHeader("If-Range", validator);
                request.setHeader("Accept-Encoding", "identity");
                transfer.setActiveRequest(request);
                try {
                    engine.execute(request, (ResponseProcessor<Void>) response -> {
                        int code = response.getCode();
                        if (code == 200 || code == 412 || code == 416) {
                            throw new RangeFallbackException("Range request returned " + code + " for " + url);
                        }
                        if (code != 206) {
                            throw HttpStatusException.from(response, url, code);
                        }
                        Header contentRange = response.getFirstHeader("Content-Range");
                        String expected = "bytes " + start + "-" + end + "/" + total;
                        if (contentRange == null || !expected.equals(contentRange.getValue().trim())) {
                            throw new RangeFallbackException("Invalid Content-Range for " + url + ": "
                                    + (contentRange == null ? "missing" : contentRange.getValue()));
                        }
                        HttpEntity entity = response.getEntity();
                        if (entity == null) {
                            throw new RangeFallbackException("Empty range response for " + url);
                        }
                        try (InputStream input = entity.getContent(); OutputStream output = Files.newOutputStream(destination.toPath())) {
                            IOUtils.copy(input, output);
                        }
                        long expectedLength = end - start + 1L;
                        if (destination.length() != expectedLength) {
                            throw new IOException("Range response length mismatch for " + url);
                        }
                        return null;
                    });
                } finally {
                    transfer.clearActiveRequest(request);
                }
                return;
            } catch (IOException e) {
                last = e;
                if (e instanceof RangeFallbackException) {
                    throw e;
                }
                if (e instanceof HttpStatusException && !((HttpStatusException) e).retryable) {
                    throw e;
                }
                if (attempt >= maxAttempts) {
                    throw new SegmentDownloadException("Range segment failed after " + maxAttempts
                            + " attempts for " + url, e);
                }
                sleepBackoff(attempt, e instanceof HttpStatusException ? ((HttpStatusException) e).retryAfterMillis : null);
            }
        }
        throw last;
    }

    private static void publishAtomically(Path temporary, Path destination) throws IOException {
        try {
            Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            throw new IOException("Atomic replacement is not supported for " + destination, e);
        }
    }

    private static void consume(HttpEntity entity) throws IOException {
        if (entity != null) {
            EntityUtils.consume(entity);
        }
    }

    private static String validatorValue(Header etag, Header lastModified) {
        if (etag != null && isStrongEntityTag(etag.getValue())) {
            return etag.getValue().trim();
        }
        if (etag != null && etag.getValue() != null && !etag.getValue().trim().isEmpty()) {
            Relauncher.LOGGER.warn("Ignoring invalid or weak ETag from Range probe: {}", etag.getValue());
        }
        if (lastModified != null && lastModified.getValue() != null && !lastModified.getValue().trim().isEmpty()) {
            return lastModified.getValue().trim();
        }
        return null;
    }

    private static boolean isStrongEntityTag(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        if (trimmed.length() < 2 || trimmed.charAt(0) != '"'
                || trimmed.charAt(trimmed.length() - 1) != '"') {
            return false;
        }
        for (int index = 1; index < trimmed.length() - 1; index++) {
            char character = trimmed.charAt(index);
            boolean valid = character == 0x21
                    || (character >= 0x23 && character <= 0x7e)
                    || (character >= 0x80 && character <= 0xff);
            if (!valid) {
                return false;
            }
        }
        return true;
    }

    private static void sleepBackoff(int attempt, Long retryAfterMillis) throws IOException {
        long delay = retryAfterMillis == null ? Math.min(MAX_BACKOFF_MILLIS, DEFAULT_BACKOFF_MILLIS << Math.min(5, Math.max(0, attempt - 1))) : retryAfterMillis;
        try {
            TimeUnit.MILLISECONDS.sleep(Math.max(0L, delay));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted during download retry backoff", e);
        }
    }

    static String downloadString(String url) throws IOException {
        final HttpEngine engine = acquireHttpEngine();
        try {
            final RaceResult winner;
            try {
                winner = selectWinner(DownloadEntry.candidateUrls(url), engine);
            } catch (Exception e) {
                if (e instanceof IOException) {
                    throw (IOException) e;
                }
                throw new IOException("Unable to select a download source for " + url, e);
            }
            final String finalUrl = winner.url;
            int maxAttempts = Math.max(1, Config.maxRetry);
            IOException last = null;
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                HttpGet request = new HttpGet(finalUrl);
                request.setConfig(DOWNLOAD_REQUEST_CONFIG);
                try {
                    return engine.execute(request, response -> {
                        int statusCode = response.getCode();
                        if (statusCode != 200) {
                            throw HttpStatusException.from(response, finalUrl, statusCode);
                        }
                        if (response.getEntity() == null) {
                            throw new IOException("Empty response entity for " + finalUrl);
                        }
                        return IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8).trim();
                    });
                } catch (HttpStatusException e) {
                    if (!e.retryable || attempt >= maxAttempts) {
                        throw e;
                    }
                    last = e;
                    sleepBackoff(attempt, e.retryAfterMillis);
                } catch (IOException e) {
                    last = e;
                    if (attempt >= maxAttempts) {
                        throw e;
                    }
                    sleepBackoff(attempt, null);
                }
            }
            throw last == null ? new IOException("Unable to download " + finalUrl) : last;
        } finally {
            releaseHttpEngine(engine);
        }
    }

    /** Executes source selection without downloading a payload, for local transport diagnostics. */
    static String selectSourceForDiagnostics(List<String> candidates) throws IOException {
        EngineConfiguration diagnosticsConfiguration = new EngineConfiguration(2, "", 0);
        final HttpEngine engine = new HttpEngine(diagnosticsConfiguration);
        final ExecutorService probePool = Executors.newFixedThreadPool(Math.max(1, candidates.size()),
                new NamedDaemonThreadFactory("Relauncher Diagnostic Probe"));
        engine.retain();
        try {
            try {
                return selectWinner(candidates, probePool, engine, 1).url;
            } catch (Exception e) {
                if (e instanceof IOException) {
                    throw (IOException) e;
                }
                throw new IOException("Unable to select a download source", e);
            }
        } finally {
            probePool.shutdown();
            shutdownRequestExecutor(probePool, engine);
            engine.retire();
            if (engine.release()) {
                engine.close();
            }
        }
    }

    private static RaceResult selectWinner(List<String> candidates, HttpEngine engine) throws Exception {
        return selectWinner(candidates, engine, Math.max(1, Config.maxRetry));
    }

    private static RaceResult selectWinner(List<String> candidates, HttpEngine engine, int maxAttempts) throws Exception {
        ExecutorService probePool = Executors.newFixedThreadPool(Math.max(1, candidates.size()),
                new NamedDaemonThreadFactory("Relauncher Source Probe"));
        try {
            return selectWinner(candidates, probePool, engine, maxAttempts);
        } finally {
            probePool.shutdown();
            shutdownRequestExecutor(probePool, engine, false);
        }
    }

    private static HttpEngine acquireHttpEngine() {
        EngineConfiguration configuration = EngineConfiguration.current();
        HttpEngine retired = null;
        HttpEngine result;
        synchronized (ENGINE_LOCK) {
            result = httpEngine;
            if (result == null || !result.matches(configuration)) {
                HttpEngine replacement = new HttpEngine(configuration);
                httpEngine = replacement;
                if (result != null && result.retire()) {
                    retired = result;
                }
                result = replacement;
            }
            result.retain();
        }
        if (retired != null) {
            retired.close();
        }
        return result;
    }

    private static void releaseHttpEngine(HttpEngine engine) {
        if (engine.release()) {
            engine.close();
        }
    }

    private static void discardHttpEngine(HttpEngine engine) {
        synchronized (ENGINE_LOCK) {
            if (httpEngine == engine) {
                httpEngine = null;
            }
            engine.retire();
        }
    }

    /** Processes one HTTP response and declares how its connection must be closed afterward. */
    private interface ResponseProcessor<T> {
        /** Reads and validates the response payload required by the current request. */
        T process(CloseableHttpResponse response) throws IOException;

        /** Keeps normal requests reusable while allowing deliberately partial probes to abort immediately. */
        default CloseMode responseCloseMode() {
            return CloseMode.GRACEFUL;
        }
    }

    private static final class EngineConfiguration {
        private final int concurrency;
        private final String proxyAddr;
        private final int proxyPort;

        private EngineConfiguration(int concurrency, String proxyAddr, int proxyPort) {
            this.concurrency = concurrency;
            this.proxyAddr = proxyAddr;
            this.proxyPort = proxyPort;
        }

        private static EngineConfiguration current() {
            String proxyAddr = Config.proxyAddr == null ? "" : Config.proxyAddr;
            return new EngineConfiguration(Math.max(1, Config.maxDownloadSession), proxyAddr, Config.proxyPort);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof EngineConfiguration)) {
                return false;
            }
            EngineConfiguration that = (EngineConfiguration) other;
            return concurrency == that.concurrency
                    && proxyPort == that.proxyPort
                    && proxyAddr.equals(that.proxyAddr);
        }

        @Override
        public int hashCode() {
            int result = concurrency;
            result = 31 * result + proxyAddr.hashCode();
            result = 31 * result + proxyPort;
            return result;
        }
    }

    private static final class TargetLock {
        private int references;
    }

    private static final class NamedDaemonThreadFactory implements ThreadFactory {
        private final String prefix;
        private final AtomicInteger sequence = new AtomicInteger();

        private NamedDaemonThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, prefix + "-" + sequence.incrementAndGet());
            thread.setDaemon(true);
            thread.setUncaughtExceptionHandler((failedThread, error) ->
                    Relauncher.LOGGER.error("Uncaught error in {}", failedThread.getName(), error));
            return thread;
        }
    }

    private static final class HttpEngine {
        private final EngineConfiguration configuration;
        private final PoolingHttpClientConnectionManager connectionManager;
        private final CloseableHttpClient client;
        private final Semaphore requests;
        private int activeRequests;
        private int references;
        private boolean retired;
        private boolean closed;

        private HttpEngine(EngineConfiguration configuration) {
            this.configuration = configuration;
            this.requests = new Semaphore(configuration.concurrency, true);
            try {
                InputStream certStream = Downloader.class.getResourceAsStream("/cacerts.jks");
                if (certStream == null) {
                    throw new IOException("Bundled trust store /cacerts.jks was not found");
                }
                KeyStore keyStore = KeyStore.getInstance("JKS");
                try (InputStream input = certStream) {
                    keyStore.load(input, "changeit".toCharArray());
                }
                SSLFactory sslFactory = SSLFactory
                        .builder()
                        .withDefaultTrustMaterial()
                        .withSystemTrustMaterial()
                        .withTrustMaterial(keyStore)
                        .withProtocols("TLSv1.0")
                        .build();
                TlsSocketStrategy tlsStrategy = new DefaultClientTlsStrategy(sslFactory.getSslContext());
                PoolingHttpClientConnectionManagerBuilder managerBuilder = PoolingHttpClientConnectionManagerBuilder
                        .create()
                        .setMaxConnTotal(configuration.concurrency)
                        .setMaxConnPerRoute(configuration.concurrency);
                if (JVMInfo.getCurrentUpdateNumber() < 341) {
                    managerBuilder.setTlsSocketStrategy(tlsStrategy);
                }
                connectionManager = managerBuilder.build();
                if (!configuration.proxyAddr.isEmpty() && configuration.proxyPort != 0) {
                    client = HttpClients.custom()
                            .setConnectionManager(connectionManager)
                            .setProxy(new HttpHost(configuration.proxyAddr, configuration.proxyPort))
                            .build();
                } else {
                    client = HttpClients.custom()
                            .setConnectionManager(connectionManager)
                            .build();
                }
            } catch (NoSuchAlgorithmException | KeyStoreException | CertificateException | IOException e) {
                throw new RuntimeException("Failed to initialize shared HTTP client", e);
            }
        }

        private int getConcurrency() {
            return configuration.concurrency;
        }

        private boolean matches(EngineConfiguration expected) {
            return configuration.equals(expected);
        }

        private synchronized void retain() {
            if (closed || retired) {
                throw new IllegalStateException("Attempted to retain a retired HTTP engine");
            }
            references++;
        }

        private synchronized boolean release() {
            if (references <= 0) {
                throw new IllegalStateException("HTTP engine reference count became negative");
            }
            references--;
            return retired && references == 0;
        }

        private synchronized boolean retire() {
            retired = true;
            return references == 0;
        }

        private synchronized void close() {
            if (closed) {
                return;
            }
            closed = true;
            try {
                client.close();
            } catch (IOException e) {
                Relauncher.LOGGER.warn("Failed to close HTTP client", e);
            }
            connectionManager.close();
        }

        private void closeConnections() {
            connectionManager.close();
        }

        private void discardIdleConnectionsAfterRangeCancellation() {
            connectionManager.closeExpired();
            connectionManager.closeIdle(TimeValue.ZERO_MILLISECONDS);
        }

        private synchronized int getActiveRequestCount() {
            return activeRequests;
        }

        private <T> T execute(HttpGet request, ResponseProcessor<T> processor) throws IOException {
            boolean acquired = false;
            boolean active = false;
            try {
                requests.acquire();
                acquired = true;
                synchronized (this) {
                    activeRequests++;
                    active = true;
                }
                CloseableHttpResponse response = client.execute(request);
                try {
                    return processor.process(response);
                } finally {
                    if (processor.responseCloseMode() == CloseMode.IMMEDIATE) {
                        response.close(CloseMode.IMMEDIATE);
                    } else {
                        response.close();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while waiting for HTTP request slot", e);
            } finally {
                if (active) {
                    synchronized (this) {
                        activeRequests--;
                    }
                }
                if (acquired) {
                    requests.release();
                }
            }
        }
    }

    private static final class Probe {
        private final long totalLength;
        private final String validator;

        private Probe(long totalLength, String validator) {
            this.totalLength = totalLength;
            this.validator = validator;
        }
    }

    private static final class RaceResult {
        private final String url;
        private final Probe probe;

        private RaceResult(String url, Probe probe) {
            this.url = url;
            this.probe = probe;
        }
    }

    private static final class RaceCandidate {
        private final HttpGet request;
        private Future<RaceResult> future;

        private RaceCandidate(HttpGet request) {
            this.request = request;
        }
    }

    private static final class SegmentTransfer {
        private final AtomicBoolean started = new AtomicBoolean();
        private final AtomicBoolean cancelled = new AtomicBoolean();
        private final AtomicReference<HttpGet> activeRequest = new AtomicReference<>();
        private final CountDownLatch stopped = new CountDownLatch(1);
        private volatile Future<Void> future;

        private void setFuture(Future<Void> submitted) {
            future = submitted;
            if (cancelled.get()) {
                cancelFuture(submitted);
            }
        }

        private boolean start() {
            return started.compareAndSet(false, true);
        }

        private void finish() {
            stopped.countDown();
        }

        private void setActiveRequest(HttpGet request) {
            activeRequest.set(request);
            if (cancelled.get()) {
                request.cancel();
            }
        }

        private void clearActiveRequest(HttpGet request) {
            activeRequest.compareAndSet(request, null);
        }

        private void cancel() {
            cancelled.set(true);
            HttpGet request = activeRequest.get();
            if (request != null) {
                request.cancel();
            }
            Future<Void> submitted = future;
            if (submitted != null) {
                cancelFuture(submitted);
            }
            request = activeRequest.get();
            if (request != null) {
                request.cancel();
            }
        }

        private void cancelFuture(Future<Void> submitted) {
            submitted.cancel(true);
            if (started.compareAndSet(false, true)) {
                stopped.countDown();
            }
        }

        private void awaitStopped() throws InterruptedException {
            stopped.await();
        }
    }

    private static final class SegmentDownloadException extends IOException {
        private SegmentDownloadException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private static final class RangeFallbackException extends IOException {
        private RangeFallbackException(String message) {
            super(message);
        }
    }

    private static final class HttpStatusException extends IOException {
        private final boolean retryable;
        private final Long retryAfterMillis;

        private HttpStatusException(String message, boolean retryable, Long retryAfterMillis) {
            super(message);
            this.retryable = retryable;
            this.retryAfterMillis = retryAfterMillis;
        }

        private static HttpStatusException from(CloseableHttpResponse response, String url, int code) {
            boolean retryable = code == 408 || code == 429 || code >= 500;
            Header retryAfter = response.getFirstHeader("Retry-After");
            Long delay = parseRetryAfter(retryAfter == null ? null : retryAfter.getValue());
            return new HttpStatusException("Unexpected HTTP status: " + code + " for " + url, retryable, delay);
        }

        private static Long parseRetryAfter(String value) {
            if (value == null || value.trim().isEmpty()) {
                return null;
            }
            String trimmed = value.trim();
            try {
                return Math.max(0L, Long.parseLong(trimmed) * 1000L);
            } catch (NumberFormatException ignored) {
                try {
                    long delay = Date.parse(trimmed) - System.currentTimeMillis();
                    return Math.max(0L, delay);
                } catch (IllegalArgumentException ignoredDate) {
                    return null;
                }
            }
        }
    }
}
