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
   (platform 36, build-tools 35/36).  los encuentra solo; si no,
   pon  y .
2. Instala Git, Node y  (GitHub CLI), y entra con .
3. Clona este repo y, al lado,  (carpeta ).
4. **La llave de firma** (no está en el repo): pon  en
    y copia  como .
   Sin ella el release sale sin firmar y  se niega a subirlo.

## Compilar

\To honour the JVM settings for this build a single-use Daemon process will be forked. For more on this, please refer to https://docs.gradle.org/8.14.3/userguide/gradle_daemon.html#sec:disabling_the_daemon in the Gradle documentation.
Daemon will be stopped at the end of the build 
> Task :app:preBuild UP-TO-DATE
> Task :app:preReleaseBuild UP-TO-DATE
> Task :app:mergeReleaseJniLibFolders UP-TO-DATE
> Task :app:mergeReleaseNativeLibs UP-TO-DATE
> Task :app:stripReleaseDebugSymbols UP-TO-DATE
> Task :app:extractReleaseNativeSymbolTables UP-TO-DATE
> Task :app:mergeReleaseNativeDebugMetadata NO-SOURCE
> Task :app:checkReleaseDuplicateClasses UP-TO-DATE
> Task :app:buildKotlinToolingMetadata UP-TO-DATE
> Task :app:checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :app:checkReleaseAarMetadata UP-TO-DATE
> Task :app:processReleaseNavigationResources UP-TO-DATE
> Task :app:compileReleaseNavigationResources UP-TO-DATE
> Task :app:generateReleaseResValues UP-TO-DATE
> Task :app:mapReleaseSourceSetPaths UP-TO-DATE
> Task :app:generateReleaseResources UP-TO-DATE
> Task :app:mergeReleaseResources UP-TO-DATE
> Task :app:packageReleaseResources UP-TO-DATE
> Task :app:parseReleaseLocalResources UP-TO-DATE
> Task :app:createReleaseCompatibleScreenManifests UP-TO-DATE
> Task :app:extractDeepLinksRelease UP-TO-DATE
> Task :app:processReleaseMainManifest UP-TO-DATE
> Task :app:processReleaseManifest UP-TO-DATE
> Task :app:processReleaseManifestForPackage UP-TO-DATE
> Task :app:processReleaseResources UP-TO-DATE
> Task :app:compileReleaseKotlin UP-TO-DATE
> Task :app:javaPreCompileRelease UP-TO-DATE
> Task :app:compileReleaseJavaWithJavac NO-SOURCE
> Task :app:dexBuilderRelease UP-TO-DATE
> Task :app:desugarReleaseFileDependencies UP-TO-DATE
> Task :app:mergeReleaseStartupProfile UP-TO-DATE
> Task :app:mergeExtDexRelease UP-TO-DATE
> Task :app:mergeDexRelease UP-TO-DATE
> Task :app:mergeReleaseArtProfile UP-TO-DATE
> Task :app:mergeReleaseGlobalSynthetics UP-TO-DATE
> Task :app:compileReleaseArtProfile UP-TO-DATE
> Task :app:mergeReleaseShaders UP-TO-DATE
> Task :app:compileReleaseShaders NO-SOURCE
> Task :app:generateReleaseAssets UP-TO-DATE
> Task :app:mergeReleaseAssets UP-TO-DATE
> Task :app:compressReleaseAssets UP-TO-DATE
> Task :app:extractReleaseVersionControlInfo UP-TO-DATE
> Task :app:extractProguardFiles UP-TO-DATE
> Task :app:generateReleaseLintVitalReportModel UP-TO-DATE
> Task :app:lintVitalAnalyzeRelease UP-TO-DATE
> Task :app:lintVitalReportRelease UP-TO-DATE
> Task :app:lintVitalRelease
> Task :app:processReleaseJavaRes UP-TO-DATE
> Task :app:mergeReleaseJavaResource UP-TO-DATE
> Task :app:optimizeReleaseResources UP-TO-DATE
> Task :app:collectReleaseDependencies UP-TO-DATE
> Task :app:sdkReleaseDependencyData UP-TO-DATE
> Task :app:validateSigningRelease UP-TO-DATE
> Task :app:writeReleaseAppMetadata UP-TO-DATE
> Task :app:writeReleaseSigningConfigVersions UP-TO-DATE
> Task :app:packageRelease UP-TO-DATE
> Task :app:createReleaseApkListingFileRedirect UP-TO-DATE
> Task :app:assembleRelease UP-TO-DATE

BUILD SUCCESSFUL in 21s
50 actionable tasks: 1 executed, 49 up-to-date
El APK sale en .

## Publicar una versión

1. Sube  y  en  y haz commit.
2. : prueba. Compila y comprueba la firma, sin subir nada.
3. : release en GitHub y PR en vowatapp.
   Con  además junta el PR a main y sale en la sección de apps de
   vowat.com.mx cuando Vercel termina.

La firma tiene que ser siempre la misma (SHA-256
); si no, el
APK no se instala encima del anterior y el script no publica.

## Por dentro

| Archivo | Qué hace |
|---|---|
| `Catalogo.kt` | Junta los archivos: índice de Android + recorrido de carpetas |
| `Copias.kt` | Encuentra copias idénticas (tamaño → bordes → SHA-256) |
| `Modelo.kt` | Ordena por fecha, agrupa copias, se actualiza al volver a la app |
| `Acciones.kt` | Abrir, compartir, borrar |
| `Miniaturas.kt` | Vista previa de fotos, videos y PDF |
| `ui/App.kt` | Las pantallas |
