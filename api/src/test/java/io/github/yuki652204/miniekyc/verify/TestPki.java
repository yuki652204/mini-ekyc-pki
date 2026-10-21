package io.github.yuki652204.miniekyc.verify;

import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.CRLReason;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.X509v2CRLBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CRLConverter;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

/**
 * テスト用の「ミニ市役所」。Vault の代わりにメモリ上で CA・証明書・CRL を作る。
 * 本番コード（src/main）は BouncyCastle に依存しない。
 */
class TestPki {

    private static final AtomicLong SERIAL = new AtomicLong(1000);

    final KeyPair caKey;
    final X509Certificate caCert;
    private final List<BigInteger> revoked = new ArrayList<>();

    TestPki(String caName) {
        this.caKey = newKeyPair();
        this.caCert = selfSigned(caKey, caName);
    }

    /** 本人の鍵と、市役所が発行した証明書のセット */
    record Citizen(KeyPair key, X509Certificate cert) {
        String certPem() {
            return toPem(cert);
        }

        String sign(String message) {
            return TestPki.sign(key.getPrivate(), message);
        }
    }

    Citizen issue(String name, String birthDate, String prefecture, Instant notBefore, Instant notAfter) {
        KeyPair key = newKeyPair();
        try {
            GeneralNames sans = new GeneralNames(new GeneralName[] {
                    new GeneralName(GeneralName.uniformResourceIdentifier, "urn:mini-jlis:birthdate:" + birthDate),
                    new GeneralName(GeneralName.uniformResourceIdentifier, "urn:mini-jlis:prefecture:" + prefecture)
            });
            var builder = new JcaX509v3CertificateBuilder(
                    caCert, BigInteger.valueOf(SERIAL.incrementAndGet()),
                    Date.from(notBefore), Date.from(notAfter),
                    new X500Name("CN=" + name), key.getPublic())
                    .addExtension(Extension.subjectAlternativeName, false, sans);
            return new Citizen(key, new JcaX509CertificateConverter().getCertificate(builder.build(signer(caKey.getPrivate()))));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    Citizen issue(String name, String birthDate, String prefecture) {
        Instant now = Instant.now();
        return issue(name, birthDate, prefecture, now.minus(1, ChronoUnit.DAYS), now.plus(30, ChronoUnit.DAYS));
    }

    /** 悪い人：市役所を通さず、自分で自分に証明書を作る */
    static Citizen selfIssued(String name, String birthDate, String prefecture) {
        TestPki fake = new TestPki("CN=Mini J-LIS Root CA"); // 名前だけ本物そっくり
        return fake.issue(name, birthDate, prefecture);
    }

    void revoke(X509Certificate cert) {
        revoked.add(cert.getSerialNumber());
    }

    X509CRL crl() {
        try {
            Date now = new Date();
            var builder = new X509v2CRLBuilder(X500Name.getInstance(caCert.getSubjectX500Principal().getEncoded()), now);
            builder.setNextUpdate(Date.from(Instant.now().plus(1, ChronoUnit.DAYS)));
            for (BigInteger serial : revoked) {
                builder.addCRLEntry(serial, now, CRLReason.keyCompromise);
            }
            return new JcaX509CRLConverter().getCRL(builder.build(signer(caKey.getPrivate())));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    // ---- helpers ----

    static KeyPair newKeyPair() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
            gen.initialize(new ECGenParameterSpec("secp256r1"));
            return gen.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static String sign(PrivateKey key, String message) {
        try {
            Signature sig = Signature.getInstance("SHA256withECDSA");
            sig.initSign(key);
            sig.update(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(sig.sign());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static String toPem(X509Certificate cert) {
        try (StringWriter sw = new StringWriter(); JcaPEMWriter w = new JcaPEMWriter(sw)) {
            w.writeObject(cert);
            w.flush();
            return sw.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static X509Certificate selfSigned(KeyPair key, String dn) {
        try {
            Instant now = Instant.now();
            X500Name name = new X500Name(dn);
            var builder = new JcaX509v3CertificateBuilder(
                    name, BigInteger.ONE,
                    Date.from(now.minus(1, ChronoUnit.DAYS)), Date.from(now.plus(3650, ChronoUnit.DAYS)),
                    name, key.getPublic())
                    .addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
            return new JcaX509CertificateConverter().getCertificate(builder.build(signer(key.getPrivate())));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static ContentSigner signer(PrivateKey key) throws Exception {
        return new JcaContentSignerBuilder("SHA256withECDSA").build(key);
    }
}
