plugins {
	kotlin("jvm") version "2.0.20"
	application
}

group = "mx.unam.ciencias.myp"
version = "1.0.0"

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.xerial:sqlite-jdbc:3.46.1.0")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
	testImplementation(kotlin("test"))
	testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
}

application {
	mainClass.set("MainKt")
}

tasks.test {
	useJUnitPlatform()
}

kotlin {
	jvmToolchain(21)
}