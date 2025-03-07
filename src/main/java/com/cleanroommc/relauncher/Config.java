package com.cleanroommc.relauncher;

import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.common.config.Configuration;

import java.io.File;

public class Config {
    public static String proxy;
    public static String javaPath;
    public static String jvmArgs;
    public static boolean useLocalPack;

    public static String javaPathDefault = "";
    public static String jvmArgsDefault = "";
    public static boolean useLocalPackDefault = false;

    public static final File configFile = new File(new File(Launch.minecraftHome, "config"), "cleanroom_relauncher.cfg");
    private static final Configuration forgeConfig = new Configuration(configFile);
    private static final String categoryGeneral = "General";

    public static void syncConfig() {
        useLocalPack = forgeConfig.getBoolean("Use Local MMC Pack", categoryGeneral, useLocalPackDefault, "Search for relauncher/Cleanroom-MMC-instance-*.zip and install from there");
        proxy = forgeConfig.getString("Proxy url", categoryGeneral, "", "Proxy URL like socks5://127.0.0.1. Only socks and http are supported.");
        javaPath = forgeConfig.getString("Java Path", categoryGeneral, javaPathDefault, "Path to javaw.exe or java binary");
        jvmArgs = forgeConfig.getString("JVM Arguments", categoryGeneral, jvmArgsDefault, "Args for Java 21 jvm");

        if (forgeConfig.hasChanged()) {
            forgeConfig.save();
        }
    }
}
