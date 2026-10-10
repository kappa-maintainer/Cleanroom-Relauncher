package com.cleanroommc.relauncher;

import net.minecraft.launchwrapper.Launch;

import java.io.File;

/**
 * Compares the Cleanroom version the instance would launch with against the version the
 * configuration pins as its target.
 *
 * <p>Both values come from local metadata only, so the decision can be taken before the game boots
 * without any network access. Unknown versions count as outdated: showing the GUI is preferable to
 * silently launching a Cleanroom version the pack author did not pin.
 */
final class VersionState {

    private VersionState() {
    }

    /**
     * Cleanroom version currently installed for this instance, or {@code null} when it cannot be
     * determined. MultiMC-style instances keep their launch metadata next to the game directory,
     * everything else goes through the pack cached by the relauncher.
     */
    static String installedVersion() {
        try {
            String fromInstance = installedVersionFromInstance();
            if (fromInstance != null) {
                return fromInstance;
            }
            return CleanroomVersionParser.readCachedVersion();
        } catch (Exception e) {
            Relauncher.LOGGER.warn("Unable to determine the installed Cleanroom version", e);
            return null;
        }
    }

    private static String installedVersionFromInstance() {
        File instance = Launch.minecraftHome.getParentFile();
        if (instance == null) {
            return null;
        }
        File packJson = new File(instance, "mmc-pack.json");
        if (!packJson.isFile()) {
            return null;
        }
        return CleanroomVersionParser.readVersionFromMMCPackJson(packJson);
    }

    /**
     * Cleanroom version the current configuration targets, or {@code null} when it cannot be
     * determined locally. Only a pinned local MMC pack provides an offline target; the
     * remote mode always resolves "latest" over the network and therefore has no local target.
     */
    static String targetVersion() {
        if (!Config.useLocalPack || Config.localPackPath.isEmpty()) {
            return null;
        }
        try {
            return CleanroomVersionParser.readVersionFromLocalMMCPack(Config.localPackPath);
        } catch (Exception e) {
            Relauncher.LOGGER.warn("Unable to read the Cleanroom version from the local MMC pack {}", Config.localPackPath, e);
            return null;
        }
    }

    /**
     * Whether the installed Cleanroom differs from the pinned target. Downgrades count as outdated
     * as well, because the pack author's pin is authoritative for the instance.
     */
    static boolean isOutdated() {
        String target = targetVersion();
        if (target == null || target.isEmpty()) {
            return true;
        }
        String installed = installedVersion();
        return installed == null || !installed.equals(target);
    }
}
