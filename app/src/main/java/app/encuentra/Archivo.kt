package app.encuentra

import android.net.Uri
import java.text.Normalizer
import java.util.Locale

/** Las familias que se pueden filtrar arriba de la lista. */
enum class Tipo(val etiqueta: String) {
    FOTO("Fotos"),
    VIDEO("Videos"),
    DOCUMENTO("Documentos"),
    AUDIO("Audio"),
    OTRO("Otros");

    companion object {
        private val documentos = setOf(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv", "rtf",
            "odt", "ods", "odp", "pages", "numbers", "key", "epub", "xml", "json", "html", "htm", "md",
        )
        private val fotos = setOf("jpg", "jpeg", "png", "gif", "webp", "heic", "heif", "bmp", "dng", "avif")
        private val videos = setOf("mp4", "mkv", "mov", "3gp", "webm", "avi", "m4v")
        private val audios = setOf("mp3", "m4a", "aac", "ogg", "opus", "wav", "flac", "amr", "wma")

        fun de(mime: String?, extension: String): Tipo {
            val m = mime.orEmpty()
            return when {
                m.startsWith("image/") || extension in fotos -> FOTO
                m.startsWith("video/") || extension in videos -> VIDEO
                m.startsWith("audio/") || extension in audios -> AUDIO
                extension in documentos || m.startsWith("text/") || m == "application/pdf" -> DOCUMENTO
                else -> OTRO
            }
        }
    }
}

/**
 * Un archivo del telefono, venga de donde venga.
 *
 * [fecha] es lo que ordena la lista: el momento mas reciente entre cuando llego
 * al telefono y cuando se modifico por ultima vez, en milisegundos.
 */
data class Archivo(
    val ruta: String,
    val nombre: String,
    val tamano: Long,
    val fecha: Long,
    val mime: String?,
    val tipo: Tipo,
    /** App o lugar del que vino: "WhatsApp", "Chrome", "Cámara"... */
    val origen: String,
    /** Sitio web del que se descargo, si Android lo apunto. */
    val descargadoDe: String?,
    /** Carpeta que lo contiene, legible: "Download", "WhatsApp/Media/WhatsApp Documents". */
    val carpeta: String,
    val enTarjetaSd: Boolean,
    /** Su entrada en el indice de medios de Android, si la tiene. */
    val uriMedios: Uri?,
) {
    val extension: String get() = nombre.substringAfterLast('.', "").lowercase(Locale.ROOT)

    /** Nombre y carpeta sin acentos ni mayusculas, para buscar. */
    val claveBusqueda: String = normalizar("$nombre $origen $carpeta")

    companion object {
        fun normalizar(texto: String): String =
            Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replace(Regex("\\p{Mn}+"), "")
                .lowercase(Locale.ROOT)
    }
}

/**
 * Lo que se ve como un renglon: un archivo y, si existe varias veces con el
 * mismo contenido exacto, sus copias. Asi nada sale repetido en la lista.
 */
data class Entrada(val archivo: Archivo, val copias: List<Archivo> = emptyList()) {
    val todas: List<Archivo> get() = listOf(archivo) + copias
}
