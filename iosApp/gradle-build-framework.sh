#!/bin/sh
# 由 Xcode Run Script 调用：构建 FarmMobile Kotlin framework
# 依据 Xcode 环境变量决定目标平台与配置
set -e
cd "$SRCROOT/.."

export JAVA_HOME="$HOME/tools/jdk-17.0.21.1+2/Contents/Home"
GRADLE="$HOME/tools/gradle-8.11.1/bin/gradle"

PLATFORM="$PLATFORM_NAME"
CONFIG="$CONFIGURATION"

echo "Building FarmMobile framework: platform=$PLATFORM config=$CONFIG"

if [ "$PLATFORM" = "iphoneos" ]; then
    if [ "$CONFIG" = "Release" ]; then
        TASK=":app:linkReleaseFrameworkIosArm64"
    else
        TASK=":app:linkDebugFrameworkIosArm64"
    fi
    FRAMEWORK_DIR="$SRCROOT/../app/build/bin/iosArm64"
else
    if [ "$CONFIG" = "Release" ]; then
        TASK=":app:linkReleaseFrameworkIosSimulatorArm64"
    else
        TASK=":app:linkDebugFrameworkIosSimulatorArm64"
    fi
    FRAMEWORK_DIR="$SRCROOT/../app/build/bin/iosSimulatorArm64"
fi

"$GRADLE" "$TASK" --console=plain -q

# 拷贝到稳定路径供 Xcode 链接（-F 搜索路径 = .framework 的父目录）
DEST="$SRCROOT/build"
mkdir -p "$DEST"
rm -rf "$DEST/FarmMobile.framework"
cp -R "$FRAMEWORK_DIR/$(echo $CONFIG | tr 'A-Z' 'a-z')Framework/FarmMobile.framework" "$DEST/"
echo "Framework ready at $DEST/FarmMobile.framework"
