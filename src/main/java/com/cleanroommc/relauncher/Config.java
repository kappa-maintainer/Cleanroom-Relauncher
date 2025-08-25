package com.cleanroommc.relauncher;

import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.common.config.Configuration;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.file.Files;

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
    public static String replaceMavenURL;
    public static boolean booted;
    public static String classPath;



    public static final File configFile = new File(new File(Launch.minecraftHome, "config"), "cleanroom_relauncher.cfg");
    private static Configuration forgeConfig = new Configuration(configFile);
    private static final String categoryGeneral = "General";
    

    public static void syncConfig() {
        useLocalPack = forgeConfig.getBoolean("Use Local MMC Pack", categoryGeneral, false, "Search for relauncher/Cleanroom-MMC-instance-*.zip and install from there");
        proxyAddr = forgeConfig.getString("Proxy Address", categoryGeneral, "", "Proxy address");
        proxyPort = forgeConfig.getInt("Proxy Port", categoryGeneral, 0, 0, 65535, "Proxy Port");
        javaPath = forgeConfig.getString("Java Path", categoryGeneral, "", "Path to javaw.exe or java binary");
        jvmArgs = forgeConfig.getString("JVM Arguments", categoryGeneral, "", "Arguments for Java 21 jvm");
        respectLibraryStructure = forgeConfig.getBoolean("Respect Library Structure", categoryGeneral, true, "Put library files under directory of their group name");
        libraryPath = forgeConfig.getString("Library Path", categoryGeneral, "", "Library path, you may point it to launcher's library path for re-using, empty for default relauncher/");
        maxRetry = forgeConfig.getInt("Maximum Retry", categoryGeneral, 5, 1, 65535, "Maximum attempts on downloading file.");
        maxDownloadSession = forgeConfig.getInt("Maximum Session", categoryGeneral, 5, 1, 65535, "Maximum session count when downloading multiple files.");
        replaceMavenURL = forgeConfig.getString("Maven Mirror URL", categoryGeneral, "", "The custom maven mirror URL.");
        booted = forgeConfig.getBoolean("Booted once", categoryGeneral, false, "Config flag to check if this was booted, DO NOT EDIT");
        classPath = forgeConfig.getString("Class Path", categoryGeneral, "", "Class path used to launch, DO NOT EDIT");
        
        if (forgeConfig.hasChanged()) {
            forgeConfig.save();
        }
    }
    
    public static void save() {
        FileUtils.deleteQuietly(configFile);
        forgeConfig = new Configuration(configFile);
        useLocalPack = forgeConfig.getBoolean("Use Local MMC Pack", categoryGeneral, useLocalPack, "Search for relauncher/Cleanroom-MMC-instance-*.zip and install from there");
        proxyAddr = forgeConfig.getString("Proxy Address", categoryGeneral, proxyAddr, "Proxy address");
        proxyPort = forgeConfig.getInt("Proxy Port", categoryGeneral, proxyPort, 0, 65535, "Proxy Port");
        javaPath = forgeConfig.getString("Java Path", categoryGeneral, javaPath, "Path to javaw.exe or java binary");
        jvmArgs = forgeConfig.getString("JVM Arguments", categoryGeneral, jvmArgs, "Arguments for Java 21 jvm");
        respectLibraryStructure = forgeConfig.getBoolean("Respect Library Structure", categoryGeneral, respectLibraryStructure, "Put library files under directory of their group name");
        libraryPath = forgeConfig.getString("Library Path", categoryGeneral, libraryPath, "Library path, you may point it to launcher's library path for re-using, empty for default relauncher/");
        maxRetry = forgeConfig.getInt("Maximum Retry", categoryGeneral, maxRetry, 1, 65535, "Maximum attempts on downloading file.");
        maxDownloadSession = forgeConfig.getInt("Maximum Session", categoryGeneral, maxDownloadSession, 1, 65535, "Maximum session count when downloading multiple files.");
        replaceMavenURL = forgeConfig.getString("Maven Mirror URL", categoryGeneral, replaceMavenURL, "The custom maven mirror URL.");
        booted = forgeConfig.getBoolean("Booted once", categoryGeneral, booted, "Config flag to check if this was booted, DO NOT EDIT");
        classPath = forgeConfig.getString("Class Path", categoryGeneral, classPath, "Class path used to launch, DO NOT EDIT");

        if (forgeConfig.hasChanged()) {
            forgeConfig.save();
        }
    }
}
