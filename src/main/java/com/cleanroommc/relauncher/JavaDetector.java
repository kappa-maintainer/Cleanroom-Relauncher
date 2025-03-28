package com.cleanroommc.relauncher;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class JavaDetector {


    private static String queryRegisterValue(String location, String name) {
        boolean last = false;

        try {
            Process process = Runtime.getRuntime().exec(new String[]{"cmd", "/c", "reg", "query", location, "/v", name});
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                for (String line; (line = reader.readLine()) != null; ) {
                    if (StringUtils.isNotBlank(line)) {
                        if (last && line.trim().startsWith(name)) {
                            int begins = line.indexOf(name);
                            if (begins > 0) {
                                String s2 = line.substring(begins + name.length());
                                begins = s2.indexOf("REG_SZ");
                                if (begins > 0) {
                                    return s2.substring(begins + "REG_SZ".length()).trim();
                                }
                            }
                        }
                        if (location.equals(line.trim())) {
                            last = true;
                        }
                    }
                }
            }
        } catch (IOException e) {
            Relauncher.LOGGER.warn("Failed to query register value of {}", location, e);
        }

        return null;
    }


    private static List<String> querySubFolders(String location) {
        List<String> res = new ArrayList<>();

        try {
            Process process = Runtime.getRuntime().exec(new String[]{"cmd", "/c", "reg", "query", location});
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.defaultCharset()))) {
                for (String line; (line = reader.readLine()) != null; ) {
                    if (line.startsWith(location) && !line.equals(location)) {
                        res.add(line);
                    }
                }
            }
        } catch (IOException e) {
            Relauncher.LOGGER.warn("Failed to query sub folders of " + location, e);
        }
        return res;
    }
}
