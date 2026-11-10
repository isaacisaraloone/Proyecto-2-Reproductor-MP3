package interfaz

import javafx.scene.control.Alert
import javafx.scene.control.TableRow
import javafx.util.Duration
import nucleo.AlmacenMusica
import reproductor.MotorAudio
import reproductor.EstadoReproduccion
import reproductor.OyenteMotorAudio
import java.io.File

class Controlador(
    private val vista: Vista,
    private val repositorio: AlmacenMusica,
    private val motorAudio: MotorAudio
) : OyenteMotorAudio {

    private var usuarioDesplazandoBarra: Boolean = false
    private var duracionTotalEnSegundos: Double = 0.0
    private var idRolaReproduciendo: Int? = null

    init {
        motorAudio.registrarOyente(this)

        configurarEventosTabla()
        configurarEventosTransporte()
        configurarEventosEdicion()
        configurarEventosBusqueda()

        actualizarDatosTabla()
    }

    private fun configurarEventosTabla() {
        vista.tablaRolas.setRowFactory {
            val fila = TableRow<FilaRola>()
            fila.setOnMouseClicked { evento ->
                if (evento.clickCount == 2 && !fila.isEmpty) {
                    reproducirRolaSeleccionada(fila.item)
                }
            }
            fila
        }

        vista.tablaRolas.selectionModel.selectedItemProperty().addListener { _, _, seleccionada ->
            seleccionada?.let { rola ->
                vista.campoTextoTitulo.text = rola.propiedadTitulo.get()
                vista.campoTextoPista.text = rola.propiedadPista.get().toString()
                vista.campoTextoAnyo.text = rola.propiedadAnyo.get().toString()
                vista.campoTextoGenero.text = rola.propiedadGenero.get()
                vista.selectorTipoInterprete.value = rola.tipoInterprete
            }
        }
    }

    private fun configurarEventosTransporte() {
        vista.botonReproducir.setOnAction {
            val seleccionada = vista.tablaRolas.selectionModel.selectedItem
            if (motorAudio.estadoActual == EstadoReproduccion.PAUSADO) {
                motorAudio.reproducir()
            } else if (seleccionada != null) {
                reproducirRolaSeleccionada(seleccionada)
            }
        }

        vista.botonPausar.setOnAction { motorAudio.pausar() }
        vista.botonDetener.setOnAction { motorAudio.detener() }

        vista.controlVolumen.valueProperty().addListener { _, _, nuevoVolumen ->
            motorAudio.volumen = nuevoVolumen.toDouble()
        }

        vista.barraProgreso.setOnMousePressed { usuarioDesplazandoBarra = true }
        vista.barraProgreso.setOnMouseReleased {
            if (duracionTotalEnSegundos > 0.0) {
                val porcentaje = vista.barraProgreso.value / 100.0
                val segundosDestino = porcentaje * duracionTotalEnSegundos
                motorAudio.desplazarA(Duration.seconds(segundosDestino))
            }
            usuarioDesplazandoBarra = false
        }
    }

    private fun configurarEventosEdicion() {
        vista.botonGuardarCambios.setOnAction {
            val seleccionada = vista.tablaRolas.selectionModel.selectedItem ?: return@setOnAction

            val nuevoTitulo = vista.campoTextoTitulo.text.trim()
            if (nuevoTitulo.isEmpty()) {
                mostrarNotificacion("Error de Validación", "El título de la rola no puede estar vacío.", Alert.AlertType.ERROR)
                return@setOnAction
            }

            val nuevaPista = vista.campoTextoPista.text.trim().toIntOrNull()
            val nuevoAnyo = vista.campoTextoAnyo.text.trim().toIntOrNull()
            if (nuevaPista == null || nuevoAnyo == null) {
                mostrarNotificacion("Error de Validación", "El número de pista y el año deben ser números enteros válidos.", Alert.AlertType.ERROR)
                return@setOnAction
            }

            val nuevoGenero = vista.campoTextoGenero.text.trim().ifEmpty { "Unknown" }
            val nuevoTipo = vista.selectorTipoInterprete.value ?: seleccionada.tipoInterprete

            repositorio.actualizarMetadatosRola(
                idRola = seleccionada.idRola,
                nuevoTitulo = nuevoTitulo,
                nuevaPista = nuevaPista,
                nuevoAnyo = nuevoAnyo,
                nuevoGenero = nuevoGenero,
                nuevoIdInterprete = seleccionada.idInterprete,
                nuevoIdAlbum = seleccionada.idAlbum
            )

            repositorio.definirInterprete(
                idInterprete = seleccionada.idInterprete,
                tipo = nuevoTipo,
                nombreInterprete = seleccionada.propiedadInterprete.get()
            )

            seleccionada.propiedadTitulo.set(nuevoTitulo)
            seleccionada.propiedadPista.set(nuevaPista)
            seleccionada.propiedadAnyo.set(nuevoAnyo)
            seleccionada.propiedadGenero.set(nuevoGenero)
            vista.tablaRolas.items
                .filter { it.idInterprete == seleccionada.idInterprete }
                .forEach { it.tipoInterprete = nuevoTipo }

            mostrarNotificacion("Listo", "Metadatos actualizados en la base de datos.")
        }
    }

    private fun configurarEventosBusqueda() {
        vista.botonBuscar.setOnAction {
            val termino = vista.campoBusqueda.text.trim()
            if (termino.isBlank()) {
                actualizarDatosTabla()
                return@setOnAction
            }

            val patron = "%$termino%"
            val resultados = repositorio.buscarRolasConWhere(
                clausulaWhere = "r.title LIKE ? OR p.name LIKE ? OR a.name LIKE ?",
                parametros = listOf(patron, patron, patron)
            )
            cargarResultadosEnTabla(resultados)
        }

        vista.botonRestablecer.setOnAction {
            vista.campoBusqueda.clear()
            actualizarDatosTabla()
        }
    }

    private fun reproducirRolaSeleccionada(fila: FilaRola) {
        val archivoAudio = File(fila.rutaCancion)
        if (archivoAudio.exists()) {
            idRolaReproduciendo = fila.idRola
            motorAudio.cargarPista(archivoAudio)
            motorAudio.reproducir()
        } else {
            mostrarNotificacion("Error", "No se localizó el archivo: ${fila.rutaCancion}", Alert.AlertType.ERROR)
        }
    }

    fun actualizarDatosTabla() {
        val rolas = repositorio.obtenerTodasLasRolasDetalladas()
        cargarResultadosEnTabla(rolas)
    }

    private fun cargarResultadosEnTabla(rolas: List<nucleo.VistaDetalleRola>) {
        val filas = rolas.map {
            FilaRola(
                idRola = it.idRola,
                idInterprete = it.idInterprete,
                idAlbum = it.idAlbum,
                rutaCancion = it.rutaCancion,
                tipoInterprete = it.tipoInterprete,
                titulo = it.titulo,
                nombreInterprete = it.nombreInterprete,
                nombreAlbum = it.nombreAlbum,
                anyo = it.anyo,
                pista = it.pista,
                genero = it.genero
            )
        }
        vista.tablaRolas.items.setAll(filas)
    }

    override fun alCambiarEstado(nuevoEstado: EstadoReproduccion) {
        when (nuevoEstado) {
            EstadoReproduccion.REPRODUCIENDO -> vista.botonReproducir.text = "Reproduciendo"
            EstadoReproduccion.PAUSADO -> vista.botonReproducir.text = "Pausado"
            EstadoReproduccion.DETENIDO -> {
                vista.botonReproducir.text = "Reproducir"
                vista.barraProgreso.value = 0.0
                vista.etiquetaTiempo.text = "00:00 / 00:00"
            }
            EstadoReproduccion.ERROR -> vista.botonReproducir.text = "Error"
        }
    }

    override fun alCambiarProgreso(tiempoActual: Duration, duracionTotal: Duration) {
        val segundosTotales = duracionTotal.toSeconds()
        if (duracionTotal.isUnknown || segundosTotales <= 0.0 || usuarioDesplazandoBarra) return

        duracionTotalEnSegundos = segundosTotales
        val segundosActuales = tiempoActual.toSeconds()

        val porcentaje = (segundosActuales / duracionTotalEnSegundos) * 100.0
        vista.barraProgreso.value = porcentaje

        vista.etiquetaTiempo.text = "${formatearTiempo(tiempoActual)} / ${formatearTiempo(duracionTotal)}"
    }

    override fun alFinalizarCancion() {
        val items = vista.tablaRolas.items
        val indiceActual = items.indexOfFirst { it.idRola == idRolaReproduciendo }
            .takeIf { it >= 0 } ?: vista.tablaRolas.selectionModel.selectedIndex

        if (indiceActual in 0 until items.size - 1) {
            val siguiente = items[indiceActual + 1]
            vista.tablaRolas.selectionModel.select(siguiente)
            reproducirRolaSeleccionada(siguiente)
        }
    }

    override fun alOcurrirError(mensaje: String) {
        mostrarNotificacion("Error de Reproducción", mensaje, Alert.AlertType.ERROR)
    }

    private fun formatearTiempo(duracion: Duration): String {
        val totalSegundos = duracion.toSeconds().toInt()
        val minutos = totalSegundos / 60
        val segundos = totalSegundos % 60
        return "%02d:%02d".format(minutos, segundos)
    }

    private fun mostrarNotificacion(
        titulo: String,
        contenido: String,
        tipo: Alert.AlertType = Alert.AlertType.INFORMATION
    ) {
        val alerta = Alert(tipo).apply {
            this.title = titulo
            this.headerText = null
            this.contentText = contenido
        }
        alerta.showAndWait()
    }
}