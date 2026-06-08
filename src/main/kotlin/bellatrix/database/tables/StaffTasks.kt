package bellatrix.database.tables

import org.jetbrains.exposed.v1.core.Table

object StaffTasks : Table("staff_tasks") {
	val taskId = long("task_id").autoIncrement()
	val guildId = varchar("guild_id", length = 32)
	val channelId = varchar("channel_id", length = 32)
	val messageId = varchar("message_id", length = 32).nullable()
	val threadId = varchar("thread_id", length = 32).nullable()
	val createdBy = varchar("created_by", length = 32)
	val assignedTo = varchar("assigned_to", length = 32).nullable()
	val content = text("content")
	val status = varchar("status", length = 16)
	val createdAt = long("created_at")
	val updatedAt = long("updated_at")
	val resolvedAt = long("resolved_at").nullable()
	val resolvedBy = varchar("resolved_by", length = 32).nullable()

	override val primaryKey = PrimaryKey(taskId)
}
