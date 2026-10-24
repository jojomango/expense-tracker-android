#!/usr/bin/env bash
# 從最新的 main 組出要上傳到 Play「內部測試」的簽章 AAB。
#
#   bash scripts/release-internal.sh               # 一般用法
#   bash scripts/release-internal.sh --skip-verify # 跳過測試與靜態檢查（只在你確定 main 已經驗證過時用）
#   bash scripts/release-internal.sh --no-tag      # 不打本機 git tag
#
# 做的事：確認在乾淨的 main → git pull → 檢查金鑰與 Firebase 設定 → ./gradlew verify →
# 用組建當下的時間當 versionCode（yyMMddHH）組出 AAB → 確認簽章與版本 → 複製到桌面 → 打 tag internal-<versionCode>。
# 上傳本身在 Play Console 手動做（自動上傳需要 Play Developer API 的服務帳戶，見 docs/store/release-checklist.md）。
#
# 開發這個 script 時才用：ALLOW_NON_MAIN=1 允許在非 main 分支、有未 commit 改動時執行，而且不會 git pull。

set -euo pipefail

skip_verify=false
make_tag=true
for arg in "$@"; do
  case "$arg" in
    --skip-verify) skip_verify=true ;;
    --no-tag) make_tag=false ;;
    -h | --help)
      sed -n '2,13p' "$0" | sed 's/^# \{0,1\}//'
      exit 0
      ;;
    *)
      echo "不認得的參數：$arg（可用 --skip-verify、--no-tag、--help）" >&2
      exit 2
      ;;
  esac
done

fail() {
  echo "✗ $*" >&2
  exit 1
}
step() { echo; echo "▶ $*"; }

repo_root=$(git rev-parse --show-toplevel 2>/dev/null) || fail "請在 android-play 的 repo 裡執行"
cd "$repo_root"

# 系統預設的 java 可能是很舊的版本；專案需要 JDK 17。
if [[ -z "${JAVA_HOME:-}" && -d "$HOME/.local/jdk17/Contents/Home" ]]; then
  export JAVA_HOME="$HOME/.local/jdk17/Contents/Home"
fi
[[ -n "${JAVA_HOME:-}" ]] || fail "找不到 JDK 17：請設定 JAVA_HOME"
keytool="$JAVA_HOME/bin/keytool"

step "確認分支與工作目錄"
branch=$(git branch --show-current)
if [[ "${ALLOW_NON_MAIN:-}" == "1" ]]; then
  echo "  （ALLOW_NON_MAIN=1：略過分支與乾淨檢查，不會 git pull）目前分支：$branch"
else
  [[ "$branch" == "main" ]] || fail "目前在 $branch，請先 git switch main"
  [[ -z "$(git status --porcelain --untracked-files=no)" ]] || fail "有未 commit 的改動，請先 commit 或還原"
  git pull --ff-only --quiet || fail "git pull 失敗（本機 main 跟雲端分岔了？）"
fi
commit=$(git rev-parse --short HEAD)
echo "  commit：$commit $(git log -1 --format=%s)"

step "確認上傳金鑰與 Firebase 設定"
[[ -f app/google-services.json ]] || fail "缺少 app/google-services.json（從 Firebase Console 下載）"
store_file="${EXPENSE_UPLOAD_STORE_FILE:-}"
if [[ -z "$store_file" && -f "$HOME/.gradle/gradle.properties" ]]; then
  store_file=$(sed -n 's/^EXPENSE_UPLOAD_STORE_FILE=//p' "$HOME/.gradle/gradle.properties" | tail -1)
fi
[[ -n "$store_file" ]] || fail "~/.gradle/gradle.properties 沒有設定 EXPENSE_UPLOAD_STORE_FILE（見 docs/store/release-checklist.md 步驟 1）"
[[ -f "$store_file" ]] || fail "找不到上傳金鑰檔：$store_file"
echo "  金鑰：$store_file"

# yyMMddHH：比 Play 已經收過的版本（目前最大是 3）大很多，而且每小時遞增。
# 同一個小時內出第二版時，接在上一個 internal tag 後面 +1。
version_code=$(date +%y%m%d%H)
last_tag_code=$(git tag -l 'internal-*' | sed 's/^internal-//' | grep -E '^[0-9]+$' | sort -n | tail -1 || true)
if [[ -n "$last_tag_code" && "$last_tag_code" -ge "$version_code" ]]; then
  version_code=$((last_tag_code + 1))
fi
version_name=$(sed -n 's/^ *versionName = "\(.*\)"$/\1/p' app/build.gradle.kts | head -1)
echo "  版本：$version_name（versionCode $version_code）"

gradle_args=("-PexpenseVersionCode=$version_code")
if $skip_verify; then
  step "組建簽章 AAB（--skip-verify：略過測試）"
  ./gradlew :app:bundleRelease "${gradle_args[@]}"
else
  step "跑 ./gradlew verify（測試、靜態檢查、組建 AAB），約 2～5 分鐘"
  ./gradlew verify "${gradle_args[@]}"
fi

aab=app/build/outputs/bundle/release/app-release.aab
[[ -f "$aab" ]] || fail "沒有產出 $aab"

step "確認 AAB"
owner=$("$keytool" -printcert -jarfile "$aab" 2>/dev/null | sed -n 's/^Owner: //p' | head -1)
[[ -n "$owner" ]] || fail "讀不到 AAB 的簽章"
[[ "$owner" != *"Android Debug"* ]] || fail "AAB 是用 debug 金鑰簽的，Play 會拒收（檢查 ~/.gradle/gradle.properties）"
manifest=app/build/intermediates/merged_manifest/release/processReleaseMainManifest/AndroidManifest.xml
built_code=$(sed -n 's/.*android:versionCode="\([0-9]*\)".*/\1/p' "$manifest" | head -1)
[[ "$built_code" == "$version_code" ]] || fail "組出來的 versionCode 是 $built_code，預期 $version_code"
echo "  簽章：$owner"
echo "  versionCode：$built_code"

out="$HOME/Desktop/expense-tracker-$version_name-$version_code.aab"
cp "$aab" "$out"

if $make_tag; then
  git tag "internal-$version_code" "$commit"
  tag_note="本機 tag internal-$version_code（要分享到 GitHub：git push origin internal-$version_code）"
else
  tag_note="沒有打 tag（--no-tag）"
fi

cat <<EOF

✓ 完成
  檔案：$out（$(du -h "$out" | cut -f1 | tr -d ' ')）
  版本：$version_name（versionCode $version_code），commit $commit
  $tag_note

下一步（手動，約 1 分鐘）：
  1. Play Console → 測試 → 內部測試 → 建立新版本
  2. 上傳上面那個 .aab（「未上傳偵錯符號檔」的警告可以略過，原因見 docs/store/release-checklist.md）
  3. 儲存並發布；手機的 Play 商店更新就會收到
  Crashlytics 的 R8 對照表已經在組建時自動上傳。
EOF
