#!/usr/bin/env bash
# 利用者役：メッセージに署名して、本人確認APIに送る
#   使い方: scripts/call-api.sh <名前> <AGE_CHECK|PREFECTURE_CHECK> [送るメッセージ]
#   第3引数を渡すと「署名したのとは別のメッセージ」を送る＝改ざんの再現
set -euo pipefail
cd "$(dirname "$0")/.."

NAME=${1:?名前を指定してください}
PURPOSE=${2:?AGE_CHECK または PREFECTURE_CHECK}
DIR="work/${NAME}"
MESSAGE="purpose=${PURPOSE}"
SENT_MESSAGE=${3:-$MESSAGE}

printf '%s' "$MESSAGE" > "$DIR/message.txt"
openssl dgst -sha256 -sign "$DIR/private.pem" -out "$DIR/message.sig" "$DIR/message.txt"

CERT_JSON=$(awk 'NF {sub(/\r/, ""); printf "%s\\n", $0}' "$DIR/cert.pem")
SIG_B64=$(openssl base64 -A -in "$DIR/message.sig")

curl -sS -X POST http://localhost:8080/verify \
  -H 'Content-Type: application/json' \
  -d "{\"certificatePem\":\"${CERT_JSON}\",\"message\":\"${SENT_MESSAGE}\",\"signatureBase64\":\"${SIG_B64}\",\"purpose\":\"${PURPOSE}\"}"
echo
