package interfaz

import javafx.application.Platform
import javafx.scene.image.Image
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jaudiotagger.audio.AudioFileIO
import java.io.ByteArrayInputStream
import java.io.File
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level
import java.util.logging.Logger

object GestorCaratula {
	
	private val cache = ConcurrentHashMap<String, Optional<Image>>()
	private val nombresArchivoCaratula = listOf(
		"cover.jpg", "cover.png", "cover.jpeg",
			"folder.jpg", "folder.png",
			"front.jpg", "front.png",
			"album.jpg", "album.png"
	)
	
	init {
	    Logger.getLogger("org.jaudiotagger").level = Level.OFF
	}
	
	fun obtenerCaratula(rutaCancion: String): Image? {
		if (rutaCancion.isBlank()) return null
		cache[rutaCancion]?.let { return it.orElse(null) }
		
		val imagen = cargarDesdeDisco(rutaCancion)
		cache[rutaCancion] = Optional.ofNullable(imagen)
		return imagen
	}
	
	fun cargarCaratulaAsync(rutaCancion: String, alCargar: (Image?) -> Unit) {
		if (rutaCancion.isBlank()) {
			alCargar(null)
			return
		}
		cache[rutaCancion]?.let { 
			alCargar(it.orElse(null))
			return
		}
		CoroutineScope(Dispatchers.IO).launch { 
			val imagen = obtenerCaratula(rutaCancion)
			Platform.runLater { alCargar(imagen) }
		}
	}
	
	private fun cargarDesdeDisco(rutaCancion: String): Image? {
		val archivo = File(rutaCancion)
		if (!archivo.exists() || !archivo.isFile) return null
		
		try {
		    val audioFile = AudioFileIO.read(archivo)
		    val bytes = audioFile.tag?.firstArtwork?.binaryData
		    if (bytes != null && bytes.isNotEmpty()) {
		        val img = Image(ByteArrayInputStream(bytes))
		        if (!img.isError) return img
		    }
		} catch (_: Exception){}
		
		val directorio = archivo.parentFile ?: return null
		for (nombre in nombresArchivoCaratula) {
			val candidato = File(directorio, nombre)
			if (candidato.exists() && candidato.isFile) {
				val img = Image(candidato.toURI().toString())
				if (!img.isError) return img
			}
		}
		return null
	}
}