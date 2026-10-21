# mini-ekyc-pki

**電子証明書で「住所・年齢」を確かめる本人確認APIの、学習用ミニ実装**

> ⚠️ これは PKI の仕組みを学ぶための**模擬環境**です。実際のマイナンバーカード（JPKI）や J-LIS とは接続しておらず、法令上の本人確認には使えません。

## なぜ作ったか

「本人確認APIは、クラウドの住所データベースを検索して判別している」と思っていました。
実際はそうではなく、API が持っているのは **信頼する発行元の証明書（印影見本）1枚だけ** でした。
このことを、自分で作って確かめたのがこのリポジトリです。

## 仕組み（印鑑登録証明書で考える）

| 実印の世界 | このリポジトリ |
|---|---|
| 市役所 | **Vault PKI**（ルートCA。秘密鍵は Vault の外に出さない） |
| 実印 | 利用者の秘密鍵（`work/` 配下、Git 管理外） |
| 印鑑登録証明書 | 利用者の証明書（生年月日・都道府県を CA が書き込み済み） |
| 印影見本 | `trust/ca.crt`（API が持つ唯一のもの） |
| 証明書の取り消し | CRL（失効リスト） |

```
[Vault PKI 市役所] ──発行・失効──▶ [利用者] ──証明書＋署名──▶ [検証API]
        │                                                      │ ① 発行元と期限
        └────────────── CRL ──────────────────────────────────▶│ ② 失効
                                                               │ ③ データの署名
                                                               ▼ ④ 最小開示
                                                  {"verified":true,"over20":true}
```

### 最小開示

目的（`purpose`）によって、返す情報を絞り込みます。

| purpose | 返すもの | 返さないもの |
|---|---|---|
| `AGE_CHECK` | `over20: true/false` | 生年月日 |
| `PREFECTURE_CHECK` | `prefecture` | 生年月日 |

## 動かし方

前提：Docker Desktop、openssl

```bash
docker compose up -d vault
./vault/setup.sh                                        # 市役所を作る → trust/ca.crt
docker compose up -d --build api                        # 検証APIを起動

./scripts/issue-citizen.sh Ichihiro 1980-04-01 aichi    # 証明書を発行してもらう
./scripts/call-api.sh Ichihiro AGE_CHECK                # ✅ {"verified":true,...,"over20":true}
./scripts/call-api.sh Ichihiro AGE_CHECK "purpose=X"    # ❌ BAD_SIGNATURE（改ざん）
./scripts/revoke.sh Ichihiro                            # 失効させる
./scripts/call-api.sh Ichihiro AGE_CHECK                # ❌ REVOKED（期限内でも使えない）
```

## テスト（検証ロジックの仕様書）

```bash
cd api && mvn test
```

| シナリオ | 期待 | 確かめていること |
|---|---|---|
| 正常 | ✅ | 市役所が発行した証明書＋本人の署名 |
| 自作の証明書 | `UNTRUSTED_ISSUER` | 名前が同じでも、発行元のハンコがなければ弾く |
| 期限切れ | `EXPIRED` | |
| 失効 | `REVOKED` | 期限内でも、取り消されていれば弾く |
| 改ざん | `BAD_SIGNATURE` | 署名後にメッセージを書き換えた |
| なりすまし | `BAD_SIGNATURE` | 他人の証明書に自分の鍵で署名した |
| 最小開示 | ✅ | 目的に必要な項目だけを返す |

## フォルダ構成

```
mini-ekyc-pki/
├── compose.yaml          # Vault と API をまとめて起動
├── vault/setup.sh        # 市役所（ルートCA）を作る
├── scripts/              # 利用者役・市役所役の操作
├── trust/                # ca.crt の置き場（API に渡す）
├── work/                 # 利用者の秘密鍵など（Git 管理外）
└── api/                  # Spring Boot の検証API
    └── src/main/java/.../verify/
        ├── VerifyService.java        # 流れの司令塔 ①→②→③→④
        └── CertificateVerifier.java  # ①②③ の検証ロジック
```

## 今後の改善ポイント

- [ ] **リプレイ攻撃対策**：API が発行する使い捨ての値（nonce）に署名させる
- [ ] CRL のキャッシュ、または OCSP への切り替え
- [ ] Vault を k8s に載せ、Argo CD で GitOps 管理する
- [ ] 中間CA を挟んだ証明書チェーン

## 技術スタック

Java 21 / Spring Boot 3.5 / HashiCorp Vault (PKI) / Docker Compose / JUnit 5 / GitHub Actions
