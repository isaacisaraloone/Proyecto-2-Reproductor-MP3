package reproductor

import javafx.application.Platform
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.util.Duration
import java.io.File

class Reproductor : MotorAudio {
	
	private var reproductorMedios: MediaPlayer? = null
    private val listaOyentes = mutableListOf<OyenteMotorAudio>()

    override var archivoActual: File? = null
        private set

    override var estadoActual: EstadoReproduccion = EstadoReproduccion.DETENIDO
        private set(nuevoEstado) {
            field = nuevoEstado
            notificarCambioEstado(nuevoEstado)
        }

    override var volumen: Double = 1.0
        set(valor) {
            val valorAjustado = valor.coerceIn(0.0, 1.0)
            field = valorAjustado
            reproductorMedios?.volume = valorAjustado
        }

    override fun cargarPista(archivo: File) {
        if (!archivo.exists() || !archivo.isFile) {
            notificarError("El archivo de audio no existe o no es válido: ${archivo.absolutePath}")
            return
        }

        liberarReproductorInterno()

        try {
            archivoActual = archivo
            val uriMedio = archivo.toURI().toString()
            val medio = Media(uriMedio)
            val reproductor = MediaPlayer(medio)

            reproductor.volume = volumen

            reproductor.setOnReady {
                val duracionTotal = medio.duration
                notificarProgreso(Duration.ZERO, duracionTotal)
            }

            reproductor.setOnPlaying {
                estadoActual = EstadoReproduccion.REPRODUCIENDO
            }

            reproductor.setOnPaused {
                estadoActual = EstadoReproduccion.PAUSADO
            }

            reproductor.setOnStopped {
                estadoActual = EstadoReproduccion.DETENIDO
            }

            reproductor.setOnEndOfMedia {
                estadoActual = EstadoReproduccion.DETENIDO
                notificarFinDePista()
            }

            reproductor.setOnError {
                val mensajeError = reproductor.error?.message ?: "Error desconocido en el motor multimedia."
                estadoActual = EstadoReproduccion.ERROR
                notificarError(mensajeError)
            }

            reproductor.currentTimeProperty().addListener { _, _, tiempoActualizado ->
                val total = reproductor.totalDuration ?: Duration.UNKNOWN
                notificarProgreso(tiempoActualizado, total)
            }

            this.reproductorMedios = reproductor
            estadoActual = EstadoReproduccion.DETENIDO
        } catch (excepcion: Exception) {
            estadoActual = EstadoReproduccion.ERROR
            notificarError("Fallo al inicializar medio: ${excepcion.localizedMessage}")
        }
    }

    override fun reproducir() {
        val reproductor = reproductorMedios
        if (reproductor == null) {
            notificarError("No hay ninguna canción cargada para reproducir.")
            return
        }

        when (estadoActual) {
            EstadoReproduccion.REPRODUCIENDO -> Unit
            EstadoReproduccion.PAUSADO, EstadoReproduccion.DETENIDO -> reproductor.play()
            EstadoReproduccion.ERROR -> Unit
        }
    }

    override fun pausar() {
        if (estadoActual == EstadoReproduccion.REPRODUCIENDO) {
            reproductorMedios?.pause()
        }
    }

    override fun detener() {
        if (estadoActual == EstadoReproduccion.REPRODUCIENDO || estadoActual == EstadoReproduccion.PAUSADO) {
            reproductorMedios?.stop()
            estadoActual = EstadoReproduccion.DETENIDO
        }
    }

    override fun desplazarA(posicion: Duration) {
        val reproductor = reproductorMedios ?: return
        val total = reproductor.totalDuration
        if (total != null && !total.isUnknown && posicion <= total) {
            reproductor.seek(posicion)
        }
    }

    override fun registrarOyente(oyente: OyenteMotorAudio) {
        if (!listaOyentes.contains(oyente)) {
            listaOyentes.add(oyente)
        }
    }

    override fun desregistrarOyente(oyente: OyenteMotorAudio) {
        listaOyentes.remove(oyente)
    }

    override fun liberarRecursos() {
        liberarReproductorInterno()
        listaOyentes.clear()
        archivoActual = null
    }

    private fun liberarReproductorInterno() {
        reproductorMedios?.let { reproductor ->
            reproductor.onReady = null
            reproductor.onPlaying = null
            reproductor.onPaused = null
            reproductor.onStopped = null
            reproductor.onEndOfMedia = null
            reproductor.onError = null
            reproductor.stop()
            reproductor.dispose()
        }
        reproductorMedios = null
    }

    private fun notificarCambioEstado(estado: EstadoReproduccion) {
        ejecutarEnHiloJavaFx {
            listaOyentes.forEach { it.alCambiarEstado(estado) }
        }
    }

    private fun notificarProgreso(actual: Duration, total: Duration) {
        ejecutarEnHiloJavaFx {
            listaOyentes.forEach { it.alCambiarProgreso(actual, total) }
        }
    }

    private fun notificarFinDePista() {
        ejecutarEnHiloJavaFx {
            listaOyentes.forEach { it.alFinalizarCancion() }
        }
    }

    private fun notificarError(mensaje: String) {
        ejecutarEnHiloJavaFx {
            listaOyentes.forEach { it.alOcurrirError(mensaje) }
        }
    }

    private fun ejecutarEnHiloJavaFx(bloque: () -> Unit) {
        if (Platform.isFxApplicationThread()) {
            bloque()
        } else {
            Platform.runLater(bloque)
        }
    }
}