package minero

import com.mpatric.mp3agic.Mp3File
import java.io.File
import java.time.Instant
import java.time.ZoneId

object Analizador {

	fun analizar(archivo: File): Extractor {
		var titulo = "Unknown"
		var interprete = "Unknown"
		var album = "Unknown"
		var genero = "Unknown"
		var anyo: Int? = null
		var pista = 0
		
		try {
		    val mp3 = Mp3File(archivo.absolutePath)
		    
		    if (mp3.hasId3v2Tag()){
		        val tag = mp3.id3v2Tag
		        tag.title?.trim()?.takeIf { it.isNotEmpty() }?.let { titulo = it } 
		        tag.artist?.trim()?.takeIf { it.isNotEmpty() }?.let { interprete = it }
		        tag.album?.trim()?.takeIf { it.isNotEmpty() }?.let { album = it }
		        tag.genreDescription?.trim()?.takeIf { it.isNotEmpty() }?.let { genero = it }
		        tag.year?.let { anyo = extraerAnyo(it) }
		        tag.track?.let { pista = extraerNumPista(it) }
		    } else if (mp3.hasId3v1Tag()) {
		        val tag1 = mp3.id3v1Tag
		        tag1.title?.trim()?.takeIf { it.isNotEmpty() }?.let { titulo = it } 
		        tag1.artist?.trim()?.takeIf { it.isNotEmpty() }?.let { interprete = it }
		        tag1.album?.trim()?.takeIf { it.isNotEmpty() }?.let { album = it }
		        tag1.genreDescription?.trim()?.takeIf { it.isNotEmpty() }?.let { genero = it }
		        tag1.year?.let { anyo = extraerAnyo(it) }
		        tag1.track?.let { pista = extraerNumPista(it) }
		    }
		    
		} catch (_: Exception) {}
		
		val anyoFinal = anyo ?: obtenerAnyoPorDefecto(archivo)
		val rutaCancion = archivo.absolutePath
		val rutaDirectorioAlbum = archivo.parentFile?.absolutePath ?: ""
		
		return Extractor(
			titulo = titulo,
			interprete = interprete,
			album = album,
			anyo = anyoFinal,
			pista = pista,
			genero = genero,
			rutaCancion = rutaCancion,
			rutaDirectorioAlbum = rutaDirectorioAlbum
		)
	}
	
	private fun extraerAnyo(fecha: String): Int? {
		val patronAnyo = Regex("""\b(19\d{2}|20\d{2})\b""")
		return patronAnyo.find(fecha)?.value?.toIntOrNull()
	}
	
	private fun extraerNumPista(pista: String): Int {
		val primerParte = pista.split("/")[0].trim()
		return primerParte.toIntOrNull() ?: 0
	}
	
	private fun obtenerAnyoPorDefecto(archivo: File): Int {
		return try {
			val ultimaModificacion = archivo.lastModified()
			Instant.ofEpochMilli(ultimaModificacion)
			.atZone(ZoneId.systemDefault())
			.year
		} catch (_: Exception) {
			2026
		}
	}
}
