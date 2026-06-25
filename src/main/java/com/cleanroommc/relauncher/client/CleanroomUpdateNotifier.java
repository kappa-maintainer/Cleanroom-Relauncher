package com.cleanroommc.relauncher.client;

import com.cleanroommc.relauncher.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.toasts.SystemToast;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.common.ForgeVersion.Status;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.concurrent.atomic.AtomicBoolean;

@SideOnly(Side.CLIENT)
public class CleanroomUpdateNotifier {

    private static final AtomicBoolean shown = new AtomicBoolean(false);

    @SubscribeEvent
    public static void onGuiOpen(GuiOpenEvent event) {
        if (shown.get() || Config.disableUpdateToast) return;
        if (!(event.getGui() instanceof GuiMainMenu)) return;

        if (checkAndShow()) return;

        new Thread(() -> {
            for (int i = 0; i < 30; i++) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    return;
                }
                if (checkAndShow()) return;
            }
            shown.set(true);
        }, "Cleanroom Update Notifier").start();
    }

    private static boolean checkAndShow() {
        if (shown.get()) return true;

        ModContainer cleanroom = Loader.instance().getIndexedModList().get("cleanroom");
        if (cleanroom == null) return true;

        ForgeVersion.CheckResult result = ForgeVersion.getResult(cleanroom);
        if (result.status == Status.PENDING) return false;

        if (!shown.compareAndSet(false, true)) return true;
        if (result.status == Status.OUTDATED || result.status == Status.BETA_OUTDATED) {
            Minecraft.getMinecraft().addScheduledTask(() ->
                Minecraft.getMinecraft().getToastGui().add(new SystemToast(
                    SystemToast.Type.TUTORIAL_HINT,
                    new TextComponentTranslation("toast.relauncher.updatereminder.title"),
                    new TextComponentTranslation("toast.relauncher.updatereminder.subtitle")
                ))
            );
        }
        return true;
    }
}
