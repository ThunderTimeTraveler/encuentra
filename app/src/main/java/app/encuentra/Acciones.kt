package app.encuentra

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

/** Abrir, compartir y borrar. */
object Acciones {

    private fun uri(contexto: Context, a: Archivo): Uri =
        FileProvider.getUriForFile(contexto, "${contexto.packageName}.archivos", File(a.ruta))

    private fun mime(a: Archivo) = a.mime ?: "*/*"

    fun abrir(contexto: Context, a: Archivo) {
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri(contexto, a), mime(a))
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            contexto.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(contexto, "No hay ninguna app para abrir este tipo de archivo", Toast.LENGTH_LONG).show()
        } catch (_: IllegalArgumentException) {
            Toast.makeText(contexto, "No se pudo abrir este archivo", Toast.LENGTH_LONG).show()
        }
    }

    fun compartir(contexto: Context, archivos: List<Archivo>) {
        if (archivos.isEmpty()) return
        val uris = try {
            archivos.map { uri(contexto, it) }
        } catch (_: IllegalArgumentException) {
            Toast.makeText(contexto, "No se pudo compartir", Toast.LENGTH_LONG).show()
            return
        }
        val mimes = archivos.map { mime(it) }.distinct()
        val tipo = when {
            mimes.size == 1 -> mimes[0]
            mimes.all { it.startsWith("image/") } -> "image/*"
            mimes.all { it.startsWith("video/") } -> "video/*"
            else -> "*/*"
        }
        val intent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris[0])
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        }.setType(tipo).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val titulo = if (uris.size == 1) "Compartir ${archivos[0].nombre}" else "Compartir ${uris.size} archivos"
        contexto.startActivity(
            Intent.createChooser(intent, titulo).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    /**
     * Borra los archivos indicados y nada mas. Solo se llama despues de que la
     * persona lo confirma. Devuelve las rutas que de verdad se fueron.
     */
    fun borrar(contexto: Context, archivos: List<Archivo>): Set<String> {
        val borradas = HashSet<String>()
        for (a in archivos) {
            val f = File(a.ruta)
            if (f.delete() || !f.exists()) {
                borradas += a.ruta
                a.uriMedios?.let { u ->
                    try {
                        contexto.contentResolver.delete(u, null, null)
                    } catch (_: Exception) {
                    }
                }
            }
        }
        // Que la galeria y los demas exploradores dejen de mostrarlos.
        if (borradas.isNotEmpty()) MediaScannerConnection.scanFile(contexto, borradas.toTypedArray(), null, null)
        return borradas
    }
}
