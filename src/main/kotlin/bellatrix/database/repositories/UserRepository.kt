package bellatrix.database.repositories

import bellatrix.database.models.User
import bellatrix.database.tables.Users
import bellatrix.i18n.SupportedLocales
import dev.kord.common.entity.Snowflake
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update

object UserRepository {
	fun getOrCreate(userId: Snowflake): User =
		transaction {
			Users.insertIgnore {
				it[Users.userId] = userId.toString()
			}

			Users
				.selectAll()
				.where { Users.userId eq userId.toString() }
				.single()
				.let {
					User(
						userId = it[Users.userId],
						locale = it[Users.locale],
					)
				}
		}

	fun updateLocale(userId: Snowflake, locale: String): User {
		val normalizedLocale = requireNotNull(SupportedLocales.normalize(locale)) {
			"Unsupported locale: $locale"
		}

		return transaction {
			Users.insertIgnore {
				it[Users.userId] = userId.toString()
			}

			Users.update({ Users.userId eq userId.toString() }) {
				it[Users.locale] = normalizedLocale
			}

			User(
				userId = userId.toString(),
				locale = normalizedLocale,
			)
		}
	}
}
