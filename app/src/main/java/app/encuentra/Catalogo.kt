package app.encuentra

import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import java.io.File
import java.util.Locale

/**
 * Junta en una sola lista TODOS los archivos del telefono: memoria interna y
 * tarjeta SD, de todas las carpetas.
 *
 * Dos fuentes:
 *  1. El indice de Android (MediaStore). Es rapido y ademas sabe que app guardo
 *     cada archivo y, en las descargas, de que pagina vino.
 *  2. Un recorrido de carpetas, para lo que el indice no tiene (por ejemplo
 *     carpetas con `.nomedia`, como algunas de WhatsApp).
 *
 * Se salta Android/data y Android/obb: Android no deja entrar ahi a nadie.
 */
class Catalogo(private val contexto: Context) {

    private val paquetes = contexto.packageManager
    private val nombresDeApp = HashMap<String, String>()
    private val raizInterna: String = Environment.getExternalStorageDirectory().absolutePath

    /** Solo el indice de Android: se muestra en menos de un segundo. */
    fun delIndice(): List<Archivo> {
        val encontrados = ArrayList<Archivo>()
        for (volumen in MediaStore.getExternalVolumeNames(contexto)) {
            val descargas = sitiosDeDescarga(volumen)
            leerVolumen(volumen, descargas, encontrados)
        }
        return encontrados
    }

    /** Lo que el indice no tenia, buscando carpeta por carpeta. */
    fun fueraDelIndice(yaVistos: Set<String>): List<Archivo> {
        val extra = ArrayList<Archivo>()
        for (raiz in raices()) recorrer(raiz, yaVistos, extra, profundidad = 0)
        return extra
    }

    private fun leerVolumen(volumen: String, descargas: Map<Long, String>, salida: MutableList<Archivo>) {
        val uri = MediaStore.Files.getContentUri(volumen)
        val columnas = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.OWNER_PACKAGE_NAME,
        )
        // 12289 (0x3001) es una carpeta, no un archivo.
        val filtro = "format != 12289 AND ${MediaStore.Files.FileColumns.SIZE} > 0"
        val cursor = try {
            contexto.contentResolver.query(uri, columnas, filtro, null, null)
        } catch (e: Exception) {
            null
        } ?: return
        cursor.use { c ->
            val iId = c.getColumnIndexOrThrow(columnas[0])
            val iRuta = c.getColumnIndexOrThrow(columnas[1])
            val iNombre = c.getColumnIndexOrThrow(columnas[2])
            val iTam = c.getColumnIndexOrThrow(columnas[3])
            val iAgregado = c.getColumnIndexOrThrow(columnas[4])
            val iModificado = c.getColumnIndexOrThrow(columnas[5])
            val iMime = c.getColumnIndexOrThrow(columnas[6])
            val iDueno = c.getColumnIndexOrThrow(columnas[7])
            while (c.moveToNext()) {
                val ruta = c.getString(iRuta) ?: continue
                if (esPrivada(ruta)) continue
                val nombre = c.getString(iNombre) ?: ruta.substringAfterLast('/')
                if (nombre.startsWith(".")) continue
                val id = c.getLong(iId)
                val fecha = maxOf(c.getLong(iAgregado), c.getLong(iModificado)) * 1000L
                val mime = c.getString(iMime)
                salida += construir(
                    ruta = ruta,
                    nombre = nombre,
                    tamano = c.getLong(iTam),
                    fecha = fecha,
                    mime = mime,
                    dueno = c.getString(iDueno),
                    descargadoDe = descargas[id],
                    uriMedios = ContentUris.withAppendedId(uri, id),
                )
            }
        }
    }

    /** id -> sitio web, para las descargas en las que el navegador lo apunto. */
    private fun sitiosDeDescarga(volumen: String): Map<Long, String> {
        val mapa = HashMap<Long, String>()
        val columnas = arrayOf(
            MediaStore.Downloads._ID,
            MediaStore.Downloads.DOWNLOAD_URI,
            MediaStore.Downloads.REFERER_URI,
        )
        try {
            contexto.contentResolver.query(MediaStore.Downloads.getContentUri(volumen), columnas, null, null, null)
                ?.use { c ->
                    while (c.moveToNext()) {
                        val sitio = sitio(c.getString(2)) ?: sitio(c.getString(1)) ?: continue
                        mapa[c.getLong(0)] = sitio
                    }
                }
        } catch (_: Exception) {
            // Algunos fabricantes no tienen estas columnas: entonces no se sabe, y no se inventa.
        }
        return mapa
    }

    private fun sitio(texto: String?): String? {
        if (texto.isNullOrBlank()) return null
        val host = try {
            Uri.parse(texto).host
        } catch (_: Exception) {
            null
        } ?: return null
        return host.removePrefix("www.")
    }

    private fun raices(): List<File> {
        val raices = ArrayList<File>()
        raices += Environment.getExternalStorageDirectory()
        val sm = contexto.getSystemService(StorageManager::class.java)
        for (v in sm.storageVolumes) {
            val dir = v.directory ?: continue
            if (raices.none { it.absolutePath == dir.absolutePath }) raices += dir
        }
        return raices.filter { it.isDirectory }
    }

    private fun recorrer(carpeta: File, yaVistos: Set<String>, salida: MutableList<Archivo>, profundidad: Int) {
        if (profundidad > 25) return
        val hijos = carpeta.listFiles() ?: return
        for (f in hijos) {
            if (f.name.startsWith(".")) continue
            if (f.isDirectory) {
                if (!esPrivada(f.absolutePath)) recorrer(f, yaVistos, salida, profundidad + 1)
                continue
            }
            val ruta = f.absolutePath
            if (ruta in yaVistos) continue
            val tamano = f.length()
            if (tamano <= 0) continue
            val ext = f.extension.lowercase(Locale.ROOT)
            salida += construir(
                ruta = ruta,
                nombre = f.name,
                tamano = tamano,
                fecha = f.lastModified(),
                mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext),
                dueno = null,
                descargadoDe = null,
                uriMedios = null,
            )
        }
    }

    private fun esPrivada(ruta: String): Boolean {
        val r = ruta.lowercase(Locale.ROOT)
        return "/android/data/" in "$r/" || "/android/obb/" in "$r/"
    }

    private fun construir(
        ruta: String,
        nombre: String,
        tamano: Long,
        fecha: Long,
        mime: String?,
        dueno: String?,
        descargadoDe: String?,
        uriMedios: Uri?,
    ): Archivo {
        val enSd = !ruta.startsWith(raizInterna)
        val carpeta = carpetaLegible(ruta)
        val ext = nombre.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return Archivo(
            ruta = ruta,
            nombre = nombre,
            tamano = tamano,
            fecha = fecha,
            mime = mime,
            tipo = Tipo.de(mime, ext),
            origen = Origenes.de(carpeta, dueno?.let { nombreDeApp(it) }),
            descargadoDe = descargadoDe,
            carpeta = carpeta,
            enTarjetaSd = enSd,
            uriMedios = uriMedios,
        )
    }

    /** "/storage/emulated/0/Download/x.pdf" -> "Download". */
    private fun carpetaLegible(ruta: String): String {
        val padre = ruta.substringBeforeLast('/')
        val partes = padre.split('/').filter { it.isNotEmpty() }
        // /storage/emulated/0/... o /storage/XXXX-XXXX/...
        val salto = if (padre.startsWith("/storage/emulated/")) 3 else 2
        return partes.drop(salto).joinToString("/").ifEmpty { "Raíz" }
    }

    private fun nombreDeApp(paquete: String): String? {
        if (paquete == contexto.packageName) return null
        nombresDeApp[paquete]?.let { return it.ifEmpty { null } }
        val nombre = try {
            paquetes.getApplicationLabel(paquetes.getApplicationInfo(paquete, 0)).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            ""
        }
        nombresDeApp[paquete] = nombre
        return nombre.ifEmpty { null }
    }
}

/** De donde vino un archivo, en palabras. Primero la carpeta, que no miente; luego la app duena. */
object Origenes {
    fun de(carpeta: String, appDuena: String?): String {
        val c = carpeta.lowercase(Locale.ROOT)
        return when {
            "whatsapp business" in c || "com.whatsapp.w4b" in c -> "WhatsApp Business"
            "whatsapp" in c -> "WhatsApp"
            "telegram" in c -> "Telegram"
            "screenshots" in c -> "Captura de pantalla"
            "screenrecord" in c || "screen recordings" in c -> "Grabación de pantalla"
            c.startsWith("dcim") -> "Cámara"
            "bluetooth" in c -> "Bluetooth"
            "instagram" in c -> "Instagram"
            "messenger" in c -> "Messenger"
            appDuena != null -> appDuena
            c.startsWith("download") -> "Descargas"
            c.startsWith("documents") -> "Documentos"
            c.startsWith("music") -> "Música"
            c.startsWith("recordings") -> "Grabadora"
            c.startsWith("pictures") -> "Imágenes"
            else -> carpeta.substringBefore('/')
        }
    }
}
