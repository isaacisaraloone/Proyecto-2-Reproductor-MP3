package nucleo

import java.sql.Connection
import java.sql.DriverManager

class GestorBaseDatos(private val rutaBd: String = "musica.db") : AutoCloseable {
	val conexion: Connection by lazy {
        DriverManager.getConnection("jdbc:sqlite:$rutaBd").also { 
                conn -> conn.createStatement().use { 
                        stmt -> stmt.execute("PRAGMA foreign_keys = ON;")
                }
        }
	}
	
	fun iniciarEsquema() {
		val sentenciasDdl = listOf(
			"""
            CREATE TABLE IF NOT EXISTS types (
                id_type INTEGER PRIMARY KEY,
                description TEXT
            );
            """.trimIndent(),
            "INSERT OR IGNORE INTO types VALUES(0, 'Person');",
            "INSERT OR IGNORE INTO types VALUES(1, 'Group');",
            "INSERT OR IGNORE INTO types VALUES(2, 'Unknown');",

            """
            CREATE TABLE IF NOT EXISTS performers (
                id_performer INTEGER PRIMARY KEY,
                id_type INTEGER,
                name TEXT,
                FOREIGN KEY (id_type) REFERENCES types(id_type)
            );
            """.trimIndent(),

            """
            CREATE TABLE IF NOT EXISTS persons (
                id_person INTEGER PRIMARY KEY,
                stage_name TEXT,
                real_name TEXT,
                birth_date TEXT,
                death_date TEXT
            );
            """.trimIndent(),

            """
            CREATE TABLE IF NOT EXISTS groups (
                id_group INTEGER PRIMARY KEY,
                name TEXT,
                start_date TEXT,
                end_date TEXT
            );
            """.trimIndent(),

            """
            CREATE TABLE IF NOT EXISTS in_group (
                id_person INTEGER,
                id_group INTEGER,
                PRIMARY KEY (id_person, id_group),
                FOREIGN KEY (id_person) REFERENCES persons(id_person),
                FOREIGN KEY (id_group) REFERENCES groups(id_group)
            );
            """.trimIndent(),

            """
            CREATE TABLE IF NOT EXISTS albums (
                id_album INTEGER PRIMARY KEY,
                path TEXT,
                name TEXT,
                year INTEGER
            );
            """.trimIndent(),
            "INSERT OR IGNORE INTO albums VALUES (0, '', 'Unknown', 0);",

            """
            CREATE TABLE IF NOT EXISTS rolas (
                id_rola INTEGER PRIMARY KEY,
                id_performer INTEGER,
                id_album INTEGER,
                path TEXT UNIQUE,
                title TEXT,
                track INTEGER,
                year INTEGER,
                genre TEXT,
                FOREIGN KEY (id_performer) REFERENCES performers(id_performer),
                FOREIGN KEY (id_album) REFERENCES albums(id_album)
            );
            """.trimIndent()
		)
		
		conexion.createStatement().use { 
			stmt -> sentenciasDdl.forEach { sql -> stmt.execute(sql) }
		}
	}

    override fun close() {
        if (!conexion.isClosed) conexion.close()
    }
}