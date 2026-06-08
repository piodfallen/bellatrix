package bellatrix.database.repositories

import bellatrix.database.models.StarboardEntry
import bellatrix.database.tables.StarboardEntries
import dev.kord.common.entity.Snowflake
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update

object StarboardEntryRepository {
	fun findByOriginalMessageId(originalMessageId: Snowflake): StarboardEntry? =
		transaction {
			StarboardEntries
				.selectAll()
				.where { StarboardEntries.originalMessageId eq originalMessageId.toString() }
				.singleOrNull()
				?.toStarboardEntry()
		}

	fun create(
		originalMessageId: Snowflake,
		starboardMessageId: Snowflake,
		channelId: Snowflake,
		guildId: Snowflake,
		starCount: Int,
	): StarboardEntry =
		transaction {
			StarboardEntries.insert {
				it[StarboardEntries.originalMessageId] = originalMessageId.toString()
				it[StarboardEntries.starboardMessageId] = starboardMessageId.toString()
				it[StarboardEntries.channelId] = channelId.toString()
				it[StarboardEntries.guildId] = guildId.toString()
				it[StarboardEntries.starCount] = starCount
			}

			StarboardEntry(
				originalMessageId = originalMessageId,
				starboardMessageId = starboardMessageId,
				channelId = channelId,
				guildId = guildId,
				starCount = starCount,
			)
		}

	fun updateStarCount(
		originalMessageId: Snowflake,
		starCount: Int,
	): Unit =
		transaction {
			StarboardEntries.update({ StarboardEntries.originalMessageId eq originalMessageId.toString() }) {
				it[StarboardEntries.starCount] = starCount
			}
		}

	fun deleteByOriginalMessageId(originalMessageId: Snowflake): Unit =
		transaction {
			StarboardEntries.deleteWhere {
				StarboardEntries.originalMessageId eq originalMessageId.toString()
			}
		}

	private fun ResultRow.toStarboardEntry(): StarboardEntry =
		StarboardEntry(
			originalMessageId = Snowflake(this[StarboardEntries.originalMessageId]),
			starboardMessageId = Snowflake(this[StarboardEntries.starboardMessageId]),
			channelId = Snowflake(this[StarboardEntries.channelId]),
			guildId = Snowflake(this[StarboardEntries.guildId]),
			starCount = this[StarboardEntries.starCount],
		)
}
