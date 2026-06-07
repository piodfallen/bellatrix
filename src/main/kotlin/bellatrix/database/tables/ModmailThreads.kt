package bellatrix.database.tables

import org.jetbrains.exposed.v1.core.Table

object ModmailThreads : Table("modmail_threads") {
	val userId = varchar("user_id", length = 32)
	val threadId = varchar("thread_id", length = 32)
	val parentMessageId = varchar("parent_message_id", length = 32).nullable()
	val lastSender = varchar("last_sender", length = 16).nullable()
	val assignedStaffId = varchar("assigned_staff_id", length = 32).nullable()
	val createdAt = long("created_at")
	val lastMessageAt = long("last_message_at")
	val closed = bool("closed").default(false)

	override val primaryKey = PrimaryKey(threadId)
}
