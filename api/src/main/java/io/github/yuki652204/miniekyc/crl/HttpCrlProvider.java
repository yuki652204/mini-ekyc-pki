package io.github.yuki652204.miniekyc.crl;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;

/**
 * Vault が公開している CRL を毎回取りに行く。
 * 学習用なのでキャッシュしていない（改善ポイント：nextUpdate までキャッシュする、OCSP にする）。
 */
public class HttpCrlProvider implements CrlProvider {

    private final HttpClient http = HttpClient.newHttpClient();
    private final URI url;

    public HttpCrlProvider(String url) {
        this.url = URI.create(url);
    }

    @Override
    public X509CRL current() {
        try {
            HttpResponse<byte[]> res = http.send(
                    HttpRequest.newBuilder(url).GET().build(),
                    HttpResponse.BodyHandlers.ofByteArray());
            if (res.statusCode() != 200) {
                throw new IllegalStateException("CRL を取得できません: HTTP " + res.statusCode());
            }
            return (X509CRL) CertificateFactory.getInstance("X.509")
                    .generateCRL(new ByteArrayInputStream(res.body()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("CRL の取得が中断されました", e);
        } catch (Exception e) {
            throw new IllegalStateException("CRL を取得できません: " + url, e);
        }
    }
}
