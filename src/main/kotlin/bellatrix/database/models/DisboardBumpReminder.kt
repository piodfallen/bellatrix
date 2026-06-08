package bellatrix.database.models

import dev.kord.common.entity.Snowflake
import java.time.Instant

data class DisboardBumpReminder(
	val guildId: Snowflake,
	val channelId: Snowflake,
	val roleId: Snowflake,
	val lastBumpAt: Instant,
	val nextReminderAt: Instant,
)
