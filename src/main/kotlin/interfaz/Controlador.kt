package interfaz

import javafx.application.Platform
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.control.OverrunStyle
import javafx.scene.control.Separator
import javafx.scene.control.TableRow
import javafx.scene.control.TextField
import javafx.scene.control.Tooltip
import javafx.scene.image.ImageView
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.text.TextAlignment
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.util.Duration
import javafx.util.StringConverter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import minero.Audio
import nucleo.AlmacenMusica
import nucleo.GestorBaseDatos
import nucleo.GrupoMusical
import nucleo.Persona
import reproductor.MotorAudio
import reproductor.EstadoReproduccion
import reproductor.OyenteMotorAudio
import java.io.File
import java.text.Normalizer

class Controlador(
    private val vista: Vista,
    private var repositorio: AlmacenMusica,
    private val motorAudio: MotorAudio,
    private var gestorBD: GestorBaseDatos? = null
) : OyenteMotorAudio {

    private var usuarioDesplazandoBarra: Boolean = false
    private var duracionTotalEnSegundos: Double = 0.0
    private var idRolaReproduciendo: Int? = null
    private var ventanaBiblioteca: Stage? = null
    private var siguienteDesplegado: Boolean = false
    private var alturaMinimaBase: Double? = null
    private val tarjetasCuadricula = mutableMapOf<Int, VBox>()

    init {
        motorAudio.registrarOyente(this)

        configurarEventosTabla()
        configurarEventosTransporte()
        configurarEventosEdicion()
        configurarEventosBusqueda()
        configurarEventosArchivos()
        configurarEventosApendices()
        configurarModosVistaBiblioteca()
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
            actualizarEstadoSeleccionCuadricula(seleccionada?.idRola)
            actualizarVistaSiguienteCancion()
        }
    }

    private fun configurarEventosTransporte() {
        vista.botonReproducir.setOnAction {
        	when (motorAudio.estadoActual){
        		EstadoReproduccion.REPRODUCIENDO -> motorAudio.pausar()
        		EstadoReproduccion.PAUSADO -> motorAudio.reproducir()
        		else -> {
        			val seleccionada = vista.tablaRolas.selectionModel.selectedItem
						?: vista.tablaRolas.items.firstOrNull()?.also {
							vista.tablaRolas.selectionModel.select(it)
						}
        			if (seleccionada != null) {
        				reproducirRolaSeleccionada(seleccionada)
        			}
        		}
        	}
        }

        vista.botonAnterior.setOnAction { reproducirAnterior() }
        vista.botonSiguiente.setOnAction { reproducirSiguiente() }
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
            if (seleccionada.idRola == idRolaReproduciendo) {
                vista.etiquetaTituloActual.text = nuevoTitulo
            }
            actualizarVistaSiguienteCancion()

            mostrarNotificacion("Listo", "Metadatos actualizados en la base de datos.")
        }
    }

	private fun configurarEventosBusqueda() {
		vista.campoBusqueda.textProperty().addListener { _, _, nuevoTexto ->
			ejecutarBusqueda(nuevoTexto)
        }
        
        vista.botonBuscar.setOnAction {
            ejecutarBusqueda(vista.campoBusqueda.text)
        }

        vista.botonRestablecer.setOnAction {
            vista.campoBusqueda.clear()
            actualizarDatosTabla()
        }
    }
    
    private fun ejecutarBusqueda(texto: String?) {
        val termino = texto?.trim().orEmpty()
        if (termino.isBlank()) {
            actualizarDatosTabla()
            return
        }

        val todas = repositorio.obtenerTodasLasRolasDetalladas()
        val miembrosPorGrupo = repositorio.obtenerNombresMiembrosPorGrupo()
        val gruposOr = dividirPorOr(termino)

        val resultados = todas.filter { rola ->
            gruposOr.any { grupoAnd ->
                val tokens = tokenizarConsulta(grupoAnd)
                tokens.isNotEmpty() && tokens.all { token -> evaluarToken(rola, token, miembrosPorGrupo) }
            }
        }

        cargarResultadosEnTabla(resultados)
    }

    private fun dividirPorOr(consulta: String): List<String> {
        val grupos = mutableListOf<String>()
        val actual = StringBuilder()
        var enComillas = false
        var i = 0
        while (i < consulta.length) {
            val c = consulta[i]
            if (c == '"') {
                enComillas = !enComillas
                actual.append(c)
                i++
            } else if (!enComillas && consulta.substring(i).matches(Regex("""^\s+OR\s+.*""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)))) {
                grupos.add(actual.toString().trim())
                actual.clear()
                val match = Regex("""^\s+OR\s+""", RegexOption.IGNORE_CASE).find(consulta.substring(i))!!
                i += match.value.length
            } else {
                actual.append(c)
                i++
            }
        }
        if (actual.isNotBlank()) {
            grupos.add(actual.toString().trim())
        }
        return grupos
    }

    private fun tokenizarConsulta(consulta: String): List<String> {
        val tokens = mutableListOf<String>()
        val actual = StringBuilder()
        var enComillas = false
        for (c in consulta) {
            when {
                c == '"' -> {
                    enComillas = !enComillas
                    actual.append(c)
                }
                (c.isWhitespace() || c == '&') && !enComillas -> {
                    if (actual.isNotEmpty()) {
                        val t = actual.toString()
                        if (!t.equals("AND", ignoreCase = true)) {
                            tokens.add(t)
                        }
                        actual.clear()
                    }
                }
                else -> actual.append(c)
            }
        }
        if (actual.isNotEmpty()) {
            val t = actual.toString()
            if (!t.equals("AND", ignoreCase = true)) {
                tokens.add(t)
            }
        }
        return tokens
    }

    private fun evaluarToken(
        rola: nucleo.VistaDetalleRola,
        tokenOriginal: String,
        miembrosPorGrupo: Map<String, List<String>>
    ): Boolean {
        var token = tokenOriginal
        var negado = false
        if (token.startsWith("-") && token.length > 1) {
            negado = true
            token = token.substring(1)
        }

        val matchPrefijo = Regex("""^([a-zA-Z]{2}):(.*)$""").find(token)
        val coincide = if (matchPrefijo != null) {
            val prefijo = matchPrefijo.groupValues[1].lowercase()
            val valorBruto = matchPrefijo.groupValues[2].trim().removeSurrounding("\"")
            if (valorBruto.isEmpty()) {
                true
            } else {
                when (prefijo) {
                    "ti" -> normalizarTexto(rola.titulo).contains(normalizarTexto(valorBruto))
                    "ar", "in" -> coincideInterpreteOMiembro(rola, normalizarTexto(valorBruto), miembrosPorGrupo)
                    "al" -> normalizarTexto(rola.nombreAlbum).contains(normalizarTexto(valorBruto))
                    "ge" -> normalizarTexto(rola.genero).contains(normalizarTexto(valorBruto))
                    "an" -> evaluarNumero(rola.anyo, valorBruto)
                    "pi" -> evaluarNumero(rola.pista, valorBruto)
                    else -> coincidenciaGeneral(rola, normalizarTexto(token.removeSurrounding("\"")), miembrosPorGrupo)
                }
            }
        } else {
            coincidenciaGeneral(rola, normalizarTexto(token.removeSurrounding("\"")), miembrosPorGrupo)
        }

        return if (negado) !coincide else coincide
    }

    private fun coincideInterpreteOMiembro(
        rola: nucleo.VistaDetalleRola,
        valorNormalizado: String,
        miembrosPorGrupo: Map<String, List<String>>
    ): Boolean {
        if (normalizarTexto(rola.nombreInterprete).contains(valorNormalizado)) return true
        val miembros = miembrosPorGrupo[rola.nombreInterprete] ?: return false
        return miembros.any { normalizarTexto(it).contains(valorNormalizado) }
    }

    private fun evaluarNumero(valorReal: Int, expresion: String): Boolean {
        val rango = Regex("""^(\d+)\.\.(\d+)$""").find(expresion)
        if (rango != null) {
            val min = rango.groupValues[1].toIntOrNull() ?: return false
            val max = rango.groupValues[2].toIntOrNull() ?: return false
            return valorReal in min..max
        }
        val comp = Regex("""^(>=|<=|>|<|=)?(\d+)$""").find(expresion)
        if (comp != null) {
            val op = comp.groupValues[1]
            val num = comp.groupValues[2].toIntOrNull() ?: return false
            return when (op) {
                ">" -> valorReal > num
                "<" -> valorReal < num
                ">=" -> valorReal >= num
                "<=" -> valorReal <= num
                "=" -> valorReal == num
                else -> valorReal.toString().contains(expresion)
            }
        }
        return valorReal.toString().contains(expresion)
    }

    private fun coincidenciaGeneral(
        rola: nucleo.VistaDetalleRola,
        valorNormalizado: String,
        miembrosPorGrupo: Map<String, List<String>>
    ): Boolean {
        return normalizarTexto(rola.titulo).contains(valorNormalizado) ||
            coincideInterpreteOMiembro(rola, valorNormalizado, miembrosPorGrupo) ||
            normalizarTexto(rola.nombreAlbum).contains(valorNormalizado) ||
            normalizarTexto(rola.genero).contains(valorNormalizado) ||
            rola.anyo.toString().contains(valorNormalizado)
    }

    private fun normalizarTexto(texto: String): String {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replace(Regex("""\p{InCombiningDiacriticalMarks}+"""), "")
            .lowercase()
    }
    
    private fun configurarEventosArchivos() {
    	vista.botonMinarDirectorio.setOnAction {
    		val ventana = vista.panelRaiz.scene?.window
    		val selectorDirectorio = DirectoryChooser().apply {
				title = "Seleccionar directorio de musica para minar"
			}
    		val directorioSeleccionado = selectorDirectorio.showDialog(ventana) ?: return@setOnAction
    	
    		vista.botonMinarDirectorio.isDisable = true
    		vista.botonMinarDirectorio.text = "Minando..."
    	
    		val minero = Audio(repositorio)
    		CoroutineScope(Dispatchers.IO).launch {
    			try {
    				val total = minero.minarDirectorio(directorioSeleccionado) {
    					cantidad, _ -> Platform.runLater {
    						vista.botonMinarDirectorio.text = "Minando ($cantidad)..."
    						actualizarDatosTabla()
    					}
    				}
    				Platform.runLater {
    					vista.botonMinarDirectorio.text = "Minar Directorio..."
    					vista.botonMinarDirectorio.isDisable = false
    					actualizarDatosTabla()
    					mostrarNotificacion(
    						"Minado Finalizado",
    						"Se guardaron $total canciones desde:\n${directorioSeleccionado.absolutePath}"
    					)
					}
    			} catch (e: Exception) {
    				Platform.runLater {
    					vista.botonMinarDirectorio.text = "Minar Directorio..."
    					vista.botonMinarDirectorio.isDisable = false
    					mostrarNotificacion(
    						"Error al minar directorio",
    						"Ocurrió un error al intentar minar el directorio: ${e.localizedMessage}",
    						Alert.AlertType.ERROR
    					)
    				}
    			}
    		}
    	}
    	
    	vista.botonCambiarBaseDatos.setOnAction{
    		val ventana = vista.panelRaiz.scene?.window
    		val selectorArchivo = FileChooser().apply {
    			title = "Seleccionar archivo de Base de Datos"
    			extensionFilters.addAll(
    				FileChooser.ExtensionFilter("Bases de datos SQLite (*.db, *.sqlite)", "*.db", "*.sqlite"),
    				FileChooser.ExtensionFilter("Todos los archivos (*.*)", "*.*")
    			)
    		}
    		val archivoBd = selectorArchivo.showOpenDialog(ventana) ?: return@setOnAction
    		
    		try {
    			gestorBD?.close()
    			val nuevoGestor = GestorBaseDatos(archivoBd.absolutePath)
    			nuevoGestor.iniciarEsquema()
    			gestorBD = nuevoGestor
    			repositorio = AlmacenMusica(nuevoGestor.conexion)
    			actualizarDatosTabla()
    			mostrarNotificacion(
    				"Base de Datos Cargada",
    				"Usando base de datos:\n${archivoBd.absolutePath}"
    			)
    		} catch (e: Exception) {
    			mostrarNotificacion(
    				"Error de Base de Dator",
    				"No se pudo abrir la base de datos: ${e.localizedMessage}",
    				Alert.AlertType.ERROR
    			)
    		}
    	}
    	
    	vista.botonArtista.setOnAction {
    		abrirVentanaIndividuos()
    	}
    	
    	vista.botonBandas.setOnAction {
    		abrirVentanaBandas()
    	}
	}
	
	private fun abrirVentanaIndividuos() {
        val ventana = Stage().apply {
            title = "Artistas"
            initModality(Modality.APPLICATION_MODAL)
            (vista.panelBiblioteca.scene?.window ?: vista.panelRaiz.scene?.window)?.let { initOwner(it) }
        }

        val campoNombreArtistico = TextField().apply { promptText = "Nombre artístico (obligatorio)" }
        val campoNombreReal = TextField().apply { promptText = "Nombre real (opcional)" }
        val campoNacimiento = TextField().apply { promptText = "Fecha de nacimiento (opcional)" }
        val campoFallecimiento = TextField().apply { promptText = "Fecha de fallecimiento (opcional)" }
        val botonCrearPersona = Button("Registrar Individuo").apply { maxWidth = Double.MAX_VALUE }
        val listaIndividuos = ListView<String>().apply { prefHeight = 140.0 }

        fun recargarIndividuos() {
            val personas = repositorio.obtenerTodasLasPersonas()
            listaIndividuos.items.setAll(
                personas.map {
                    if (it.nombreReal.isNotBlank()) "${it.nombreArtistico} — ${it.nombreReal}" else it.nombreArtistico
                }
            )
        }

        botonCrearPersona.setOnAction {
            val artistico = campoNombreArtistico.text.trim()
            if (artistico.isEmpty()) {
                mostrarNotificacion("Error de Validación", "El nombre artístico es obligatorio.", Alert.AlertType.ERROR)
                return@setOnAction
            }
            if (repositorio.buscarPersonaPorNombreArtistico(artistico) != null) {
                mostrarNotificacion("Aviso", "Ya existe un individuo registrado con ese nombre artístico.")
                return@setOnAction
            }
            repositorio.crearPersona(
                Persona(
                    nombreArtistico = artistico,
                    nombreReal = campoNombreReal.text.trim(),
                    fechaNacimiento = campoNacimiento.text.trim(),
                    fechaFallecimiento = campoFallecimiento.text.trim()
                )
            )
            campoNombreArtistico.clear()
            campoNombreReal.clear()
            campoNacimiento.clear()
            campoFallecimiento.clear()
            recargarIndividuos()
            mostrarNotificacion("Listo", "Individuo '$artistico' registrado en la base de datos.")
        }

        recargarIndividuos()

        val layout = VBox(
            8.0,
            Label("Registrar Individuo (sin pista)").apply { style = "-fx-font-weight: bold; -fx-font-size: 13px;" },
            campoNombreArtistico,
            campoNombreReal,
            campoNacimiento,
            campoFallecimiento,
            botonCrearPersona,
            Separator(),
            Label("Individuos registrados:"),
            listaIndividuos
        ).apply {
            padding = Insets(14.0)
            prefWidth = 360.0
        }

        ventana.scene = Scene(layout)
        ventana.showAndWait()
    }

    private fun abrirVentanaBandas() {
        val ventana = Stage().apply {
            title = "Bandas"
            initModality(Modality.APPLICATION_MODAL)
            (vista.panelBiblioteca.scene?.window ?: vista.panelRaiz.scene?.window)?.let { initOwner(it) }
        }

        val selectorBanda = ComboBox<GrupoMusical>().apply {
            maxWidth = Double.MAX_VALUE
            promptText = "Selecciona una banda"
            converter = object : StringConverter<GrupoMusical>() {
                override fun toString(g: GrupoMusical?): String = g?.nombre ?: ""
                override fun fromString(s: String?): GrupoMusical? = null
            }
        }
        val selectorIndividuo = ComboBox<Persona>().apply {
            maxWidth = Double.MAX_VALUE
            promptText = "Selecciona un individuo registrado"
            converter = object : StringConverter<Persona>() {
                override fun toString(p: Persona?): String =
                    if (p == null) "" else if (p.nombreReal.isNotBlank()) "${p.nombreArtistico} (${p.nombreReal})" else p.nombreArtistico
                override fun fromString(s: String?): Persona? = null
            }
        }
        val botonAgregarABanda = Button("Agregar Individuo a Banda").apply { maxWidth = Double.MAX_VALUE }
        val listaMiembros = ListView<Persona>().apply {
            prefHeight = 140.0
            setCellFactory {
                object : javafx.scene.control.ListCell<Persona>() {
                    override fun updateItem(p: Persona?, empty: Boolean) {
                        super.updateItem(p, empty)
                        text = if (empty || p == null) null
                        else if (p.nombreReal.isNotBlank()) "${p.nombreArtistico} — ${p.nombreReal}" else p.nombreArtistico
                    }
                }
            }
        }
        val botonQuitarDeBanda = Button("Quitar Seleccionado de Banda").apply { maxWidth = Double.MAX_VALUE }

        selectorBanda.items.setAll(repositorio.obtenerTodosLosGrupos())
        selectorIndividuo.items.setAll(repositorio.obtenerTodasLasPersonas())

        fun actualizarMiembros() {
            val banda = selectorBanda.value
            if (banda == null) {
                listaMiembros.items.clear()
            } else {
                listaMiembros.items.setAll(repositorio.obtenerMiembrosDeGrupo(banda.idGrupo))
            }
        }

        selectorBanda.setOnAction { actualizarMiembros() }

        botonAgregarABanda.setOnAction {
            val banda = selectorBanda.value
            val persona = selectorIndividuo.value
            if (banda == null || persona == null) {
                mostrarNotificacion("Error de Validación", "Debes seleccionar una banda y un individuo.", Alert.AlertType.ERROR)
                return@setOnAction
            }
            repositorio.agregarPersonaAGrupo(persona.idPersona, banda.idGrupo)
            actualizarMiembros()
        }

        botonQuitarDeBanda.setOnAction {
            val banda = selectorBanda.value
            val miembro = listaMiembros.selectionModel.selectedItem
            if (banda == null || miembro == null) {
                mostrarNotificacion("Error de Validación", "Selecciona una banda y un integrante de la lista para quitarlo.", Alert.AlertType.ERROR)
                return@setOnAction
            }
            repositorio.quitarPersonaDeGrupo(miembro.idPersona, banda.idGrupo)
            actualizarMiembros()
        }

        val layout = VBox(
            8.0,
            Label("Agregar Individuo a Banda").apply { style = "-fx-font-weight: bold; -fx-font-size: 13px;" },
            Label("Banda:"),
            selectorBanda,
            Label("Individuo:"),
            selectorIndividuo,
            botonAgregarABanda,
            Separator(),
            Label("Integrantes de la banda:"),
            listaMiembros,
            botonQuitarDeBanda
        ).apply {
            padding = Insets(14.0)
            prefWidth = 360.0
        }

        ventana.scene = Scene(layout)
        ventana.showAndWait()
    }
	
    fun cerrarVentanas() {
        ventanaBiblioteca?.close()
        ventanaBiblioteca = null
    }
    
	fun cerrarBaseDatos() {
		gestorBD?.close()
	}
	
	private fun configurarEventosApendices() {
        vista.botonApendiceSiguiente.setOnAction {
            siguienteDesplegado = !siguienteDesplegado
            vista.panelSiguienteCancion.isVisible = siguienteDesplegado
            vista.panelSiguienteCancion.isManaged = siguienteDesplegado
            vista.botonApendiceSiguiente.text = if (siguienteDesplegado) "▲ Siguiente canción" else "▼ Siguiente canción"
            actualizarVistaSiguienteCancion()
            ajustarVentanaPrincipalAlContenido()
        }
        vista.botonApendiceBiblioteca.setOnAction {
            abrirVentanaBiblioteca()
        }
    }
	
    private fun ajustarVentanaPrincipalAlContenido(){
        val stage = vista.panelRaiz.scene?.window as? Stage ?: return
        val escena = stage.scene ?: return
        val base = alturaMinimaBase ?: stage.minHeight.also { alturaMinimaBase = it }
        val altoDecoracion = (stage.height - escena.height).coerceAtLeast(0.0)
        
        vista.panelRaiz.prefWidth = escena.width
        vista.panelRaiz.applyCss()
        vista.panelRaiz.layout()
        val altoNecesario = maxOf(vista.panelRaiz.prefHeight(escena.width) + altoDecoracion, base)
        
        if (!siguienteDesplegado) stage.minHeight = base
        stage.sizeToScene()
        if (siguienteDesplegado) stage.minHeight = altoNecesario
    }
    
	private fun configurarModosVistaBiblioteca() {
        vista.grupoModoVista.selectedToggleProperty().addListener { _, anterior, nuevo ->
            if (nuevo == null) {
                anterior?.isSelected = true
                return@addListener
            }
            val mostrarCuadricula = (nuevo == vista.botonVistaCuadricula)
            vista.scrollCuadricula.isVisible = mostrarCuadricula
            vista.scrollCuadricula.isManaged = mostrarCuadricula
            vista.tablaRolas.isVisible = !mostrarCuadricula
            vista.tablaRolas.isManaged = !mostrarCuadricula
        }
    }
	
	private fun abrirVentanaBiblioteca() {
        val existente = ventanaBiblioteca
        if (existente != null) {
            if (!existente.isShowing) {
                existente.show()
            }
            existente.toFront()
            existente.requestFocus()
            return
        }
        val ventana = Stage().apply {
            title = "Biblioteca y Gestión de Música"
            scene = Scene(vista.panelBiblioteca, 1020.0, 640.0)
            minWidth = 820.0
            minHeight = 520.0
            (vista.panelRaiz.scene?.window as? Stage)?.let { principal ->
                x = principal.x + principal.width + 12.0
                y = principal.y
            }
        }
        ventanaBiblioteca = ventana
        ventana.show()
    }

    private fun reproducirRolaSeleccionada(fila: FilaRola) {
        val archivoAudio = File(fila.rutaCancion)
        if (archivoAudio.exists()) {
            idRolaReproduciendo = fila.idRola
            actualizarPanelPrincipalConRola(fila)
            actualizarVistaSiguienteCancion()
            motorAudio.cargarPista(archivoAudio)
            motorAudio.reproducir()
        } else {
            mostrarNotificacion("Error", "No se localizó el archivo: ${fila.rutaCancion}", Alert.AlertType.ERROR)
        }
    }

    private fun actualizarPanelPrincipalConRola(fila: FilaRola) {
        vista.etiquetaTituloActual.text = fila.propiedadTitulo.get()
        vista.etiquetaInterpreteActual.text = "${fila.propiedadInterprete.get()} — ${fila.propiedadAlbum.get()}"
        vista.imagenCaratulaPrincipal.image = null
        vista.etiquetaSinCaratulaPrincipal.isVisible = true
        GestorCaratula.cargarCaratulaAsync(fila.rutaCancion) { img ->
            if (idRolaReproduciendo == fila.idRola) {
                vista.imagenCaratulaPrincipal.image = img
                vista.etiquetaSinCaratulaPrincipal.isVisible = (img == null)
            }
        }
    }
    
    private fun actualizarVistaSiguienteCancion() {
        val items = vista.tablaRolas.items
        val indiceActual = items.indexOfFirst { it.idRola == idRolaReproduciendo }
            .takeIf { it >= 0 } ?: vista.tablaRolas.selectionModel.selectedIndex
        if (indiceActual in 0 until items.size - 1) {
            val siguiente = items[indiceActual + 1]
            vista.etiquetaSiguienteTitulo.text = siguiente.propiedadTitulo.get()
            vista.etiquetaSiguienteDetalle.text = "${siguiente.propiedadInterprete.get()} — ${siguiente.propiedadAlbum.get()}"
            vista.imagenCaratulaSiguiente.image = null
            vista.etiquetaSinCaratulaSiguiente.isVisible = true
            GestorCaratula.cargarCaratulaAsync(siguiente.rutaCancion) { img ->
                vista.imagenCaratulaSiguiente.image = img
                vista.etiquetaSinCaratulaSiguiente.isVisible = (img == null)
            }
        } else {
            vista.etiquetaSiguienteTitulo.text = "No hay siguiente canción"
            vista.etiquetaSiguienteDetalle.text = ""
            vista.imagenCaratulaSiguiente.image = null
            vista.etiquetaSinCaratulaSiguiente.isVisible = true
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
        reconstruirCuadricula(filas)
        actualizarVistaSiguienteCancion()
    }
    
    private fun reconstruirCuadricula(filas: List<FilaRola>){
        vista.panelCuadricula.children.clear()
        tarjetasCuadricula.clear()
        val idSeleccionado = vista.tablaRolas.selectionModel.selectedItem?.idRola
        for (fila in filas) {
            val marcador = Label("♪").apply {
                style = "-fx-font-size: 34px; -fx-text-fill: #64748B;"
            }
            val imagen = ImageView().apply {
                fitWidth = 116.0
                fitHeight = 116.0
                isPreserveRatio = true
                isSmooth = true
            }
            val marcoCaratula = StackPane(marcador, imagen).apply {
                prefWidth = 120.0
                prefHeight = 120.0
                minWidth = 120.0
                minHeight = 120.0
                style = "-fx-background-color: #1E293B; -fx-background-radius: 6px;"
            }
            GestorCaratula.cargarCaratulaAsync(fila.rutaCancion) { img ->
                imagen.image = img
                marcador.isVisible = (img == null)
            }
            
            val etiquetaNombre = Label().apply {
                textProperty().bind(fila.propiedadTitulo)
                maxWidth = 120.0
                isWrapText = true
                textAlignment = TextAlignment.CENTER
                alignment = Pos.CENTER
                textOverrun = OverrunStyle.ELLIPSIS
                style = "-fx-font-size: 11px; -fx-font-weight: bold;"
            }
            val tarjeta = VBox(6.0, marcoCaratula, etiquetaNombre).apply {
                padding = Insets(8.0)
                prefWidth = 136.0
                alignment = Pos.TOP_CENTER
                Tooltip.install(this, Tooltip("${fila.propiedadTitulo.get()}\n${fila.propiedadInterprete.get()}"))
                setOnMouseClicked { evento ->
                    vista.tablaRolas.selectionModel.select(fila)
                    if (evento.clickCount == 2) {
                        reproducirRolaSeleccionada(fila)
                    }
                }
            }
            tarjetasCuadricula[fila.idRola] = tarjeta
            vista.panelCuadricula.children.add(tarjeta)
        }
        actualizarEstadoSeleccionCuadricula(idSeleccionado)
    }
    
    private fun actualizarEstadoSeleccionCuadricula(idSeleccionado: Int?) {
        tarjetasCuadricula.forEach { (idRola, tarjeta) ->
            tarjeta.style = if (idRola == idSeleccionado) {
                "-fx-background-color: #DBEAFE; -fx-background-radius: 8px; -fx-border-color: #2563EB; -fx-border-radius: 8px; -fx-cursor: hand;"
            } else {
                "-fx-background-color: #F8FAFC; -fx-background-radius: 8px; -fx-border-color: #E2E8F0; -fx-border-radius: 8px; -fx-cursor: hand;"
            }
        }
    }

    override fun alCambiarEstado(nuevoEstado: EstadoReproduccion) {
        when (nuevoEstado) {
            EstadoReproduccion.REPRODUCIENDO -> {
            	vista.botonReproducir.text = "⏸"
            	vista.botonReproducir.tooltip = Tooltip("Pausar")
            }
            EstadoReproduccion.PAUSADO -> {
                vista.botonReproducir.text = "▶"
                vista.botonReproducir.tooltip = Tooltip("Reproducir")
            }
            EstadoReproduccion.DETENIDO -> {
                vista.botonReproducir.text = "▶"
                vista.botonReproducir.tooltip = Tooltip("Reproducir")
                vista.barraProgreso.value = 0.0
                vista.etiquetaTiempo.text = "00:00 / 00:00"
            }
            EstadoReproduccion.ERROR -> {
            	vista.botonReproducir.text = "▶"
            	vista.botonReproducir.tooltip = Tooltip("Reproducir")
            }
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
    
    private fun reproducirSiguiente() {
        val items = vista.tablaRolas.items
        val indiceActual = items.indexOfFirst { it.idRola == idRolaReproduciendo }
            .takeIf { it >= 0 } ?: vista.tablaRolas.selectionModel.selectedIndex

        if (indiceActual in 0 until items.size - 1) {
            val siguiente = items[indiceActual + 1]
            vista.tablaRolas.selectionModel.select(siguiente)
            reproducirRolaSeleccionada(siguiente)
        }
    }

    private fun reproducirAnterior() {
        val items = vista.tablaRolas.items
        val indiceActual = items.indexOfFirst { it.idRola == idRolaReproduciendo }
            .takeIf { it >= 0 } ?: vista.tablaRolas.selectionModel.selectedIndex

        if (indiceActual > 0) {
            val anterior = items[indiceActual - 1]
            vista.tablaRolas.selectionModel.select(anterior)
            reproducirRolaSeleccionada(anterior)
        }
    }

    override fun alFinalizarCancion() {
        reproducirSiguiente()
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