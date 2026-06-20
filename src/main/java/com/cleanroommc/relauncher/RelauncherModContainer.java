package com.cleanroommc.relauncher;

import com.google.common.eventbus.EventBus;
import net.minecraftforge.fml.common.DummyModContainer;
import net.minecraftforge.fml.common.LoadController;
import net.minecraftforge.fml.common.ModMetadata;

import java.util.Arrays;
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
        return true;
    }

    @Override
    public String getGuiClassName() {
        return "com.cleanroommc.relauncher.RelauncherGuiFactory";
    }
}
