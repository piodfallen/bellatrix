package bellatrix.database.models

import dev.kord.common.entity.Snowflake
import java.time.Instant

data class StaffTask(
	val taskId: Long,
	val guildId: Snowflake,
	val channelId: Snowflake,
	val messageId: Snowflake?,
	val threadId: Snowflake?,
	val createdBy: Snowflake,
	val assignedTo: Snowflake?,
	val content: String,
	val status: StaffTaskStatus,
	val createdAt: Instant,
	val updatedAt: Instant,
	val resolvedAt: Instant?,
	val resolvedBy: Snowflake?,
)
