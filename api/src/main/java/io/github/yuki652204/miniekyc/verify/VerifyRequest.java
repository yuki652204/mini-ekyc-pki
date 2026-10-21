package io.github.yuki652204.miniekyc.verify;

/**
 * 利用者から届くもの。
 * @param certificatePem  利用者の証明書（市役所のハンコ付き）
 * @param message         署名した内容
 * @param signatureBase64 利用者が自分の秘密鍵で押したハンコ
 * @param purpose         何の確認か
 */
public record VerifyRequest(
        String certificatePem,
        String message,
        String signatureBase64,
        Purpose purpose) {
}
