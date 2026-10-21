package io.github.yuki652204.miniekyc.verify;

import java.security.GeneralSecurityException;
import java.security.Signature;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Date;

import io.github.yuki652204.miniekyc.crl.CrlProvider;
import io.github.yuki652204.miniekyc.verify.VerificationException.Reason;

/**
 * ★ここがあなたの担当です★ 今日の openssl 実験でやったことを、Java 標準API（java.security）で書きます。
 * 外部ライブラリは使いません。
 *
 * ゴール：VerifyServiceTest をすべて緑にする mvn test
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
	 * ヒント： - cert.verify(公開鍵) … ハンコ照合。合わなければ例外が飛ぶ。どの公開鍵で照合する？ -
	 * cert.checkValidity(now) … 期限チェック - ハンコ不一致 → Reason.UNTRUSTED_ISSUER / 期限外 →
	 * Reason.EXPIRED
	 */
	public void checkIssuer(X509Certificate cert, Date now) {
		// ハンコ照合：この証明書は市役所の公印で押されているか？
		try {
			cert.verify(trustAnchor.getPublicKey());
		} catch (GeneralSecurityException e) {
			throw new VerificationException(Reason.UNTRUSTED_ISSUER, "信頼するCAの署名ではありません", e);
		}

		// 期限チェック：今日は有効期限の中か？
		try {
			cert.checkValidity(now);
		} catch (CertificateExpiredException | CertificateNotYetValidException e) {
			throw new VerificationException(Reason.EXPIRED, "有効期限外です", e);
		}
	}

	/**
	 * ② 失効していないか：期限内でも、市役所が取り消していないか？
	 *
	 * ヒント： - crlProvider.current() で最新の失効リスト（X509CRL）が手に入る -
	 * X509CRL#isRevoked(cert) - 発展：その失効リスト自体が本物か？も確かめると堅い（crl.verify(...)） - 失効 →
	 * Reason.REVOKED
	 */
	public void checkRevocation(X509Certificate cert) {
		// 最新の掲示板をもらう
		X509CRL crl = crlProvider.current();

		// 掲示板そのものが、市役所の公印付きか？
		try {
			crl.verify(trustAnchor.getPublicKey());
		} catch (GeneralSecurityException e) {
			throw new VerificationException(Reason.UNTRUSTED_ISSUER, "失効リストが信頼するCAの署名ではありません", e);
		}

		// この証明書の番号が、掲示板に載っていないか？
		if (crl.isRevoked(cert)) {
			throw new VerificationException(Reason.REVOKED, "失効しています");
		}
	}

	/**
	 * ③ 何に押したか：このメッセージに、証明書の持ち主のハンコが押されているか？
	 *
	 * openssl でいうと：openssl dgst -sha256 -verify public.pem -signature data.sig
	 * data.txt
	 *
	 * ヒント： - java.security.Signature.getInstance("SHA256withECDSA") -
	 * initVerify(どの公開鍵？) → update(message) → verify(signature) - 公開鍵は public.pem
	 * ではなく、証明書の中に入っている - 不一致 → Reason.BAD_SIGNATURE
	 */
	public void checkDataSignature(X509Certificate cert, byte[] message, byte[] signature) {
		try {
			Signature sig = Signature.getInstance("SHA256withECDSA");
			sig.initVerify(cert.getPublicKey()); // public.pem → 本人の印影
			sig.update(message); // data.txt → 何に押したか
			if (!sig.verify(signature)) { // data.sig → 押されたハンコ
				throw new VerificationException(Reason.BAD_SIGNATURE, "署名が一致しません");
			}
		} catch (GeneralSecurityException e) {
			throw new VerificationException(Reason.BAD_SIGNATURE, "署名を検証できません", e);
		}
	}
}
