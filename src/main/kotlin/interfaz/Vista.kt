package interfaz

import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.image.ImageView
import javafx.scene.layout.*
import nucleo.TipoInterprete
import javax.swing.JToggleButton

class Vista {
	val panelRaiz = BorderPane()
	
	val botonMinarDirectorio = Button("Minar directorio")
	val botonCambiarBaseDatos = Button("Base de Datos")
	val barraSuperiorPrincipal = HBox(10.0, botonMinarDirectorio, botonCambiarBaseDatos).apply { 
		padding = Insets (10.0, 10.0, 6.0, 10.0)
		alignment = Pos.CENTER
	}
	val etiquetaSinCaratulaPrincipal = Label("♪").apply {
		style = "-fx-font-size: 64px; -fx-text-fill: #64748B;"
	}
	val imagenCaratulaPrincipal = ImageView().apply {
		fitWidth = 200.0
		fitHeight = 200.0
		isPreserveRatio = true
		isSmooth = true
	}
	val contenedorCaratulaPrincipal = StackPane(etiquetaSinCaratulaPrincipal, imagenCaratulaPrincipal).apply {
		prefWidth = 210.0
		prefHeight = 210.0
		maxWidth = 210.0
		maxHeight = 210.0
		style = "-fx-background-color: #1E293B; -fx-background-radius: 10px; -fx-border-color: #334155; -fx-border-radius: 10px;"
	}
	val etiquetaTituloActual = Label("Sin reproducción").apply {
		style = "-fx-font-weight: bold; -fx-font-size: 14px;"
		maxWidth = 310.0
	}
	val etiquetaInterpreteActual = Label("Mina una carpeta o abre la biblioteca").apply {
		style = "-fx-text-fill: #64748B; -fx-font-size: 12px;"
		maxWidth = 310.0
	}
	val panelCentralPrincipal = VBox(8.0, contenedorCaratulaPrincipal, etiquetaTituloActual, etiquetaInterpreteActual).apply {
		padding = Insets(8.0, 12.0, 8.0, 12.0)
		alignment = Pos.CENTER
	}
	val botonApendiceBiblioteca = Button("›").apply {
		prefWidth = 26.0
		minWidth = 26.0
		maxHeight = Double.MAX_VALUE
		tooltip = Tooltip("Abrir biblioteca, buscador y editor")
		style = "-fx-font-size: 16px; -fx-font-weight: bold; -fx-background-color: #F8C8DC; -fx-border-color: #E5A3BF; -fx-text-fill: #4A152C; -fx-background-radius: 6px 0 0 6px; -fx-border-radius: 6px 0 0 6px; -fx-cursor: hand;"
	}
	val botonAnterior = Button("⏮").apply {
		style = "-fx-font-size: 15px; -fx-cursor: hand;"
		prefWidth = 42.0
		tooltip = Tooltip("Canción anterior")
	}
	val botonReproducir = Button("▶").apply {
		style = "-fx-font-size: 15px; -fx-cursor: hand;"
		prefWidth = 46.0
		tooltip = Tooltip("Reproducir")
	}
	val botonSiguiente = Button("⏭").apply {
		style = "-fx-font-size: 15px; -fx-cursor: hand;"
		prefWidth = 42.0
		tooltip = Tooltip("Siguiente canción")
	}
	val botonDetener = Button("⏹").apply {
		style = "-fx-font-size: 15px; -fx-cursor: hand;"
		prefWidth = 42.0
		tooltip = Tooltip("Detener")
	}
	val barraProgreso = Slider(0.0, 100.0, 0.0).apply {
		HBox.setHgrow(this, Priority.ALWAYS)
	}
	val etiquetaTiempo = Label("00:00 / 00:00")
	val controlVolumen = Slider(0.0, 1.0, 1.0).apply {
		prefWidth = 120.0
	}
	val botonApendiceSiguiente = Button("▼ Siguiente canción").apply {
		maxWidth = Double.MAX_VALUE
		style = "-fx-font-size: 11px; -fx-font-weight: bold; -fx-background-color: #F8C8DC; -fx-border-color: #E5A3BF; -fx-text-fill: #4A152C; -fx-background-radius: 6px; -fx-border-radius: 6px; -fx-cursor: hand;"
		tooltip = Tooltip("Mostrar u ocultar la siguiente canción")
	}
	val etiquetaSinCaratulaSiguiente = Label("♪").apply {
		style = "-fx-font-size: 18px; -fx-text-fill: #64748B;"
	}
	val imagenCaratulaSiguiente = ImageView().apply {
		fitWidth = 44.0
		fitHeight = 44.0
		isPreserveRatio = true
		isSmooth = true
	}
	val contenedorCaratulaSiguiente = StackPane(etiquetaSinCaratulaSiguiente, imagenCaratulaSiguiente).apply {
		prefWidth = 46.0
		prefHeight = 46.0
		minWidth = 46.0
		minHeight = 46.0
		style = "-fx-background-color: #1E293B; -fx-background-radius: 6px;"
	}
	val etiquetaSiguienteTitulo = Label("No hay siguiente canción").apply {
		style = "-fx-font-weight: bold; -fx-font-size: 12px;"
		maxWidth = 260.0
	}
	val etiquetaSiguienteDetalle = Label("").apply {
		style = "-fx-text-fill: #64748B; -fx-font-size: 11px;"
		maxWidth = 260.0
	}
	val panelSiguienteCancion = VBox(
		6.0,
		Separator(),
		HBox(
			10.0,
			contenedorCaratulaSiguiente,
			VBox(2.0, Label("Siguiente canción:").apply { style = "-fx-font-size: 10px; -fx-text-fill: #64748B;" }, etiquetaSiguienteTitulo, etiquetaSiguienteDetalle)
		).apply {
			alignment = Pos.CENTER_LEFT
			padding = Insets(2.0, 4.0, 4.0, 4.0)
		}
	).apply {
		isVisible = false
		isManaged = false
	}
	val panelReproductorPrincipal = VBox(
		8.0,
		HBox(8.0, barraProgreso, etiquetaTiempo).apply { alignment = Pos.CENTER },
		HBox(8.0, botonAnterior, botonReproducir, botonSiguiente, botonDetener).apply { alignment = Pos.CENTER },
		HBox(8.0, Label("Volumen:"), controlVolumen).apply { alignment = Pos.CENTER },
		botonApendiceSiguiente,
		panelSiguienteCancion
	).apply {
		padding = Insets(4.0, 12.0, 10.0, 12.0)
		alignment = Pos.CENTER
	}
	val panelBiblioteca = BorderPane()
	val campoBusqueda = TextField().apply {
		promptText = "Buscar (ej. ar:Queen & an:>1975, ti:..., ge:..., al:...)"
		HBox.setHgrow(this, Priority.ALWAYS)
	}
	val botonBuscar = Button("Buscar")
	val botonRestablecer = Button("Restablecer")
	val grupoModoVista = ToggleGroup()
	val botonVistaLista = ToggleButton("Lista").apply { 
		toggleGroup = grupoModoVista
		isSelected = true
	}
	val botonVistaCuadricula = ToggleButton("Cuadricula").apply { 
		toggleGroup = grupoModoVista
	}
	
	val barraBusqueda = HBox(
		10.0,
		campoBusqueda,
		botonBuscar,
		botonRestablecer,
		Separator(Orientation.VERTICAL),
		Label ("Vista:"),
		botonVistaLista,
		botonVistaCuadricula
	).apply {
		padding = Insets(10.0)
		alignment = Pos.CENTER
	}
	
	val tablaRolas = TableView<FilaRola>().apply {
		columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
		
		val columnaPista = TableColumn<FilaRola, Number>("Pista").apply {
			setCellValueFactory { it.value.propiedadPista }
			prefWidth = 45.0
		}
		val columnaTitulo = TableColumn<FilaRola, String>("Título").apply {
			setCellValueFactory { it.value.propiedadTitulo }
		}
		val columnaInterprete = TableColumn<FilaRola, String>("Intérprete").apply {
			setCellValueFactory { it.value.propiedadInterprete }
		}
		val columnaAlbum = TableColumn<FilaRola, String>("Álbum").apply {
			setCellValueFactory { it.value.propiedadAlbum }
		}
		val columnaAnyo = TableColumn<FilaRola, Number>("Año").apply {
			setCellValueFactory { it.value.propiedadAnyo }
			prefWidth = 60.0
		}
		val columnaGenero = TableColumn<FilaRola, String>("Género").apply {
			setCellValueFactory { it.value.propiedadGenero }
		}
		
		columns.addAll(columnaPista, columnaTitulo, columnaInterprete, columnaAlbum, columnaAnyo, columnaGenero)
	}
	
	val panelCuadricula = FlowPane(12.0, 12.0).apply { 
		padding = Insets(12.0)
		alignment = Pos.TOP_LEFT
	}
	val scrollCuadricula = ScrollPane(panelCuadricula).apply { 
		isFitToWidth = true
		isVisible = false
		isManaged = false
	}
	val contenedorCentralBiblioteca = StackPane(tablaRolas, scrollCuadricula)
	
	val campoTextoTitulo = TextField()
	val campoTextoPista = TextField()
	val campoTextoAnyo = TextField()
	val campoTextoGenero = TextField()
	val selectorTipoInterprete = ComboBox<TipoInterprete>().apply {
		items.addAll(TipoInterprete.PERSONA, TipoInterprete.GRUPO, TipoInterprete.DESCONOCIDO)
		maxWidth = Double.MAX_VALUE
	}
	val botonGuardarCambios = Button("Actualizar en Base de Datos").apply {
		maxWidth = Double.MAX_VALUE
	}
	
	val botonArtista = Button("Artista").apply { 
		maxWidth = Double.MAX_VALUE
	}
	val botonBandas = Button("Bandas").apply {
		maxWidth = Double.MAX_VALUE
	}
	
	val panelEdicion = VBox(8.0).apply {
		padding = Insets(10.0)
		prefWidth = 260.0
		children.addAll(
			Label("Detalles de Selección").apply { style = "-fx-font-weight: bold; -fx-font-size: 14px;" },
			Label("Título de la Rola:"), campoTextoTitulo,
			Label("Número de Pista:"), campoTextoPista,
			Label("Año:"), campoTextoAnyo,
			Label("Género:"), campoTextoGenero,
			Separator(),
			Label("Clasificar Intérprete:").apply { style = "-fx-font-weight: bold;" },
			selectorTipoInterprete,
			Separator(),
			botonGuardarCambios,
			Separator(),
			botonArtista,
			botonBandas
		)
	}
	
	init {
		panelRaiz.top = barraSuperiorPrincipal
		panelRaiz.center = panelCentralPrincipal
		panelRaiz.right = botonApendiceBiblioteca
		panelRaiz.bottom = panelReproductorPrincipal
		
		panelBiblioteca.top = barraBusqueda
		panelBiblioteca.center = contenedorCentralBiblioteca
		panelBiblioteca.right = panelEdicion
	}
}