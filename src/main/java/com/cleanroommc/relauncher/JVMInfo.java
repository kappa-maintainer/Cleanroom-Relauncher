package com.cleanroommc.relauncher;

import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class JVMInfo {
    private File file = null;
    private int specification = 0;
    private String arch;
    private String version;
    private String vendor;
    public JVMInfo(@Nonnull String path) {
        if (Strings.isNullOrEmpty(path)) return;
        this.file = new File(path);
        ProcessBuilder builder = new ProcessBuilder(path, "-XshowSettings:properties");
        try {
            Process p = builder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            List<String> output = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                output.add(line);
            }
            reader.close();
            for (String s : output) {
                s = StringUtils.deleteWhitespace(s);
                if (s.startsWith("java.specification.version=")) {
                    s = s.substring(s.indexOf("=") + 1);
                    if (s.startsWith("1.")) {
                        specification = Integer.parseInt(s.substring(s.indexOf(".") + 1));
                    } else {
                        specification = Integer.parseInt(s);
                    }
                }
                if (s.startsWith("java.version=")) {
                    s = s.substring(s.indexOf("=") + 1);
                    version = s;
                }
                if (s.startsWith("java.vm.vendor=")) {
                    s = s.substring(s.indexOf("=") + 1);
                    vendor = s;
                }
                if (s.startsWith("os.arch=")) {
                    s = s.substring(s.indexOf("=") + 1);
                    arch = s;
                }
                if (s.contains("options")) break;
            }
        } catch (IOException e) {
            Relauncher.LOGGER.error(e);
        }
    }

    public File getFile() {
        return file;
    }

    public int getSpecification() {
        return specification;
    }

    public String getArch() {
        return arch;
    }

    public String getVersion() {
        return version;
    }

    public String getVendor() {
        return vendor;
    }

    @Override
    public String toString() {
        return specification + "|" + arch + "|" + version + "|" + vendor;
    }
    
    public static JVMInfo getCurrent() {
        String bin = System.getProperty("java.home")  + File.separatorChar + "bin" + File.separatorChar;
        File binary;
        if (SystemUtils.IS_OS_WINDOWS) {
            binary = new File(bin + "javaw.exe");
            if (!binary.exists()) {
                binary = new File(bin + "java.exe");
            }
        } else {
            binary = new File(bin + "java");
        }
        return new JVMInfo(binary.getAbsolutePath());
    }
    
    public static int getCurrentUpdateNumber() {
        String version = getCurrent().getVersion();
        if (version.contains("_")) {
            return Integer.parseInt(version.split("_")[1]);
        } else {
            return 0;
        }
    }
}
