package app.encuentra

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Formato {
    private val es = Locale.forLanguageTag("es")

    fun tamano(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024L * 1024 * 1024 -> String.format(es, "%.1f MB", bytes / 1048576.0)
        else -> String.format(es, "%.2f GB", bytes / 1073741824.0)
    }

    private fun inicioDelDia(desfaseDias: Int): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        add(Calendar.DAY_OF_YEAR, -desfaseDias)
    }.timeInMillis

    /** Hora si es de hoy; dia y mes si es de este ano; si no, con ano. */
    fun fechaCorta(ms: Long): String {
        if (ms <= 0) return "sin fecha"
        val patron = when {
            ms >= inicioDelDia(0) -> "HH:mm"
            Calendar.getInstance().apply { timeInMillis = ms }.get(Calendar.YEAR) ==
                Calendar.getInstance().get(Calendar.YEAR) -> "d MMM, HH:mm"
            else -> "d MMM yyyy"
        }
        return SimpleDateFormat(patron, es).format(Date(ms))
    }

    fun fechaLarga(ms: Long): String =
        if (ms <= 0) "sin fecha" else SimpleDateFormat("EEEE d 'de' MMMM 'de' yyyy, HH:mm", es).format(Date(ms))

    /** El encabezado del grupo en la lista. */
    fun seccion(ms: Long): String {
        if (ms <= 0) return "Sin fecha"
        return when {
            ms >= inicioDelDia(0) -> "Hoy"
            ms >= inicioDelDia(1) -> "Ayer"
            ms >= inicioDelDia(6) -> "Últimos 7 días"
            ms >= inicioDelDia(29) -> "Últimos 30 días"
            else -> SimpleDateFormat("MMMM yyyy", es).format(Date(ms)).replaceFirstChar { it.uppercase(es) }
        }
    }
}
