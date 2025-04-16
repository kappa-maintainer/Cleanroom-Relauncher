package com.cleanroommc.relauncher;

import com.cleanroommc.javautils.api.JavaInstall;
import com.cleanroommc.javautils.spi.JavaLocator;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public class JavaDetector {


    public static List<JVMInfo> getInstalledJVMs() {
        List<JavaInstall> javaInstalls = new ArrayList<>();
        List<JVMInfo> out = new ArrayList<>();
        ServiceLoader<JavaLocator> providers = ServiceLoader.load(JavaLocator.class);
        for (JavaLocator locator : providers) {
            javaInstalls.addAll(locator.all());
        }
        for (JavaInstall install : javaInstalls) {
            String path = install.executable(true).getAbsolutePath();
            JVMInfo info = new JVMInfo(path);
            if (info.getSpecification() >= 21) {
                out.add(info);
            }
        }
        return out;
    }
}
