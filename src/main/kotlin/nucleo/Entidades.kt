package nucleo

enum class TipoInterprete (val id: Int, val description: String) {
	PERSONA(0, "Person"),
	GRUPO(1, "Group"),
	DESCONOCIDO(2, "Unknown");
	
	companion object {
		fun desdeId(id: Int): TipoInterprete = entries.find { it.id == id } ?: DESCONOCIDO
	}
}

data class Interprete (
	val idInterprete: Int = 0,
	val tipo: TipoInterprete = TipoInterprete.DESCONOCIDO,
	val nombre: String
)

data class Persona(
	val idPersona: Int = 0,
	val nombreArtistico: String,
	val nombreReal: String = "",
	val fechaNacimiento: String = "",
	val fechaFallecimiento: String = ""
)

data class GrupoMusical(
	val idGrupo: Int = 0,
	val nombre: String,
	val fechaInicio: String = "",
	val fechaFin: String = ""
)

data class Album(
	val idAlbum: Int = 0,
	val ruta: String,
	val nombre: String,
	val anyo: Int
)

data class Rola(
	val idRola: Int = 0,
	val idInterprete: Int,
	val idAlbum: Int,
	val ruta: String,
	val titulo: String,
	val pista: Int,
	val anyo: Int,
	val genero: String
)

data class VistaDetalleRola(
	val idRola: Int,
	val titulo: String,
	val idInterprete: Int,
	val nombreInterprete: String,
	val tipoInterprete: TipoInterprete,
	val idAlbum: Int,
	val nombreAlbum: String,
	val rutaAlbum: String,
	val pista: Int,
	val anyo: Int,
	val genero: String,
	val rutaCancion: String
)