package bellatrix.database

import org.jetbrains.exposed.v1.jdbc.Database

object DatabaseFactory {
	fun init() {
		DatabaseMigrationService.migrate()

		Database.connect(
			url = DatabaseConfig.url,
			driver = DatabaseConfig.DRIVER,
			user = DatabaseConfig.user,
			password = DatabaseConfig.password,
		)
	}
}
