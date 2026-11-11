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

## Compilar

Hace falta `D:\tools\jdk21` y `D:\tools\android-sdk`:

```
sh compilar.sh :app:assembleRelease
```

El APK sale en `app/build/outputs/apk/release/app-release.apk`. Se instala
copiándolo al teléfono. La primera vez pide «Acceso a todos los archivos».

## Por dentro

| Archivo | Qué hace |
|---|---|
| `Catalogo.kt` | Junta los archivos: índice de Android + recorrido de carpetas |
| `Copias.kt` | Encuentra copias idénticas (tamaño → bordes → SHA-256) |
| `Modelo.kt` | Ordena por fecha, agrupa copias, se actualiza al volver a la app |
| `Acciones.kt` | Abrir, compartir, borrar |
| `Miniaturas.kt` | Vista previa de fotos, videos y PDF |
| `ui/App.kt` | Las pantallas |
