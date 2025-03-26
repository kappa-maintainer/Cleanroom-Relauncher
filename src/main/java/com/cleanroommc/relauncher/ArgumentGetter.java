package com.cleanroommc.relauncher;

import net.minecraft.launchwrapper.Launch;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ArgumentGetter {
    public static List<String> getLaunchArgs() {
        URL vanillaJar = Launch.classLoader.getSources().stream().filter(url -> {
            String path = url.getPath();
            return  !path.contains("libraries") && !path.contains("files-2.1") && (path.contains("versions") || path.endsWith("recompiled_minecraft-1.12.2.jar"));
        }).findFirst().get();
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
