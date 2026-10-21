package io.github.yuki652204.miniekyc.verify;

import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * 証明書の SubjectAltName（URI）から属性を取り出す。
 *   urn:mini-jlis:birthdate:1980-04-01
 *   urn:mini-jlis:prefecture:aichi
 * 属性は市役所（CA）が証明書に書き込んで署名しているので、
 * ① のチェックを通った証明書なら「市役所が保証した値」として扱える。
 */
public record CitizenAttributes(String name, LocalDate birthDate, String prefecture) {

    private static final int SAN_TYPE_URI = 6;
    private static final String BIRTHDATE = "urn:mini-jlis:birthdate:";
    private static final String PREFECTURE = "urn:mini-jlis:prefecture:";

    public static CitizenAttributes from(X509Certificate cert) {
        LocalDate birthDate = null;
        String prefecture = null;
        try {
            Collection<List<?>> sans = cert.getSubjectAlternativeNames();
            if (sans != null) {
                for (List<?> san : sans) {
                    if (!Integer.valueOf(SAN_TYPE_URI).equals(san.get(0))) {
                        continue;
                    }
                    String uri = (String) san.get(1);
                    if (uri.startsWith(BIRTHDATE)) {
                        birthDate = LocalDate.parse(uri.substring(BIRTHDATE.length()));
                    } else if (uri.startsWith(PREFECTURE)) {
                        prefecture = uri.substring(PREFECTURE.length());
                    }
                }
            }
        } catch (CertificateParsingException e) {
            throw new VerificationException(VerificationException.Reason.MALFORMED, "SAN を読めません", e);
        }
        String name = cert.getSubjectX500Principal().getName();
        return new CitizenAttributes(name, birthDate, prefecture);
    }
}
