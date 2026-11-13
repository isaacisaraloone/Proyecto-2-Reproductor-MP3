package minero

import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.util.logging.Level
import java.util.logging.Logger

object Analizador {
	
	init {
		Logger.getLogger("org.jaudiotagger").level = Level.OFF
	}

	fun analizar(archivo: File): Extractor {
		var titulo = "Unknown"
		var interprete = "Unknown"
		var album = "Unknown"
		var genero = "Unknown"
		var anyo: Int? = null
		var pista = 0
		
		try {
		    val audioFile = AudioFileIO.read(archivo)
		    val tag = audioFile.tag
		    if (tag != null){
		        tag.getFirst(FieldKey.TITLE).trim().takeIf { it.isNotEmpty() }?.let { titulo = it }
		        tag.getFirst(FieldKey.ARTIST).trim().takeIf { it.isNotEmpty() }?.let { interprete = it }
		        tag.getFirst(FieldKey.ALBUM).trim().takeIf { it.isNotEmpty() }?.let { album = it }
		        tag.getFirst(FieldKey.GENRE).trim().takeIf { it.isNotEmpty() }?.let { genero = it }
		        tag.getFirst(FieldKey.YEAR).trim().takeIf { it.isNotEmpty() }?.let { anyo = extraerAnyo(it) }
		        tag.getFirst(FieldKey.TRACK).trim().takeIf { it.isNotEmpty() }?.let { pista = extraerNumPista(it) }
		    }
		} catch (e: Exception) {
			System.err.println("Error al analizar ${archivo.name}: ${e.message}}")
		}
		
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