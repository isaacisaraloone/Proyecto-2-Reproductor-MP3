package nucleo

import java.sql.Connection
import java.sql.ResultSet
import java.sql.Statement

class AlmacenMusica (private val conexion: Connection) {
	fun obtenerOCrearInterprete (nombre: String, porDefecto: TipoInterprete = TipoInterprete.DESCONOCIDO): Int{
		val nombreLimpio = nombre.trim().ifEmpty { "Unknown" }
		val consultaSeleccion = "SELECT id_performer FROM performers WHERE name = ? LIMIT 1;"
		conexion.prepareStatement(consultaSeleccion).use {
			sentencia -> sentencia.setString(1, nombreLimpio)
			val resultado = sentencia.executeQuery()
			if (resultado.next()) return resultado.getInt("id_performer")
		}
		val consultaInsercion = "INSERT INTO performers (id_type, name) VALUES (?, ?);"
		conexion.prepareStatement(consultaInsercion, Statement.RETURN_GENERATED_KEYS).use { sentencia ->
			sentencia.setInt(1, porDefecto.id)
			sentencia.setString(2, nombreLimpio)
			sentencia.executeUpdate()
			val clavesGeneradas = sentencia.generatedKeys
			if (clavesGeneradas.next()) return clavesGeneradas.getInt(1)
		}
		throw IllegalStateException("No se pudo insertar el interprete: $nombreLimpio")
	}
	
	fun obtenerOCrearAlbum(nombre: String, rutaDirectorio: String, anyo: Int): Int {
		val nombreLimpio = nombre.trim().ifEmpty { "Unknown" }
		if (nombreLimpio.equals("Unknown", ignoreCase = true) && rutaDirectorio.isBlank()) {
			return 0
		}
		val consultaSeleccion = "SELECT id_album FROM albums WHERE name = ? AND path = ? LIMIT 1;"
		conexion.prepareStatement(consultaSeleccion).use { sentencia ->
			sentencia.setString(1, nombreLimpio)
			sentencia.setString(2, rutaDirectorio)
			val resultado = sentencia.executeQuery()
			if (resultado.next()) return resultado.getInt("id_album")
		}
		 val consultaInsercion = "INSERT id_album FROM albums WHERE name = ? AND path = ? LIMIT 1;"
		 conexion.prepareStatement(consultaInsercion, Statement.RETURN_GENERATED_KEYS).use { sentencia ->
			sentencia.setString(1, rutaDirectorio)
			sentencia.setString(2, nombreLimpio)
			sentencia.setInt(3, anyo)
			sentencia.executeUpdate()
		}
		 return 0
	}
	
	fun insertarOIgnorarRola(rola: Rola): Int {
		val consultaVerificacion = "SELECT id_rola FROM rolas WHERE path = ? LIMIT 1;"
		conexion.prepareStatement(consultaVerificacion).use { sentencia ->
			sentencia.setString(1, rola.ruta)
			val resultado = sentencia.executeQuery()
			if (resultado.next()) return resultado.getInt("id_rola")
		 }
		 
		val consultaInsercion = """
		 	INSERT INTO rolas (id_performer, id_album, path, title, track, year, genre)
		 	VALUES (?, ?, ?, ?, ?, ?, ?);
		""".trimIndent()
		 
		conexion.prepareStatement(consultaInsercion, Statement.RETURN_GENERATED_KEYS).use { sentencia ->
			sentencia.setInt(1, rola.idInterprete)
			sentencia.setInt(2, rola.idAlbum)
			sentencia.setString(3, rola.ruta)
			sentencia.setString(4, rola.titulo)
			sentencia.setInt(5, rola.pista)
			sentencia.setInt(6, rola.anyo)
			sentencia.setString(7, rola.genero)
			sentencia.executeUpdate()
			val clavesGeneradas = sentencia.generatedKeys
			if (clavesGeneradas.next()) return clavesGeneradas.getInt(1)
		 }
		throw IllegalStateException("Error al insertar la rola: ${rola.titulo}")
	}
	
	fun actualizarMetadatosRola(
		idRola: Int,
		nuevoTitulo: String,
		nuevaPista: Int,
		nuevoAnyo: Int,
		nuevoGenero: String,
		nuevoIdInterprete: Int,
		nuevoIdAlbum: Int
	) {
		val consulta = """
			UPDATE rolas
			SET  title = ?, track = ?, genre = ?, id_performer = ?, id_album = ?
			WHERE id_rola = ?;
		""".trimIndent()
		
		conexion.prepareStatement(consulta).use { sentencia ->
			sentencia.setString(1, nuevoTitulo.trim())
			sentencia.setInt(2, nuevaPista)
			sentencia.setInt(3, nuevoAnyo)
			sentencia.setString(4, nuevoGenero.trim())
			sentencia.setInt(5, nuevoIdInterprete)
			sentencia.setInt(6, nuevoIdAlbum)
			sentencia.setInt(7, idRola)
			sentencia.executeUpdate()
		}
	}
	
	fun definirInterprete(idInterprete: Int, tipo: TipoInterprete, nombreInterprete: String){
		val consultaActualizacion = "UPDATE performers SET id_type = ? WHERE id_performer = ?;"
		conexion.prepareStatement(consultaActualizacion).use { sentencia ->
			sentencia.setInt(1, tipo.id)
			sentencia.setString(2, nombreInterprete.trim())
			sentencia.setInt(3, idInterprete)
			sentencia.executeUpdate()
		}
		
		when (tipo) {
			TipoInterprete.PERSONA -> {
				if (buscarPersonaPorNombreArtistico(nombreInterprete) == null) {
					crearPersona(Persona(nombreArtistico = nombreInterprete.trim()))
				}
			}
			TipoInterprete.GRUPO -> {
				if (buscarGrupoPorNombre(nombreInterprete) == null) {
					crearGrupo(GrupoMusical(nombre = nombreInterprete.trim()))
				}
			}
			TipoInterprete.DESCONOCIDO -> Unit
		}
	}
	
	fun crearPersona(persona: Persona): Int {
		val consulta = """
			INSERT INTO persons (stage_name, real_name, birth_date, death_date)
			VALUES (?, ?, ?, ?);
		""".trimIndent()
		conexion.prepareStatement(consulta, Statement.RETURN_GENERATED_KEYS).use { sentencia ->
			sentencia.setString(1, persona.nombreArtistico.trim())
			sentencia.setString(2, persona.nombreReal.trim())
			sentencia.setString(3, persona.fechaNacimiento.trim())
			sentencia.setString(4, persona.fechaFallecimiento.trim())
			sentencia.executeUpdate()
			val clavesGeneradas = sentencia.generatedKeys
			if (clavesGeneradas.next()) return clavesGeneradas.getInt(1)
		}
		throw IllegalStateException("No se pudo registrar a la persona: ${persona.nombreArtistico}")
	}
	
	fun buscarPersonaPorNombreArtistico(nombreArtistico: String): Persona? {
		val consulta = "SELECT * FROM persons WHERE stage_name = ? LIMIT 1;"
		conexion.prepareStatement(consulta).use { sentencia ->
			sentencia.setString(1, nombreArtistico.trim())
			val resultado = sentencia.executeQuery()
			if (resultado.next()) {
				return Persona(
					idPersona = resultado.getInt("id_person"),
					nombreArtistico = resultado.getString("stage_name") ?: "",
					nombreReal = resultado.getString("real_name") ?: "",
					fechaNacimiento = resultado.getString("birth_date") ?: "",
					fechaFallecimiento = resultado.getString("death_date") ?: ""
				)
			}
		 }
		return null
	}
	
	fun crearGrupo(grupo: GrupoMusical): Int {
		val consulta = "INSERT INTO groups (name, start_date, end_date) VALUES (?, ?, ?);"
		conexion.prepareStatement(consulta, Statement.RETURN_GENERATED_KEYS).use { sentencia ->
			sentencia.setString(1, grupo.nombre.trim())
			sentencia.setString(2, grupo.fechaInicio.trim())
			sentencia.setString(3, grupo.fechaFin.trim())
			sentencia.executeUpdate()
			val clavesGeneradas = sentencia.generatedKeys
			if (clavesGeneradas.next()) return clavesGeneradas.getInt(1)
		}
		throw IllegalStateException("No se pudo registrar el grupo: ${grupo.nombre}")
	}
	
	fun buscarGrupoPorNombre(nombre: String): GrupoMusical? {
		val consulta = "SELECT * FROM groups WHERE name = ? LIMIT 1;"
		conexion.prepareStatement(consulta).use { sentencia ->
			sentencia.setString(1, nombre.trim())
			val resultado = sentencia.executeQuery()
			if (resultado.next()) {
				return GrupoMusical(
					idGrupo = resultado.getInt("id_group"),
					nombre = resultado.getString("name") ?: "",
					fechaInicio = resultado.getString("start_date") ?: "",
					fechaFin = resultado.getString("end_date") ?: ""
				)
			}
		}
		return null
	}
	
	fun agregarPersonaAGrupo(idPersona: Int, idGrupo: Int){
		val consulta = "INSERT OR IGNORE INTO in_group (id_person, id_group) VALUES (?, ?);"
		conexion.prepareStatement(consulta).use { sentencia ->
			sentencia.setInt(1, idPersona)
			sentencia.setInt(2, idGrupo)
			sentencia.executeUpdate()
		 }
	}
	
	fun obtenerMiembrosDeGrupo(idGrupo: Int): List<Persona> {
		val miembros = mutableListOf<Persona>()
		val consulta = """
			SELECT p.* FROM persons p
			INNER JOIN in_group ig ON p.id_person = ig.id_person
			WHERE ig.id_group = ?
			ORDER BY p.stage_name ASC;
		""".trimIndent()
		conexion.prepareStatement(consulta).use { sentencia ->
			sentencia.setInt(1, idGrupo)
			val resultado = sentencia.executeQuery()
			while (resultado.next()) {
				miembros.add(
					Persona(
						idPersona = resultado.getInt("id_person"),
						nombreArtistico = resultado.getString("stage_name") ?: "",
						nombreReal = resultado.getString("real_name") ?: "",
						fechaNacimiento = resultado.getString("birth_date") ?: "",
						fechaFallecimiento = resultado.getString("death_date") ?: ""
					)
				)
			}
		}
		return miembros
	}
	
	fun buscarRolasConWhere(clausulaWhere: String, parametros: List<Any>): List<VistaDetalleRola>{
		val consultaBase = StringBuilder(
			"""
			SELECT
				r.id_rola, r.title, r.track, r.year, r.genre, r.path,
				p.id_performer, p.name AS performer_name, p.id_type AS performer_type,
				a.id_album, a.name AS album_name, a.path AS album_path
			FROM rolas r
			LEFT JOIN performers p ON r.id_performe = p.id_performer
			LEFT JOIN albums a ON r.id_album = a.id_album
			""".trimIndent()
		)
		
		if (clausulaWhere.isNotBlank()) {
			consultaBase.append(" ORDER BY p.name ASC, a.name ASC, r.track ASC;")
		}
		val resultados = mutableListOf<VistaDetalleRola>()
		conexion.prepareStatement(consultaBase.toString()).use { sentencia ->
			parametros.forEachIndexed { indice, parametro ->
				when (parametro) {
					is Int -> sentencia.setInt(indice + 1, parametro)
					is String -> sentencia.setString(indice + 1, parametro)
					else -> sentencia.setObject(indice + 1, parametro)
				}
			}
			val conjuntoResultados = sentencia.executeQuery()
			while (conjuntoResultados.next()) {
				resultados.add(mapearFilaAVistaDetalleRola(conjuntoResultados))
			}
		}
		return resultados
	}
	
	fun obtenerTodasLasRolasDetalladas(): List<VistaDetalleRola> {
		return buscarRolasConWhere("", emptyList())
	}
	
	private fun mapearFilaAVistaDetalleRola(resultado: ResultSet): VistaDetalleRola {
		return VistaDetalleRola(
			idRola = resultado.getInt("id_rola"),
			titulo = resultado.getString("title") ?: "Unknown",
			idInterprete = resultado.getInt("id_performer"),
			nombreInterprete = resultado.getString("performer_name") ?: "Unknown",
			tipoInterprete = TipoInterprete.desdeId(resultado.getInt("performer_type")),
			idAlbum = resultado.getInt("id_album"),
			nombreAlbum = resultado.getString("album_name") ?: "Unknown",
			rutaAlbum = resultado.getString("album_path") ?: "",
			pista = resultado.getInt("track"),
			anyo = resultado.getInt("year"),
			genero = resultado.getString("genre") ?: "Unknown",
			rutaCancion = resultado.getString("path") ?: ""
		)
	}
}