# Encuentra

App de Android para **encontrar tus archivos** sin andar buscando en carpetas.

Al abrirla ves **todos** los archivos del teléfono —memoria interna y tarjeta SD,
de WhatsApp, Descargas, la cámara, Telegram y cualquier otra app— en **una sola
lista**, siempre **del más reciente al más viejo**, sin importar el tipo.

- **Compartir** desde la misma lista (botón en cada renglón), o varios a la vez
  manteniendo presionado para seleccionar.
- **Buscar** por nombre, app o carpeta, sin importar acentos ni mayúsculas.
- **Filtrar** por Fotos, Videos, Documentos, Audio u Otros.
- **De dónde vino**: la app que lo guardó y, si el navegador lo apuntó, el sitio
  del que se descargó.
- **Vista previa** de fotos, videos y PDF.
- **Sin repetidos**: si el mismo archivo (idéntico, byte por byte) está en varios
  lugares, sale una sola vez con «Está en N lugares».
- **Borrar** siempre con confirmación. La app nunca borra nada por su cuenta.

Nada sale del teléfono: la app no pide permiso de internet.

## Preparar una computadora nueva

1. Instala Android Studio (trae Java y el SDK) o pon JDK 17+ y el SDK de Android
   (platform 36, build-tools 35/36). `compilar.sh` los encuentra solo; si no,
   pon `JAVA_HOME` y `ANDROID_HOME`.
2. Instala Git, Node y `gh` (GitHub CLI), y entra con `gh auth login`.
3. Clona este repo y, al lado, `vowat-energy/vowatapp` (carpeta `../vowatapp`).
4. **Opcional, para firmar en esta máquina** (si no, firma GitHub): pon `encuentra-firma.keystore` en
   `firma/` y copia `keystore.properties.ejemplo` como `keystore.properties`.
   Sin la llave, `compilar.sh` deja el release sin firmar (no se instala).

## Compilar

```
sh compilar.sh :app:assembleRelease
```

El APK sale en `app/build/outputs/apk/release/app-release.apk`.

## Publicar una versión

1. Sube `versionCode` y `versionName` en `app/build.gradle.kts` y haz commit.
2. `sh scripts/publicar.sh`: prueba, no sube nada.
3. `sh scripts/publicar.sh --publicar --juntar`: release en GitHub, PR en
   vowatapp y juntado a main. Sale en la sección de apps de vowat.com.mx
   cuando Vercel termina (unos minutos).

**Quién firma.** Si la máquina tiene `keystore.properties`, firma ella. Si no,
firma **GitHub** (`.github/workflows/publicar.yml`) con la llave guardada como
secreto cifrado del repo (`FIRMA_KEYSTORE_B64`). Así cualquier computadora con
`gh` puede publicar sin tener la llave. El script sube la etiqueta `vX.Y.Z`,
espera a que GitHub cree la release y baja el APK de ahí para vowat.

Para la parte de vowat hacen falta Node, un clon de `vowat-energy/vowatapp` en
`../vowatapp` y las build-tools del SDK de Android (las usa
`publicar-app.mjs` para medir el APK).

La firma tiene que ser siempre la misma (SHA-256
`ff5bead462bfb4da481d87e4e9db532d5493c136c408ecf35a22be89b3d94615`); si no, el
APK no se instala encima del anterior, y ni GitHub ni el script la publican.

## Por dentro

| Archivo | Qué hace |
|---|---|
| `Catalogo.kt` | Junta los archivos: índice de Android + recorrido de carpetas |
| `Copias.kt` | Encuentra copias idénticas (tamaño → bordes → SHA-256) |
| `Modelo.kt` | Ordena por fecha, agrupa copias, se actualiza al volver a la app |
| `Acciones.kt` | Abrir, compartir, borrar |
| `Miniaturas.kt` | Vista previa de fotos, videos y PDF |
| `ui/App.kt` | Las pantallas |
