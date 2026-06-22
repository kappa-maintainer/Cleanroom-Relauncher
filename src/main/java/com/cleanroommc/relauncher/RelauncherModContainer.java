package com.cleanroommc.relauncher;

import com.cleanroommc.relauncher.client.CleanroomUpdateNotifier;
import com.google.common.eventbus.EventBus;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.FMLFileResourcePack;
import net.minecraftforge.fml.common.DummyModContainer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.LoadController;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModMetadata;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;

public class RelauncherModContainer extends DummyModContainer {

    public RelauncherModContainer() {
        super(new ModMetadata());
        ModMetadata meta = getMetadata();
        meta.modId = "relauncher";
        meta.name = "Cleanroom Relauncher";
        meta.description = "Relaunches Minecraft 1.12.2 with Java 21+ for Cleanroom compatibility.";
        meta.version = "0.6.0";
        meta.authorList = Collections.singletonList("kappa_maintainer");
    }

    @Override
    public boolean registerBus(EventBus bus, LoadController controller) {
        if (FMLCommonHandler.instance().getSide().isClient()) {
            Loader.instance().setActiveModContainer(this);
            try {
                MinecraftForge.EVENT_BUS.register(CleanroomUpdateNotifier.class);
            } finally {
                Loader.instance().setActiveModContainer(null);
            }
        }
        return true;
    }

    @Override
    public String getGuiClassName() {
        return "com.cleanroommc.relauncher.RelauncherGuiFactory";
    }

    @Override
    public File getSource() {
        try {
            URL location = getClass().getProtectionDomain().getCodeSource().getLocation();
            String path = location.toString();
            if (path.startsWith("jar")) {
                JarURLConnection connection = (JarURLConnection) location.openConnection();
                connection.setUseCaches(false);
                return new File(connection.getJarFileURL().toURI());
            } else if (path.startsWith("file") && path.endsWith(".class")) {
                return new File(URI.create(path.substring(0, path.lastIndexOf('/'))).getPath());
            } else {
                return new File(location.toURI());
            }
        } catch (URISyntaxException | IOException e) {
            return null;
        }
    }

    @Override
    public Class<?> getCustomResourcePackClass() {
        return FMLFileResourcePack.class;
    }
}
