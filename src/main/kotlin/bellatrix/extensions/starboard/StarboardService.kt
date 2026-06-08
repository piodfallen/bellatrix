package bellatrix.extensions.starboard

import bellatrix.common.discord.Channels
import bellatrix.common.discord.Colors
import bellatrix.common.discord.StarboardConfig
import bellatrix.database.models.StarboardEntry
import bellatrix.database.repositories.StarboardEntryRepository
import bellatrix.i18n.GuildLocaleResolver
import bellatrix.i18n.Translations
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.MessageBehavior
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.entity.Attachment
import dev.kord.core.entity.Message
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.entity.User
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.MessageBuilder
import dev.kord.rest.builder.message.actionRow
import dev.kord.rest.builder.message.embed
import dev.kordex.core.utils.getJumpUrl
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Locale
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class StarboardService(
	private val repository: StarboardEntryRepository = StarboardEntryRepository,
) {
	private val locks = mutableMapOf<Snowflake, Mutex>()
	private val locksMutex = Mutex()

	suspend fun handleReaction(
		kord: Kord,
		guildId: Snowflake?,
		channelId: Snowflake,
		messageId: Snowflake,
		emoji: ReactionEmoji,
	) {
		if (guildId == null || emoji != STAR_EMOJI) return

		val starboardChannelId = Channels.starboard ?: return
		if (channelId == starboardChannelId) return

		lockFor(messageId).withLock {
			val message = MessageBehavior(channelId, messageId, kord).fetchMessageOrNull()

			if (message == null) {
				removeEntryUnlocked(kord, messageId)
				return@withLock
			}

			if (message.author?.id == kord.selfId) return@withLock

			val starCount = message.getReactors(STAR_EMOJI).count { !it.isBot }
			val entry = repository.findByOriginalMessageId(messageId)

			if (starCount < StarboardConfig.minimumStars) {
				if (entry != null) removeEntryUnlocked(kord, messageId, entry)
				return@withLock
			}

			if (entry == null) {
				createEntry(message, guildId, starboardChannelId, starCount)
			} else {
				updateEntry(kord, message, entry, starCount)
			}
		}
	}

	suspend fun removeEntry(
		kord: Kord,
		originalMessageId: Snowflake,
	) {
		lockFor(originalMessageId).withLock {
			removeEntryUnlocked(kord, originalMessageId)
		}
	}

	suspend fun refreshEntry(
		kord: Kord,
		channelId: Snowflake,
		originalMessageId: Snowflake,
	) {
		lockFor(originalMessageId).withLock {
			val entry = repository.findByOriginalMessageId(originalMessageId) ?: return@withLock
			val message = MessageBehavior(channelId, originalMessageId, kord).fetchMessageOrNull()

			if (message == null || message.author?.id == kord.selfId) {
				removeEntryUnlocked(kord, originalMessageId, entry)
				return@withLock
			}

			val starCount = message.getReactors(STAR_EMOJI).count { !it.isBot }

			if (starCount < StarboardConfig.minimumStars) {
				removeEntryUnlocked(kord, originalMessageId, entry)
			} else {
				updateEntry(kord, message, entry, starCount)
			}
		}
	}

	private suspend fun removeEntryUnlocked(
		kord: Kord,
		originalMessageId: Snowflake,
	) {
		val entry = repository.findByOriginalMessageId(originalMessageId) ?: return
		removeEntryUnlocked(kord, originalMessageId, entry)
	}

	private suspend fun createEntry(
		message: Message,
		guildId: Snowflake,
		starboardChannelId: Snowflake,
		starCount: Int,
	) {
		val locale = GuildLocaleResolver.resolve(message.kord.getGuildOrNull(guildId))

		val starboardMessage = MessageChannelBehavior(starboardChannelId, message.kord).createMessage {
			applyStarboardMessage(message, starCount, locale)
		}

		repository.create(
			originalMessageId = message.id,
			starboardMessageId = starboardMessage.id,
			channelId = message.channelId,
			guildId = guildId,
			starCount = starCount,
		)
	}

	private suspend fun updateEntry(
		kord: Kord,
		message: Message,
		entry: StarboardEntry,
		starCount: Int,
	) {
		val locale = GuildLocaleResolver.resolve(kord.getGuildOrNull(entry.guildId))

		val updated = runCatching {
			MessageBehavior(requireNotNull(Channels.starboard), entry.starboardMessageId, kord).edit {
				applyStarboardMessage(message, starCount, locale)
			}
		}.isSuccess

		if (updated) {
			repository.updateStarCount(message.id, starCount)
		} else {
			repository.deleteByOriginalMessageId(message.id)
			createEntry(message, entry.guildId, requireNotNull(Channels.starboard), starCount)
		}
	}

	private suspend fun removeEntryUnlocked(
		kord: Kord,
		originalMessageId: Snowflake,
		entry: StarboardEntry,
	) {
		runCatching {
			MessageBehavior(requireNotNull(Channels.starboard), entry.starboardMessageId, kord).delete()
		}

		repository.deleteByOriginalMessageId(originalMessageId)
	}

	private suspend fun lockFor(messageId: Snowflake): Mutex =
		locksMutex.withLock {
			locks.getOrPut(messageId) { Mutex() }
		}

	private fun EmbedBuilder.applyStarboardEmbed(
		message: Message,
		starCount: Int,
		locale: Locale,
	) {
		val messageAuthor = message.author

		author {
			name = messageAuthor?.tag ?: Translations.Starboard.unknownUser
				.withLocale(locale)
				.translate()
			icon = messageAuthor?.avatarUrl()
		}

		description = message.starboardDescription()
		image = message.attachments.firstOrNull(Attachment::isImage)?.url
		timestamp = message.timestamp
		color = Colors.warning

		field(
			Translations.Starboard.Field.stars
				.withLocale(locale)
				.translate(),
			inline = true,
		) { "$STAR $starCount" }

		field(
			Translations.Starboard.Field.channel
				.withLocale(locale)
				.translate(),
			inline = true,
		) {
			MessageChannelBehavior(message.channelId, message.kord).mention
		}
	}

	private fun MessageBuilder.applyStarboardMessage(
		message: Message,
		starCount: Int,
		locale: Locale,
	) {
		content = null
		embeds = mutableListOf()
		components = mutableListOf()

		embed {
			applyStarboardEmbed(message, starCount, locale)
		}

		actionRow {
			linkButton(message.getJumpUrl()) {
				label = Translations.Starboard.Button.jump
					.withLocale(locale)
					.translate()
			}
		}
	}

	private fun Message.starboardDescription(): String? {
		val text = content.trim().takeIf(String::isNotBlank)

		val attachments = attachments
			.filterNot(Attachment::isImage)
			.take(MAX_ATTACHMENT_LINKS)
			.joinToString(separator = "\n") { attachment ->
				"[${attachment.filename}](${attachment.url})"
			}
			.takeIf(String::isNotBlank)

		return listOfNotNull(text, attachments)
			.joinToString(separator = "\n\n")
			.take(MAX_DESCRIPTION_LENGTH)
			.takeIf(String::isNotBlank)
	}

	private fun User.avatarUrl(): String =
		(avatar ?: defaultAvatar).cdnUrl.toUrl()

	private companion object {
		const val STAR = "⭐"
		val STAR_EMOJI = ReactionEmoji.Unicode(STAR)
		const val MAX_ATTACHMENT_LINKS = 5
		const val MAX_DESCRIPTION_LENGTH = 4000
	}
}
