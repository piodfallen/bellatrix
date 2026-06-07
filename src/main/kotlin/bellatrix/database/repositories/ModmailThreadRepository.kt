package bellatrix.database.repositories

import bellatrix.database.models.ModmailMessageSender
import bellatrix.database.models.ModmailThread
import bellatrix.database.tables.ModmailThreads
import dev.kord.common.entity.Snowflake
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant

object ModmailThreadRepository {
	fun findOpenByUserId(userId: Snowflake): ModmailThread? =
		transaction {
			ModmailThreads
				.selectAll()
				.where {
					ModmailThreads.userId eq userId.toString() and
						(ModmailThreads.closed eq false)
				}
				.singleOrNull()
				?.toModmailThread()
		}

	fun findOpenByThreadId(threadId: Snowflake): ModmailThread? =
		transaction {
			ModmailThreads
				.selectAll()
				.where {
					ModmailThreads.threadId eq threadId.toString() and
						(ModmailThreads.closed eq false)
				}
				.singleOrNull()
				?.toModmailThread()
		}

	fun create(
		userId: Snowflake,
		threadId: Snowflake,
		parentMessageId: Snowflake,
		now: Instant,
	): ModmailThread =
		transaction {
			ModmailThreads.insert {
				it[ModmailThreads.userId] = userId.toString()
				it[ModmailThreads.threadId] = threadId.toString()
				it[ModmailThreads.parentMessageId] = parentMessageId.toString()
				it[lastSender] = null
				it[assignedStaffId] = null
				it[createdAt] = now.toEpochMilli()
				it[lastMessageAt] = now.toEpochMilli()
				it[closed] = false
			}

			ModmailThread(
				userId = userId,
				threadId = threadId,
				parentMessageId = parentMessageId,
				lastSender = null,
				assignedStaffId = null,
				createdAt = now,
				lastMessageAt = now,
				closed = false,
			)
		}

	fun touch(
		threadId: Snowflake,
		now: Instant,
		sender: ModmailMessageSender,
	): Unit =
		transaction {
			ModmailThreads.update({
				ModmailThreads.threadId eq threadId.toString() and
					(ModmailThreads.closed eq false)
			}) {
				it[lastMessageAt] = now.toEpochMilli()
				it[lastSender] = sender.name
			}
		}

	fun assignIfUnassigned(threadId: Snowflake, staffId: Snowflake): Boolean =
		transaction {
			ModmailThreads.update({
				ModmailThreads.threadId eq threadId.toString() and
					ModmailThreads.assignedStaffId.isNull() and
					(ModmailThreads.closed eq false)
			}) {
				it[assignedStaffId] = staffId.toString()
			} == 1
		}

	fun close(threadId: Snowflake): ModmailThread? =
		transaction {
			val thread = ModmailThreads
				.selectAll()
				.where {
					ModmailThreads.threadId eq threadId.toString() and
						(ModmailThreads.closed eq false)
				}
				.singleOrNull()
				?.toModmailThread()

			if (thread != null) {
				ModmailThreads.update({ ModmailThreads.threadId eq threadId.toString() }) {
					it[closed] = true
				}
			}

			thread
		}

	fun findInactiveOpen(cutoff: Instant): List<ModmailThread> =
		transaction {
			ModmailThreads
				.selectAll()
				.where {
					ModmailThreads.closed eq false and
						(ModmailThreads.lastMessageAt less cutoff.toEpochMilli())
				}
				.map { it.toModmailThread() }
		}

	private fun ResultRow.toModmailThread(): ModmailThread =
		ModmailThread(
			userId = Snowflake(this[ModmailThreads.userId]),
			threadId = Snowflake(this[ModmailThreads.threadId]),
			parentMessageId = this[ModmailThreads.parentMessageId]?.let(::Snowflake),
			lastSender = this[ModmailThreads.lastSender]?.let(ModmailMessageSender::valueOf),
			assignedStaffId = this[ModmailThreads.assignedStaffId]?.let(::Snowflake),
			createdAt = Instant.ofEpochMilli(this[ModmailThreads.createdAt]),
			lastMessageAt = Instant.ofEpochMilli(this[ModmailThreads.lastMessageAt]),
			closed = this[ModmailThreads.closed],
		)
}
