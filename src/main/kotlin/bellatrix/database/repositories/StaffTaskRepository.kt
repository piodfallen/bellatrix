package bellatrix.database.repositories

import bellatrix.database.models.StaffTask
import bellatrix.database.models.StaffTaskStatus
import bellatrix.database.tables.StaffTasks
import dev.kord.common.entity.Snowflake
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant

object StaffTaskRepository {
	fun create(
		guildId: Snowflake,
		channelId: Snowflake,
		createdBy: Snowflake,
		content: String,
		now: Instant,
	): StaffTask =
		transaction {
			val taskId = StaffTasks.insert {
				it[StaffTasks.guildId] = guildId.toString()
				it[StaffTasks.channelId] = channelId.toString()
				it[StaffTasks.messageId] = null
				it[StaffTasks.threadId] = null
				it[StaffTasks.createdBy] = createdBy.toString()
				it[assignedTo] = null
				it[StaffTasks.content] = content
				it[status] = StaffTaskStatus.Open.name
				it[createdAt] = now.toEpochMilli()
				it[updatedAt] = now.toEpochMilli()
				it[resolvedAt] = null
				it[resolvedBy] = null
			}[StaffTasks.taskId]

			requireNotNull(findByIdQuery(taskId))
		}

	fun attachDiscordResources(
		taskId: Long,
		messageId: Snowflake,
		threadId: Snowflake,
		now: Instant,
	): StaffTask? =
		transaction {
			StaffTasks.update({ StaffTasks.taskId eq taskId }) {
				it[StaffTasks.messageId] = messageId.toString()
				it[StaffTasks.threadId] = threadId.toString()
				it[updatedAt] = now.toEpochMilli()
			}

			findByIdQuery(taskId)
		}

	fun findById(taskId: Long): StaffTask? =
		transaction {
			findByIdQuery(taskId)
		}

	fun assignIfAvailable(
		taskId: Long,
		staffId: Snowflake,
		now: Instant,
	): StaffTask? =
		transaction {
			val updated = StaffTasks.update({
				StaffTasks.taskId eq taskId and
					StaffTasks.assignedTo.isNull() and
					(StaffTasks.status eq StaffTaskStatus.Open.name)
			}) {
				it[assignedTo] = staffId.toString()
				it[status] = StaffTaskStatus.InProgress.name
				it[updatedAt] = now.toEpochMilli()
			}

			if (updated == 1) findByIdQuery(taskId) else null
		}

	fun updateStatus(
		taskId: Long,
		status: StaffTaskStatus,
		staffId: Snowflake,
		now: Instant,
	): StaffTask? =
		transaction {
			StaffTasks.update({ StaffTasks.taskId eq taskId }) {
				it[StaffTasks.status] = status.name
				it[updatedAt] = now.toEpochMilli()
				it[resolvedAt] = if (status == StaffTaskStatus.Resolved) now.toEpochMilli() else null
				it[resolvedBy] = if (status == StaffTaskStatus.Resolved) staffId.toString() else null
				if (status == StaffTaskStatus.Open) {
					it[assignedTo] = null
				}
			}

			findByIdQuery(taskId)
		}

	private fun findByIdQuery(taskId: Long): StaffTask? =
		StaffTasks
			.selectAll()
			.where { StaffTasks.taskId eq taskId }
			.singleOrNull()
			?.toStaffTask()

	private fun ResultRow.toStaffTask(): StaffTask =
		StaffTask(
			taskId = this[StaffTasks.taskId],
			guildId = Snowflake(this[StaffTasks.guildId]),
			channelId = Snowflake(this[StaffTasks.channelId]),
			messageId = this[StaffTasks.messageId]?.let(::Snowflake),
			threadId = this[StaffTasks.threadId]?.let(::Snowflake),
			createdBy = Snowflake(this[StaffTasks.createdBy]),
			assignedTo = this[StaffTasks.assignedTo]?.let(::Snowflake),
			content = this[StaffTasks.content],
			status = StaffTaskStatus.valueOf(this[StaffTasks.status]),
			createdAt = Instant.ofEpochMilli(this[StaffTasks.createdAt]),
			updatedAt = Instant.ofEpochMilli(this[StaffTasks.updatedAt]),
			resolvedAt = this[StaffTasks.resolvedAt]?.let(Instant::ofEpochMilli),
			resolvedBy = this[StaffTasks.resolvedBy]?.let(::Snowflake),
		)
}
