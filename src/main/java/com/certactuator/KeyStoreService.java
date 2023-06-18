package com.certactuator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Component
@PropertySource("classpath:keyStore.properties")
public class KeyStoreService {
    private final int MS_IN_DAY = 24 * 60 * 60 * 1000;
    @Value("${cacerts.password}")
    private String password;

    private KeyStore loadCacertsKeyStore() {
        String relativeCacertsPath = "/lib/security/cacerts".replace("/", File.separator);
        String pathToCacertsKeyStore = System.getProperty("java.home") + relativeCacertsPath;
        return loadKeyStore(pathToCacertsKeyStore);
    }

    private KeyStore loadKeyStore(String pathToKeyStore) {
        try (FileInputStream fileKeyStore = new FileInputStream(pathToKeyStore)) {
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            keystore.load(fileKeyStore, password.toCharArray());
            return keystore;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public Map<String, Long> getCertificatesExpiryDate() {
        KeyStore keyStore = loadCacertsKeyStore();
        Map<String, Long> result = new HashMap<>();
        long currentTime = new Date().getTime();
        try {
            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                Certificate certificate = keyStore.getCertificate(alias);
                if (certificate instanceof X509Certificate) {
                    long expireTime = ((X509Certificate) certificate).getNotAfter().getTime();
                    result.put(alias, (expireTime - currentTime) / MS_IN_DAY);
                }
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
