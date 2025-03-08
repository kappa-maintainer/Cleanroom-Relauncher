package com.cleanroommc.relauncher;

import javax.swing.*;
import java.awt.*;

public class GUIUtils {
    public static final int scaledWidth;
    public static final int scaledHeight;
    public static final int screenWidth;
    public static final int screenHeight;
    static {
        Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
        screenWidth = (int) size.getWidth();
        screenHeight = (int) size.getHeight();
        scaledWidth = (int) ((double) 854 / 1920 * size.getWidth());
        scaledHeight = (int) ((double) 480 / 1080 * size.getWidth());
    }

    public static void setCentral(Component c) {
        c.setLocation(screenWidth / 2 - c.getWidth() / 2, screenHeight / 2 - c.getHeight() / 2);
    }
}
