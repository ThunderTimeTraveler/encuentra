package app.encuentra

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.media.ThumbnailUtils
import android.os.ParcelFileDescriptor
import android.util.LruCache
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
import java.io.File

/** Vista previa de fotos, videos y PDF, con el motor que Android trae dentro. */
object Miniaturas {

    private val memoria = object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 8).toInt()) {
        override fun sizeOf(key: String, value: Bitmap) = value.allocationByteCount
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val hilos = Dispatchers.IO.limitedParallelism(3)

    fun puedeTener(a: Archivo) = a.tipo == Tipo.FOTO || a.tipo == Tipo.VIDEO || a.extension == "pdf"

    fun enMemoria(a: Archivo, lado: Int): Bitmap? = memoria.get("${a.ruta}#$lado#${a.fecha}")

    suspend fun cargar(contexto: Context, a: Archivo, lado: Int): Bitmap? {
        if (!puedeTener(a)) return null
        val clave = "${a.ruta}#$lado#${a.fecha}"
        memoria.get(clave)?.let { return it }
        return withContext(hilos) {
            val bmp = try {
                crear(contexto, a, lado)
            } catch (_: Throwable) {
                null
            }
            if (bmp != null) memoria.put(clave, bmp)
            bmp
        }
    }

    private fun crear(contexto: Context, a: Archivo, lado: Int): Bitmap? {
        val tamano = Size(lado, lado)
        if (a.tipo == Tipo.FOTO || a.tipo == Tipo.VIDEO) {
            a.uriMedios?.let { u ->
                try {
                    return contexto.contentResolver.loadThumbnail(u, tamano, null)
                } catch (_: Exception) {
                }
            }
            val f = File(a.ruta)
            return if (a.tipo == Tipo.FOTO) ThumbnailUtils.createImageThumbnail(f, tamano, null)
            else ThumbnailUtils.createVideoThumbnail(f, tamano, null)
        }
        // PDF: la primera pagina.
        ParcelFileDescriptor.open(File(a.ruta), ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { pdf ->
                if (pdf.pageCount == 0) return null
                pdf.openPage(0).use { pagina ->
                    val escala = lado.toFloat() / maxOf(pagina.width, pagina.height)
                    val bmp = Bitmap.createBitmap(
                        maxOf(1, (pagina.width * escala).toInt()),
                        maxOf(1, (pagina.height * escala).toInt()),
                        Bitmap.Config.ARGB_8888,
                    )
                    bmp.eraseColor(Color.WHITE)
                    pagina.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    return bmp
                }
            }
        }
    }
}
