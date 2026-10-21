package io.github.yuki652204.miniekyc.verify;

/** 検証に失敗したときに投げる。reason で「どの段階で弾かれたか」を伝える */
public class VerificationException extends RuntimeException {

    public enum Reason {
        /** ① 信頼する市役所（ルートCA）のハンコではない */
        UNTRUSTED_ISSUER,
        /** ① 有効期限の外 */
        EXPIRED,
        /** ② 期限内だが失効している */
        REVOKED,
        /** ③ データの署名が一致しない（改ざん or 別人の鍵） */
        BAD_SIGNATURE,
        /** そもそも証明書や署名の形式が読めない */
        MALFORMED
    }

    private final Reason reason;

    public VerificationException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public VerificationException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
