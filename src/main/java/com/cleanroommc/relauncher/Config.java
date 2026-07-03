package com.cleanroommc.relauncher;

import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.common.config.Configuration;
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
    public static boolean useMirror;
    public static String localPackPath;
    public static boolean showConfigGUI;
    public static boolean alwaysShowConfigGUI;
    public static String classPath;
    public static boolean disableUpdateToast;



    public static final File configFile = new File(new File(Launch.minecraftHome, "config"), "cleanroom_relauncher.cfg");
    private static Configuration forgeConfig = new Configuration(configFile);
    private static final String categoryGeneral = "General";

    static Configuration getForgeConfig() {
        return forgeConfig;
    }

    public static void syncConfig() {
        showConfigGUI = forgeConfig.getBoolean("Show Config", categoryGeneral, true, Messages.get("config.show_config_gui"));
        useLocalPack = forgeConfig.getBoolean("Use Local MMC Pack", categoryGeneral, false, Messages.get("config.use_local_pack"));
        proxyAddr = forgeConfig.getString("Proxy Address", categoryGeneral, "", Messages.get("config.proxy_addr"));
        proxyPort = forgeConfig.getInt("Proxy Port", categoryGeneral, 0, 0, 65535, Messages.get("config.proxy_port"));
        javaPath = forgeConfig.getString("Java Path", categoryGeneral, "", Messages.get("config.java_path"));
        jvmArgs = forgeConfig.getString("JVM Arguments", categoryGeneral, "-Xmx4g -Xms4g", Messages.get("config.jvm_args"));
        respectLibraryStructure = forgeConfig.getBoolean("Place in Group Name", categoryGeneral, true, Messages.get("config.respect_library_structure"));
        libraryPath = forgeConfig.getString("Library Path", categoryGeneral, "", Messages.get("config.library_path"));
        maxRetry = forgeConfig.getInt("Maximum Retry", categoryGeneral, 5, 1, 65535, Messages.get("config.max_retry"));
        maxDownloadSession = forgeConfig.getInt("Maximum Session", categoryGeneral, 5, 1, 65535, Messages.get("config.max_download_session"));
        useMirror = forgeConfig.getBoolean("Use Mirror", categoryGeneral, false, Messages.get("config.use_mirror"));
        alwaysShowConfigGUI = forgeConfig.getBoolean("Always Show Config", categoryGeneral, false, Messages.get("config.always_show_config"));
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
        showConfigGUI = forgeConfig.getBoolean("Show Config", categoryGeneral, showConfigGUI, Messages.get("config.show_config_gui"));
        useLocalPack = forgeConfig.getBoolean("Use Local MMC Pack", categoryGeneral, useLocalPack, Messages.get("config.use_local_pack"));
        proxyAddr = forgeConfig.getString("Proxy Address", categoryGeneral, proxyAddr, Messages.get("config.proxy_addr"));
        proxyPort = forgeConfig.getInt("Proxy Port", categoryGeneral, proxyPort, 0, 65535, Messages.get("config.proxy_port"));
        javaPath = forgeConfig.getString("Java Path", categoryGeneral, javaPath, Messages.get("config.java_path"));
        jvmArgs = forgeConfig.getString("JVM Arguments", categoryGeneral, jvmArgs, Messages.get("config.jvm_args"));
        respectLibraryStructure = forgeConfig.getBoolean("Place in Group Name", categoryGeneral, respectLibraryStructure, Messages.get("config.respect_library_structure"));
        libraryPath = forgeConfig.getString("Library Path", categoryGeneral, libraryPath, Messages.get("config.library_path"));
        maxRetry = forgeConfig.getInt("Maximum Retry", categoryGeneral, maxRetry, 1, 65535, Messages.get("config.max_retry"));
        maxDownloadSession = forgeConfig.getInt("Maximum Session", categoryGeneral, maxDownloadSession, 1, 65535, Messages.get("config.max_download_session"));
        useMirror = forgeConfig.getBoolean("Use Mirror", categoryGeneral, useMirror, Messages.get("config.use_mirror"));
        alwaysShowConfigGUI = forgeConfig.getBoolean("Always Show Config", categoryGeneral, alwaysShowConfigGUI, Messages.get("config.always_show_config"));
        localPackPath = forgeConfig.getString("Local MMC Pack Path", categoryGeneral, localPackPath, Messages.get("config.local_pack_path"));
        classPath = forgeConfig.getString("Class Path", categoryGeneral, classPath, Messages.get("config.class_path"));
        disableUpdateToast = forgeConfig.getBoolean("Disable Update Toast", categoryGeneral, disableUpdateToast, Messages.get("config.disable_update_toast"));

        if (forgeConfig.hasChanged()) {
            forgeConfig.save();
        }
    }
}
