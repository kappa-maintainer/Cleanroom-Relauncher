package com.cleanroommc.relauncher;

import io.netty.util.internal.logging.Slf4JLoggerFactory;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.JavaVersion;
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.io.IoBuilder;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;

@IFMLLoadingPlugin.MCVersion("1.12.2")
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
            if (Config.javaPath.isEmpty()) {
                LOGGER.warn("Config file created, now fill your java 21 path");
                FMLCommonHandler.instance().exitJava(0, true);
            }
            if (entry.startsWith("org.prismlauncher") || entry.startsWith("org.multimc") || entry.startsWith("org.polymc")) {
                // MMC-based, start installation
                LOGGER.info("MMC detected");
                MMCPackDownloader.downloadAndExtract();
                File packDir = new File(workingDir, "mmcpack");
                File[] fileList = packDir.listFiles();
                if (fileList != null) {
                    for (File file: fileList) {
                        if (!file.getName().equals(".packignore") && !file.getName().equals("instance.cfg")) {
                            try {
                                Files.copy(file.toPath(), Paths.get(Launch.minecraftHome.getParentFile().toPath() + File.pathSeparator + file.getAbsolutePath().replace(workingDir.getAbsolutePath(), "")));
                            } catch (IOException e) {
                                LOGGER.error("Error while copying mmc pack", e);
                            }
                        }
                    }
                }
                LOGGER.info("Install finish, please restart instance in Java 21");

            } else {
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
                    FMLCommonHandler.instance().exitJava(1, false);
                }
                FMLCommonHandler.instance().exitJava(0, false);




            }
            //FMLCommonHandler.instance().exitJava(0, true);
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
