package com.cleanroommc.relauncher;

import io.github.toolfactory.jvm.Driver;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.JavaVersion;
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@IFMLLoadingPlugin.MCVersion("1.12.2")
public class Relauncher implements IFMLLoadingPlugin {

    public static final File workingDir = new File(Launch.minecraftHome, "relauncher");
    public static final Logger LOGGER = LogManager.getLogger("cleanroom_relauncher");
    static final Object o = new Object();
    private static final MethodHandle exit;

    static {
        try {
            // For FMLSecurityManager
            exit = MethodHandles.lookup().findStatic(System.class, "exit", MethodType.methodType(void.class, int.class));
            Driver driver = Driver.Factory.getNew();
            Field security = Arrays.stream(driver.getDeclaredFields(System.class)).filter(field -> field.getName().equals("security")).findFirst().get();
            driver.setFieldValue(null, security, null);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public Relauncher() throws Throwable {
        if (!SystemUtils.isJavaVersionAtLeast(JavaVersion.JAVA_9)) { // Java 9 shouldn't be possible on Forge
            StackTraceElement[] stacks = Thread.currentThread().getStackTrace();
            String entry = stacks[stacks.length - 1].getClassName();
            if (!workingDir.exists()) {
                workingDir.mkdirs();
            }
            if (entry.startsWith("org.prismlauncher") || entry.startsWith("org.multimc") || entry.startsWith("org.polymc")) {
                Config.syncConfig();
                // MMC-based, start installation
                LOGGER.info("MMC detected");
                JDialog jDialog = new JDialog();
                jDialog.setLayout(new GridLayout());
                jDialog.add(new JLabel("MMC-based launcher detected, will install Cleanroom MMC pack"));
                jDialog.pack();
                jDialog.setAlwaysOnTop(true);
                GUIUtils.setCentral(jDialog);
                jDialog.addWindowListener(new ResumeListener());
                jDialog.setVisible(true);
                synchronized (o) {
                    try {
                        o.wait();
                    } catch (InterruptedException e) {
                        LOGGER.error(e);
                    }
                }
                MMCPackDownloader.downloadAndExtract();
                File packDir = new File(workingDir, "mmcpack");
                File instance = Launch.minecraftHome.getParentFile();
                File libraries = new File(instance, "libraries");
                File patches = new File(instance, "patches");
                libraries.mkdirs();
                patches.mkdirs();
                try {
                    FileUtils.copyDirectory(new File(packDir, "libraries"), libraries);
                    FileUtils.copyDirectory(new File(packDir, "patches"), patches);
                    FileUtils.copyFile(new File(packDir, "mmc-pack.json"), new File(instance, "mmc-pack.json"));
                } catch (IOException e) {
                    LOGGER.error("Error while copying mmc pack", e);
                }
                LOGGER.info("Install finish, please restart instance in Java 21");

            } else {
                LOGGER.info("Checking config");
                if (!Files.exists(new File(new File(Launch.minecraftHome, "config"), "cleanroom_relauncher.cfg").toPath())) {
                    LOGGER.info("No config found, asking for input");
                    Initializer.InitJavaAndArg();
                }
                Config.syncConfig();
                MMCPackDownloader.downloadAndExtract();
                MMCPackParser.parseMMCPack();
                List<String> args = ArgumentGetter.getLaunchArgs();
                ProcessBuilder relaunch = new ProcessBuilder(args);
                try {

                    Process p = relaunch.directory(Launch.minecraftHome).inheritIO().start();

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
                    exit.invoke(1);
                }


            }
            exit.invoke(0);
        }
        // Do nothing on Java 9+
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

    public static class ResumeListener implements WindowListener {
        @Override
        public void windowOpened(WindowEvent windowEvent) {

        }

        @Override
        public void windowClosing(WindowEvent windowEvent) {
            o.notify();
        }

        @Override
        public void windowClosed(WindowEvent windowEvent) {

        }

        @Override
        public void windowIconified(WindowEvent windowEvent) {

        }

        @Override
        public void windowDeiconified(WindowEvent windowEvent) {

        }

        @Override
        public void windowActivated(WindowEvent windowEvent) {

        }

        @Override
        public void windowDeactivated(WindowEvent windowEvent) {

        }
    }
}
