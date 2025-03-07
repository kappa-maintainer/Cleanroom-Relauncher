package com.cleanroommc.relauncher;

import net.minecraft.launchwrapper.Launch;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ArgumentGetter {
    public static final String cpSplitter = SystemUtils.IS_OS_WINDOWS ? ";" : ":";
    public static List<String> getLaunchArgs() {
        URL vanillaJar = Launch.classLoader.getSources().stream().filter(url -> {
            String path = url.getPath();
            return  !path.contains("libraries") && path.contains("versions");
        }).findFirst().get();
        Relauncher.LOGGER.info("Vanilla jar detected: {}", vanillaJar.getFile());
        List<String> result = new ArrayList<>();
        result.add(Config.javaPath);
        if (!Config.jvmArgs.isEmpty()) {
            result.addAll(Arrays.asList(Config.jvmArgs.split(" ")));
        }
        result.add("-cp");
        result.add(MMCPackParser.cp + new File(vanillaJar.getFile()));
        List<String> origin = new ArrayList<>(Arrays.asList(System.getProperty("sun.java.command").split(" ")));
        origin.remove(0);
        origin.add(0, "top.outlands.foundation.boot.Foundation");
        result.addAll(origin);
        return result;
    }
}
