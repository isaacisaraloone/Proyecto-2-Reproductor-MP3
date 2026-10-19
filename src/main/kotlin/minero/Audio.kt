package minero

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File

class Audio(private val almacen: Almacen) {
	suspend fun minarDirectorio(directorio: File,
		progresar: (cantidadProcesada: Int, nombreArchivo: String) -> Unit = { _, _ -> }
		): Int = withContext(Dispatchers.IO) {
		if (!directorio.exists() || !directorio.isDirectory) {
			return@withContext 0
		}
		
		var totalProcesadas = 0
		val archivosMP3 = directorio.walkTopDown().filter { it.isFile && it.extension.equals("mp3", ignoreCase = true) }
	
		for (archivo in archivosMP3) {
			ensureActive()
			val metadatos = Analizador.analizar(archivo)
			almacen.guardarCancion(metadatos)
			totalProcesadas++
			progresar(totalProcesadas, archivo.name)
		}
		totalProcesadas
	}
}
