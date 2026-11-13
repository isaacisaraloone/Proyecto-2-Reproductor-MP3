package interfaz

import javafx.application.Application
import javafx.scene.Scene
import javafx.stage.Stage
import nucleo.GestorBaseDatos
import nucleo.AlmacenMusica
import reproductor.Reproductor

class Main : Application() {

	private val motorAudio = Reproductor()
	private val gestorBD = GestorBaseDatos(obtenerRutaBaseDatosXdg())
	private val almacenMusica = AlmacenMusica(gestorBD.conexion)
	private var controlador: Controlador? = null

	private fun obtenerRutaBaseDatosXdg(): String {
		val xdgDataHome = System.getenv("XDG_DATA_HOME")?.takeIf { it.isNotBlank() }
			?: (System.getProperty("user.home")+ "/.local/share")
		val directorio = java.io.File(xdgDataHome, "reproductor-mp3")
		if (!directorio.exists()) {
			directorio.mkdirs()
		}
		return java.io.File(directorio, "musica.db").absolutePath
	}
	
	override fun start(escenarioPrin: Stage) {
		gestorBD.iniciarEsquema()

		val vista = Vista()
		controlador = Controlador(vista, almacenMusica, motorAudio, gestorBD)

		val escena = Scene(vista.panelRaiz, 1000.0, 620.0)
		escenarioPrin.title = "Reproductor de Música"
		escenarioPrin.scene = escena
		escenarioPrin.show()
	}

	override fun stop() {
		motorAudio.liberarRecursos()
		controlador?.cerrarBaseDatos() ?: gestorBD.close()
	}
}

fun main(argumentos: Array<String>) {
	Application.launch(Main::class.java, *argumentos)
}