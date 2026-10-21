package io.github.yuki652204.miniekyc.crl;

import java.security.cert.X509CRL;

/** 最新の失効リスト（CRL）を返す。本番は Vault から、テストはメモリから取得する */
public interface CrlProvider {
    X509CRL current();
}
