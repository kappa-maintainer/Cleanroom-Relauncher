package com.cleanroommc.relauncher;

import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.common.config.Configuration;

import java.io.File;

public class Config {
    public static String javaPath;
    public static String jvmArgs;
    public static boolean useLocalPack;
    public static String proxyAddr;
    public static int proxyPort;
    public static boolean enableMultipartDownload;
    public static boolean respectLibraryStructure;
    public static String libraryPath;

    // Defaults
    public static String javaPathDefault = "";
    public static String jvmArgsDefault = "";
    public static boolean useLocalPackDefault = false;
    public static String proxyAddrDefault = "";
    public static int proxyPortDefault = 0;
    public static boolean enableMultipartDownloadDefault = true;
    public static boolean respectLibraryStructureDefault = false;
    public static String libraryPathDefault = "";


    public static final File configFile = new File(new File(Launch.minecraftHome, "config"), "cleanroom_relauncher.cfg");
    private static final Configuration forgeConfig = new Configuration(configFile);
    private static final String categoryGeneral = "General";

    public static void syncOnly() {
        javaPath = javaPathDefault;
        jvmArgs = jvmArgsDefault;
        useLocalPack = useLocalPackDefault;
        proxyAddr = proxyAddrDefault;
        proxyPort = proxyPortDefault;
        enableMultipartDownload = enableMultipartDownloadDefault;
        respectLibraryStructure = respectLibraryStructureDefault;
        libraryPath = libraryPathDefault;

    }

    public static void syncConfig() {
        useLocalPack = forgeConfig.getBoolean("Use Local MMC Pack", categoryGeneral, useLocalPackDefault, "Search for relauncher/Cleanroom-MMC-instance-*.zip and install from there");
        proxyAddr = forgeConfig.getString("Proxy Address", categoryGeneral, proxyAddrDefault, "Proxy address");
        proxyPort = forgeConfig.getInt("Proxy Port", categoryGeneral, proxyPortDefault, 0, 65535, "Proxy Port");
        javaPath = forgeConfig.getString("Java Path", categoryGeneral, javaPathDefault, "Path to javaw.exe or java binary");
        jvmArgs = forgeConfig.getString("JVM Arguments", categoryGeneral, jvmArgsDefault, "Args for Java 21 jvm");
        enableMultipartDownload = forgeConfig.getBoolean("Enable Multi-part Download", categoryGeneral, enableMultipartDownloadDefault, "Use this to enable multi-part download");
        respectLibraryStructure = forgeConfig.getBoolean("Respect Library Structure", categoryGeneral, respectLibraryStructureDefault, "Put library files under directory of their group name");
        libraryPath = forgeConfig.getString("Library Path", categoryGeneral, libraryPathDefault, "Library path, you may point it to launcher's library path for re-using, empty for default relauncher/");

        if (forgeConfig.hasChanged()) {
            forgeConfig.save();
        }
    }
}
