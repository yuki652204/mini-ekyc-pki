package io.github.yuki652204.miniekyc.config;

import io.github.yuki652204.miniekyc.crl.CrlProvider;
import io.github.yuki652204.miniekyc.crl.HttpCrlProvider;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PkiConfig {

    /** 印影見本（ルートCA証明書）。起動時に1回だけ読み込む */
    @Bean
    public X509Certificate trustAnchor(@Value("${miniekyc.trust-anchor-path}") Path path)
            throws IOException, GeneralSecurityException {
        try (InputStream in = Files.newInputStream(path)) {
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(in);
        }
    }

    @Bean
    public CrlProvider crlProvider(@Value("${miniekyc.crl-url}") String crlUrl) {
        return new HttpCrlProvider(crlUrl);
    }

    /** 年齢計算で「今日」を差し替えられるように Clock を Bean にする */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
