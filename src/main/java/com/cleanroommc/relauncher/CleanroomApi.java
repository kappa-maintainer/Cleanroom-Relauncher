package com.cleanroommc.relauncher;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;

/**
 * Client for the official Cleanroom download API.
 *
 * <p>This is the same endpoint the official Cleanroom Updater mod uses
 * ({@code https://download.cleanroommc.com/api/download}), and it replaces the previous
 * {@code maven-metadata.xml} scraping. The maven metadata is a full version list whose ordering must
 * be guessed by hand, while this endpoint reports the authoritative current release together with
 * its artifact URLs and digests.
 *
 * <p>Example response:
 * <pre>
 * {"version":"0.6.13-alpha","region":"CN",
 *  "maven_mmc":".../cleanroom-0.6.13-alpha.zip","zip_digest":"sha256:a6de95ff...",
 *  "maven_installer":".../cleanroom-0.6.13-alpha-installer.jar","installer_digest":"sha256:063561f5...",
 *  "published_at":"2026-09-12T13:25:19Z"}
 * </pre>
 */
public final class CleanroomApi {

    public static final String ENDPOINT = "https://download.cleanroommc.com/api/download";

    private CleanroomApi() {
    }

    /** Fetches the current release description. */
    public static Release fetch() throws IOException {
        String body = Downloader.downloadString(ENDPOINT);
        JsonObject json = new JsonParser().parse(body).getAsJsonObject();
        return new Release(
                required(json, "version"),
                optional(json, "maven_mmc"),
                optional(json, "maven_installer"),
                optional(json, "published_at"));
    }

    private static String required(JsonObject json, String key) throws IOException {
        if (!json.has(key) || json.get(key).isJsonNull()) {
            throw new IOException("Cleanroom API response is missing required field: " + key);
        }
        return json.get(key).getAsString();
    }

    private static String optional(JsonObject json, String key) {
        if (!json.has(key) || json.get(key).isJsonNull()) {
            return null;
        }
        return json.get(key).getAsString();
    }

    /** Immutable description of one Cleanroom release. */
    public static final class Release {
        private final String version;
        private final String mavenMmc;
        private final String mavenInstaller;
        private final String publishedAt;

        private Release(String version, String mavenMmc, String mavenInstaller, String publishedAt) {
            this.version = version;
            this.mavenMmc = mavenMmc;
            this.mavenInstaller = mavenInstaller;
            this.publishedAt = publishedAt;
        }

        public String getVersion() {
            return version;
        }

        /** Maven URL of the MMC pack zip, or {@code null} when the API did not report one. */
        public String getMavenMmc() {
            return mavenMmc;
        }

        /** Maven URL of the installer jar, or {@code null} when the API did not report one. */
        public String getMavenInstaller() {
            return mavenInstaller;
        }

        /** ISO-8601 publication timestamp, or {@code null} when the API did not report one. */
        public String getPublishedAt() {
            return publishedAt;
        }

        @Override
        public String toString() {
            return "Cleanroom " + version;
        }
    }
}
