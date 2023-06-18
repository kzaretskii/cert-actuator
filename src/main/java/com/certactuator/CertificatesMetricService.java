package com.certactuator;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.Tags;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CertificatesMetricService {
    private MultiGauge certificatesGauge;
    private KeyStoreService keyStoreService;

    public CertificatesMetricService(MeterRegistry registry, KeyStoreService keyStoreService) {
        certificatesGauge = MultiGauge.builder("MultiGauge.certificates").register(registry);
        this.keyStoreService = keyStoreService;
    }

    @Scheduled(fixedRateString = "PT01H")
    public void updateCertificatesGauge() {
        Map<String, Long> certificatesExpireData = keyStoreService.getCertificatesExpiryDate();
        Iterable<MultiGauge.Row<?>> rows = certificatesExpireData.entrySet().stream()
                .map(this::toRow)
                .collect(Collectors.toList());
        certificatesGauge.register(rows, true);
    }

    public MultiGauge.Row<Number> toRow(Map.Entry<String, Long> entry) {
        return MultiGauge.Row.of(Tags.of("name", entry.getKey()), entry.getValue());
    }

    public void setCertificatesGauge(MultiGauge certificatesGauge) {
        this.certificatesGauge = certificatesGauge;
    }

    public void setKeyStoreLoader(KeyStoreService keyStoreService) {
        this.keyStoreService = keyStoreService;
    }
}
