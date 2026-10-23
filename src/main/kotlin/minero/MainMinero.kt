package minero

import kotlinx.coroutines.runBlocking
import java.io.File
import nucleo.GestorBaseDatos

fun main(argumentos: Array<String>) = runBlocking {  
	if (argumentos.isEmpty()) {
		println("Uso: java -jar minero.jar <directorioMusica> [rutaBaseDeDatos.sqlite]")
		return@runBlocking
	}
	
	val rutaMusica = File(argumentos[0])
	val rutaBaseDatos =  if (argumentos.size > 1) argumentos[1] else "musica.db"
	
	if (!rutaMusica.exists() || !rutaMusica.isDirectory){
		println("No existe el directorio ingresado: ${rutaMusica.absolutePath}")
		return@runBlocking
	}
	
	val gestorBd = GestorBaseDatos(rutaBaseDatos)
	gestorBd.iniciarEsquema()
	
	val almacen = Almacen(gestorBd.conexion)
	val minero = Audio(almacen)
	
	println("Minado en: ${rutaMusica.absolutePath}")
	println("Labase de datos destino esta en: $rutaBaseDatos")
	
	val total = minero.minarDirectorio(rutaMusica) {
		cantidad, archivo -> print("\rCanciones procesadas: $cantidad")
	}
	
	println("Minado finalizado. Total de rolas guardadas: $total")
	gestorBd.close()
}