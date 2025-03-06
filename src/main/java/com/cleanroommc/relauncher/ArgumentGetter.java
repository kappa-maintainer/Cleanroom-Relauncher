package com.cleanroommc.relauncher;

import net.minecraft.launchwrapper.Launch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ArgumentGetter {
    public static List<String> getLaunchArgs() {
        String vanillaJar = Launch.classLoader.getSources().stream().filter(url -> {
            String path = url.getPath();
            return  !path.contains("libraries") && path.contains("versions");
        }).findFirst().get().getPath();
        List<String> result = new ArrayList<>();
        result.add(Config.javaPath);
        if (!Config.jvmArgs.isEmpty()) {
            result.addAll(Arrays.asList(Config.jvmArgs.split(" ")));
        }
        result.add("-cp");
        result.add(MMCPackParser.cp + ":" + vanillaJar);
        List<String> origin = new ArrayList<>(Arrays.asList(System.getProperty("sun.java.command").split(" ")));
        origin.remove(0);
        origin.add(0, "top.outlands.foundation.boot.Foundation");
        result.addAll(origin);
        return result;
    }
}
