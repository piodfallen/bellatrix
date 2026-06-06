package bellatrix.database.tables

import bellatrix.i18n.UserLocales
import org.jetbrains.exposed.v1.core.Table

object Users : Table("users") {
	val userId = varchar("user_id", length = 32)
	val locale = varchar("locale", length = 16).default(UserLocales.DEFAULT_TAG)

	override val primaryKey = PrimaryKey(userId)
}
