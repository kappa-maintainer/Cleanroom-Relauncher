package com.cleanroommc.relauncher;

import org.intellij.lang.annotations.MagicConstant;

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

    public static void enlargeFont(Component component) {
        enlargeFont(component, Font.PLAIN, 20);
    }

    public static void enlargeFont(Component component, @MagicConstant(intValues = {Font.PLAIN, Font.BOLD, Font.ITALIC}) int type, int size) {
        Font font = component.getFont();
        component.setFont(new Font(font.getName(), type, size));
    }

}
