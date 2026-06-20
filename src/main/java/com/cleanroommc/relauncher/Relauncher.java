package com.cleanroommc.relauncher;

import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.fml.ExitWrapper;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.apache.commons.lang3.JavaVersion;
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import javax.swing.UIManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

@IFMLLoadingPlugin.MCVersion("1.12.2")
public class Relauncher implements IFMLLoadingPlugin {

    public static final File workingDir = new File(Launch.minecraftHome, "relauncher");
    public static final Logger LOGGER = LogManager.getLogger("cleanroom_relauncher");
    static final Object o = new Object();

    public Relauncher() throws Throwable {
        if (!SystemUtils.isJavaVersionAtLeast(JavaVersion.JAVA_9)) { // Java 9 shouldn't be possible on Forge
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            StackTraceElement[] stacks = Thread.currentThread().getStackTrace();
            if (isServer(stacks)) return;
            String entry = stacks[stacks.length - 1].getClassName();
            if (!workingDir.exists()) {
                workingDir.mkdirs();
            }
            LOGGER.info("Checking config");
            Config.syncConfig();
            if (entry.startsWith("org.prismlauncher") || entry.startsWith("org.multimc") || entry.startsWith("org.polymc")) {
                MMCInstaller.showGUI();
            } else {
            if (Config.alwaysShowConfigGUI || Config.showConfigGUI) {
                Initializer.InitJavaAndArg();
            }
            Initializer.hideWindow();
            List<String> args = ArgumentGetter.getLaunchArgs();
            ProcessBuilder relaunch = new ProcessBuilder(args);
            try {
                Process p = relaunch.directory(Launch.minecraftHome).inheritIO().start();
                if (!Config.alwaysShowConfigGUI) {
                    Config.showConfigGUI = false;
                }
                Config.save();
                    Runtime.getRuntime().addShutdownHook(new Thread(p::destroy));
                    BufferedReader inputReader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                    String line;
                    while (p.isAlive()) {
                        while ((line = inputReader.readLine()) != null) {
                            LOGGER.info(line);
                        }
                        while ((line = errorReader.readLine()) != null) {
                            LOGGER.info(line);
                        }
                    }
                } catch (IOException e) {
                    LOGGER.info("Launch failed: ", e);
                    ExitWrapper.exit(1);
                }


            }
            ExitWrapper.exit(0);
        } else {
            Messages.getLocale();
        }
    }

    private static boolean isServer(StackTraceElement[] stack) {
        for (StackTraceElement e : stack) {
            if (e.getClassName().equals("net.minecraftforge.fml.relauncher.ServerLaunchWrapper")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String[] getASMTransformerClass() {
        return null;
    }

    @Override
    public String getModContainerClass() {
        return "com.cleanroommc.relauncher.RelauncherModContainer";
    }

    @Nullable
    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {

    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }

}
