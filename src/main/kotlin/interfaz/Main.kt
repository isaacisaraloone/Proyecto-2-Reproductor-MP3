package interfaz

import javafx.application.Application
import javafx.scene.Scene
import javafx.stage.Stage
import nucleo.GestorBaseDatos
import nucleo.AlmacenMusica
import reproductor.Reproductor

class Main : Application() {

	private val motorAudio = Reproductor()
	private val gestorBD = GestorBaseDatos("musica.db")
	private val almacenMusica = AlmacenMusica(gestorBD.conexion)

	override fun start(escenarioPrin: Stage) {
		gestorBD.iniciarEsquema()

		val vista = Vista()
		Controlador(vista, almacenMusica, motorAudio)

		val escena = Scene(vista.panelRaiz, 1000.0, 620.0)
		escenarioPrin.title = "Reproductor de Música"
		escenarioPrin.scene = escena
		escenarioPrin.show()
	}

	override fun stop() {
		motorAudio.liberarRecursos()
		gestorBD.close()
	}
}

fun main(argumentos: Array<String>) {
	Application.launch(Main::class.java, *argumentos)
}