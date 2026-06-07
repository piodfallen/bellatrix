package bellatrix.database.models

import dev.kord.common.entity.Snowflake
import java.time.Instant

data class ModmailThread(
	val userId: Snowflake,
	val threadId: Snowflake,
	val parentMessageId: Snowflake?,
	val lastSender: ModmailMessageSender?,
	val assignedStaffId: Snowflake?,
	val createdAt: Instant,
	val lastMessageAt: Instant,
	val closed: Boolean,
)
