package bellatrix.database.tables

import org.jetbrains.exposed.v1.core.Table

object DisboardBumpReminders : Table("disboard_bump_reminders") {
	val guildId = varchar("guild_id", length = 32)
	val channelId = varchar("channel_id", length = 32)
	val roleId = varchar("role_id", length = 32)
	val lastBumpAt = long("last_bump_at")
	val nextReminderAt = long("next_reminder_at")

	override val primaryKey = PrimaryKey(guildId)
}
