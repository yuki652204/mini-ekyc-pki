package io.github.yuki652204.miniekyc.verify;

import io.github.yuki652204.miniekyc.crl.CrlProvider;
import io.github.yuki652204.miniekyc.verify.VerificationException.Reason;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * ★ここがあなたの担当です★
 * 今日の openssl 実験でやったことを、Java 標準API（java.security）で書きます。
 * 外部ライブラリは使いません。
 *
 * ゴール：VerifyServiceTest をすべて緑にする
 *   mvn test
 *
 * 失敗したら VerificationException(Reason.XXX, "理由") を投げてください。
 */
public class CertificateVerifier {

    /** 印影見本（ルートCA証明書）。API が事前に持つ唯一のもの */
    private final X509Certificate trustAnchor;
    /** 最新の失効リストをくれる係 */
    private final CrlProvider crlProvider;

    public CertificateVerifier(X509Certificate trustAnchor, CrlProvider crlProvider) {
        this.trustAnchor = trustAnchor;
        this.crlProvider = crlProvider;
    }

    /**
     * ① 誰が押したか：この証明書は、信頼する市役所のハンコ付きか？期限内か？
     *
     * openssl でいうと：openssl verify -CAfile ca.crt user.crt
     *
     * ヒント：
     *   - cert.verify(公開鍵) … ハンコ照合。合わなければ例外が飛ぶ。どの公開鍵で照合する？
     *   - cert.checkValidity(now) … 期限チェック
     *   - ハンコ不一致 → Reason.UNTRUSTED_ISSUER / 期限外 → Reason.EXPIRED
     */
    public void checkIssuer(X509Certificate cert, Date now) {
        // TODO ①
        throw new UnsupportedOperationException("TODO ①: checkIssuer を実装してください");
    }

    /**
     * ② 失効していないか：期限内でも、市役所が取り消していないか？
     *
     * ヒント：
     *   - crlProvider.current() で最新の失効リスト（X509CRL）が手に入る
     *   - X509CRL#isRevoked(cert)
     *   - 発展：その失効リスト自体が本物か？も確かめると堅い（crl.verify(...)）
     *   - 失効 → Reason.REVOKED
     */
    public void checkRevocation(X509Certificate cert) {
        // TODO ②
        throw new UnsupportedOperationException("TODO ②: checkRevocation を実装してください");
    }

    /**
     * ③ 何に押したか：このメッセージに、証明書の持ち主のハンコが押されているか？
     *
     * openssl でいうと：openssl dgst -sha256 -verify public.pem -signature data.sig data.txt
     *
     * ヒント：
     *   - java.security.Signature.getInstance("SHA256withECDSA")
     *   - initVerify(どの公開鍵？) → update(message) → verify(signature)
     *   - 公開鍵は public.pem ではなく、証明書の中に入っている
     *   - 不一致 → Reason.BAD_SIGNATURE
     */
    public void checkDataSignature(X509Certificate cert, byte[] message, byte[] signature) {
        // TODO ③
        throw new UnsupportedOperationException("TODO ③: checkDataSignature を実装してください");
    }
}
