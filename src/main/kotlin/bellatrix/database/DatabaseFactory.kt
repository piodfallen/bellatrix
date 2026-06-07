package bellatrix.database

import bellatrix.database.tables.ModmailThreads
import bellatrix.database.tables.Users
import dev.kordex.core.utils.env
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object DatabaseFactory {
	private val databaseUrl = env("DATABASE_URL")
	private val databaseUser = env("DATABASE_USER")
	private val databasePassword = env("DATABASE_PASSWORD")

	fun init() {
		Database.connect(
			url = databaseUrl,
			driver = "org.postgresql.Driver",
			user = databaseUser,
			password = databasePassword,
		)

		transaction {
			SchemaUtils.createMissingTablesAndColumns(Users, ModmailThreads)
		}
	}
}
