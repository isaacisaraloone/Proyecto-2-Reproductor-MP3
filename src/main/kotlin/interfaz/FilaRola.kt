package interfaz

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import nucleo.TipoInterprete

class FilaRola(
	val idRola: Int,
	val idInterprete: Int,
	val idAlbum: Int,
	val rutaCancion: String,
	var tipoInterprete: TipoInterprete,
	titulo: String,
	nombreInterprete: String,
	nombreAlbum: String,
	anyo: Int,
	pista: Int,
	genero: String
) {
	val propiedadTitulo = SimpleStringProperty(titulo)
	val propiedadInterprete = SimpleStringProperty(nombreInterprete)
	val propiedadAlbum = SimpleStringProperty(nombreAlbum)
	val propiedadAnyo = SimpleIntegerProperty(anyo)
	val propiedadPista = SimpleIntegerProperty(pista)
	val propiedadGenero = SimpleStringProperty(genero)
}