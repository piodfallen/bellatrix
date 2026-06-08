package bellatrix.database.repositories

import bellatrix.database.models.DisboardBumpReminder
import bellatrix.database.tables.DisboardBumpReminders
import dev.kord.common.entity.Snowflake
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant

object DisboardBumpReminderRepository {
	fun upsert(
		guildId: Snowflake,
		channelId: Snowflake,
		roleId: Snowflake,
		lastBumpAt: Instant,
		nextReminderAt: Instant,
	): DisboardBumpReminder =
		transaction {
			val updated = DisboardBumpReminders.update({ DisboardBumpReminders.guildId eq guildId.toString() }) {
				it[DisboardBumpReminders.channelId] = channelId.toString()
				it[DisboardBumpReminders.roleId] = roleId.toString()
				it[DisboardBumpReminders.lastBumpAt] = lastBumpAt.toEpochMilli()
				it[DisboardBumpReminders.nextReminderAt] = nextReminderAt.toEpochMilli()
			}

			if (updated == 0) {
				DisboardBumpReminders.insert {
					it[DisboardBumpReminders.guildId] = guildId.toString()
					it[DisboardBumpReminders.channelId] = channelId.toString()
					it[DisboardBumpReminders.roleId] = roleId.toString()
					it[DisboardBumpReminders.lastBumpAt] = lastBumpAt.toEpochMilli()
					it[DisboardBumpReminders.nextReminderAt] = nextReminderAt.toEpochMilli()
				}
			}

			requireNotNull(findByGuildIdQuery(guildId))
		}

	fun findAll(): List<DisboardBumpReminder> =
		transaction {
			DisboardBumpReminders
				.selectAll()
				.map { it.toDisboardBumpReminder() }
		}

	fun findByGuildId(guildId: Snowflake): DisboardBumpReminder? =
		transaction {
			findByGuildIdQuery(guildId)
		}

	fun deleteByGuildId(guildId: Snowflake): Unit =
		transaction {
			DisboardBumpReminders.deleteWhere {
				DisboardBumpReminders.guildId eq guildId.toString()
			}
		}

	private fun findByGuildIdQuery(guildId: Snowflake): DisboardBumpReminder? =
		DisboardBumpReminders
			.selectAll()
			.where { DisboardBumpReminders.guildId eq guildId.toString() }
			.singleOrNull()
			?.toDisboardBumpReminder()

	private fun ResultRow.toDisboardBumpReminder(): DisboardBumpReminder =
		DisboardBumpReminder(
			guildId = Snowflake(this[DisboardBumpReminders.guildId]),
			channelId = Snowflake(this[DisboardBumpReminders.channelId]),
			roleId = Snowflake(this[DisboardBumpReminders.roleId]),
			lastBumpAt = Instant.ofEpochMilli(this[DisboardBumpReminders.lastBumpAt]),
			nextReminderAt = Instant.ofEpochMilli(this[DisboardBumpReminders.nextReminderAt]),
		)
}
