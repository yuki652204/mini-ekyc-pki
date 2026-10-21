#!/usr/bin/env bash
# 市役所役：証明書を失効させる（引っ越し・紛失の再現）
#   使い方: scripts/revoke.sh <名前>
set -euo pipefail
cd "$(dirname "$0")/.."

NAME=${1:?名前を指定してください}
CERT="work/${NAME}/cert.pem"

# openssl の serial=0A1B2C... を Vault 形式の 0a:1b:2c... に変換
SERIAL=$(openssl x509 -in "$CERT" -noout -serial | cut -d= -f2 \
  | tr 'A-F' 'a-f' | sed 's/../&:/g; s/:$//')

echo "▶ シリアル ${SERIAL} を失効"
docker compose exec -T vault vault write pki/revoke serial_number="${SERIAL}"
echo "✅ 失効しました。期限内でも、もう本人確認には使えません"
