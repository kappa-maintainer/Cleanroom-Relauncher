package com.cleanroommc.relauncher;

/**
 * Controls when the relauncher shows its configuration GUI on startup.
 *
 * <p>This replaces the previous {@code Show Config} / {@code Always Show Config} boolean pair, which
 * conflated the user preference with the "already shown once" latch and had to be rewritten on every
 * launch. The latch is now derived from the installed version (see {@link VersionState}), so the
 * configuration file is never modified at runtime.
 */
public enum ShowConfigMode {

    /**
     * Show the GUI only when the instance does not run on Cleanroom yet, or when the installed
     * Cleanroom does not match the version pinned by the configuration.
     */
    WHEN_OUTDATED,

    /** Always show the configuration GUI, regardless of the installed version. */
    ALWAYS,

    /** Never show the configuration GUI. */
    NEVER;

    private static final String[] NAMES = buildNames();

    private static String[] buildNames() {
        ShowConfigMode[] modes = values();
        String[] names = new String[modes.length];
        for (int i = 0; i < modes.length; i++) {
            names[i] = modes[i].name();
        }
        return names;
    }

    /** Valid values accepted by the Forge configuration, in declaration order. */
    public static String[] names() {
        return NAMES.clone();
    }

    /** Parses a configuration value, falling back to {@link #WHEN_OUTDATED} for anything unknown. */
    public static ShowConfigMode parse(String value) {
        if (value != null) {
            String trimmed = value.trim();
            for (ShowConfigMode mode : values()) {
                if (mode.name().equalsIgnoreCase(trimmed)) {
                    return mode;
                }
            }
        }
        return WHEN_OUTDATED;
    }

    /** Localized label used by the configuration GUI. */
    public String getDisplayName() {
        return Messages.get("show_config_mode." + name());
    }
}
