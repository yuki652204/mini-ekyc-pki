package io.github.yuki652204.miniekyc.verify;

import io.github.yuki652204.miniekyc.crl.CrlProvider;
import io.github.yuki652204.miniekyc.verify.VerificationException.Reason;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 本人確認の流れを組み立てる司令塔。
 *   ① 証明書は本物か（誰が押したか）
 *   ② 失効していないか
 *   ③ データの署名は正しいか（何に押したか）
 *   ④ 目的に必要な情報だけを返す（最小開示）
 */
@Service
public class VerifyService {

    private final CertificateVerifier verifier;
    private final Clock clock;

    public VerifyService(X509Certificate trustAnchor, CrlProvider crlProvider, Clock clock) {
        this.verifier = new CertificateVerifier(trustAnchor, crlProvider);
        this.clock = clock;
    }

    public VerifyResponse verify(VerifyRequest req) {
        try {
            X509Certificate cert = parseCertificate(req.certificatePem());
            byte[] message = req.message().getBytes(StandardCharsets.UTF_8);
            byte[] signature = decodeSignature(req.signatureBase64());

            verifier.checkIssuer(cert, Date.from(clock.instant())); // ①
            verifier.checkRevocation(cert);                          // ②
            verifier.checkDataSignature(cert, message, signature);   // ③

            return VerifyResponse.ok(disclose(CitizenAttributes.from(cert), req.purpose())); // ④
        } catch (VerificationException e) {
            return VerifyResponse.ng(e.reason());
        }
    }

    /** ④ 最小開示：目的ごとに返す項目を絞る */
    Map<String, Object> disclose(CitizenAttributes attrs, Purpose purpose) {
        return switch (purpose) {
            case AGE_CHECK -> {
                if (attrs.birthDate() == null) {
                    throw new VerificationException(Reason.MALFORMED, "証明書に生年月日がありません");
                }
                int age = Period.between(attrs.birthDate(), LocalDate.now(clock)).getYears();
                yield Map.of("over20", age >= 20);
            }
            case PREFECTURE_CHECK -> {
                if (attrs.prefecture() == null) {
                    throw new VerificationException(Reason.MALFORMED, "証明書に都道府県がありません");
                }
                yield Map.of("prefecture", attrs.prefecture());
            }
        };
    }

    private static X509Certificate parseCertificate(String pem) {
        try {
            return (X509Certificate) CertificateFactory.getInstance("X.509")
                    .generateCertificate(new ByteArrayInputStream(pem.getBytes(StandardCharsets.US_ASCII)));
        } catch (Exception e) {
            throw new VerificationException(Reason.MALFORMED, "証明書を読めません", e);
        }
    }

    private static byte[] decodeSignature(String base64) {
        try {
            return Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            throw new VerificationException(Reason.MALFORMED, "署名を読めません", e);
        }
    }
}
