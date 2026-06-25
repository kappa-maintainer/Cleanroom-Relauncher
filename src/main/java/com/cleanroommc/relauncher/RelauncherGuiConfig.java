package com.cleanroommc.relauncher;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

@SideOnly(Side.CLIENT)
public class RelauncherGuiConfig extends GuiConfig {

    public RelauncherGuiConfig(GuiScreen parentScreen) {
        super(parentScreen,
                getConfigElements(),
                "relauncher",
                false,
                true,
                Messages.get("config.gui.title"),
                Messages.get("config.gui.hint"));
    }

    private static List<IConfigElement> getConfigElements() {
        return new ArrayList<>(new ConfigElement(Config.getForgeConfig().getCategory("General")).getChildElements());
    }

    @Override
    public void onGuiClosed() {
        if (Config.getForgeConfig().hasChanged()) {
            Config.getForgeConfig().save();
        }
        super.onGuiClosed();
    }
}
