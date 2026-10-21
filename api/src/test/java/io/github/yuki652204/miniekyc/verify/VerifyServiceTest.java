package io.github.yuki652204.miniekyc.verify;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * 今日の openssl 実験を、そのまま自動テストにしたもの。
 * CertificateVerifier の ①②③ を実装すると、すべて緑になる。
 */
class VerifyServiceTest {

    private static final String MESSAGE = "purpose=AGE_CHECK";

    private TestPki cityHall;
    private VerifyService service;

    @BeforeEach
    void setUp() {
        cityHall = new TestPki("CN=Mini J-LIS Root CA");
        Clock clock = Clock.fixed(Instant.parse("2026-10-21T00:00:00Z"), ZoneId.of("Asia/Tokyo"));
        // API に渡すのは ca.crt（印影見本）と CRL の取得方法だけ。ca.key は渡さない
        service = new VerifyService(cityHall.caCert, cityHall::crl, clock);
    }

    private VerifyResponse call(TestPki.Citizen who, String sentMessage, String signature, Purpose purpose) {
        return service.verify(new VerifyRequest(who.certPem(), sentMessage, signature, purpose));
    }

    @Nested
    @DisplayName("① 誰が押したか（発行元と期限）")
    class Issuer {

        @Test
        @DisplayName("正常：市役所が発行した証明書なら通る")
        void genuine() {
            var ichihiro = cityHall.issue("Ichihiro", "1980-04-01", "aichi");
            var res = call(ichihiro, MESSAGE, ichihiro.sign(MESSAGE), Purpose.AGE_CHECK);
            assertThat(res.verified()).isTrue();
        }

        @Test
        @DisplayName("悪い人：名前が同じでも、自作の証明書は弾かれる（evil.crt の再現）")
        void selfIssued() {
            var evil = TestPki.selfIssued("Ichihiro", "1980-04-01", "aichi");
            var res = call(evil, MESSAGE, evil.sign(MESSAGE), Purpose.AGE_CHECK);
            assertThat(res.verified()).isFalse();
            assertThat(res.reason()).isEqualTo("UNTRUSTED_ISSUER");
        }

        @Test
        @DisplayName("期限切れ：市役所の証明書でも、期限外なら弾かれる")
        void expired() {
            Instant past = Instant.parse("2025-01-01T00:00:00Z");
            var old = cityHall.issue("Ichihiro", "1980-04-01", "aichi", past, past.plus(30, ChronoUnit.DAYS));
            var res = call(old, MESSAGE, old.sign(MESSAGE), Purpose.AGE_CHECK);
            assertThat(res.reason()).isEqualTo("EXPIRED");
        }
    }

    @Nested
    @DisplayName("② 失効していないか")
    class Revocation {

        @Test
        @DisplayName("失効：期限内でも、失効していれば弾かれる（引っ越しの再現）")
        void revoked() {
            var ichihiro = cityHall.issue("Ichihiro", "1980-04-01", "aichi");
            cityHall.revoke(ichihiro.cert());
            var res = call(ichihiro, MESSAGE, ichihiro.sign(MESSAGE), Purpose.AGE_CHECK);
            assertThat(res.reason()).isEqualTo("REVOKED");
        }
    }

    @Nested
    @DisplayName("③ 何に押したか（データの署名）")
    class DataSignature {

        @Test
        @DisplayName("改ざん：署名後にメッセージを書き換えると弾かれる（名古屋市→東京都の再現）")
        void tampered() {
            var ichihiro = cityHall.issue("Ichihiro", "1980-04-01", "aichi");
            var res = call(ichihiro, "purpose=PREFECTURE_CHECK", ichihiro.sign(MESSAGE), Purpose.AGE_CHECK);
            assertThat(res.reason()).isEqualTo("BAD_SIGNATURE");
        }

        @Test
        @DisplayName("なりすまし：他人の証明書に、自分の鍵の署名を付けても弾かれる")
        void wrongKey() {
            var ichihiro = cityHall.issue("Ichihiro", "1980-04-01", "aichi");
            var evilKey = TestPki.newKeyPair();
            var res = call(ichihiro, MESSAGE, TestPki.sign(evilKey.getPrivate(), MESSAGE), Purpose.AGE_CHECK);
            assertThat(res.reason()).isEqualTo("BAD_SIGNATURE");
        }
    }

    @Nested
    @DisplayName("④ 最小開示")
    class MinimalDisclosure {

        @Test
        @DisplayName("年齢確認では over20 だけを返し、生年月日は返さない")
        void ageOnly() {
            var adult = cityHall.issue("Ichihiro", "1980-04-01", "aichi");
            var minor = cityHall.issue("Taro", "2010-04-01", "aichi");
            assertThat(call(adult, MESSAGE, adult.sign(MESSAGE), Purpose.AGE_CHECK).disclosed())
                    .isEqualTo(Map.of("over20", true));
            assertThat(call(minor, MESSAGE, minor.sign(MESSAGE), Purpose.AGE_CHECK).disclosed())
                    .isEqualTo(Map.of("over20", false));
        }

        @Test
        @DisplayName("地域確認では都道府県だけを返す")
        void prefectureOnly() {
            var ichihiro = cityHall.issue("Ichihiro", "1980-04-01", "aichi");
            String msg = "purpose=PREFECTURE_CHECK";
            assertThat(call(ichihiro, msg, ichihiro.sign(msg), Purpose.PREFECTURE_CHECK).disclosed())
                    .isEqualTo(Map.of("prefecture", "aichi"));
        }
    }
}
