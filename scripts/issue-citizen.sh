#!/usr/bin/env bash
# 利用者役：鍵を作り、市役所（Vault）に申請して証明書を発行してもらう
#   使い方: scripts/issue-citizen.sh <名前> <生年月日 YYYY-MM-DD> <都道府県>
#   例    : scripts/issue-citizen.sh Ichihiro 1980-04-01 aichi
set -euo pipefail
cd "$(dirname "$0")/.."

NAME=${1:?名前を指定してください}
BIRTH=${2:?生年月日を指定してください (YYYY-MM-DD)}
PREF=${3:?都道府県を指定してください (例: aichi)}
DIR="work/${NAME}"
mkdir -p "$DIR"

echo "▶ 鍵ペアを作成（秘密鍵＝本人だけのハンコ。work/ は Git 管理外）"
openssl genpkey -algorithm EC -pkeyopt ec_paramgen_curve:P-256 -out "$DIR/private.pem"

echo "▶ 申請書（CSR）を作成"
openssl req -new -key "$DIR/private.pem" -subj "/CN=${NAME}" -out "$DIR/request.csr"

echo "▶ 市役所に署名してもらう（属性は CA が証明書に書き込む）"
docker compose exec -T vault vault write -field=certificate pki/sign/citizen \
  csr=- \
  common_name="${NAME}" \
  uri_sans="urn:mini-jlis:birthdate:${BIRTH},urn:mini-jlis:prefecture:${PREF}" \
  ttl=720h < "$DIR/request.csr" > "$DIR/cert.pem"

echo "✅ 発行完了: $DIR/cert.pem"
openssl x509 -in "$DIR/cert.pem" -noout -subject -serial -ext subjectAltName
