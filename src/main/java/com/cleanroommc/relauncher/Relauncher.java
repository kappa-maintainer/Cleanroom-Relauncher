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
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

@IFMLLoadingPlugin.MCVersion("1.12.2")
public class Relauncher implements IFMLLoadingPlugin {

    public static final File workingDir = new File(Launch.minecraftHome, "relauncher");
    public static final Logger LOGGER = LogManager.getLogger("cleanroom_relauncher");
    static final Object o = new Object();

    public Relauncher() throws Throwable {
        if (!SystemUtils.isJavaVersionAtLeast(JavaVersion.JAVA_9)) { // Java 9 shouldn't be possible on Forge
            CertFixer.fixCert();
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            StackTraceElement[] stacks = Thread.currentThread().getStackTrace();
            if (isServer(stacks)) return;
            String entry = stacks[stacks.length - 1].getClassName();
            if (!workingDir.exists()) {
                workingDir.mkdirs();
            }
            if (entry.startsWith("org.prismlauncher") || entry.startsWith("org.multimc") || entry.startsWith("org.polymc")) {
                MMCInstaller.showGUI();
            } else {
                LOGGER.info("Checking config");
                if (!Files.exists(new File(new File(Launch.minecraftHome, "config"), "cleanroom_relauncher.cfg").toPath())) {
                    LOGGER.info("No config found, asking for input");
                    Initializer.InitJavaAndArg();
                    Config.syncConfig();
                } else {
                    Config.syncConfig();
                    MMCPackParser.parseMMCPack();
                }
                if (Config.javaPath.isEmpty()) {
                    LOGGER.info("Java path empty, resetting");
                    Files.delete(Config.configFile.toPath());
                    Initializer.InitJavaAndArg();
                }
                Initializer.hideWindow();
                List<String> args = ArgumentGetter.getLaunchArgs();
                ProcessBuilder relaunch = new ProcessBuilder(args);
                try {

                    Process p = relaunch.directory(Launch.minecraftHome).inheritIO().start();
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
        }
        // Do nothing on Java 9+
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
        return null;
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
