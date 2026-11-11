#!/bin/sh
# Publica la version que diga app/build.gradle.kts, en GitHub y en vowat.com.mx.
#
#   sh scripts/publicar.sh                       # PRUEBA: no sube nada
#   sh scripts/publicar.sh --publicar            # release en GitHub + PR en vowatapp
#   sh scripts/publicar.sh --publicar --juntar   # ademas junta el PR a main (sale en la web)
#
# DOS FORMAS DE FIRMAR, elegidas solas:
#   - Esta maquina tiene la llave (keystore.properties): compila y firma aqui.
#   - No la tiene: la compila y firma GitHub (.github/workflows/publicar.yml,
#     con la llave guardada como secreto cifrado del repo). Este script sube la
#     etiqueta vX.Y.Z, espera la release y baja el APK de ahi.
#
# Variables opcionales:
#   VOWATAPP=<ruta>    clon de vowat-energy/vowatapp (por omision ../vowatapp)
#
# Nunca publica si: hay cambios sin commit, la version ya existe en GitHub, o el
# APK no esta firmado con LA llave de siempre (no se instalaria encima).
set -e

HUELLA="ff5bead462bfb4da481d87e4e9db532d5493c136c408ecf35a22be89b3d94615"
REPO="ThunderTimeTraveler/encuentra"

morir() { echo "✗ $*" >&2; exit 1; }

PUBLICAR=0; JUNTAR=0
for a in "$@"; do
  case "$a" in
    --publicar) PUBLICAR=1 ;;
    --juntar) JUNTAR=1 ;;
    *) morir "opción desconocida: $a" ;;
  esac
done

RAIZ=$(cd "$(dirname "$0")/.." && pwd)
cd "$RAIZ"

VERSION=$(sed -n 's/.*versionName = "\([^"]*\)".*/\1/p' app/build.gradle.kts)
[ -n "$VERSION" ] || morir "no encuentro versionName en app/build.gradle.kts"
APK="$RAIZ/Encuentra-$VERSION.apk"
echo "→ Encuentra $VERSION"

command -v gh >/dev/null || morir "falta gh (GitHub CLI); instálalo y entra con: gh auth login"
gh auth status >/dev/null 2>&1 || morir "gh no tiene sesión: gh auth login"
[ -z "$(git status --porcelain)" ] || morir "hay cambios sin commit; haz commit antes de publicar"

if [ -f keystore.properties ]; then MODO=local; else MODO=github; fi
echo "→ firma: $([ $MODO = local ] && echo 'en esta máquina' || echo 'en GitHub (secreto del repo)')"

YA=0; gh release view "v$VERSION" --repo "$REPO" >/dev/null 2>&1 && YA=1

# ---------- PRUEBA ----------
if [ "$PUBLICAR" = 0 ]; then
  [ "$YA" = 1 ] && echo "! v$VERSION ya existe en GitHub: para publicar hay que subir versionCode/versionName"
  if [ $MODO = local ]; then
    sh compilar.sh :app:assembleRelease
    cp app/build/outputs/apk/release/app-release.apk "$APK"
    echo "✓ compilado: $APK"
  else
    echo "→ pidiendo a GitHub una compilación de prueba (no publica nada)…"
    gh workflow run publicar.yml --repo "$REPO" --ref main
    sleep 8
    ID=$(gh run list --repo "$REPO" --workflow publicar.yml --event workflow_dispatch -L 1 --json databaseId --jq '.[0].databaseId')
    gh run watch "$ID" --repo "$REPO" --exit-status >/dev/null || morir "la compilación de prueba en GitHub falló: gh run view $ID --repo $REPO --log-failed"
    echo "✓ GitHub compiló y firmó con la llave correcta"
  fi
  echo "Prueba terminada. Para publicar: sh scripts/publicar.sh --publicar --juntar"
  exit 0
fi

# ---------- GITHUB ----------
[ "$YA" = 0 ] || morir "v$VERSION ya está publicada en GitHub: sube versionCode/versionName"
git push origin main

if [ $MODO = local ]; then
  sh compilar.sh :app:assembleRelease
  cp app/build/outputs/apk/release/app-release.apk "$APK"
  git tag "v$VERSION" && git push origin "v$VERSION"
  gh release create "v$VERSION" "$APK" --repo "$REPO" --title "Encuentra $VERSION" \
    --notes "Encuentra $VERSION. Se instala encima de la versión anterior. Requiere Android 11 o superior."
else
  git tag "v$VERSION" && git push origin "v$VERSION"
  echo "→ GitHub está compilando y firmando v$VERSION…"
  sleep 10
  ID=$(gh run list --repo "$REPO" --workflow publicar.yml --event push -L 1 --json databaseId --jq '.[0].databaseId')
  gh run watch "$ID" --repo "$REPO" --exit-status >/dev/null || morir "la publicación en GitHub falló: gh run view $ID --repo $REPO --log-failed"
  rm -f "$APK"
  gh release download "v$VERSION" --repo "$REPO" --pattern "Encuentra-$VERSION.apk" --dir "$RAIZ"
fi
[ -f "$APK" ] || morir "no tengo el APK $APK"
echo "✓ GitHub: https://github.com/$REPO/releases/tag/v$VERSION"

# ---------- VOWAT.COM.MX (sección de apps) ----------
# publicar-app.mjs vuelve a medir el APK (versión, tamaño, huella, firma) con
# las herramientas del SDK de Android (build-tools): hacen falta aquí.
VOWATAPP=${VOWATAPP:-$RAIZ/../vowatapp}
[ -d "$VOWATAPP/.git" ] || morir "no encuentro el clon de vowatapp en $VOWATAPP (pon VOWATAPP=<ruta>)"
cd "$VOWATAPP"
[ -z "$(git status --porcelain)" ] || morir "vowatapp tiene cambios sin commit; no lo toco"
RAMA="apps/encuentra-$VERSION"
git fetch -q origin main
git checkout -q -b "$RAMA" origin/main
node scripts/publicar-app.mjs encuentra "$APK" --icono public/descargas/iconos/encuentra.png
FIRMA_VOWAT=$(sed -n "/\"slug\": \"encuentra\"/,/}/s/.*\"firmaSha256\": \"\([0-9a-f]*\)\".*/\1/p" lib/apps-descarga/publicadas.ts)
[ "$FIRMA_VOWAT" = "$HUELLA" ] || morir "el APK está firmado con OTRA llave ($FIRMA_VOWAT). No se publica en vowat."
git add lib/apps-descarga/publicadas.ts public/descargas/apps/encuentra.json public/descargas/apps/index.json public/descargas/encuentra.apk
git commit -q -m "apps: Encuentra $VERSION"
git push -q -u origin "$RAMA"
gh pr create --repo vowat-energy/vowatapp --base main --head "$RAMA" --title "apps: Encuentra $VERSION" \
  --body "Encuentra $VERSION en la sección de apps. Misma firma de siempre. Medido con scripts/publicar-app.mjs. Release: https://github.com/$REPO/releases/tag/v$VERSION"
if [ "$JUNTAR" = 1 ]; then
  gh pr merge "$RAMA" --repo vowat-energy/vowatapp --merge
  echo "✓ vowat: juntado a main; Vercel la publica en unos minutos"
else
  echo "✓ vowat: PR abierto. Para que salga en la web: gh pr merge $RAMA --repo vowat-energy/vowatapp --merge"
fi
