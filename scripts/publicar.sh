#!/bin/sh
# Publica la version que diga app/build.gradle.kts, en GitHub y en vowat.com.mx.
#
#   sh scripts/publicar.sh                 # PRUEBA: compila y comprueba, no sube nada
#   sh scripts/publicar.sh --publicar      # sube: release en GitHub + PR en vowatapp
#   sh scripts/publicar.sh --publicar --juntar   # ademas junta el PR a main (sale en la web)
#
# Variables opcionales:
#   NOTAS="..."        texto de la release en GitHub
#   VOWATAPP=<ruta>    clon de vowat-energy/vowatapp (por omision ../vowatapp)
#
# Antes de subir nada comprueba que:
#   - no haya cambios sin commit en este repo;
#   - la version no este ya publicada en GitHub;
#   - el APK este firmado con LA llave de siempre (si no, no se instala encima).
set -e

HUELLA="ff5bead462bfb4da481d87e4e9db532d5493c136c408ecf35a22be89b3d94615"

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

[ -f keystore.properties ] || morir "falta keystore.properties (copia keystore.properties.ejemplo y pon la llave encuentra-firma.keystore)"
[ -z "$(git status --porcelain)" ] || morir "hay cambios sin commit; haz commit antes de publicar"

sh compilar.sh :app:assembleRelease
cp app/build/outputs/apk/release/app-release.apk "$APK"

# La firma: con apksigner del SDK que encontro compilar.sh.
SDK=$(sed -n 's/^sdk.dir=//p' local.properties | sed 's/\\:/:/')
FIRMADOR=$(ls -d "$SDK"/build-tools/*/ 2>/dev/null | sort -V | tail -1)
[ -n "$FIRMADOR" ] || morir "no encuentro build-tools en $SDK"
if [ -f "$FIRMADOR/apksigner.bat" ]; then FIRMADOR="$FIRMADOR/apksigner.bat"; else FIRMADOR="$FIRMADOR/apksigner"; fi
TIENE=$("$FIRMADOR" verify --print-certs "$APK" | sed -n 's/.*SHA-256 digest: //p' | head -1 | tr -d '\r' | tr 'A-F' 'a-f')
[ "$TIENE" = "$HUELLA" ] || morir "el APK está firmado con OTRA llave ($TIENE). No se publica: no se instalaría encima."
echo "✓ firma correcta"
echo "✓ APK: $APK"

if [ "$PUBLICAR" = 0 ]; then
  echo "Prueba terminada. Para subirla: sh scripts/publicar.sh --publicar"
  exit 0
fi

# ---- GitHub ----
gh release view "v$VERSION" >/dev/null 2>&1 && morir "v$VERSION ya está publicada en GitHub: sube versionCode/versionName"
git push origin main
gh release create "v$VERSION" "$APK" --title "Encuentra $VERSION" \
  --notes "${NOTAS:-Encuentra $VERSION. Se instala encima de la versión anterior. Requiere Android 11 o superior.}"
echo "✓ GitHub: v$VERSION"

# ---- vowat.com.mx (sección de apps) ----
VOWATAPP=${VOWATAPP:-$RAIZ/../vowatapp}
[ -d "$VOWATAPP/.git" ] || morir "no encuentro el clon de vowatapp en $VOWATAPP (pon VOWATAPP=<ruta>)"
cd "$VOWATAPP"
[ -z "$(git status --porcelain)" ] || morir "vowatapp tiene cambios sin commit; no lo toco"
RAMA="apps/encuentra-$VERSION"
git fetch -q origin main
git checkout -q -b "$RAMA" origin/main
node scripts/publicar-app.mjs encuentra "$APK" --icono public/descargas/iconos/encuentra.png
git add lib/apps-descarga/publicadas.ts public/descargas/apps/encuentra.json public/descargas/apps/index.json public/descargas/encuentra.apk
git commit -q -m "apps: Encuentra $VERSION"
git push -q -u origin "$RAMA"
gh pr create --base main --head "$RAMA" --title "apps: Encuentra $VERSION" \
  --body "Encuentra $VERSION en la sección de apps. Misma firma de siempre. Medido con scripts/publicar-app.mjs. Release: https://github.com/ThunderTimeTraveler/encuentra/releases/tag/v$VERSION"
if [ "$JUNTAR" = 1 ]; then
  gh pr merge "$RAMA" --merge
  echo "✓ vowat: juntado a main; Vercel la publica en unos minutos"
else
  echo "✓ vowat: PR abierto. Para que salga en la web: gh pr merge $RAMA --repo vowat-energy/vowatapp --merge"
fi
