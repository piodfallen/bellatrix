package bellatrix.database.models

import dev.kord.common.entity.Snowflake

data class StarboardEntry(
	val originalMessageId: Snowflake,
	val starboardMessageId: Snowflake,
	val channelId: Snowflake,
	val guildId: Snowflake,
	val starCount: Int,
)
