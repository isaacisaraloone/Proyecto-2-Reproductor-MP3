package minero

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class MineroTest {
	@Test
	fun probarValoresPorOmision() {
		val archivoTemp = File.createTempFile("rolaPrueba", ".mp3")
		archivoTemp.deleteOnExit()
		
		val metadatos = Analizador.analizar(archivoTemp)
		
		assertEquals("Unknown", metadatos.titulo)
		assertEquals("Unknown", metadatos.interprete)
		assertEquals("Unknown", metadatos.album)
		assertEquals(0, metadatos.pista)
		assertEquals("Unknown", metadatos.genero)
	}
}