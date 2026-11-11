#!/bin/sh
# Uso: sh compilar.sh <tareas de gradle>   p. ej.  sh compilar.sh :app:assembleRelease
#
# Busca Java y el SDK de Android solo: primero lo que ya este en el entorno
# (JAVA_HOME / ANDROID_HOME), luego D:\tools y luego donde los deja Android Studio.

primero_que_exista() {
  for d in "$@"; do [ -n "$d" ] && [ -d "$d" ] && { echo "$d"; return; }; done
}

JAVA_HOME=$(primero_que_exista "$JAVA_HOME" /d/tools/jdk21 "/c/Program Files/Android/Android Studio/jbr" "$LOCALAPPDATA/Programs/Android Studio/jbr")
ANDROID_HOME=$(primero_que_exista "$ANDROID_HOME" "$ANDROID_SDK_ROOT" /d/tools/android-sdk "$LOCALAPPDATA/Android/Sdk")
[ -n "$JAVA_HOME" ] || { echo "No encuentro Java (JDK 17+). Pon JAVA_HOME."; exit 1; }
[ -n "$ANDROID_HOME" ] || { echo "No encuentro el SDK de Android. Pon ANDROID_HOME."; exit 1; }
export JAVA_HOME ANDROID_HOME GRADLE_OPTS=-Xmx96m
export PATH="$JAVA_HOME/bin:$PATH"

# Gradle necesita saber donde esta el SDK; ese archivo es de cada maquina.
if command -v cygpath >/dev/null 2>&1; then SDK=$(cygpath -m "$ANDROID_HOME"); else SDK="$ANDROID_HOME"; fi
echo "sdk.dir=$(echo "$SDK" | sed 's/:/\:/')" > local.properties

./gradlew --no-daemon --max-workers=1 -Pkotlin.compiler.execution.strategy=in-process "-Dorg.gradle.jvmargs=-Xmx1100m -XX:+UseSerialGC -XX:MaxMetaspaceSize=512m" "$@"
