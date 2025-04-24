package com.cleanroommc.relauncher;

import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.fml.ExitWrapper;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ArgumentGetter {
    public static List<String> getLaunchArgs() {
        URL vanillaJar = null;
        try {
            vanillaJar = Class.forName("net.minecraft.client.ClientBrandRetriever", false, LaunchClassLoader.class.getClassLoader()).getProtectionDomain().getCodeSource().getLocation();
        } catch (ClassNotFoundException e) {
            Relauncher.LOGGER.error("Can't get vanilla jar, impossible to relaunch");
            ExitWrapper.exit(1);
        }
        Relauncher.LOGGER.info("Vanilla jar detected: {}", vanillaJar.getFile());
        List<String> result = new ArrayList<>();
        result.add(Config.javaPath);
        if (!Config.jvmArgs.isEmpty()) {
            result.addAll(Arrays.asList(Config.jvmArgs.split(" ")));
        }
        result.add("-cp");
        result.add(MMCPackParser.getClassPath() + new File(vanillaJar.getFile()));
        List<String> origin = new ArrayList<>(Arrays.asList(System.getProperty("sun.java.command").split(" ")));
        origin.remove(0);
        origin.add(0, "top.outlands.foundation.boot.Foundation");
        result.addAll(origin);
        return result;
    }
}
