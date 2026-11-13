package interfaz

import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.*
import nucleo.TipoInterprete

class Vista {
	val panelRaiz = BorderPane()
	
	val campoBusqueda = TextField().apply {
		promptText = "Buscar por título, intérprete o álbum"
		HBox.setHgrow(this, Priority.ALWAYS)
	}
	val botonBuscar = Button("Buscar")
	val botonRestablecer = Button("Restablecer")
	val botonMinarDirectorio = Button("Minar Directorio")
	val botonCambiarBaseDatos = Button("Base de Datos")
	val botonIndividuos = Button("Artista")
	val botonBandas = Button("Bandas")
	val barraBusqueda = HBox(
		10.0,
		campoBusqueda,
		botonBuscar,
		botonRestablecer,
		Separator(Orientation.VERTICAL),
		botonMinarDirectorio,
		botonCambiarBaseDatos,
		botonIndividuos,
		botonBandas
	).apply {
		padding = Insets(10.0)
		alignment = Pos.CENTER
	}
	
	val tablaRolas = TableView<FilaRola>().apply {
		columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
		
		val columnaPista = TableColumn<FilaRola, Number>("Pista").apply {
			setCellValueFactory { it.value.propiedadPista }
			prefWidth = 40.0
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
	
	val campoTextoTitulo = TextField()
	val campoTextoPista = TextField()
	val campoTextoAnyo = TextField()
	val campoTextoGenero = TextField()
	val selectorTipoInterprete = ComboBox<TipoInterprete>().apply {
		items.addAll(TipoInterprete.PERSONA, TipoInterprete.GRUPO, TipoInterprete.DESCONOCIDO)
	}
	val botonGuardarCambios = Button("Actualizar en Base de Datos").apply {
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
			botonGuardarCambios
		)
	}
	val botonAnterior = Button("Anterior")
	val botonReproducir = Button("Play")
	val botonSiguiente = Button("Siguiente")
	val botonDetener = Button("Detener")
	val barraProgreso = Slider(0.0, 100.0, 0.0).apply {
		HBox.setHgrow(this, Priority.ALWAYS)
	}
	val etiquetaTiempo = Label("00:00 / 00:00")
	val controlVolumen = Slider(0.0, 1.0, 1.0).apply {
		prefWidth = 100.0
	}
	val barraInferior = HBox(10.0, botonAnterior, botonReproducir, botonSiguiente, botonDetener, barraProgreso, etiquetaTiempo, Label("Volumen:"), controlVolumen).apply {
		padding = Insets(10.0)
		alignment = Pos.CENTER
	}
	
	init {
		panelRaiz.top = barraBusqueda
		panelRaiz.center = tablaRolas
		panelRaiz.right = panelEdicion
		panelRaiz.bottom = barraInferior
	}
}