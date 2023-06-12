package com.certactuator.certactuator;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.Tags;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.stream.Collectors;

@Component
@EnableScheduling
public class CertificatesMetric {
    private Random random = new Random();
    MultiGauge certificatesGauge = null;
    public CertificatesMetric(MeterRegistry registry) {
        certificatesGauge = MultiGauge.builder("test.certificates").register(registry);
    }

    @Scheduled(fixedRateString = "PT30S")
    public void updateCertificatesGauge() {
        Map<String, Long> certificatesExpire = getCertificatesExpiryDate();
        certificatesGauge.register(certificatesExpire.entrySet().stream()
                .map(this::toRow)
                .collect(Collectors.toList()), true);
    }

    private MultiGauge.Row<Number> toRow(Map.Entry<String, Long> entry) {
        return MultiGauge.Row.of(Tags.of("name", entry.getKey()), entry.getValue());
    }

    private KeyStore loadKeyStore() {
        String relativeCacertsPath = "/lib/security/cacerts".replace("/", File.separator);
        String filename = System.getProperty("java.home") + relativeCacertsPath;
        try {
            FileInputStream is = new FileInputStream(filename);

            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            String password = "changeit";
            keystore.load(is, password.toCharArray());
            return keystore;
        }catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private Map<String, Long> getCertificatesExpiryDate() {
        KeyStore keyStore = loadKeyStore();
        Map<String, Long> result = new HashMap<>();
        long currentTime = new Date().getTime();
        try {
            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                Certificate certificate = keyStore.getCertificate(alias);
                if (certificate instanceof X509Certificate) {
                    long expireTime = ((X509Certificate) certificate).getNotAfter().getTime();
                    result.put(alias, (expireTime - currentTime)/(24*60*60*1000));
                }
            }
            return result;
        }catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
