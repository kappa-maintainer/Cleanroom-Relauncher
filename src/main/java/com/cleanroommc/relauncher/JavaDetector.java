package com.cleanroommc.relauncher;

import com.cleanroommc.javautils.api.JavaInstall;
import com.cleanroommc.javautils.spi.JavaLocator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

public class JavaDetector {
    private static final List<JavaInstall> javaInstalls = new ArrayList<>();


    public static List<JVMInfo> getInstalledJVMs() {
        List<JVMInfo> out = new ArrayList<>();
        Set<String> javaPaths = new HashSet<>();
        if (javaInstalls.isEmpty()) {
            ServiceLoader<JavaLocator> providers = ServiceLoader.load(JavaLocator.class);
            for (JavaLocator locator : providers) {
                javaInstalls.addAll(locator.all());
            }
        }
        for (JavaInstall install : javaInstalls) {
            try {
                String path = install.executable(true).getCanonicalPath();
                if (!javaPaths.contains(path)) {
                    javaPaths.add(path);
                    JVMInfo info = new JVMInfo(path);
                    if (info.getSpecification() >= 21) {
                        out.add(info);
                    }
                }
            } catch (IOException ignored) {
            }
        }
        return out;
    }
}
