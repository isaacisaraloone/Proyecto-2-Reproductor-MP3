package reproductor

import javafx.util.Duration
import java.io.File

enum class EstadoReproduccion {
	DETENIDO,
	REPRODUCIENDO,
	PAUSADO,
	ERROR
}

interface OyenteMotorAudio {
	fun alCambiarEstado(nuevoEstado: EstadoReproduccion)
	fun alCambiarProgreso(tiempoActual: Duration, duracionTotal: Duration)
	fun alFinalizarCancion()
	fun alOcurrirError(mensaje: String)
}

interface MotorAudio {
	val estadoActual: EstadoReproduccion
	val archivoActual: File?
	var volumen: Double
	
	fun cargarPista(archivo: File)
	fun reproducir()
	fun pausar()
	fun detener()
	fun desplazarA(posicion: Duration)
	fun registrarOyente(oyente: OyenteMotorAudio)
	fun desregistrarOyente(oyente: OyenteMotorAudio)
	fun liberarRecursos()
}