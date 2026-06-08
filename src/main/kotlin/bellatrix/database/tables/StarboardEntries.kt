package bellatrix.database.tables

import org.jetbrains.exposed.v1.core.Table

object StarboardEntries : Table("starboard_entries") {
	val originalMessageId = varchar("original_message_id", length = 32)
	val starboardMessageId = varchar("starboard_message_id", length = 32)
	val channelId = varchar("channel_id", length = 32)
	val guildId = varchar("guild_id", length = 32)
	val starCount = integer("star_count")

	override val primaryKey = PrimaryKey(originalMessageId)
}
