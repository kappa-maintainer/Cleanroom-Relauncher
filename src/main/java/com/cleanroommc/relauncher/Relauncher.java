package com.cleanroommc.relauncher;

import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.apache.commons.lang3.JavaVersion;
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Map;

public class Relauncher implements IFMLLoadingPlugin {

    public static final File workingDir = new File(Launch.minecraftHome, "relauncher");
    public static final Logger LOGGER = LogManager.getLogger("cleanroom_relauncher");

    public Relauncher() {
        if (!SystemUtils.isJavaVersionAtLeast(JavaVersion.JAVA_9)) { // Java 9 shouldn't be possible on Forge
            StackTraceElement[] stacks = Thread.currentThread().getStackTrace();
            String entry = stacks[stacks.length - 1].getClassName();
            Config.syncConfig();
            if (!workingDir.exists()) {
                workingDir.mkdirs();
            }
            if (entry.startsWith("org.prismlauncher") || entry.startsWith("org.multimc") || entry.startsWith("org.polymc")) {
                // MMC-based, start installation
            } else {
                MMCPackSetup.downloadAndParse();


            }
        }
        // Do nothing on Java 9+
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    public String getModContainerClass() {
        return "";
    }

    @Nullable
    @Override
    public String getSetupClass() {
        return "";
    }

    @Override
    public void injectData(Map<String, Object> data) {

    }

    @Override
    public String getAccessTransformerClass() {
        return "";
    }
}
