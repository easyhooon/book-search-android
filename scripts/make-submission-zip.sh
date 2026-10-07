#!/usr/bin/env bash
# 제출 ZIP을 만듭니다: 소스, .git, local.properties, debug APK, 시연 영상.
# build/, .gradle/ 등 빌드 산출물과 IDE 설정은 넣지 않습니다.
# local.properties의 sdk.dir은 이 기기의 경로라서 빼고 넣습니다. SDK는 Android Studio나 ANDROID_HOME으로 찾습니다.
# 사용법: scripts/make-submission-zip.sh [출력 경로]  (기본값: 저장소 상위 폴더의 book-search-android.zip)
set -euo pipefail

name=book-search-android
root="$(cd "$(dirname "$0")/.." && pwd)"
out="${1:-$root/../$name.zip}"
out="$(cd "$(dirname "$out")" && pwd)/$(basename "$out")"

fail() { echo "오류: $1" >&2; exit 1; }

cd "$root"
[ -z "$(git status --porcelain)" ] || fail "커밋하지 않은 변경이 있습니다."
[ -f local.properties ] || fail "local.properties가 없습니다."
grep -q '^KAKAO_REST_API_KEY=..*' local.properties || fail "local.properties에 KAKAO_REST_API_KEY 값이 없습니다."
[ -f demo.mp4 ] || fail "시연 영상 demo.mp4가 없습니다."

./gradlew assembleDebug
apk=app/build/outputs/apk/debug/app-debug.apk
[ -f "$apk" ] || fail "$apk 가 없습니다."

stage="$(mktemp -d)"
trap 'rm -rf "$stage"' EXIT

rsync -a \
  --exclude 'build/' \
  --exclude '.gradle/' \
  --exclude '.kotlin/' \
  --exclude '.idea/' \
  --exclude '.cxx/' \
  --exclude '.externalNativeBuild/' \
  --exclude '/captures/' \
  --exclude '.DS_Store' \
  --exclude '*.iml' \
  --exclude '*.apk' \
  --exclude '*.zip' \
  "$root/" "$stage/$name/"
grep -v '^sdk\.dir=' "$root/local.properties" > "$stage/$name/local.properties"
cp "$apk" "$stage/$name/app-debug.apk"

rm -f "$out"
(cd "$stage" && zip -qry -X "$out" "$name")
echo "생성: $out ($(du -h "$out" | cut -f1))"
