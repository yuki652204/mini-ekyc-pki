package io.github.yuki652204.miniekyc.verify;

import java.util.Map;

/**
 * 検証結果。disclosed には目的に必要な最小限の情報だけを入れる。
 * 例：AGE_CHECK なら {"over20": true}。生年月日そのものは返さない。
 */
public record VerifyResponse(boolean verified, String reason, Map<String, Object> disclosed) {

    public static VerifyResponse ok(Map<String, Object> disclosed) {
        return new VerifyResponse(true, null, disclosed);
    }

    public static VerifyResponse ng(VerificationException.Reason reason) {
        return new VerifyResponse(false, reason.name(), Map.of());
    }
}
