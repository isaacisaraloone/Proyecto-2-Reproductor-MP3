package minero

import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZoneId

object Analizador {

	
	
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
