#!/usr/bin/env bash
# 市役所（ルートCA）を Vault の中に作る
#   - ca.key は Vault の外に一度も出ない（generate/internal）
#   - 外に出すのは ca.crt（印影見本）だけ
set -euo pipefail
cd "$(dirname "$0")/.."

vault() { docker compose exec -T vault vault "$@"; }

echo "▶ PKI エンジンを有効化"
vault secrets enable pki >/dev/null 2>&1 || echo "  (有効化済み)"
vault secrets tune -max-lease-ttl=87600h pki

echo "▶ ルートCAを作成（秘密鍵は Vault 内部に保管）"
mkdir -p trust
vault write -field=certificate pki/root/generate/internal \
  common_name="Mini J-LIS Root CA" \
  key_type=ec key_bits=256 \
  ttl=87600h > trust/ca.crt

echo "▶ CRL（失効リスト）の配布場所を設定"
vault write pki/config/urls \
  issuing_certificates="http://vault:8200/v1/pki/ca" \
  crl_distribution_points="http://vault:8200/v1/pki/crl"

echo "▶ 住民向け証明書のルール（ロール）を作成"
# 属性（生年月日・都道府県）は SubjectAltName の URI に入れる。
# CA が署名するので、利用者が後から書き換えることはできない。
vault write pki/roles/citizen \
  allow_any_name=true \
  enforce_hostnames=false \
  allowed_uri_sans="urn:mini-jlis:*" \
  key_type=any \
  server_flag=false client_flag=true \
  max_ttl=720h

echo "✅ 完了: trust/ca.crt を API に渡します"
openssl x509 -in trust/ca.crt -noout -subject -enddate
