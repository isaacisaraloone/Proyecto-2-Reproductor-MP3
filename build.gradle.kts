plugins {
	kotlin("jvm") version "2.0.20"
	application
	id("org.openjfx.javafxplugin") version "0.1.0"
}
	
javafx {
	version = "21.0.4"
	modules = listOf("javafx.controls", "javafx.media")
}

group = "mx.unam.ciencias.myp"
version = "1.0.0"

repositories {
	mavenCentral()
}

dependencies {
	implementation("net.jthink:jaudiotagger:3.0.1")
	implementation("org.xerial:sqlite-jdbc:3.46.1.0")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
}

application {
	mainClass.set("interfaz.MainKt")
}

tasks.register<JavaExec>("abrirMinero") {
	group = "application"
	description = "Ejecuta el minero de forma independiente"
	classpath = sourceSets["main"].runtimeClasspath
	mainClass.set("minero.MainMineroKt")

	if (project.hasProperty("args")) {
		args = (project.property("args") as String).split(" ")
	}
}

tasks.register<Exec>("compilarReporte") {
	group = "documentation"
	description = "Compila el reporte a PDF usando XeLaTeX"
	workingDir = file("reporte")
	commandLine("xelatex", "-interaction=nonstopmode", "reporte.tex")
}

kotlin {
	jvmToolchain(21)
}