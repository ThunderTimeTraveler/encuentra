package app.encuentra.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.encuentra.Acciones
import app.encuentra.Archivo
import app.encuentra.Entrada
import app.encuentra.Formato
import app.encuentra.Miniaturas
import app.encuentra.Modelo
import app.encuentra.Tipo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun App() {
    val modelo: Modelo = viewModel()
    var permiso by remember { mutableStateOf(Environment.isExternalStorageManager()) }
    // Cada vez que se vuelve a la app: se revisa el permiso y se traen los archivos nuevos.
    LifecycleResumeEffect(Unit) {
        permiso = Environment.isExternalStorageManager()
        if (permiso) modelo.actualizar()
        onPauseOrDispose { }
    }
    if (permiso) PantallaLista(modelo) else PantallaPermiso()
}

@Composable
private fun PantallaPermiso() {
    val ctx = LocalContext.current
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(28.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Encuentra", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text(
            "Para juntar en una sola lista todos tus archivos —los de WhatsApp, Descargas, la cámara, " +
                "Telegram, la tarjeta SD…— Android pide que actives «Acceso a todos los archivos».\n\n" +
                "Nada sale del teléfono: la app no usa internet.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = {
            val paraEsta = Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:${ctx.packageName}"),
            )
            try {
                ctx.startActivity(paraEsta)
            } catch (_: ActivityNotFoundException) {
                ctx.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            }
        }) { Text("Activar acceso") }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun PantallaLista(modelo: Modelo) {
    val ctx = LocalContext.current
    val alcance = rememberCoroutineScope()
    val estado by modelo.estado.collectAsStateWithLifecycle()

    var consulta by rememberSaveable { mutableStateOf("") }
    var filtro by rememberSaveable { mutableStateOf<Tipo?>(null) }
    var seleccion by remember { mutableStateOf(emptySet<String>()) }
    var detalle by remember { mutableStateOf<Entrada?>(null) }
    var porBorrar by remember { mutableStateOf<List<Archivo>?>(null) }
    var diagnostico by remember { mutableStateOf<String?>(null) }

    val visibles = remember(estado.entradas, consulta, filtro) {
        val palabras = Archivo.normalizar(consulta).split(' ').filter { it.isNotBlank() }
        estado.entradas.filter { e ->
            (filtro == null || e.archivo.tipo == filtro) &&
                palabras.all { p -> e.todas.any { p in it.claveBusqueda } }
        }
    }
    // Renglones de la lista: encabezado de fecha ("Hoy", "Ayer"...) o archivo.
    val filas = remember(visibles) {
        val salida = ArrayList<Any>(visibles.size + 24)
        var anterior: String? = null
        for (e in visibles) {
            val s = Formato.seccion(e.archivo.fecha)
            if (s != anterior) {
                salida += s
                anterior = s
            }
            salida += e
        }
        salida
    }
    val porRuta = remember(visibles) { visibles.associateBy { it.archivo.ruta } }

    val lista = rememberLazyListState()
    // Lo mas reciente siempre a la vista: si llega algo nuevo y estabas arriba, se queda arriba.
    LaunchedEffect(visibles.firstOrNull()?.archivo?.ruta, consulta, filtro) {
        if (lista.firstVisibleItemIndex <= 2) lista.scrollToItem(0)
    }

    BackHandler(enabled = seleccion.isNotEmpty()) { seleccion = emptySet() }

    fun elegidos(): List<Archivo> = seleccion.mapNotNull { porRuta[it] }.map { it.archivo }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Surface(tonalElevation = 2.dp) {
            Column(Modifier.statusBarsPadding().padding(top = 8.dp, bottom = 4.dp)) {
                if (seleccion.isEmpty()) {
                    Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = consulta,
                            onValueChange = { consulta = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Buscar por nombre, app o carpeta") },
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            trailingIcon = {
                                if (consulta.isNotEmpty()) {
                                    IconButton(onClick = { consulta = "" }) { Icon(Icons.Default.Close, "Borrar búsqueda") }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(28.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        )
                        IconButton(onClick = { modelo.actualizar(completo = true) }) {
                            Icon(Icons.Default.Refresh, "Buscar de nuevo")
                        }
                        IconButton(onClick = {
                            alcance.launch { diagnostico = modelo.diagnostico() }
                        }) { Icon(Icons.Default.Info, "Diagnóstico") }
                    }
                } else {
                    Row(Modifier.padding(horizontal = 4.dp).height(56.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { seleccion = emptySet() }) { Icon(Icons.Default.Close, "Cancelar") }
                        Text(
                            "${seleccion.size} seleccionado${if (seleccion.size == 1) "" else "s"}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { Acciones.compartir(ctx, elegidos()) }) {
                            Icon(Icons.Default.Share, "Compartir")
                        }
                        IconButton(onClick = {
                            porBorrar = seleccion.mapNotNull { porRuta[it] }.flatMap { it.todas }
                        }) { Icon(Icons.Default.Delete, "Borrar") }
                    }
                }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        FilterChip(selected = filtro == null, onClick = { filtro = null }, label = { Text("Todos") })
                    }
                    items(Tipo.entries) { t ->
                        FilterChip(
                            selected = filtro == t,
                            onClick = { filtro = if (filtro == t) null else t },
                            label = { Text(t.etiqueta) },
                        )
                    }
                }
            }
        }

        if (estado.cargando) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(
                "Juntando tus archivos…",
                Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            Text(
                buildString {
                    append(String.format(Locale.forLanguageTag("es"), "%,d", visibles.size))
                    append(if (visibles.size == 1) " archivo" else " archivos")
                    append(", los más recientes primero")
                    if (estado.revisando) append(" · revisando carpetas y copias…")
                },
                Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (visibles.isEmpty()) {
                Text(
                    if (consulta.isNotBlank()) "Nada coincide con «$consulta»." else "No hay archivos de este tipo.",
                    Modifier.padding(24.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        LazyColumn(
            state = lista,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            items(
                filas,
                key = { if (it is String) "h:$it" else (it as Entrada).archivo.ruta },
                contentType = { if (it is String) 0 else 1 },
            ) { fila ->
                if (fila is String) {
                    Text(
                        fila,
                        Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    val e = fila as Entrada
                    val ruta = e.archivo.ruta
                    Fila(
                        entrada = e,
                        enSeleccion = seleccion.isNotEmpty(),
                        seleccionado = ruta in seleccion,
                        alTocar = {
                            if (seleccion.isNotEmpty()) {
                                seleccion = if (ruta in seleccion) seleccion - ruta else seleccion + ruta
                            } else {
                                detalle = e
                            }
                        },
                        alMantener = { seleccion = seleccion + ruta },
                        alCompartir = { Acciones.compartir(ctx, listOf(e.archivo)) },
                    )
                }
            }
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }

    detalle?.let { e ->
        ModalBottomSheet(
            onDismissRequest = { detalle = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Detalle(
                e,
                alBorrar = { porBorrar = it },
            )
        }
    }

    diagnostico?.let { texto ->
        AlertDialog(
            onDismissRequest = { diagnostico = null },
            title = { Text("Diagnóstico") },
            text = {
                SelectionContainer {
                    Text(
                        texto,
                        Modifier.verticalScroll(rememberScrollState()),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            confirmButton = { TextButton(onClick = { diagnostico = null }) { Text("Cerrar") } },
        )
    }

    porBorrar?.let { archivos ->
        val lugares = archivos.size
        val total = archivos.sumOf { it.tamano }
        AlertDialog(
            onDismissRequest = { porBorrar = null },
            title = {
                Text(
                    if (lugares == 1) "¿Borrar ${archivos[0].nombre}?" else "¿Borrar $lugares archivos?",
                )
            },
            text = {
                Text(
                    "Se borra${if (lugares == 1) "" else "n"} del teléfono para siempre " +
                        "(${Formato.tamano(total)}). No se puede deshacer.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        porBorrar = null
                        alcance.launch {
                            val borradas = withContext(Dispatchers.IO) { Acciones.borrar(ctx, archivos) }
                            modelo.quitar(borradas)
                            seleccion = seleccion - borradas
                            detalle = null
                            val fallaron = archivos.size - borradas.size
                            Toast.makeText(
                                ctx,
                                if (fallaron == 0) "Borrado" else "$fallaron no se pudieron borrar",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Borrar") }
            },
            dismissButton = { TextButton(onClick = { porBorrar = null }) { Text("Cancelar") } },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Fila(
    entrada: Entrada,
    enSeleccion: Boolean,
    seleccionado: Boolean,
    alTocar: () -> Unit,
    alMantener: () -> Unit,
    alCompartir: () -> Unit,
) {
    val a = entrada.archivo
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (seleccionado) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.background)
            .combinedClickable(onClick = alTocar, onLongClick = alMantener)
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Miniatura(a, 52.dp, 160)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(a.nombre, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                "${a.origen} · ${Formato.tamano(a.tamano)} · ${Formato.fechaCorta(a.fecha)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (entrada.copias.isNotEmpty()) {
                Text(
                    "Está en ${entrada.copias.size + 1} lugares",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
        if (enSeleccion) {
            Checkbox(checked = seleccionado, onCheckedChange = { alTocar() })
        } else {
            IconButton(onClick = alCompartir) { Icon(Icons.Default.Share, "Compartir ${a.nombre}") }
        }
    }
}

@Composable
private fun Miniatura(a: Archivo, tam: Dp, lado: Int) {
    val ctx = LocalContext.current
    val imagen by produceState<Bitmap?>(Miniaturas.enMemoria(a, lado), a.ruta, a.fecha) {
        if (value == null) value = Miniaturas.cargar(ctx, a, lado)
    }
    val fondo = when (a.tipo) {
        Tipo.FOTO, Tipo.VIDEO -> MaterialTheme.colorScheme.surfaceVariant
        Tipo.DOCUMENTO -> MaterialTheme.colorScheme.primaryContainer
        Tipo.AUDIO -> MaterialTheme.colorScheme.tertiaryContainer
        Tipo.OTRO -> MaterialTheme.colorScheme.secondaryContainer
    }
    Box(
        Modifier.size(tam).clip(RoundedCornerShape(8.dp)).background(fondo),
        contentAlignment = Alignment.Center,
    ) {
        val bmp = imagen
        if (bmp != null) {
            Image(bmp.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Text(
                a.extension.uppercase(Locale.ROOT).take(4).ifEmpty { "?" },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        if (a.tipo == Tipo.VIDEO) {
            Text(
                "▶",
                Modifier.align(Alignment.BottomEnd).padding(3.dp),
                color = androidx.compose.ui.graphics.Color.White,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun Detalle(e: Entrada, alBorrar: (List<Archivo>) -> Unit) {
    val ctx = LocalContext.current
    val a = e.archivo
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
    ) {
        if (Miniaturas.puedeTener(a)) {
            val grande by produceState<Bitmap?>(null, a.ruta) { value = Miniaturas.cargar(ctx, a, 1080) }
            grande?.let {
                Image(
                    it.asImageBitmap(),
                    "Vista previa",
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .combinedClickableAbrir { Acciones.abrir(ctx, a) },
                    contentScale = ContentScale.Fit,
                )
                Spacer(Modifier.height(12.dp))
            }
        }
        Text(a.nombre, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Dato("Fecha", Formato.fechaLarga(a.fecha))
        Dato("De dónde vino", a.origen)
        a.descargadoDe?.let { Dato("Descargado de", it) }
        Dato("Dónde está", (if (a.enTarjetaSd) "Tarjeta SD" else "Memoria interna") + " › " + a.carpeta)
        Dato("Tamaño", Formato.tamano(a.tamano))

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { Acciones.abrir(ctx, a) }) { Text("Abrir") }
            OutlinedButton(onClick = { Acciones.compartir(ctx, listOf(a)) }) { Text("Compartir") }
            TextButton(
                onClick = { alBorrar(e.todas) },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text("Borrar") }
        }

        if (e.copias.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            Text(
                "El mismo archivo, idéntico, está también en:",
                style = MaterialTheme.typography.titleSmall,
            )
            for (c in e.copias) {
                Text(
                    "• ${if (c.enTarjetaSd) "Tarjeta SD" else "Memoria interna"} › ${c.carpeta}  (${c.origen}, ${Formato.fechaCorta(c.fecha)})",
                    Modifier.padding(top = 6.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { alBorrar(e.copias) }) {
                Text(
                    "Dejar solo este y borrar ${if (e.copias.size == 1) "la copia" else "las ${e.copias.size} copias"}",
                )
            }
        }
    }
}

@Composable
private fun Dato(etiqueta: String, valor: String) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, style = MaterialTheme.typography.bodyLarge)
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.combinedClickableAbrir(alTocar: () -> Unit): Modifier =
    this.combinedClickable(onClick = alTocar)
