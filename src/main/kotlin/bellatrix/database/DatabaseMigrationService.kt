package bellatrix.database

import org.flywaydb.core.Flyway

object DatabaseMigrationService {
	private const val MIGRATION_LOCATION = "classpath:db/migration"
	private const val BASELINE_VERSION = "0"

	fun migrate() {
		Flyway.configure()
			.dataSource(DatabaseConfig.url, DatabaseConfig.user, DatabaseConfig.password)
			.locations(MIGRATION_LOCATION)
			.baselineOnMigrate(true)
			.baselineVersion(BASELINE_VERSION)
			.validateMigrationNaming(true)
			.load()
			.migrate()
	}
}
