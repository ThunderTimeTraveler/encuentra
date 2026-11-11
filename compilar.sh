#!/bin/sh
# Uso: sh compilar.sh <tareas de gradle>   p. ej.  sh compilar.sh :app:assembleDebug
export JAVA_HOME=/d/tools/jdk21 ANDROID_HOME=/d/tools/android-sdk GRADLE_OPTS=-Xmx96m
export PATH="$JAVA_HOME/bin:$PATH"
[ -f local.properties ] || echo "sdk.dir=D\:/tools/android-sdk" > local.properties
./gradlew --no-daemon --max-workers=1 -Pkotlin.compiler.execution.strategy=in-process "-Dorg.gradle.jvmargs=-Xmx1100m -XX:+UseSerialGC -XX:MaxMetaspaceSize=512m" "$@"
