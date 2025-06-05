package com.cleanroommc.relauncher;

import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.fml.ExitWrapper;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ArgumentGetter {
    public static List<String> getLaunchArgs() {
        URI vanillaJar = null;
        try {
            vanillaJar = new URI(Class.forName("net.minecraft.client.ClientBrandRetriever", false, LaunchClassLoader.class.getClassLoader()).getProtectionDomain().getCodeSource().getLocation().toString());
        } catch (ClassNotFoundException | URISyntaxException e) {
            Relauncher.LOGGER.error("Can't get vanilla jar, impossible to relaunch");
            ExitWrapper.exit(1);
        }
        Relauncher.LOGGER.info("Vanilla jar detected: {}", vanillaJar.getPath());
        List<String> result = new ArrayList<>();
        result.add(Config.javaPath);
        if (!Config.jvmArgs.isEmpty()) {
            result.addAll(Arrays.asList(Config.jvmArgs.split(" ")));
        }
        result.add("-cp");
        result.add(MMCPackParser.getClassPath() + new File(vanillaJar.getPath()));
        String[] args = System.getProperty("sun.java.command").split(" -");
        List<String> origin = new ArrayList<>();
        origin.add(0, "top.outlands.foundation.boot.Foundation");
        for (String pair : args) {
            if (pair.startsWith("-")) {
                pair = "-" + pair;
                int cut = pair.indexOf(" ");
                origin.add(pair.substring(0, cut));
                origin.add(pair.substring(cut + 1));

            }
        }
        result.addAll(origin);
        return result;
    }
}
