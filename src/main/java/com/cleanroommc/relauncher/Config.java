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
    public static boolean chineseMode;
    public static boolean showConfigGUI;
    public static boolean alwaysShowConfigGUI;
    public static String classPath;



    public static final File configFile = new File(new File(Launch.minecraftHome, "config"), "cleanroom_relauncher.cfg");
    private static Configuration forgeConfig = new Configuration(configFile);
    private static final String categoryGeneral = "General";

    static Configuration getForgeConfig() {
        return forgeConfig;
    }

    public static void syncConfig() {
        showConfigGUI = forgeConfig.getBoolean(Messages.get("config.name.show_config_gui"), categoryGeneral, true, Messages.get("config.show_config_gui"));
        useLocalPack = forgeConfig.getBoolean(Messages.get("config.name.use_local_pack"), categoryGeneral, false, Messages.get("config.use_local_pack"));
        proxyAddr = forgeConfig.getString(Messages.get("config.name.proxy_addr"), categoryGeneral, "", Messages.get("config.proxy_addr"));
        proxyPort = forgeConfig.getInt(Messages.get("config.name.proxy_port"), categoryGeneral, 0, 0, 65535, Messages.get("config.proxy_port"));
        javaPath = forgeConfig.getString(Messages.get("config.name.java_path"), categoryGeneral, "", Messages.get("config.java_path"));
        jvmArgs = forgeConfig.getString(Messages.get("config.name.jvm_args"), categoryGeneral, "-Xmx4g -Xms4g", Messages.get("config.jvm_args"));
        respectLibraryStructure = forgeConfig.getBoolean(Messages.get("config.name.respect_library_structure"), categoryGeneral, true, Messages.get("config.respect_library_structure"));
        libraryPath = forgeConfig.getString(Messages.get("config.name.library_path"), categoryGeneral, "", Messages.get("config.library_path"));
        maxRetry = forgeConfig.getInt(Messages.get("config.name.max_retry"), categoryGeneral, 5, 1, 65535, Messages.get("config.max_retry"));
        maxDownloadSession = forgeConfig.getInt(Messages.get("config.name.max_download_session"), categoryGeneral, 5, 1, 65535, Messages.get("config.max_download_session"));
        chineseMode = forgeConfig.getBoolean(Messages.get("config.name.chinese_mode"), categoryGeneral, false, Messages.get("config.chinese_mode"));
        alwaysShowConfigGUI = forgeConfig.getBoolean(Messages.get("config.name.always_show_config"), categoryGeneral, false, Messages.get("config.always_show_config"));
        classPath = forgeConfig.getString(Messages.get("config.name.class_path"), categoryGeneral, "", Messages.get("config.class_path"));
        
        if (forgeConfig.hasChanged()) {
            forgeConfig.save();
        }
    }
    
    public static void save() {
        FileUtils.deleteQuietly(configFile);
        forgeConfig = new Configuration(configFile);
        showConfigGUI = forgeConfig.getBoolean(Messages.get("config.name.show_config_gui"), categoryGeneral, showConfigGUI, Messages.get("config.show_config_gui"));
        useLocalPack = forgeConfig.getBoolean(Messages.get("config.name.use_local_pack"), categoryGeneral, useLocalPack, Messages.get("config.use_local_pack"));
        proxyAddr = forgeConfig.getString(Messages.get("config.name.proxy_addr"), categoryGeneral, proxyAddr, Messages.get("config.proxy_addr"));
        proxyPort = forgeConfig.getInt(Messages.get("config.name.proxy_port"), categoryGeneral, proxyPort, 0, 65535, Messages.get("config.proxy_port"));
        javaPath = forgeConfig.getString(Messages.get("config.name.java_path"), categoryGeneral, javaPath, Messages.get("config.java_path"));
        jvmArgs = forgeConfig.getString(Messages.get("config.name.jvm_args"), categoryGeneral, jvmArgs, Messages.get("config.jvm_args"));
        respectLibraryStructure = forgeConfig.getBoolean(Messages.get("config.name.respect_library_structure"), categoryGeneral, respectLibraryStructure, Messages.get("config.respect_library_structure"));
        libraryPath = forgeConfig.getString(Messages.get("config.name.library_path"), categoryGeneral, libraryPath, Messages.get("config.library_path"));
        maxRetry = forgeConfig.getInt(Messages.get("config.name.max_retry"), categoryGeneral, maxRetry, 1, 65535, Messages.get("config.max_retry"));
        maxDownloadSession = forgeConfig.getInt(Messages.get("config.name.max_download_session"), categoryGeneral, maxDownloadSession, 1, 65535, Messages.get("config.max_download_session"));
        chineseMode = forgeConfig.getBoolean(Messages.get("config.name.chinese_mode"), categoryGeneral, chineseMode, Messages.get("config.chinese_mode"));
        alwaysShowConfigGUI = forgeConfig.getBoolean(Messages.get("config.name.always_show_config"), categoryGeneral, alwaysShowConfigGUI, Messages.get("config.always_show_config"));
        classPath = forgeConfig.getString(Messages.get("config.name.class_path"), categoryGeneral, classPath, Messages.get("config.class_path"));

        if (forgeConfig.hasChanged()) {
            forgeConfig.save();
        }
    }
}
