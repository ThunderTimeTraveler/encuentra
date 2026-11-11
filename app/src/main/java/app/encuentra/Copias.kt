package app.encuentra

import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest

/**
 * Encuentra archivos con EXACTAMENTE el mismo contenido, aunque se llamen
 * distinto o esten en otra carpeta. Nunca borra nada: solo agrupa, para que
 * cada archivo salga una sola vez en la lista.
 *
 * Tres pasadas para no leer de mas:
 *   1. Mismo tamano (un tamano unico no puede tener copia).
 *   2. Mismos primeros y ultimos 64 KB.
 *   3. Misma huella SHA-256 del archivo completo.
 */
object Copias {
    private const val BORDE = 64 * 1024

    /** Devuelve grupos de 2 o mas archivos identicos. */
    fun buscar(archivos: List<Archivo>, cancelado: () -> Boolean): List<List<Archivo>> {
        val grupos = ArrayList<List<Archivo>>()
        val porTamano = archivos.groupBy { it.tamano }.values.filter { it.size > 1 }
        for (mismoTamano in porTamano) {
            if (cancelado()) return grupos
            val porBordes = mismoTamano.groupBy { huella(it, soloBordes = true) }
                .filterKeys { it != null }.values.filter { it.size > 1 }
            for (candidatos in porBordes) {
                if (cancelado()) return grupos
                // Si el archivo cabe entero en los bordes, la huella de bordes ya es completa.
                val iguales = if (candidatos[0].tamano <= 2L * BORDE) listOf(candidatos)
                else candidatos.groupBy { huella(it, soloBordes = false) }
                    .filterKeys { it != null }.values.filter { it.size > 1 }
                grupos += iguales
            }
        }
        return grupos
    }

    private fun huella(a: Archivo, soloBordes: Boolean): String? = try {
        val md = MessageDigest.getInstance("SHA-256")
        RandomAccessFile(File(a.ruta), "r").use { f ->
            val buf = ByteArray(BORDE)
            if (soloBordes) {
                val n = f.read(buf)
                if (n > 0) md.update(buf, 0, n)
                if (f.length() > BORDE) {
                    f.seek(maxOf(BORDE.toLong(), f.length() - BORDE))
                    val m = f.read(buf)
                    if (m > 0) md.update(buf, 0, m)
                }
            } else {
                while (true) {
                    val n = f.read(buf)
                    if (n <= 0) break
                    md.update(buf, 0, n)
                }
            }
        }
        md.digest().joinToString("") { "%02x".format(it) }
    } catch (_: Exception) {
        null
    }
}
