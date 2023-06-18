package com.certactuator;

import io.micrometer.core.instrument.MultiGauge;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest
class CertificatesMetricServiceTest {
    @Autowired
    private CertificatesMetricService service;
    @Mock
    private MultiGauge certificatesGauge;
    @Mock
    private KeyStoreService keyStoreService;

    @BeforeEach
    void setUp() {
        service.setCertificatesGauge(certificatesGauge);
        service.setKeyStoreLoader(keyStoreService);
    }

    @Test
    void updateCertificatesGauge() {
        Map<String, Long> certificatesExpireData = Map.of("Test1", 10L, "Test2", 15L);
        when(keyStoreService.getCertificatesExpiryDate()).thenReturn(certificatesExpireData);
        service.updateCertificatesGauge();
        ArgumentCaptor<Iterable<MultiGauge.Row<?>>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(certificatesGauge).register(captor.capture(), anyBoolean());
        Iterable<MultiGauge.Row<?>> rows = captor.getValue();
        assertThat(rows).isNotEmpty();
    }
}