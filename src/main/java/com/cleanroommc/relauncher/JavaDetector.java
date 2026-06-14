package com.cleanroommc.relauncher;

import com.cleanroommc.javautils.api.JavaInstall;
import com.cleanroommc.javautils.spi.JavaLocator;

import java.io.IOException;
import java.nio.file.Path;
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
            Path exec = install.executable(true);
            String path = exec.toString();
            String dedupKey;
            try {
                dedupKey = exec.toRealPath().toString();
            } catch (IOException e) {
                dedupKey = path;
            }
            if (!javaPaths.contains(dedupKey)) {
                javaPaths.add(dedupKey);
                JVMInfo info = new JVMInfo(path);
                if (info.getSpecification() >= 21) {
                    out.add(info);
                }
            }
        }
        return out;
    }
}
