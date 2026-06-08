package bellatrix.database

import dev.kordex.core.utils.env

object DatabaseConfig {
	const val DRIVER = "org.postgresql.Driver"

	val url = env("DATABASE_URL")
	val user = env("DATABASE_USER")
	val password = env("DATABASE_PASSWORD")
}
