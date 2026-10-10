package com.cleanroommc.relauncher;

import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.fml.ExitWrapper;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
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

    /** Main class of a Cleanroom launch, as declared by the {@code net.minecraftforge} patch. */
    private static final String CLEANROOM_MAIN_CLASS = "top.outlands.foundation.boot.Foundation";
    private static final String CLEANROOM_VERSION_CLASS = "com.cleanroommc.common.CleanroomVersion";

    public Relauncher() throws Throwable {
        StackTraceElement[] stacks = Thread.currentThread().getStackTrace();
        if (isServer(stacks)) return;

        boolean mmcLike = isMMCLike(stacks[stacks.length - 1].getClassName());
        boolean cleanroom = isRunningCleanroom();

        if (!workingDir.exists()) {
            workingDir.mkdirs();
        }
        LOGGER.info("Checking config");
        Config.syncConfig();

        boolean showConfig = shouldShowConfig(cleanroom);
        LOGGER.info("Startup decision: launcher={}, cleanroom={}, mode={}, showConfig={}, installed={}, target={}",
                mmcLike ? "mmc" : "other", cleanroom, Config.showConfigMode, showConfig,
                VersionState.installedVersion(), VersionState.targetVersion());

        if (mmcLike) {
            // MultiMC-style launchers own the launch metadata, so installing the pack into the
            // instance is the only action available; there is nothing to relaunch here.
            if (showConfig) {
                applyLookAndFeel();
                MMCInstaller.showGUI();
                ExitWrapper.exit(0);
            }
            return;
        }

        if (cleanroom) {
            // This JVM was started by the relaunch below, which already applied the pack. Running
            // the workflow again would relaunch forever, so the config GUI is never shown here.
            LOGGER.info("Already running Cleanroom, no relaunch needed");
            return;
        }

        // Everything below runs under Forge, i.e. before the switch to Cleanroom.
        if (showConfig || !canRelaunchSilently()) {
            if (!showConfig) {
                LOGGER.warn("Config GUI is disabled but the relaunch settings are incomplete; showing the config GUI instead");
            }
            applyLookAndFeel();
            Initializer.InitJavaAndArg();
        } else {
            LOGGER.info("Config GUI is disabled; relaunching with the existing settings");
        }

        Initializer.hideWindow();
        List<String> args = ArgumentGetter.getLaunchArgs();
        ProcessBuilder relaunch = new ProcessBuilder(args);
        try {
            Process p = relaunch.directory(Launch.minecraftHome).inheritIO().start();
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

        ExitWrapper.exit(0);
    }

    /**
     * Whether the configuration GUI should be shown on this launch.
     *
     * <p>{@link ShowConfigMode#WHEN_OUTDATED} needs no persisted latch: "already configured" is
     * expressed by the instance running the pinned Cleanroom version, which keeps the configuration
     * file identical across launches instead of rewriting it after every successful start.
     */
    private static boolean shouldShowConfig(boolean cleanroom) {
        switch (Config.showConfigMode) {
            case ALWAYS:
                return true;
            case NEVER:
                return false;
            case WHEN_OUTDATED:
            default:
                return !cleanroom || VersionState.isOutdated();
        }
    }

    /** Whether relaunching is possible without asking the user for Java settings first. */
    private static boolean canRelaunchSilently() {
        return !Config.javaPath.isEmpty() && !Config.classPath.isEmpty();
    }

    private static void applyLookAndFeel() throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    }

    private static boolean isMMCLike(String entry) {
        return entry.startsWith("org.prismlauncher") || entry.startsWith("org.multimc") || entry.startsWith("org.polymc");
    }

    /**
     * Whether this JVM already runs Cleanroom. The launch command is authoritative; the class check
     * is only a fallback for launchers that do not expose it through {@code sun.java.command}.
     */
    private static boolean isRunningCleanroom() {
        String command = System.getProperty("sun.java.command");
        if (command != null && !command.trim().isEmpty()) {
            String trimmed = command.trim();
            int space = trimmed.indexOf(' ');
            return CLEANROOM_MAIN_CLASS.equals(space < 0 ? trimmed : trimmed.substring(0, space));
        }
        try {
            Class.forName(CLEANROOM_VERSION_CLASS, false, Relauncher.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
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
