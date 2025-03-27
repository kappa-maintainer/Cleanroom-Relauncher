package com.cleanroommc.relauncher;

import net.minecraft.launchwrapper.Launch;
import org.apache.commons.io.IOUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CertFixer {
    public static void addLetsEncryptCertificate() throws Exception
    {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        Path ksPath = Paths.get(System.getProperty("java.home"),"lib", "security", "cacerts");
        keyStore.load(Files.newInputStream(ksPath), "changeit".toCharArray());

        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        String[] certs = new String[] {"e5-cross.der", "e6-cross.der", "isrg-root-x1-cross-signed.der", "isrg-root-x2-cross-signed.der", "r10.der", "r11.der"};

        for (String cert: certs){
            InputStream certStream = CertFixer.class.getResourceAsStream(cert);

            InputStream caInput = new BufferedInputStream(certStream);
            Certificate crt = cf.generateCertificate(caInput);

            keyStore.setCertificateEntry(cert.split("\\.")[0], crt);
        }

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore);
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), null);
        SSLContext.setDefault(sslContext);
    }

    public static void fixCert()
    {
        String version = System.getProperty("java.version");
        Pattern p = Pattern.compile("^(\\d+\\.\\d+).*?_(\\d+).*");
        Matcher matcher = p.matcher(version);
        String majorVersion;
        int minorVersion;
        if (matcher.matches())
        {
            majorVersion = matcher.group(1);
            minorVersion = Integer.parseInt(matcher.group(2));
        } else {
            majorVersion = "1.7";
            minorVersion = 110;
            Relauncher.LOGGER.info("Regex to parse Java version failed - applying anyway.");
        }

        switch (majorVersion)
        {
            case "1.7":
                if (minorVersion >= 111)
                {
                    Relauncher.LOGGER.info("Not running as Java version is at least Java 7u111.");
                    return;
                }
                break;
            case "1.8":
                if (minorVersion >= 101)
                {
                    Relauncher.LOGGER.info("Not running as Java version is at least Java 8u101.");
                    return;
                }
                break;
        }

        String body = "";
        try {
            Relauncher.LOGGER.info("Adding Let's Encrypt certificate...");
            addLetsEncryptCertificate();
            Relauncher.LOGGER.info("Done, attempting to connect to https://helloworld.letsencrypt.org...");
            URL url = new URL("https://helloworld.letsencrypt.org");
            URLConnection conn = url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            InputStream inputStream = conn.getInputStream();
            body = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        } catch (Exception e) {
            Relauncher.LOGGER.error("An error occurred whilst adding the Let's Encrypt root certificate. I'm afraid you wont be able to access resources with a Let's Encrypt certificate D:", e);
        }

        if (body.isEmpty())
        {
            Relauncher.LOGGER.error("An unknown error occurred whilst adding the Let's Encrypt root certificate. I'm afraid you may not be able to access resources with a Let's Encrypt certificate D:");
        } else {
            Relauncher.LOGGER.info("Done - you are now able to access resources with a Let's Encrypt certificate :D");
        }
    }
    
}
