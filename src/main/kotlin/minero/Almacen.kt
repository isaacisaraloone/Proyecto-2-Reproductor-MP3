package minero

import java.sql.Connection
import java.sql.Statement

class Almacen (private val conexion: Connection) {
	fun guardarCancion(metadatos: Extractor) {
		val idInterprete = obtenerOCrearInterprete(metadatos.interprete)
		val idAlbum = obtenerOCrearAlbum(metadatos.album, metadatos.rutaDirectorioAlbum, metadatos.anyo)
		
		val consultaInsercion = """
			INSERT INTO rolas (id_performer, id_album, path, title, track, year, genre)
			VALUES (?, ?, ?, ?, ?, ?, ?);
		""".trimIndent()
		
		conexion.prepareStatement(consultaInsercion).use {sentencia ->
			sentencia.setInt(1, idInterprete)
			sentencia.setInt(2, idAlbum)
			sentencia.setString(3, metadatos.rutaCancion)
			sentencia.setString(4, metadatos.titulo)
			sentencia.setInt(5, metadatos.pista)
			sentencia.setInt(6, metadatos.anyo)
			sentencia.setString(7, metadatos.genero)
			sentencia.executeUpdate()
		}
	}
	
	private fun obtenerOCrearInterprete(nombre: String): Int {
		val consultaSeleccion = "SELECT id_performer FROM performers WHERE name = ? LIMIT 1;"
		conexion.prepareStatement(consultaSeleccion).use { sentencia ->
		sentencia.setString(1, nombre)
		val resultado = sentencia.executeQuery()
		if (resultado.next()) return resultado.getInt("id_performer")	
		}
		
		val consultaInsercion = "INSERT INTO performers (id_type, name) VALUES (2, ?);"
		conexion.prepareStatement(consultaInsercion, Statement.RETURN_GENERATED_KEYS).use{sentencia ->
			sentencia.setString(1, nombre)
			sentencia.executeUpdate()
			val clavesGeneradas = sentencia.generatedKeys
			if (clavesGeneradas.next()) return clavesGeneradas.getInt(1)
		}
		return 1
	}
	
	private fun obtenerOCrearAlbum(nombre: String, rutaDirectorio: String, anyo: Int): Int {
		if (nombre.equals("Unknown", ignoreCase = true) && rutaDirectorio.isBlank()) {
			return 0
		}
		
		val consultaSeleccion = "SELECT id_album FROM albums WHERE name = ? AND path = ? LIMIT 1;"
		conexion.prepareStatement(consultaSeleccion).use { sentencia ->
		sentencia.setString(1, nombre)
		sentencia.setString(2, rutaDirectorio)
		val resultado = sentencia.executeQuery()
		if (resultado.next()) return resultado.getInt("id_album")	
		}
		
		val consultaInsercion = "INSERT INTO albums (path, name, year) VALUES (?, ?, ?);"
		conexion.prepareStatement(consultaInsercion, Statement.RETURN_GENERATED_KEYS).use{sentencia ->
			sentencia.setString(1, rutaDirectorio)
			sentencia.setString(2, nombre)
			sentencia.setInt(3, anyo)
			sentencia.executeUpdate()
			val clavesGeneradas = sentencia.generatedKeys
			if (clavesGeneradas.next()) return clavesGeneradas.getInt(1)
		}
		return 0
	}
}