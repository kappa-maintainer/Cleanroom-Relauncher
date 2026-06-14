package com.cleanroommc.relauncher;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

public class Messages {
    private static final ResourceBundle bundle;

    static {
        Locale locale = Locale.getDefault();
        bundle = ResourceBundle.getBundle("com.cleanroommc.relauncher.messages", locale, new UTF8Control());
    }

    public static String get(String key) {
        return bundle.getString(key);
    }

    public static String get(String key, Object... args) {
        return MessageFormat.format(bundle.getString(key), args);
    }

    public static Locale getLocale() {
        return bundle.getLocale();
    }

    private static class UTF8Control extends ResourceBundle.Control {
        @Override
        public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
                throws IllegalAccessException, InstantiationException, IOException {
            String bundleName = toBundleName(baseName, locale);
            String resourceName = toResourceName(bundleName, "properties");
            ResourceBundle bundle = null;
            InputStream stream = null;
            if (reload) {
                URL url = loader.getResource(resourceName);
                if (url != null) {
                    URLConnection connection = url.openConnection();
                    if (connection != null) {
                        connection.setUseCaches(false);
                        stream = connection.getInputStream();
                    }
                }
            } else {
                stream = loader.getResourceAsStream(resourceName);
            }
            if (stream != null) {
                try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    bundle = new PropertyResourceBundle(reader);
                }
            }
            return bundle;
        }
    }
}
