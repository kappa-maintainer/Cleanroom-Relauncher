package com.cleanroommc.relauncher;

import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.fml.ExitWrapper;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.JavaVersion;
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.*;
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
                jDialog.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent windowEvent) {
                        synchronized (o) {
                            o.notify();
                        }
                    }
                });
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
