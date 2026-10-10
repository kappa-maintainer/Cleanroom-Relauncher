package com.cleanroommc.relauncher;

import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import org.apache.commons.io.FileUtils;

import java.io.File;

public class Config {
    public static String javaPath;
    public static String jvmArgs;
    public static boolean useLocalPack;
    public static String proxyAddr;
    public static int proxyPort;
    public static boolean respectLibraryStructure;
    public static String libraryPath;
    public static int maxRetry;
    public static int maxDownloadSession;
    public static String localPackPath;
    public static ShowConfigMode showConfigMode;
    public static String classPath;
    public static boolean disableUpdateToast;



    public static final File configFile = new File(new File(Launch.minecraftHome, "config"), "cleanroom_relauncher.cfg");
    private static Configuration forgeConfig = new Configuration(configFile);
    private static final String categoryGeneral = "General";

    /**
     * Keys from older releases that no longer exist. They are dropped on load so their stale values
     * cannot be mistaken for the current schema.
     */
    private static final String[] OBSOLETE_KEYS = { "Always Show Config", "Use Mirror" };

    private static void removeObsoleteKeys() {
        boolean changed = false;
        for (String key : OBSOLETE_KEYS) {
            if (forgeConfig.getCategory(categoryGeneral).containsKey(key)) {
                forgeConfig.getCategory(categoryGeneral).remove(key);
                changed = true;
            }
        }
        if (changed) {
            forgeConfig.save();
        }
    }

    /**
     * Drops the legacy boolean {@code Show Config} value so the enum default applies. The key itself
     * is reused by the mode, so it can only be removed while it still holds a boolean.
     */
    private static void migrateShowConfigKey() {
        Property legacy = forgeConfig.getCategory(categoryGeneral).get("Show Config");
        if (legacy == null) {
            return;
        }
        String value = legacy.getString();
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            Relauncher.LOGGER.info("Dropping legacy boolean Show Config={} in favour of {}", value, ShowConfigMode.WHEN_OUTDATED);
            forgeConfig.getCategory(categoryGeneral).remove("Show Config");
            forgeConfig.save();
        }
    }

    static Configuration getForgeConfig() {
        return forgeConfig;
    }

    public static void syncConfig() {
        removeObsoleteKeys();
        migrateShowConfigKey();
        showConfigMode = ShowConfigMode.parse(forgeConfig.getString("Show Config", categoryGeneral, ShowConfigMode.WHEN_OUTDATED.name(), Messages.get("config.show_config"), ShowConfigMode.names()));
        useLocalPack = forgeConfig.getBoolean("Use Local MMC Pack", categoryGeneral, false, Messages.get("config.use_local_pack"));
        proxyAddr = forgeConfig.getString("Proxy Address", categoryGeneral, "", Messages.get("config.proxy_addr"));
        proxyPort = forgeConfig.getInt("Proxy Port", categoryGeneral, 0, 0, 65535, Messages.get("config.proxy_port"));
        javaPath = forgeConfig.getString("Java Path", categoryGeneral, "", Messages.get("config.java_path"));
        jvmArgs = forgeConfig.getString("JVM Arguments", categoryGeneral, "-Xmx4g -Xms4g", Messages.get("config.jvm_args"));
        respectLibraryStructure = forgeConfig.getBoolean("Place in Group Name", categoryGeneral, true, Messages.get("config.respect_library_structure"));
        libraryPath = forgeConfig.getString("Library Path", categoryGeneral, "", Messages.get("config.library_path"));
        maxRetry = forgeConfig.getInt("Maximum Retry", categoryGeneral, 5, 1, 65535, Messages.get("config.max_retry"));
        maxDownloadSession = forgeConfig.getInt("Maximum Session", categoryGeneral, 24, 1, 65535, Messages.get("config.max_download_session"));
        localPackPath = forgeConfig.getString("Local MMC Pack Path", categoryGeneral, "", Messages.get("config.local_pack_path"));
        classPath = forgeConfig.getString("Class Path", categoryGeneral, "", Messages.get("config.class_path"));
        disableUpdateToast = forgeConfig.getBoolean("Disable Update Toast", categoryGeneral, false, Messages.get("config.disable_update_toast"));

        if (forgeConfig.hasChanged()) {
            forgeConfig.save();
        }
    }
    
    public static void save() {
        FileUtils.deleteQuietly(configFile);
        forgeConfig = new Configuration(configFile);
        showConfigMode = ShowConfigMode.parse(forgeConfig.getString("Show Config", categoryGeneral, showConfigMode.name(), Messages.get("config.show_config"), ShowConfigMode.names()));
        useLocalPack = forgeConfig.getBoolean("Use Local MMC Pack", categoryGeneral, useLocalPack, Messages.get("config.use_local_pack"));
        proxyAddr = forgeConfig.getString("Proxy Address", categoryGeneral, proxyAddr, Messages.get("config.proxy_addr"));
        proxyPort = forgeConfig.getInt("Proxy Port", categoryGeneral, proxyPort, 0, 65535, Messages.get("config.proxy_port"));
        javaPath = forgeConfig.getString("Java Path", categoryGeneral, javaPath, Messages.get("config.java_path"));
        jvmArgs = forgeConfig.getString("JVM Arguments", categoryGeneral, jvmArgs, Messages.get("config.jvm_args"));
        respectLibraryStructure = forgeConfig.getBoolean("Place in Group Name", categoryGeneral, respectLibraryStructure, Messages.get("config.respect_library_structure"));
        libraryPath = forgeConfig.getString("Library Path", categoryGeneral, libraryPath, Messages.get("config.library_path"));
        maxRetry = forgeConfig.getInt("Maximum Retry", categoryGeneral, maxRetry, 1, 65535, Messages.get("config.max_retry"));
        maxDownloadSession = forgeConfig.getInt("Maximum Session", categoryGeneral, maxDownloadSession, 1, 65535, Messages.get("config.max_download_session"));
        localPackPath = forgeConfig.getString("Local MMC Pack Path", categoryGeneral, localPackPath, Messages.get("config.local_pack_path"));
        classPath = forgeConfig.getString("Class Path", categoryGeneral, classPath, Messages.get("config.class_path"));
        disableUpdateToast = forgeConfig.getBoolean("Disable Update Toast", categoryGeneral, disableUpdateToast, Messages.get("config.disable_update_toast"));

        if (forgeConfig.hasChanged()) {
            forgeConfig.save();
        }
    }
}
