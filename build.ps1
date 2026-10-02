# PowerShell 原生构建 NagramX Video Timer LSPosed 模块（libxposed 现代打包 · API 102）
param([string]$OutputApk = '')
$ErrorActionPreference = "Stop"

$BT = if ($env:BT_W) { $env:BT_W } else { "D:\AndroidSDK\build-tools\34.0.0" }
$AJ = if ($env:AJ_W) { $env:AJ_W } else { "D:\AndroidSDK\platforms\android-34\android.jar" }
$JDK = if ($env:JDK_W) { $env:JDK_W } else { "D:\JAVA\bin" }

$SCRIPT_DIR = Split-Path -Parent $MyInvocation.MyCommand.Path
$MOD = Join-Path $SCRIPT_DIR "module"
$API = Join-Path $MOD "lib\api-102.jar"
$OUT = Join-Path $MOD ('build/package-' + [guid]::NewGuid().ToString('N'))
$KS = Join-Path $MOD "mod.keystore"
$RES = Join-Path $MOD "res"
$META = Join-Path $MOD "meta"
$META_X = Join-Path $META "META-INF\xposed"
$MANIFEST = Join-Path $MOD "AndroidManifest.xml"

$D8 = Join-Path $BT "d8.bat"
$AAPT2 = Join-Path $BT "aapt2.exe"
$AAPT = Join-Path $BT "aapt.exe"
$ZIPALIGN = Join-Path $BT "zipalign.exe"
$APKSIGNER = Join-Path $BT "apksigner.bat"
$JAVAC = Join-Path $JDK "javac.exe"
$JAR = Join-Path $JDK "jar.exe"
$KEYTOOL = Join-Path $JDK "keytool.exe"

foreach ($f in @($D8, $AAPT2, $AAPT, $ZIPALIGN, $APKSIGNER, $JAVAC, $JAR, $KEYTOOL, $AJ, $API,
                  (Join-Path $META_X "java_init.list"), (Join-Path $META_X "module.prop"),
                  (Join-Path $META_X "scope.list"))) {
    if (-not (Test-Path -LiteralPath $f)) {
        Write-Error "缺少依赖工具或文件: $f"
        exit 1
    }
}

$ENTRY_CLASS = (Get-Content -Path (Join-Path $META_X "java_init.list") -Raw).Trim()

Write-Host "[0/7] 一致性检查：代码 TARGET_PACKAGE <-> meta/META-INF/xposed/scope.list" -ForegroundColor Cyan
$codePkgs = Get-ChildItem -Path (Join-Path $MOD "src") -Filter "*.java" -Recurse |
    Select-String -Pattern 'TARGET_PACKAGE\s*=\s*"([^"]+)"' |
    ForEach-Object { $_.Matches.Groups[1].Value } |
    Sort-Object -Unique

$scopePkgs = Get-Content -Path (Join-Path $META_X "scope.list") |
    Where-Object { $_ -and -not $_.StartsWith("#") } |
    ForEach-Object { $_.Trim() } |
    Where-Object { $_ -ne "" } |
    Sort-Object -Unique

$diff = Compare-Object -ReferenceObject $codePkgs -DifferenceObject $scopePkgs
if ($diff) {
    Write-Error "代码里的目标包名与静态作用域声明不一致！"
    exit 1
}
foreach ($pkg in $codePkgs) {
    Write-Host "        $pkg"
}

Write-Host "[0b/7] 入口类与 java_init.list 一致性检查" -ForegroundColor Cyan
$entrySimple = $ENTRY_CLASS.Split(".")[-1]
$entryOk = Get-ChildItem -Path (Join-Path $MOD "src") -Filter "$entrySimple.java" -Recurse |
    Select-String -Pattern "class\s+$entrySimple\s+extends\s+XposedModule"
if (-not $entryOk) {
    Write-Error "入口类 $ENTRY_CLASS 不是 XposedModule 子类 —— 现代打包下会被静默跳过，拒绝出包"
    exit 1
}
Write-Host "        $ENTRY_CLASS extends XposedModule"

New-Item -ItemType Directory -Path (Join-Path $OUT "classes") -Force | Out-Null
New-Item -ItemType Directory -Path (Join-Path $OUT "res") -Force | Out-Null

$srcFiles = Get-ChildItem -Path (Join-Path $MOD "src") -Filter "*.java" -Recurse | ForEach-Object { $_.FullName }
Write-Host "[1/7] javac -> class（$($srcFiles.Count) 个源文件）" -ForegroundColor Cyan
& $JAVAC '-J-Duser.language=en' '-J-Duser.country=US' `
    -encoding UTF-8 -source 8 -target 8 -nowarn `
    -cp "$AJ;$API" -d (Join-Path $OUT "classes") $srcFiles
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "[2/7] jar" -ForegroundColor Cyan
& $JAR cf (Join-Path $OUT "classes.jar") -C (Join-Path $OUT "classes") .
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "[3/7] d8 -> classes.dex" -ForegroundColor Cyan
& $D8 --min-api 26 --lib $AJ --lib $API --output $OUT (Join-Path $OUT "classes.jar")
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "[4/7] aapt2 compile 资源" -ForegroundColor Cyan
& $AAPT2 compile --dir $RES -o (Join-Path $OUT "res\res.zip")
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "[5/7] aapt2 link -> base.apk" -ForegroundColor Cyan
& $AAPT2 link -o (Join-Path $OUT "base.apk") -I $AJ `
    --manifest $MANIFEST `
    --min-sdk-version 26 --target-sdk-version 28 `
    (Join-Path $OUT "res\res.zip")
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "[6/7] 加入 dex 与现代打包元数据" -ForegroundColor Cyan
Copy-Item (Join-Path $OUT "base.apk") (Join-Path $OUT "unsigned.apk")
Push-Location $OUT
try {
    & $AAPT add unsigned.apk classes.dex | Out-Null
    if ($LASTEXITCODE -ne 0) { throw '添加 classes.dex 失败' }
} finally {
    Pop-Location
}
Push-Location $META
try {
    & $AAPT add (Join-Path $OUT "unsigned.apk") `
        META-INF/xposed/module.prop META-INF/xposed/scope.list META-INF/xposed/java_init.list | Out-Null
    if ($LASTEXITCODE -ne 0) { throw '添加 Xposed 元数据失败' }
} finally {
    Pop-Location
}

Write-Host "[7/7] zipalign + 签名" -ForegroundColor Cyan
if (-not (Test-Path -LiteralPath $KS)) {
    & $KEYTOOL -genkeypair -keystore $KS -alias mod `
        -keyalg RSA -keysize 2048 -validity 10000 `
        -storepass android -keypass android -dname "CN=nagramx-videotimer, O=local" | Out-Null
    if ($LASTEXITCODE -ne 0) { throw '生成本地签名失败' }
    Write-Host "      新建 keystore: $KS"
} else {
    Write-Host "      复用已有 keystore: $KS"
}

& $ZIPALIGN -f -p 4 (Join-Path $OUT "unsigned.apk") (Join-Path $OUT "aligned.apk")
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

& $APKSIGNER sign --ks $KS --ks-pass pass:android `
    --key-pass pass:android --v2-signing-enabled true `
    --out (Join-Path $OUT "module.apk") (Join-Path $OUT "aligned.apk")
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "`n=== 构建期验证 ===" -ForegroundColor Green
& $APKSIGNER verify --print-certs (Join-Path $OUT "module.apk")
if ($LASTEXITCODE -ne 0) { throw 'APK 签名验证失败' }

$rootBuild = Join-Path $SCRIPT_DIR "build"
if (-not (Test-Path -LiteralPath $rootBuild)) {
    New-Item -ItemType Directory -Path $rootBuild -Force | Out-Null
}
$finalApk = if ($OutputApk) { [IO.Path]::GetFullPath($OutputApk) } else { Join-Path $rootBuild "NagramXVideoTimer-v1.2.0.apk" }
Copy-Item (Join-Path $OUT "module.apk") $finalApk -Force
Write-Host "已同步输出: $finalApk" -ForegroundColor Green
