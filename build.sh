#!/usr/bin/env bash
#
# NagramX Video Timer —— 无 Gradle 极速构建（LSPosed 模块 · libxposed 现代打包 API 102）
#
set -euo pipefail

export MSYS2_ARG_CONV_EXCL='*'
w() { cygpath -w "$1"; }

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MOD="$ROOT/module"
OUT="$MOD/build"

# 工具链
BT_W="${BT_W:-D:\\AndroidSDK\\build-tools\\34.0.0}"
AJ_W="${AJ_W:-D:\\AndroidSDK\\platforms\\android-34\\android.jar}"
JDK_W="${JDK_W:-D:\\JAVA\\bin}"

MOD_W="$(w "$MOD")"
SRC_W="$MOD_W\\src"
API_W="$MOD_W\\lib\\api-102.jar"
OUT_W="$MOD_W\\build"
KS_W="$MOD_W\\mod.keystore"
RES_W="$MOD_W\\res"
META_W="$MOD_W\\meta"
MANIFEST_W="$MOD_W\\AndroidManifest.xml"

D8="$BT_W\\d8.bat"
AAPT2="$BT_W\\aapt2.exe"
AAPT="$BT_W\\aapt.exe"
ZIPALIGN="$BT_W\\zipalign.exe"
APKSIGNER="$BT_W\\apksigner.bat"
JAVAC="$JDK_W\\javac.exe"
JAR="$JDK_W\\jar.exe"
KEYTOOL="$JDK_W\\keytool.exe"

for f in "$D8" "$AAPT2" "$AAPT" "$ZIPALIGN" "$APKSIGNER" "$JAVAC" "$JAR" "$KEYTOOL" "$AJ_W" "$API_W"; do
  [ -e "$f" ] || { echo "缺少依赖: $f" >&2; exit 1; }
done

META_DIR="$MOD/meta/META-INF/xposed"
for f in "$META_DIR/java_init.list" "$META_DIR/module.prop" "$META_DIR/scope.list"; do
  [ -e "$f" ] || { echo "缺少现代打包元数据: $f" >&2; exit 1; }
done

ENTRY_CLASS="$(tr -d ' \r\n' < "$META_DIR/java_init.list")"

echo "[0/7] 一致性检查：代码 TARGET_PACKAGE <-> meta/META-INF/xposed/scope.list"
CODE_PKGS="$(grep -rhoE 'TARGET_PACKAGE = "[^"]+"' "$MOD/src" | sed -E 's/.*"([^"]+)".*/\1/' | sort -u)"
SCOPE_PKGS="$(grep -vE '^\s*(#|$)' "$META_DIR/scope.list" | tr -d ' \r' | sort -u)"
if [ "$CODE_PKGS" != "$SCOPE_PKGS" ]; then
  echo "  代码里的目标包名与静态作用域声明不一致：" >&2
  echo "  --- 代码 TARGET_PACKAGE:" >&2; echo "$CODE_PKGS" >&2
  echo "  --- scope.list:" >&2; echo "$SCOPE_PKGS" >&2
  exit 1
fi
echo "$CODE_PKGS" | sed 's/^/        /'

echo "[0b/7] 入口类与 java_init.list 一致性检查"
if ! grep -rqE "class ${ENTRY_CLASS##*.} extends XposedModule" "$MOD/src"; then
  echo "  入口类 ${ENTRY_CLASS} 不是 XposedModule 子类 —— 现代打包下会被静默跳过，拒绝出包" >&2
  exit 1
fi
echo "        $ENTRY_CLASS extends XposedModule"

rm -rf "$OUT"
mkdir -p "$OUT\\classes" "$OUT\\res"

echo "[1/7] javac -> class（$(find "$MOD/src" -name '*.java' | wc -l) 个源文件）"
mapfile -t SRCS_W < <(find "$MOD/src" -name '*.java' -print0 | xargs -0 -n1 cygpath -w | sort)

"$JAVAC" -J-Duser.language=en -J-Duser.country=US \
  -encoding UTF-8 -source 8 -target 8 -nowarn \
  -cp "$AJ_W;$API_W" -d "$OUT_W\\classes" "${SRCS_W[@]}"

echo "[2/7] jar（d8 不接受目录）"
"$JAR" cf "$OUT_W\\classes.jar" -C "$OUT_W\\classes" .

echo "[3/7] d8 -> classes.dex（api-102.jar 只作 --lib，不进 dex）"
"$D8" --min-api 26 --lib "$AJ_W" --lib "$API_W" --output "$OUT_W" "$OUT_W\\classes.jar"

echo "[4/7] aapt2 compile 资源"
"$AAPT2" compile --dir "$RES_W" -o "$OUT_W\\res\\res.zip"

echo "[5/7] aapt2 link -> base.apk"
"$AAPT2" link -o "$OUT_W\\base.apk" -I "$AJ_W" \
  --manifest "$MANIFEST_W" \
  --min-sdk-version 26 --target-sdk-version 28 \
  "$OUT_W\\res\\res.zip"

echo "[6/7] 加入 dex 与现代打包元数据"
cp "$OUT_W\\base.apk" "$OUT_W\\unsigned.apk"
( cd "$OUT" && "$AAPT" add unsigned.apk classes.dex >/dev/null )
( cd "$MOD/meta" && "$AAPT" add "$OUT_W\\unsigned.apk" \
    META-INF/xposed/module.prop META-INF/xposed/scope.list META-INF/xposed/java_init.list >/dev/null )

echo "[7/7] zipalign + 签名"
if [ ! -e "$KS_W" ]; then
  "$KEYTOOL" -genkeypair -keystore "$KS_W" -alias mod \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass android -keypass android -dname "CN=nagramx-videotimer, O=local" >/dev/null 2>&1
  echo "      新建 keystore（之后重建复用）: $KS_W"
else
  echo "      复用已有 keystore: $KS_W"
fi
"$ZIPALIGN" -f -p 4 "$OUT_W\\unsigned.apk" "$OUT_W\\aligned.apk"
"$APKSIGNER" sign --ks "$KS_W" --ks-pass pass:android \
  --key-pass pass:android --v2-signing-enabled true \
  --out "$OUT_W\\module.apk" "$OUT_W\\aligned.apk" >/dev/null

echo
echo "=== 构建期验证 ==="
"$APKSIGNER" verify --print-certs "$OUT_W\\module.apk" | head -3

echo "-- APK 根目录下的 META-INF/xposed/（现代打包三件套）："
APK_META="$(unzip -l "$OUT/module.apk" | awk '{print $4}' | grep -E '^META-INF/xposed/' | sort)"
echo "$APK_META" | sed 's/^/   /'
for want in META-INF/xposed/java_init.list META-INF/xposed/module.prop META-INF/xposed/scope.list; do
  echo "$APK_META" | grep -qx "$want" || { echo "   缺少 $want ！现代打包下等于静默不生效" >&2; exit 1; }
done

echo "-- 确认没有回落 legacy 路径（assets/xposed_init 不该存在）："
if unzip -l "$OUT/module.apk" | grep -q 'assets/xposed_init'; then
  echo "   APK 里还有 assets/xposed_init，会让『是不是现代打包』变得含糊" >&2; exit 1
fi
echo "   ok  没有 assets/xposed_init"

echo "-- APK 内 java_init.list："
APK_INIT="$(unzip -p "$OUT/module.apk" META-INF/xposed/java_init.list | tr -d ' \r\n')"
echo "   $APK_INIT"
[ "$APK_INIT" = "$ENTRY_CLASS" ] || { echo "   java_init.list 与入口类不一致！" >&2; exit 1; }

echo "-- APK 内 module.prop："
unzip -p "$OUT/module.apk" META-INF/xposed/module.prop | sed 's/^/   /'

echo "-- APK 内 scope.list："
unzip -p "$OUT/module.apk" META-INF/xposed/scope.list | sed 's/^/   /'

echo "-- dex 内含入口类："
if unzip -p "$OUT/module.apk" classes.dex | grep -a "L${ENTRY_CLASS//./\/};" > /dev/null; then
  echo "   ok  L${ENTRY_CLASS//./\/};"
else
  echo "   入口类不在 dex 里！" >&2; exit 1
fi

echo "-- manifest："
"$AAPT2" dump xmltree "$OUT_W\\module.apk" --file AndroidManifest.xml \
  | grep -E 'versionName|versionCode|minSdkVersion|description|xposed' | sed 's/^ */   /'

echo
echo "=== 产物 ==="
"$JAR" tf "$OUT_W\\module.apk" | sed 's/^/   /'
ls -lh "$OUT/module.apk"
sha256sum "$OUT/module.apk"

mkdir -p "$ROOT/build"
FINAL="$ROOT/build/NagramXVideoTimer-v1.2.0.apk"
cp "$OUT/module.apk" "$FINAL"
echo "已同步输出: $FINAL"
