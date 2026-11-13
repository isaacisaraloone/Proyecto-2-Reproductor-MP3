### Modelado y Programación

=================================

# Proyecto 2 Reproductor MP3

=================================

Proyecto desarrollado para la materia **Modelado y Programacion**. Consiste en un reproductor de archivos MP3 y una base de datos relacional, con inmutabilidad en archivos locales, donde se recorren recursivamente los directorios musicales para extraer los metadatos ID3v2.4 mediante un minero de metadatos el cual puede ser usado desde la interfaz grafica (**GUI**) o desde la consola, donde el minero cumple con la funcion de extraer los metadatos los cuales van a servir para poblar una base de datos.

## Requisitos

- **JDK 21**
- **Gradle** (incluido con `./gradlew`)

## Ejecucion

Para compilar el proyecto:
```bash
./gradlew compileKotlin
```

Ejecutar aplicacion grafica:
```bash
./gradlew run
```

Ejecutar el minero de forma independiente:
```bash
./gradlew abrirMinero -Pargs="<ruta_carpeta_musica> <ruta_base_datos.db>"
```