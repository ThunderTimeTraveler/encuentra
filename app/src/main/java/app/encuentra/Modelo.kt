package app.encuentra

import android.app.Application
import android.os.Environment
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class Estado(
    /** Todo, del mas reciente al mas viejo, sin repetidos. */
    val entradas: List<Entrada> = emptyList(),
    val cargando: Boolean = true,
    /** Recorriendo carpetas y comparando copias en segundo plano. */
    val revisando: Boolean = false,
)

class Modelo(app: Application) : AndroidViewModel(app) {

    private val catalogo = Catalogo(app)
    private val _estado = MutableStateFlow(Estado())
    val estado: StateFlow<Estado> = _estado

    private var trabajo: Job? = null
    private var ultimaRevisionCompleta = 0L
    private var extras: List<Archivo> = emptyList()
    private var grupos: List<List<Archivo>> = emptyList()

    /**
     * Se llama cada vez que la app vuelve a primer plano: lo que se acaba de
     * descargar aparece arriba sin hacer nada.
     *
     * El indice de Android se lee siempre (es rapido). El recorrido completo de
     * carpetas y la busqueda de copias, que leen disco, como mucho cada 5 minutos
     * salvo que se pida a mano.
     */
    fun actualizar(completo: Boolean = false) {
        if (!Environment.isExternalStorageManager()) return
        if (trabajo?.isActive == true && !completo) return
        trabajo?.cancel()
        trabajo = viewModelScope.launch {
            val indice = withContext(Dispatchers.IO) { catalogo.delIndice() }
            val vistos = indice.mapTo(HashSet()) { it.ruta }
            publicar(indice + extras.filter { it.ruta !in vistos }, grupos, cargando = false)

            val toca = completo || SystemClock.elapsedRealtime() - ultimaRevisionCompleta > 5 * 60_000L ||
                ultimaRevisionCompleta == 0L
            if (!toca) return@launch
            _estado.update { it.copy(revisando = true) }

            val visitadas = HashSet<String>()
            val mensajeria = withContext(Dispatchers.IO) { catalogo.deMensajeria(vistos, visitadas) }
            publicar(indice + mensajeria, grupos, cargando = false, revisando = true)
            val resto = withContext(Dispatchers.IO) { catalogo.fueraDelIndice(vistos, visitadas) }
            extras = mensajeria + resto
            val todos = indice + extras
            publicar(todos, grupos, cargando = false, revisando = true)

            val job = coroutineContext[Job]
            grupos = withContext(Dispatchers.IO) { Copias.buscar(todos) { job?.isActive != true } }
            if (!isActive) return@launch
            ultimaRevisionCompleta = SystemClock.elapsedRealtime()
            publicar(todos, grupos, cargando = false, revisando = false)
        }
    }

    /** Que ve la app (permisos, carpetas de WhatsApp...), para mostrarlo en pantalla. */
    suspend fun diagnostico(): String = withContext(Dispatchers.IO) {
        val e = _estado.value
        val porOrigen = e.entradas.flatMap { it.todas }.groupingBy { it.origen }.eachCount()
            .entries.sortedByDescending { it.value }.take(12)
            .joinToString("\n") { "  ${it.key}: ${it.value}" }
        catalogo.diagnostico() + "\nEn la lista ahora: ${e.entradas.size} renglones" +
            (if (e.revisando) " (todavía revisando)" else "") + "\n" + porOrigen
    }

    /** Despues de borrar: se quitan de la lista sin volver a buscar todo. */
    fun quitar(rutas: Set<String>) {
        if (rutas.isEmpty()) return
        extras = extras.filter { it.ruta !in rutas }
        grupos = grupos.map { g -> g.filter { it.ruta !in rutas } }.filter { it.size > 1 }
        val restantes = _estado.value.entradas.flatMap { it.todas }.filter { it.ruta !in rutas }
        publicar(restantes, grupos, cargando = false, revisando = _estado.value.revisando)
    }

    private fun publicar(
        archivos: List<Archivo>,
        grupos: List<List<Archivo>>,
        cargando: Boolean,
        revisando: Boolean = _estado.value.revisando,
    ) {
        val existentes = archivos.associateBy { it.ruta }
        val enGrupo = HashSet<String>()
        val entradas = ArrayList<Entrada>()
        for (g in grupos) {
            // Con los datos mas nuevos de cada copia; las que ya no estan, fuera.
            val vivos = g.mapNotNull { existentes[it.ruta] }.sortedByDescending { it.fecha }
            if (vivos.size < 2) continue
            vivos.forEach { enGrupo += it.ruta }
            entradas += Entrada(vivos[0], vivos.drop(1))
        }
        for (a in archivos) if (a.ruta !in enGrupo) entradas += Entrada(a)
        entradas.sortByDescending { it.archivo.fecha }
        _estado.update { it.copy(entradas = entradas, cargando = cargando, revisando = revisando) }
    }
}
